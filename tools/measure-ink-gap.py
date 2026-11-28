"""测量资料卡里两处「墨迹间隙」：名字→ID 行、ID 行→数据行。

做法（按 lead 的要求，不猜坐标）：
1. 从 `uiautomator dump` 的 XML 里取三处文本节点的 bounds（名字 / ID 行 / 数据行的「关注」标签）；
2. 在每个节点的 bounds（左右各内缩 2px、上下各外扩 `--pad`）内逐行扫描像素，
   找出「第一行有墨迹」和「最后一行有墨迹」；背景色取窗口内出现次数最多的颜色；
3. 间隙 = 下一块的首墨迹行 − 上一块的末墨迹行 − 1；
4. 断言两者差 ≤ 2px。

用法：python tools/measure-ink-gap.py --xml shots/_u.xml --shot shots/profile_preview_dark2.png
"""
from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print('需要 Pillow', file=sys.stderr)
    raise SystemExit(2)


def parse_bounds(text: str):
    m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', text or '')
    if not m:
        return None
    return tuple(int(v) for v in m.groups())  # left, top, right, bottom


def ink_rows(image: Image.Image, box, threshold: int = 60, pad: int = 2):
    """返回 (首墨迹行, 末墨迹行)；用「与卡片主色差 > threshold」判定墨迹。

    pad：把扫描窗口上下左右各外扩几个像素。墨迹盒紧贴字形时（见
    UserProfileCard 的 InkLine），墨迹会顶到节点 bounds 的边上，外扩 2px 才不会切掉首末行。

    背景色不再取「两个角」——那是老版本的取巧做法，墨迹贴边时角上就是字形本身。
    改成取整个窗口里出现次数最多的颜色（卡片底色，字形只占少数像素）。
    """
    left, top, right, bottom = box
    left += 2 - pad
    right -= 2 - pad
    top -= pad
    bottom += pad
    px = image.load()
    hist = {}
    for y in range(top, bottom, 2):
        for x in range(left, right, 2):
            c = px[x, y]
            hist[c] = hist.get(c, 0) + 1
    bg = max(hist, key=hist.get)
    rows = []
    for y in range(top, bottom):
        count = 0
        for x in range(left, right, 2):
            c = px[x, y]
            if abs(c[0] - bg[0]) + abs(c[1] - bg[1]) + abs(c[2] - bg[2]) > threshold:
                count += 1
        if count >= 2:
            rows.append(y)
    if not rows:
        return None
    return rows[0], rows[-1]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--xml', required=True, type=Path)
    ap.add_argument('--shot', required=True, type=Path)
    ap.add_argument('--name', default='特没谱先生')
    ap.add_argument('--meta-prefix', default='ID ')
    ap.add_argument('--stat', default='关注')
    ap.add_argument('--pad', type=int, default=2, help='扫描窗口外扩像素（默认 2）')
    args = ap.parse_args()

    root = ET.parse(args.xml).getroot()
    nodes = {}
    for node in root.iter('node'):
        text = node.get('text') or ''
        if text == args.name:
            nodes.setdefault('name', parse_bounds(node.get('bounds')))
        elif text.startswith(args.meta_prefix) and 'YEAR' in text:
            nodes.setdefault('meta', parse_bounds(node.get('bounds')))
        elif text == args.stat:
            box = parse_bounds(node.get('bounds'))
            # 「关注」出现两次：取靠上的那个（数据行），按钮里的在更下面
            if box and ('stat' not in nodes or box[1] < nodes['stat'][1]):
                nodes['stat'] = box

    for node in root.iter('node'):
        if (node.get('content-desc') or '') == '用户头像':
            nodes['avatar'] = parse_bounds(node.get('bounds'))
            break

    missing = [k for k in ('name', 'meta', 'stat', 'avatar') if k not in nodes]
    if missing:
        print(f'缺少节点：{missing}', file=sys.stderr)
        return 2

    image = Image.open(args.shot).convert('RGB')
    ink = {}
    # 头像只取 bounds（用来对齐），不做墨迹检测（它是照片，没有统一背景色）
    for key, box in nodes.items():
        if key == 'avatar':
            continue
        found = ink_rows(image, box, pad=args.pad)
        if not found:
            print(f'{key} 的 bounds 内没找到墨迹', file=sys.stderr)
            return 2
        ink[key] = found

    gap1 = ink['meta'][0] - ink['name'][1] - 1
    gap2 = ink['stat'][0] - ink['meta'][1] - 1
    avatar_top, avatar_bottom = nodes['avatar'][1], nodes['avatar'][3]
    name_top_delta = ink['name'][0] - avatar_top
    stat_bottom_delta = avatar_bottom - ink['stat'][1]
    print(f'节点 bounds      : name={nodes["name"]} meta={nodes["meta"]} stat={nodes["stat"]}')
    print(f'墨迹行（首,末）   : name={ink["name"]} meta={ink["meta"]} stat={ink["stat"]}  (窗口外扩 {args.pad}px)')
    print(f'name→id 墨迹间隙  : {gap1} px')
    print(f'id→stat 墨迹间隙  : {gap2} px')
    print(f'差值             : {abs(gap1 - gap2)} px')
    print(f'名字墨迹顶 - 头像顶 : {name_top_delta} px（要求 |Δ| ≤ 2）')
    print(f'头像底 - 数据墨迹底 : {stat_bottom_delta} px（要求 |Δ| ≤ 2）')
    ok_gap = abs(gap1 - gap2) <= 2
    ok_top = abs(name_top_delta) <= 2
    ok_bottom = abs(stat_bottom_delta) <= 2
    print(('[OK] ' if ok_gap else '[FAIL] ') + '两处视觉间隙差 ≤ 2px')
    print(('[OK] ' if ok_top else '[FAIL] ') + '名字顶与头像顶平齐（±2px）')
    print(('[OK] ' if ok_bottom else '[FAIL] ') + '数据底与头像底平齐（±2px）')
    return 0 if (ok_gap and ok_top and ok_bottom) else 1


if __name__ == '__main__':
    raise SystemExit(main())
