#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""两张同尺寸截图的像素差（T1 迁移前后对比）。

用法：
  python scripts/pixel-diff.py A.png B.png [--crop 0:400] [--out shots/_diff.png]

输出（stdout，一行一个指标）：
  size            两张图的尺寸
  mean_abs        平均绝对差，归一化到 0..1（= 每通道 |a-b| 的均值 / 255）
  max_abs         最大单通道差（0..255）
  diff_ratio      |差| > 2/255 的像素占比
  equal_ratio     完全相同的像素占比
加 --crop y1:y2 时只比较该纵向区间（用于只比顶栏/底栏 chrome）。
加 --out 时输出可视化：左 A / 中 B / 右 diff 热力图（放大 3 倍的差值）。
"""
from __future__ import annotations

import argparse
import sys

import numpy as np
from PIL import Image


def load(path: str) -> np.ndarray:
    im = Image.open(path).convert("RGB")
    return np.asarray(im, dtype=np.int16)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("a")
    ap.add_argument("b")
    ap.add_argument("--crop", default="")
    ap.add_argument("--out", default="")
    ap.add_argument("--label", default="")
    args = ap.parse_args()

    a = load(args.a)
    b = load(args.b)
    if a.shape != b.shape:
        print(f"size mismatch: {a.shape} vs {b.shape}", file=sys.stderr)
        return 2

    if args.crop:
        y1, y2 = (int(v) for v in args.crop.split(":"))
        a = a[y1:y2]
        b = b[y1:y2]

    d = np.abs(a - b)
    mean_abs = float(d.mean() / 255.0)
    max_abs = int(d.max())
    diff_ratio = float((d.max(axis=2) > 2).mean())
    equal_ratio = float((d.max(axis=2) == 0).mean())

    tag = args.label or "diff"
    print(f"[{tag}] size={a.shape[1]}x{a.shape[0]} crop={args.crop or 'full'}")
    print(f"[{tag}] mean_abs={mean_abs:.5f}")
    print(f"[{tag}] max_abs={max_abs}")
    print(f"[{tag}] diff_ratio={diff_ratio:.4f} equal_ratio={equal_ratio:.4f}")

    if args.out:
        heat = (np.clip(d.max(axis=2) * 6, 0, 255)).astype(np.uint8)
        heat_rgb = np.stack([heat, np.zeros_like(heat), np.zeros_like(heat)], axis=2)
        combo = np.concatenate([a.astype(np.uint8), b.astype(np.uint8), heat_rgb], axis=1)
        Image.fromarray(combo).save(args.out)
        print(f"wrote {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
