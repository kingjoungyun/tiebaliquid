"""测量资料卡里两处「墨迹间隙」：名字→ID 行、ID 行→数据行。

做法（按 lead 的要求，不猜坐标）：
1. 从 `uiautomator dump` 的 XML 里取三处文本节点的 bounds（名字 / ID 行 / 数据行的「关注」标签）；
2. 在每个节点的 bounds（左右各内缩 2px）内逐行扫描像素，找出「第一行有墨迹」和「最后一行有墨迹」；
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


def ink_rows(image: Image.Image, box, threshold: int = 60):
    """返回 (首墨迹行, 末墨迹行)；用「与卡片主色差 > threshold」判定墨迹。"""
    left, top, right, bottom = box
    left += 2
    right -= 2
    px = image.load()
    # 采样卡片主色（用右上角一小块，避开文字）
    bg_samples = [px[right - 4, top + 2], px[left + 2, bottom - 2]]
    bg = max(set(bg_samples), key=bg_samples.count)
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

    missing = [k for k in ('name', 'meta', 'stat') if k not in nodes]
    if missing:
        print(f'缺少节点：{missing}', file=sys.stderr)
        return 2

    image = Image.open(args.shot).convert('RGB')
    ink = {}
    for key, box in nodes.items():
        found = ink_rows(image, box)
        if not found:
            print(f'{key} 的 bounds 内没找到墨迹', file=sys.stderr)
            return 2
        ink[key] = found

    gap1 = ink['meta'][0] - ink['name'][1] - 1
    gap2 = ink['stat'][0] - ink['meta'][1] - 1
    print(f'节点 bounds      : name={nodes["name"]} meta={nodes["meta"]} stat={nodes["stat"]}')
    print(f'墨迹行（首,末）   : name={ink["name"]} meta={ink["meta"]} stat={ink["stat"]}')
    print(f'name→id 墨迹间隙  : {gap1} px')
    print(f'id→stat 墨迹间隙  : {gap2} px')
    print(f'差值             : {abs(gap1 - gap2)} px')
    ok = abs(gap1 - gap2) <= 2
    print('[OK] 两处视觉间隙一致（差 ≤ 2px）' if ok else '[FAIL] 两处视觉间隙差 > 2px')
    return 0 if ok else 1


if __name__ == '__main__':
    raise SystemExit(main())
