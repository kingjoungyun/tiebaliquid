#!/usr/bin/env python3
"""截图基线对比（最小可用版）。

用法（从工作区根执行，或在仓库根执行也可以，路径自己给）：

    python tools/pixel-diff.py --baseline tools/baseline --current shots/current
    python tools/pixel-diff.py --baseline tools/baseline --current shots/current --threshold 8

行为：
- 对 `baseline/*.png` 里每张图，找 `current/` 里同名文件；
- 逐像素算每个通道的**平均绝对差**（0~255），三通道再取平均；
- 超过 `--threshold`（默认 8）判为「有差异」，打印明细；
- 同时把差异写成语义化的「差异图」到 `--diff-dir`（默认 `<current>/__diff`），
  差异图里：一致的像素压暗，差异像素标红，方便肉眼定位；
- 报告差异最大的区域（按 8×8 网格统计，给出前 3 个）。

退出码：0 = 全部在阈值内；1 = 有差异；2 = 用法/文件问题。
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

GRID = 8  # 差异最大区域用 8x8 网格


def mean_channel_diff(a: Image.Image, b: Image.Image) -> float:
    """三通道平均绝对差（0~255）。尺寸不同直接返回 255。"""
    if a.size != b.size:
        return 255.0
    diff = ImageChops.difference(a.convert('RGB'), b.convert('RGB'))
    hist = diff.histogram()  # R,G,B 各 256 桶
    total = a.size[0] * a.size[1]
    acc = 0
    for channel in range(3):
        buckets = hist[channel * 256:(channel + 1) * 256]
        acc += sum(i * n for i, n in enumerate(buckets))
    return acc / (total * 3)


def diff_image(a: Image.Image, b: Image.Image, out_path: Path) -> None:
    """差异图：底色压暗的原图 + 红色高亮差异像素。"""
    base = a.convert('RGB')
    other = b.convert('RGB')
    if base.size != other.size:
        base.save(out_path)
        return
    diff = ImageChops.difference(base, other).convert('L')
    pix = diff.load()
    out = base.copy()
    out_pix = out.load()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            d = pix[x, y]
            if d > 24:
                out_pix[x, y] = (255, 0, 0)
            else:
                r, g, bl = out_pix[x, y]
                out_pix[x, y] = (r // 3, g // 3, bl // 3)
    out.save(out_path)


def top_regions(a: Image.Image, b: Image.Image, top_n: int = 3):
    """按 GRID×GRID 网格找平均差异最大的区域（返回 0~1 的归一化坐标 + 差值）。"""
    if a.size != b.size:
        return []
    diff = ImageChops.difference(a.convert('RGB'), b.convert('RGB')).convert('L')
    w, h = diff.size
    cw, ch = max(1, w // GRID), max(1, h // GRID)
    regions = []
    for gy in range(GRID):
        for gx in range(GRID):
            box = (gx * cw, gy * ch, min(w, (gx + 1) * cw), min(h, (gy + 1) * ch))
            tile = diff.crop(box)
            hist = tile.histogram()
            total = max(1, tile.size[0] * tile.size[1])
            average = sum(i * n for i, n in enumerate(hist)) / total
            regions.append((average, gx / GRID, gy / GRID))
    regions.sort(reverse=True)
    return regions[:top_n]


def main() -> int:
    ap = argparse.ArgumentParser(description='截图基线对比')
    ap.add_argument('--baseline', required=True, type=Path)
    ap.add_argument('--current', required=True, type=Path)
    ap.add_argument('--threshold', type=float, default=8.0,
                    help='每通道平均差阈值（0~255），默认 8')
    ap.add_argument('--diff-dir', type=Path, default=None,
                    help='差异图输出目录，默认 <current>/__diff')
    args = ap.parse_args()

    if not args.baseline.is_dir():
        print(f'基线目录不存在：{args.baseline}', file=sys.stderr)
        return 2
    if not args.current.is_dir():
        print(f'当前截图目录不存在：{args.current}', file=sys.stderr)
        return 2

    diff_dir = args.diff_dir or (args.current / '__diff')
    diff_dir.mkdir(parents=True, exist_ok=True)

    baselines = sorted(p for p in args.baseline.glob('*.png'))
    if not baselines:
        print(f'基线目录里没有 png：{args.baseline}', file=sys.stderr)
        return 2

    failures = []
    missing = []
    print(f'阈值：每通道平均差 ≤ {args.threshold}')
    print(f'{"文件":28s} {"平均差":>8s}  说明')
    for base_path in baselines:
        cur_path = args.current / base_path.name
        if not cur_path.exists():
            missing.append(base_path.name)
            print(f'{base_path.name:28s} {"MISSING":>8s}  当前目录没有这张')
            continue
        with Image.open(base_path) as bi, Image.open(cur_path) as ci:
            a = bi.copy()
            b = ci.copy()
        value = mean_channel_diff(a, b)
        status = 'OK' if value <= args.threshold else 'DIFF'
        print(f'{base_path.name:28s} {value:8.2f}  {status}')
        if value > args.threshold:
            failures.append(base_path.name)
            diff_image(a, b, diff_dir / base_path.name)
            for avg, rx, ry in top_regions(a, b):
                print(f'    最大差异区域：x≈{rx:.2f} y≈{ry:.2f} 平均差 {avg:.1f}')

    print()
    if missing:
        print(f'缺少 {len(missing)} 张：{", ".join(missing)}')
    if failures:
        print(f'[FAIL] {len(failures)} 张超阈值，差异图在：{diff_dir}')
        print(f'   {", ".join(failures)}')
        return 1
    print('[OK] 全部在阈值内')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
