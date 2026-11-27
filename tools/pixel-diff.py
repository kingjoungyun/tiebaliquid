#!/usr/bin/env python3
"""截图基线对比（带 ROI：只比该比的地方）。

用法（从工作区根执行）：

    python tools/pixel-diff.py --baseline tools/baseline --current shots/current
    python tools/pixel-diff.py --baseline tools/baseline --current shots/current --full
    python tools/pixel-diff.py --baseline tools/baseline --current shots/current --threshold 8

## 为什么要 ROI

信息流（首页 / 帖子页）的内容每次都不一样，全屏 diff 噪声极大，真正要看的是
**固定不变的部分**：状态栏 + 顶栏（今天出过白条的地方）、底栏 dock / 帖子页底栏双胶囊。

| 页面 | 比对区域 |
| --- | --- |
| `home` | `top_chrome` + `dock` |
| `thread` | `top_chrome` + `bottom_bar` |
| `notifications` | `top_chrome` |
| `settings` / `mine` / `search` | `full`（纯色页，整屏都比） |

`--full` 强制全屏比对（调试用）。

## 判定

逐像素算每个通道的**平均绝对差**（0~255），区域内三通道再取平均；
任一区域超阈值即判该图 DIFF。超阈值的图会写差异图到 `<current>/__diff/`
（一致像素压暗、差异像素标红、非 ROI 区域直接压暗），并打印差异最大的 3 个网格。

退出码：0 = 全绿；1 = 有差异；2 = 用法/文件问题。
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image, ImageChops
except ImportError:  # pragma: no cover
    print('需要 Pillow：pip install pillow', file=sys.stderr)
    raise SystemExit(2)

GRID = 8

# 区域名 → 取矩形的方式（按图高百分比 / 固定像素，两种都支持）
REGION_SPEC = {
    # 状态栏 + 顶栏（约前 200px，按比例兜底）
    'top_chrome': ('top', 200, 0.085),
    # 底部 dock 条
    'dock': ('bottom', 180, 0.075),
    # 帖子页底部工具栏（双胶囊更高一些）
    'bottom_bar': ('bottom', 220, 0.095),
    # 整屏
    'full': ('full', 0, 0.0),
}

# 每页只比「固定不变」的部分
ROI_BY_PAGE = {
    'home': ['top_chrome', 'dock'],
    'thread': ['top_chrome', 'bottom_bar'],
    'notifications': ['top_chrome'],
    'settings': ['full'],
    'mine': ['full'],
    'search': ['full'],
}


def page_name(stem: str) -> str:
    """`home_dark` → `home`；未知页面回退 full。"""
    for suffix in ('_light', '_dark'):
        if stem.endswith(suffix):
            return stem[:-len(suffix)]
    return stem


def region_boxes(width: int, height: int, names: list[str]):
    """把区域名解析成 (name, box) 列表。"""
    out = []
    for name in names:
        kind, fixed, ratio = REGION_SPEC[name]
        size = max(fixed, int(height * ratio))
        if kind == 'top':
            out.append((name, (0, 0, width, min(height, size))))
        elif kind == 'bottom':
            out.append((name, (0, max(0, height - size), width, height)))
        else:
            out.append((name, (0, 0, width, height)))
    return out


def mean_abs_diff(a: Image.Image, b: Image.Image) -> float:
    """三通道平均绝对差（0~255）；尺寸不同返回 255。"""
    if a.size != b.size:
        return 255.0
    diff = ImageChops.difference(a.convert('RGB'), b.convert('RGB'))
    hist = diff.histogram()
    total = a.size[0] * a.size[1]
    acc = 0
    for channel in range(3):
        buckets = hist[channel * 256:(channel + 1) * 256]
        acc += sum(i * n for i, n in enumerate(buckets))
    return acc / (total * 3)


def write_diff_image(a: Image.Image, b: Image.Image, boxes, out_path: Path) -> None:
    """差异图：ROI 内容一致的像素压暗，差异像素标红，非 ROI 区域整体压暗。"""
    base = a.convert('RGB')
    other = b.convert('RGB')
    if base.size != other.size:
        base.save(out_path)
        return
    diff = ImageChops.difference(base, other).convert('L')
    src = base.load()
    dpx = diff.load()
    out = base.copy()
    out_px = out.load()
    w, h = out.size
    for x in range(w):
        for y in range(h):
            r, g, bl = src[x, y]
            out_px[x, y] = (r // 3, g // 3, bl // 3)
    for _, box in boxes:
        x0, y0, x1, y1 = box
        for y in range(y0, y1):
            for x in range(x0, x1):
                r, g, bl = src[x, y]
                if dpx[x, y] > 24:
                    out_px[x, y] = (255, 0, 0)
                else:
                    out_px[x, y] = (r, g, bl)
    out.save(out_path)


def top_regions_in_box(a: Image.Image, b: Image.Image, box, top_n: int = 3):
    x0, y0, x1, y1 = box
    diff = ImageChops.difference(a.convert('RGB'), b.convert('RGB')).convert('L').crop((x0, y0, x1, y1))
    w, h = diff.size
    cw, ch = max(1, w // GRID), max(1, h // GRID)
    regions = []
    for gy in range(GRID):
        for gx in range(GRID):
            tile = diff.crop((gx * cw, gy * ch, min(w, (gx + 1) * cw), min(h, (gy + 1) * ch)))
            hist = tile.histogram()
            total = max(1, tile.size[0] * tile.size[1])
            average = sum(i * n for i, n in enumerate(hist)) / total
            regions.append((average, gx / GRID, gy / GRID))
    regions.sort(reverse=True)
    return regions[:top_n]


def main() -> int:
    ap = argparse.ArgumentParser(description='截图基线对比（ROI）')
    ap.add_argument('--baseline', required=True, type=Path)
    ap.add_argument('--current', required=True, type=Path)
    ap.add_argument('--threshold', type=float, default=8.0,
                    help='每通道平均差阈值（0~255），默认 8')
    ap.add_argument('--diff-dir', type=Path, default=None)
    ap.add_argument('--full', action='store_true', help='忽略 ROI，整屏比对')
    args = ap.parse_args()

    if not args.baseline.is_dir():
        print(f'基线目录不存在：{args.baseline}', file=sys.stderr)
        return 2
    if not args.current.is_dir():
        print(f'当前截图目录不存在：{args.current}', file=sys.stderr)
        return 2

    diff_dir = args.diff_dir or (args.current / '__diff')
    diff_dir.mkdir(parents=True, exist_ok=True)

    baselines = sorted(args.baseline.glob('*.png'))
    if not baselines:
        print(f'基线目录里没有 png：{args.baseline}', file=sys.stderr)
        return 2

    failures, missing = [], []
    print(f'阈值：每通道平均差 ≤ {args.threshold}   模式：{"FULL" if args.full else "ROI"}')
    print(f'{"文件":28s} {"区域":12s} {"平均差":>8s}')
    for base_path in baselines:
        cur_path = args.current / base_path.name
        if not cur_path.exists():
            missing.append(base_path.name)
            print(f'{base_path.name:28s} {"-":12s} {"MISSING":>8s}')
            continue
        with Image.open(base_path) as bi, Image.open(cur_path) as ci:
            a, b = bi.copy(), ci.copy()

        page = page_name(base_path.stem)
        names = ['full'] if args.full else ROI_BY_PAGE.get(page, ['full'])
        boxes = region_boxes(b.size[0], b.size[1], names)

        worst = 0.0
        for name, box in boxes:
            value = mean_abs_diff(a.crop(box), b.crop(box))
            worst = max(worst, value)
            print(f'{base_path.name:28s} {name:12s} {value:8.2f}')
            if value > args.threshold:
                write_diff_image(a, b, boxes, diff_dir / base_path.name)
                for avg, rx, ry in top_regions_in_box(a, b, box):
                    print(f'    区域内最大差异：x≈{rx:.2f} y≈{ry:.2f} 平均差 {avg:.1f}')
        if worst > args.threshold:
            failures.append(base_path.name)

    print()
    if missing:
        print(f'缺少 {len(missing)} 张：{", ".join(missing)}')
    if failures:
        print(f'[FAIL] {len(failures)} 张超阈值，差异图在：{diff_dir}')
        print('   ' + ', '.join(failures))
        return 1
    print('[OK] 全部在阈值内')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
