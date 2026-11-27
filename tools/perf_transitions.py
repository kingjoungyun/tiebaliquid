#!/usr/bin/env python3
"""转场时间线分析：从 SurfaceFlinger --latency 的 present 时间戳看卡在哪。

对每次 run：t0 = 第一次上屏；统计
  - t0 之后前 400ms 内的上屏帧数（350ms 转场动画窗口）
  - 最大间隔及其相对 t0 的位置（判断停顿发生在动画开头还是中段）
  - 到第 10 帧所需时间（60fps 理想约 150ms）
用法：python scripts/perf_transitions.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import perf_parse as P

PHASES = ["Bpush", "Bpop", "Bcold", "Bwarm", "Btab"]


def analyze_run(ts, refresh_ms=16.667):
    if len(ts) < 3:
        return None
    t0 = ts[0]
    rel = [(t - t0) / 1e6 for t in ts]
    diffs = [(rel[i + 1] - rel[i], rel[i]) for i in range(len(rel) - 1)]
    gaps = [d for d, _ in diffs]
    max_gap, max_gap_at = max(diffs, key=lambda x: x[0])
    win = [r for r in rel if r <= 400]
    # 到第 10 帧（索引 9）所用时间
    t10 = rel[9] if len(rel) > 9 else None
    win_gaps = [gaps[i] for i in range(len(gaps)) if rel[i + 1] <= 400]
    return {
        "frames": len(ts),
        "frames_400": len(win),
        "t10": t10,
        "max_gap": max_gap,
        "max_gap_at": max_gap_at,
        "win_gaps": win_gaps,
        "span": rel[-1],
    }


def main():
    for ph in PHASES:
        files = sorted(P.RAW.glob(f"{ph}_*_sf.txt"))
        if not files:
            continue
        runs = []
        for f in files:
            sf = P.parse_sf(P.read_text(f))
            if not sf:
                continue
            r = analyze_run(sf["_ts"])
            if r:
                runs.append(r)
        if not runs:
            continue
        print(f"\n== {ph} ({len(runs)} runs) ==")
        print(f"{'#':>2} {'frames':>6} {'<=400ms':>7} {'t10(ms)':>8} {'maxgap':>7} {'at(ms)':>7} {'span(ms)':>8}")
        for i, r in enumerate(runs, 1):
            t10 = f"{r['t10']:.0f}" if r["t10"] is not None else "-"
            print(f"{i:>2} {r['frames']:>6} {r['frames_400']:>7} {t10:>8} "
                  f"{r['max_gap']:>7.1f} {r['max_gap_at']:>7.0f} {r['span']:>8.0f}")
        # 汇总
        import statistics
        t10s = [r["t10"] for r in runs if r["t10"]]
        gaps = [r["max_gap"] for r in runs]
        print(f"   frames<=400ms median={statistics.median([r['frames_400'] for r in runs]):.0f}  "
              f"median max_gap={statistics.median(gaps):.1f}ms" +
              (f"  median t10={statistics.median(t10s):.0f}ms" if t10s else ""))


if __name__ == "__main__":
    main()
