#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把 A/B 两张设置页截图拼成左右对照图（Pillow）。

用法：
  python scripts/compare-settings.py A路径 B路径 输出路径 [--labels "A 现有设置页" "B 纯 Glasmio"]

设计：
- 两张图等比缩放到同一高度（默认 1800px），纵向居中对齐；
- 顶部加 96px 标签带，左侧写 A / B，字体用系统微软雅黑（Pillow 不保证有中文字体）；
- 中间留 24px 分隔线，底部不加水印（避免遮盖内容）。
"""
from __future__ import annotations

import argparse
import os

from PIL import Image, ImageDraw, ImageFont

LABEL_HEIGHT = 96
GAP = 24
TARGET_HEIGHT = 1800
BG = (24, 24, 27)
LABEL_BG = (36, 36, 40)
LABEL_FG = (245, 245, 247)
ACCENT = (10, 132, 255)

FONT_CANDIDATES = [
    r"C:\Windows\Fonts\msyh.ttc",
    r"C:\Windows\Fonts\msyhbd.ttc",
    r"C:\Windows\Fonts\segoeui.ttf",
]


def load_font(size: int) -> ImageFont.FreeTypeFont:
    for path in FONT_CANDIDATES:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return ImageFont.load_default()


def fit(image: Image.Image, height: int) -> Image.Image:
    scale = height / image.height
    return image.resize((round(image.width * scale), height), Image.LANCZOS)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("a")
    ap.add_argument("b")
    ap.add_argument("out")
    ap.add_argument("--labels", nargs=2, default=["A · 现有设置页", "B · 纯 Glasmio"])
    ap.add_argument("--height", type=int, default=TARGET_HEIGHT)
    args = ap.parse_args()

    a = fit(Image.open(args.a).convert("RGB"), args.height)
    b = fit(Image.open(args.b).convert("RGB"), args.height)

    width = a.width + GAP + b.width
    canvas = Image.new("RGB", (width, args.height + LABEL_HEIGHT), BG)
    canvas.paste(a, (0, LABEL_HEIGHT))
    canvas.paste(b, (a.width + GAP, LABEL_HEIGHT))

    draw = ImageDraw.Draw(canvas)
    draw.rectangle([0, 0, width, LABEL_HEIGHT], fill=LABEL_BG)
    font = load_font(44)
    for x, label in ((0, args.labels[0]), (a.width + GAP, args.labels[1])):
        draw.rectangle([x, 0, x + a.width, LABEL_HEIGHT], fill=LABEL_BG)
        # 标签居中
        box = draw.textbbox((0, 0), label, font=font)
        draw.text((x + (a.width - (box[2] - box[0])) / 2 - box[0], (LABEL_HEIGHT - box[3]) / 2 - box[1]),
                  label, font=font, fill=LABEL_FG)
    # A/B 角标
    badge_font = load_font(40)
    for x, letter in ((0, "A"), (a.width + GAP, "B")):
        draw.rounded_rectangle([x + 20, 26, x + 68, 74], radius=12, fill=ACCENT)
        box = draw.textbbox((0, 0), letter, font=badge_font)
        draw.text((x + 44 - (box[2] - box[0]) / 2 - box[0], 50 - (box[3] - box[1]) / 2 - box[1]),
                  letter, font=badge_font, fill=(255, 255, 255))
    draw.line([a.width + GAP // 2, 0, a.width + GAP // 2, args.height + LABEL_HEIGHT], fill=(90, 90, 96), width=2)

    canvas.save(args.out, optimize=True)
    print(f"{args.out}: {canvas.width}x{canvas.height}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
