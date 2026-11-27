#!/usr/bin/env python3
"""解析 adb gfxinfo 原始 dump，输出每阶段统计（Markdown + JSON + 每帧 CSV）。

为什么不是简单取 framestats：
这台 HONOR（Android 15 / MagicOS）的 `dumpsys gfxinfo <pkg> framestats` 只回约 10 帧，
不足以算 p95；但整份 `dumpsys gfxinfo <pkg>` 里的 1ms 直方图覆盖 reset 之后的全部帧。
所以：帧数 / jank% / p50-p95 用「多次 reset 的直方图合并」算，
framestats 只用来做 CPU/GPU 拆解（样本量小，单独标注 n）。

用法：python scripts/perf_parse.py [perf-raw 目录]
"""
import csv
import json
import re
import statistics
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "perf-raw"
OUT = ROOT / "perf-out"

PHASES = ["A", "Aopt", "Aopt2", "Aopt3", "Aopt4", "Aoff", "B", "Bcold", "Bwarm", "Bpush", "Bpop", "Btab", "C1", "C2", "D", "Doff", "Foff", "Fov", "Poff", "Pov"]
LABELS = {
    "A": "A 首页滚动 · 优化前（整页录制）",
    "Aopt": "A* 首页滚动 · 优化①（reuseContent）",
    "Aopt2": "A** 首页滚动 · 优化②（隔帧录制）",
    "Aopt3": "A*** 首页滚动 · 优化③（半分辨率录制）",
    "Aopt4": "A**** 首页滚动 · 最终（半分辨率 + 30Hz）",
    "Aoff": "A' 首页滚动 · 整页录制 OFF（纯色 backdrop）",
    "A2": "A2 首页滚动 · 整页录制 ON（复测）",
    "B": "B 帖子转场（混合）",
    "Bcold": "B1 转场 · 冷帖子（首次打开）",
    "Bwarm": "B2 转场 · 热帖子（同帖反复打开）",
    "Bpush": "B3 转场 · 只测点进",
    "Bpop": "B4 转场 · 只测返回",
    "Btab": "B0 对照 · 只切 tab（无数据加载）",
    "C1": "C1 慢速拖 dock（2s）",
    "C2": "C2 dock 收缩 → 点开",
    "D": "D 首页静止 5s · 录制 ON",
    "Doff": "D' 首页静止 5s · 录制 OFF",
    "Foff": "F- 吧页滚动 · 顶栏在内容里（旧）",
    "Fov": "F+ 吧页滚动 · 顶栏进 overlay（新）",
    "Poff": "P- 设置页滚动 · 纯色 host（旧）",
    "Pov": "P+ 设置页滚动 · 双层 host（新）",
}

FRAME_FIELDS = [
    "flags", "frame_timeline_vsync_id", "intended_vsync", "vsync", "input_event_id",
    "handle_input_start", "animation_start", "perform_traversals_start", "draw_start",
    "frame_deadline", "frame_start_time", "frame_interval", "sync_queued", "sync_start",
    "issue_draw_commands_start", "swap_buffers", "frame_completed", "dequeue_buffer_duration",
    "queue_buffer_duration", "gpu_completed", "swap_buffers_completed", "display_present_time",
    "command_submission_completed",
]

# 注意：本 ROM 的 dump 表头把 FrameInterval / FrameStartTime 的顺序印反了（数据里 10=FrameStartTime,
# 11=FrameInterval）。所以按**位置**解析，不用表头名字。
assert FRAME_FIELDS[10] == "frame_start_time" and FRAME_FIELDS[11] == "frame_interval"


def read_text(p: Path) -> str:
    for enc in ("utf-8-sig", "utf-8", "gbk"):
        try:
            return p.read_text(encoding=enc)
        except UnicodeDecodeError:
            continue
    return p.read_text(encoding="utf-8", errors="replace")


def parse_stats(text: str) -> dict:
    """解析整份 gfxinfo：取第一个有帧数的 section。"""
    def grab(pattern, cast=float, default=None):
        m = re.search(pattern, text)
        if not m:
            return default
        return cast(m.group(1))

    frames = grab(r"Total frames rendered:\s*(\d+)", int, 0)
    janky = grab(r"Janky frames:\s*(\d+)", int, 0)
    jank_pct = grab(r"Janky frames:\s*\d+\s*\(([\d.]+)%\)", float, 0.0)
    percentiles = {}
    for p in (50, 90, 95, 99):
        percentiles[p] = grab(rf"{p}th percentile:\s*(\d+)ms", int, None)

    hist = {}
    hm = re.search(r"\nHISTOGRAM:([^\n]*)", text)
    if hm:
        for lo, cnt in re.findall(r"(\d+)ms=(\d+)", hm.group(1)):
            hist[int(lo)] = hist.get(int(lo), 0) + int(cnt)
    gpu = {}
    gm = re.search(r"GPU HISTOGRAM:([^\n]*)", text)
    if gm:
        for lo, cnt in re.findall(r"(\d+)ms=(\d+)", gm.group(1)):
            gpu[int(lo)] = gpu.get(int(lo), 0) + int(cnt)
    missed = grab(r"Number Missed Vsync:\s*(\d+)", int, None)
    slow_ui = grab(r"Number Slow UI thread:\s*(\d+)", int, None)
    slow_draw = grab(r"Number Slow issue draw commands:\s*(\d+)", int, None)
    return {
        "frames": frames, "janky": janky, "jank_pct": jank_pct,
        "p50": percentiles[50], "p90": percentiles[90], "p95": percentiles[95],
        "p99": percentiles[99],
        "hist": hist, "gpu_hist": gpu,
        "missed_vsync": missed, "slow_ui": slow_ui, "slow_draw": slow_draw,
    }


def hist_percentile(hist: dict, q: float):
    total = sum(hist.values())
    if total == 0:
        return None
    target = q * total
    acc = 0
    for lo in sorted(hist):
        acc += hist[lo]
        if acc >= target:
            return lo + 0.5  # 桶是 [lo, lo+1) ms
    return max(hist) + 0.5


def parse_framestats(text: str):
    rows = []
    in_data = False
    for line in text.splitlines():
        if line.startswith("---PROFILEDATA---"):
            if in_data:
                break
            in_data = True
            continue
        if not in_data:
            continue
        if line.startswith("Flags,"):
            continue
        if not line.strip():
            continue
        parts = line.rstrip(",").split(",")
        if len(parts) < 19:
            continue
        rec = {}
        for i, name in enumerate(FRAME_FIELDS):
            if i < len(parts):
                try:
                    rec[name] = int(parts[i])
                except ValueError:
                    rec[name] = None
        rows.append(rec)
    return rows


def frame_metrics(rows):
    out = []
    for r in rows:
        iv = r.get("intended_vsync")
        fc = r.get("frame_completed")
        if not iv or not fc or fc <= 0:
            continue
        d = {
            "total_ms": (fc - iv) / 1e6,
            "cpu_ms": (r.get("issue_draw_commands_start", 0) - iv) / 1e6
            if r.get("issue_draw_commands_start") else None,
            "draw_ms": (fc - r["draw_start"]) / 1e6 if r.get("draw_start") else None,
            "gpu_ms": (r["gpu_completed"] - r["swap_buffers"]) / 1e6
            if r.get("gpu_completed") and r.get("swap_buffers") and r["gpu_completed"] > r["swap_buffers"] else None,
            "present_ms": (r["display_present_time"] - iv) / 1e6
            if r.get("display_present_time") and r["display_present_time"] > iv else None,
            "vsync": r.get("vsync"),
        }
        out.append(d)
    out.sort(key=lambda x: x["total_ms"])
    return out


def parse_sf(text: str):
    """SurfaceFlinger --latency：第一行是刷新周期 ns，其后 127 行 desired/actual/ready。"""
    lines = [l.strip() for l in text.splitlines() if l.strip() and l.strip() != "---"]
    if not lines:
        return None
    try:
        refresh = int(lines[0])
    except ValueError:
        return None
    ts = []
    for l in lines[1:]:
        parts = l.split()
        if len(parts) < 3:
            continue
        try:
            ts.append(int(parts[1]))
        except ValueError:
            continue
    ts = sorted(t for t in ts if 0 < t < 4_000_000_000_000_000_000)
    if len(ts) < 5:
        return None
    deltas = [(b - a) / 1e6 for a, b in zip(ts, ts[1:]) if b > a]
    span_ms = (ts[-1] - ts[0]) / 1e6
    return {
        "n": len(ts),
        "refresh_ms": refresh / 1e6,
        "fps": round((len(ts) - 1) / span_ms * 1000, 1) if span_ms > 0 else None,
        "deltas": deltas,
        "span_ms": round(span_ms, 1),
        "_ts": ts,
    }


def summarize_sf(per_iter):
    valid = [x for x in per_iter if x]
    if not valid:
        return None
    all_deltas = [d for x in valid for d in x["deltas"]]
    refresh = valid[0]["refresh_ms"]
    gaps = [d for d in all_deltas if d > 1.5 * refresh]
    return {
        "runs": len(valid),
        "fps_median": round(statistics.median([x["fps"] for x in valid]), 1),
        "fps_min": round(min(x["fps"] for x in valid), 1),
        "frames": sum(x["n"] for x in valid),
        "gap_pct": round(100.0 * len(gaps) / len(all_deltas), 2) if all_deltas else None,
        "gap_p50": round(pct(all_deltas, 0.5), 1) if all_deltas else None,
        "gap_p95": round(pct(all_deltas, 0.95), 1) if all_deltas else None,
        "refresh_ms": round(refresh, 2),
    }


def pct(vals, q):
    vals = [v for v in vals if v is not None]
    if not vals:
        return None
    vals = sorted(vals)
    k = (len(vals) - 1) * q
    f = int(k)
    c = min(f + 1, len(vals) - 1)
    return vals[f] + (vals[c] - vals[f]) * (k - f)


def collect_phase(phase: str):
    stats_files = sorted(RAW.glob(f"{phase}_*_stats.txt"))
    if not stats_files:
        return None
    total_frames = total_jank = 0
    hist = {}
    gpu_hist = {}
    per_iter = []
    fs_rows = []
    sf_runs = []
    for sf in stats_files:
        st = parse_stats(read_text(sf))
        total_frames += st["frames"]
        total_jank += st["janky"]
        for k, v in st["hist"].items():
            hist[k] = hist.get(k, 0) + v
        for k, v in st["gpu_hist"].items():
            gpu_hist[k] = gpu_hist.get(k, 0) + v
        per_iter.append(st)
        ff = sf.with_name(sf.name.replace("_stats.txt", "_fs.txt"))
        if ff.exists():
            fs_rows.extend(parse_framestats(read_text(ff)))
        gf = sf.with_name(sf.name.replace("_stats.txt", "_sf.txt"))
        if gf.exists():
            sf_runs.append(parse_sf(read_text(gf)))

    fm = frame_metrics(fs_rows)
    res = {
        "phase": phase,
        "label": LABELS.get(phase, phase),
        "iterations": len(stats_files),
        "frames": total_frames,
        "janky": total_jank,
        "jank_pct": round(100.0 * total_jank / total_frames, 2) if total_frames else None,
        "p50": hist_percentile(hist, 0.50),
        "p90": hist_percentile(hist, 0.90),
        "p95": hist_percentile(hist, 0.95),
        "p99": hist_percentile(hist, 0.99),
        "per_iteration": [
            {k: it[k] for k in ("frames", "janky", "jank_pct", "p50", "p90", "p95")}
            for it in per_iter
        ],
        "fs": {
            "n": len(fm),
            "total_p50": round(pct([x["total_ms"] for x in fm], 0.5), 2) if fm else None,
            "total_p90": round(pct([x["total_ms"] for x in fm], 0.9), 2) if fm else None,
            "total_p95": round(pct([x["total_ms"] for x in fm], 0.95), 2) if fm else None,
            "cpu_p50": round(pct([x["cpu_ms"] for x in fm if x["cpu_ms"]], 0.5), 2) if fm else None,
            "gpu_p50": round(pct([x["gpu_ms"] for x in fm if x["gpu_ms"]], 0.5), 2) if fm else None,
            "gpu_p90": round(pct([x["gpu_ms"] for x in fm if x["gpu_ms"]], 0.9), 2) if fm else None,
            "present_p50": round(pct([x["present_ms"] for x in fm if x["present_ms"]], 0.5), 2) if fm else None,
        },
        "hist": hist,
        "gpu_hist": gpu_hist,
        "sf": summarize_sf(sf_runs),
        "_fm": fm,
    }
    return res


def fmt(v, nd=1, suffix=""):
    return "-" if v is None else f"{v:.{nd}f}{suffix}"


def main():
    OUT.mkdir(exist_ok=True)
    results = {}
    for ph in PHASES:
        r = collect_phase(ph)
        if r:
            results[ph] = r
    if not results:
        print("no data in", RAW)
        return 1

    # 每帧明细 CSV
    for ph, r in results.items():
        with open(OUT / f"frames_{ph}.csv", "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["total_ms", "cpu_ms", "draw_ms", "gpu_ms", "present_ms"])
            for x in r["_fm"]:
                w.writerow([
                    round(x["total_ms"], 2),
                    None if x["cpu_ms"] is None else round(x["cpu_ms"], 2),
                    None if x["draw_ms"] is None else round(x["draw_ms"], 2),
                    None if x["gpu_ms"] is None else round(x["gpu_ms"], 2),
                    None if x["present_ms"] is None else round(x["present_ms"], 2),
                ])

    lines = []
    lines.append("| 场景 | 采样轮次 | 帧数 | jank% | p50 (ms) | p90 (ms) | p95 (ms) | p99 (ms) |")
    lines.append("|---|---:|---:|---:|---:|---:|---:|---:|")
    for ph, r in results.items():
        lines.append(
            f"| {r['label']} | {r['iterations']} | {r['frames']} | "
            f"{fmt(r['jank_pct'])}% | {fmt(r['p50'])} | {fmt(r['p90'])} | {fmt(r['p95'])} | {fmt(r['p99'])} |"
        )
    table = "\n".join(lines)

    lines2 = ["| 场景 | framestats 样本 n | 总帧时 p50/p90/p95 | CPU(提交绘制) p50 | GPU p50/p90 | 上屏延迟 p50 |",
              "|---|---:|---|---:|---|---:|"]
    for ph, r in results.items():
        fs = r["fs"]
        lines2.append(
            f"| {r['label']} | {fs['n']} | {fmt(fs['total_p50'], 2)} / {fmt(fs['total_p90'], 2)} / {fmt(fs['total_p95'], 2)} | "
            f"{fmt(fs['cpu_p50'], 2)} | {fmt(fs['gpu_p50'], 2)} / {fmt(fs['gpu_p90'], 2)} | {fmt(fs['present_p50'], 2)} |"
        )
    table2 = "\n".join(lines2)

    lines3 = ["| 场景 | 上屏帧数 | 实测 fps（中位） | 最差一轮 fps | 上屏间隔 p50/p95 (ms) | >1.5×vsync 间隔占比 |",
              "|---|---:|---:|---:|---|---:|"]
    for ph, r in results.items():
        sf = r.get("sf")
        if not sf:
            continue
        lines3.append(
            f"| {r['label']} | {sf['frames']} | {fmt(sf['fps_median'])} | {fmt(sf['fps_min'])} | "
            f"{fmt(sf['gap_p50'])} / {fmt(sf['gap_p95'])} | {fmt(sf['gap_pct'])}% |"
        )
    table3 = "\n".join(lines3)

    print(table)
    print()
    print(table2)
    print()
    print(table3)
    print()
    if "A" in results and "Aoff" in results:
        a, b = results["A"], results["Aoff"]
        print(f"A/B: 录制ON p50={fmt(a['p50'])}ms p95={fmt(a['p95'])}ms jank={fmt(a['jank_pct'])}% | "
              f"OFF p50={fmt(b['p50'])}ms p95={fmt(b['p95'])}ms jank={fmt(b['jank_pct'])}%")

    payload = {ph: {k: v for k, v in r.items() if k != "_fm"} for ph, r in results.items()}
    (OUT / "perf-results.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    (OUT / "perf-tables.md").write_text(table + "\n\n" + table2 + "\n\n" + table3 + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())


