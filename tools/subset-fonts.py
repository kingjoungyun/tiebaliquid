#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把本机安装的 HarmonyOS Sans SC 子集化成 Android 可用的三档字重。

可重跑：
    C:\\Users\\39982\\.dsh\\dsh-runtimes\\dsh-primary-runtime\\dependencies\\python\\python.exe scripts\\subset-fonts.py

子集范围（A3：扩到 CJK 基本区）：
  - ASCII 0x20-0x7E
  - **CJK 统一表意文字基本区 U+4E00-U+9FFF（20992 字）**
  - **CJK 扩展 A U+3400-U+4DBF（6592 字）**：验收样例里的「㵘 U+3D58」「䶮 U+4DAE」在扩展 A，
    只收基本区会让这两个字掉出 cmap，所以一并收进来（原字体对扩展 A 覆盖 6582/6592）
  - GB2312 全字集（含 6763 汉字与 682 个非汉字：中文标点、全角符号、序号、注音等）
  - 中文标点补充（见 EXTRA_PUNCTUATION）与 CJK 符号/标点区 U+3000-U+303F、全角形式 U+FF00-U+FFEF

产物（文件名必须小写，AAPT 只认小写资源名）：
  sources/TiebaApp/app/src/main/res/font/harmony_sans_sc_{regular,medium,semibold}.ttf

字体默认带 hinting，`--no-hinting` 能再省一点；`--layout-features=*` 保住 kerning/liga，
中文排版基本用不到但拉丁部分需要，代价很小。
"""
from __future__ import annotations

import os
import sys

from fontTools import subset

# 仓库/工作区定位：scripts/ 的上一级 = 工作区根
WORKSPACE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(WORKSPACE, "sources", "TiebaApp", "app", "src", "main", "res", "font")
FONT_DIR = os.path.join(os.environ["LOCALAPPDATA"], "Microsoft", "Windows", "Fonts")

WEIGHTS = {
    "regular": "HarmonyOS_SansSC_Regular.ttf",
    "medium": "HarmonyOS_SansSC_Medium.ttf",
    "semibold": "HarmonyOS_SansSC_Semibold.ttf",
}

EXTRA_PUNCTUATION = "　、。〈〉《》「」『』【】〔〕・ー…“”‘’—～·×÷±≤≥°′″€¥£©®™←↑→↓"

# 验收用的生僻字（分布在基本区与扩展 A），子集后逐个查 cmap。
RARE_SAMPLE = "㵘䶮龘靐齾魑魅魍魉饕餮龖爨灪麤彠鱻羴猋犇"


def build_charset() -> str:
    chars = {chr(c) for c in range(0x20, 0x7F)}  # ASCII
    # CJK 统一表意文字基本区（20992 字）
    chars.update(chr(c) for c in range(0x4E00, 0xA000))
    # CJK 扩展 A（生僻字「㵘」「䶮」在这里）
    chars.update(chr(c) for c in range(0x3400, 0x4DC0))
    # CJK 符号和标点 / 全角形式（GB2312 补充符号的超集）
    chars.update(chr(c) for c in range(0x3000, 0x3040))
    chars.update(chr(c) for c in range(0xFF00, 0xFFF0))
    # GB2312 双字节区：0xA1A1-0xF7FE（含中文标点与全部 GB2312 汉字）
    for hi in range(0xA1, 0xF8):
        for lo in range(0xA1, 0xFF):
            try:
                chars.add(bytes([hi, lo]).decode("gb2312"))
            except UnicodeDecodeError:
                pass
    chars |= set(EXTRA_PUNCTUATION)
    return "".join(sorted(chars))


def subset_one(src: str, out: str, text_file: str) -> None:
    subset.main([
        src,
        f"--text-file={text_file}",
        f"--output-file={out}",
        "--layout-features=*",
        "--no-hinting",
        "--desubroutinize",
        "--name-IDs=1,2,3,4,5,6,13,14",  # family/style/full/version/copyright/license
        "--name-languages=0x409,0x804",
    ])


def verify_cmap(path: str, sample: str) -> None:
    from fontTools.ttLib import TTFont

    cmap = TTFont(path).getBestCmap() or {}
    missing = [ch for ch in sample if ord(ch) not in cmap]
    total_cjk = sum(1 for c in range(0x4E00, 0xA000) if c in cmap)
    total_ext_a = sum(1 for c in range(0x3400, 0x4DC0) if c in cmap)
    print(
        f"  cmap: {len(cmap)} code points | CJK basic {total_cjk} | Ext-A {total_ext_a}"
        f" | 生僻字 {len(sample) - len(missing)}/{len(sample)}"
    )
    if missing:
        print(
            "  MISSING: " + " ".join(f"{ch}(U+{ord(ch):04X})" for ch in missing),
            file=sys.stderr,
        )


def main() -> int:
    os.makedirs(OUT_DIR, exist_ok=True)
    chars = build_charset()
    text_file = os.path.join(OUT_DIR, ".harmony_subset_chars.txt")
    with open(text_file, "w", encoding="utf-8") as f:
        f.write(chars)
    print(f"charset: {len(chars)} codepoints")

    total_in = total_out = 0
    for name, filename in WEIGHTS.items():
        src = os.path.join(FONT_DIR, filename)
        if not os.path.exists(src):
            print(f"MISSING: {src}", file=sys.stderr)
            return 2
        out = os.path.join(OUT_DIR, f"harmony_sans_sc_{name}.ttf")
        subset_one(src, out, text_file)
        size_in, size_out = os.path.getsize(src), os.path.getsize(out)
        total_in += size_in
        total_out += size_out
        print(f"{name:<9} {size_in / 1048576:6.2f} MB -> {size_out / 1048576:6.2f} MB  ({size_out / size_in:.1%})")
        verify_cmap(out, RARE_SAMPLE)

    os.remove(text_file)
    print(f"{'total':<9} {total_in / 1048576:6.2f} MB -> {total_out / 1048576:6.2f} MB  ({total_out / total_in:.1%})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
