"""
论文用统计检验工具
==================
针对「同一批用例、不同配置」的配对比较，输出 SCI 投稿所需的统计量：

  · 均值差 + bootstrap 95% 置信区间（不依赖分布假设）
  · 配对 t 检验 / Wilcoxon 符号秩检验
  · Cohen's d（配对版）、Wilcoxon 效应量 r
  · Holm 多重比较校正

设计原则：**bootstrap CI 为主，p 值为辅**。审稿人越来越看重效应量与区间，
而非单独的显著性声明。

用法
----
  # A/B 两组逐用例指标文件（JSON: {case_id: value} 或 [{case_id, value}, ...]）
  python tests/stats_tests.py --a runs/full.json --b runs/b4_no_debate.json \
      --name-a "Full" --name-b "No debate" --metric coverage

  # 多组 vs 基线（自动 Holm 校正）
  python tests/stats_tests.py --baseline runs/full.json \
      --compare runs/b1.json runs/b3.json runs/b4.json --metric hallucination_rate

  # 直接读评测产物（test-data/eval_report.json 的 live_results）
  python tests/stats_tests.py --from-report test-data/eval_report.json --metric coverage
"""
import argparse
import json
import os
import sys

import numpy as np

try:
    from scipy import stats as sps
    HAS_SCIPY = True
except ImportError:  # pragma: no cover
    HAS_SCIPY = False


def load_series(path, metric):
    """载入 {case_id: value}。支持 dict / list-of-dict / eval_report.json。"""
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    if isinstance(data, dict) and "live_results" in data:
        rows = data["live_results"]
        out = {}
        for r in rows:
            v = r
            for k in metric.split("."):
                v = v.get(k, {}) if isinstance(v, dict) else None
                if v is None:
                    break
            if v is not None:
                out[r["case_id"]] = float(v)
        return out
    if isinstance(data, dict):
        return {k: float(v) for k, v in data.items()}
    return {r["case_id"]: float(r[metric]) for r in data}


def paired_compare(a, b, n_boot=10000, seed=42):
    """配对比较，a - b。返回统计量字典。"""
    keys = sorted(set(a) & set(b))
    if len(keys) < 2:
        raise ValueError(f"可配对用例不足（{len(keys)}）")
    x = np.array([a[k] for k in keys], dtype=float)
    y = np.array([b[k] for k in keys], dtype=float)
    d = x - y
    n = len(d)
    mean_d = float(d.mean())

    rng = np.random.default_rng(seed)
    idx = rng.integers(0, n, size=(n_boot, n))
    boot = d[idx].mean(axis=1)
    ci_lo, ci_hi = (float(np.percentile(boot, 2.5)), float(np.percentile(boot, 97.5)))

    res = {
        "n": n,
        "mean_a": float(x.mean()),
        "mean_b": float(y.mean()),
        "mean_diff": mean_d,
        "ci95": [ci_lo, ci_hi],
        "sd_diff": float(d.std(ddof=1)),
        "cohens_d": float(mean_d / d.std(ddof=1)) if d.std(ddof=1) > 0 else float("nan"),
    }

    if HAS_SCIPY:
        t_stat, t_p = sps.ttest_rel(x, y)
        res["t_p"] = float(t_p)
        try:
            w_stat, w_p = sps.wilcoxon(x, y, zero_method="wilcox")
        except ValueError:
            w_stat, w_p = float("nan"), float("nan")
        res["wilcoxon_p"] = float(w_p)
        res["wilcoxon_r"] = float(abs(w_stat - n * (n + 1) / 4) / np.sqrt(n * (n + 1) * (2 * n + 1) / 24)) \
            if not np.isnan(w_stat) else float("nan")
    else:
        # 正态近似的 Wilcoxon 符号秩（无 scipy 时的降级实现）
        ranks = sps_rankdata_manual(np.abs(d))
        signs = np.sign(d)
        w_plus = float(ranks[signs > 0].sum())
        mu = n * (n + 1) / 4
        sigma = np.sqrt(n * (n + 1) * (2 * n + 1) / 24)
        z = (w_plus - mu) / sigma if sigma > 0 else 0.0
        res["wilcoxon_z"] = float(z)
        res["wilcoxon_r"] = float(abs(z) / np.sqrt(n))
        res["note"] = "scipy 不可用，Wilcoxon 使用正态近似"
    return res


def sps_rankdata_manual(a):
    order = np.argsort(a)
    ranks = np.empty(len(a), dtype=float)
    ranks[order] = np.arange(1, len(a) + 1)
    return ranks


def holm(pvals):
    """Holm-Bonferroni 校正，返回校正后 p 值（保持输入顺序）。"""
    m = len(pvals)
    order = np.argsort(pvals)
    adj = np.empty(m, dtype=float)
    running = 0.0
    for i, idx in enumerate(order):
        val = (m - i) * pvals[idx]
        running = max(running, val)
        adj[idx] = min(1.0, running)
    return adj


def fmt_p(p):
    if p is None or (isinstance(p, float) and np.isnan(p)):
        return "--"
    if p < 0.001:
        return "$<$0.001"
    return f"{p:.3f}"


def latex_row(name, r, higher_better=True):
    """生成论文表格一行的 LaTeX 片段。"""
    star = ""
    p = r.get("wilcoxon_p", r.get("t_p"))
    if p is not None and not np.isnan(p):
        if p < 0.001:
            star = "$^{***}$"
        elif p < 0.01:
            star = "$^{**}$"
        elif p < 0.05:
            star = "$^{*}$"
    return (f"{name} & {r['mean_a']:.4f} & {r['mean_b']:.4f} & "
            f"{r['mean_diff']:+.4f} & [{r['ci95'][0]:+.4f}, {r['ci95'][1]:+.4f}] & "
            f"{r['cohens_d']:+.2f} & {fmt_p(p)}{star} \\\\")


def main():
    ap = argparse.ArgumentParser(description="论文统计检验")
    ap.add_argument("--a", help="配置 A 的逐用例指标 JSON")
    ap.add_argument("--b", help="配置 B 的逐用例指标 JSON")
    ap.add_argument("--baseline", help="基线（多组比较时）")
    ap.add_argument("--compare", nargs="*", help="待比较的多个配置")
    ap.add_argument("--metric", default="coverage", help="指标名（支持 a.b 形式）")
    ap.add_argument("--name-a", default="A")
    ap.add_argument("--name-b", default="B")
    ap.add_argument("--higher-better", action="store_true")
    ap.add_argument("--latex", action="store_true", help="额外输出 LaTeX 表格行")
    args = ap.parse_args()

    if args.a and args.b:
        a, b = load_series(args.a, args.metric), load_series(args.b, args.metric)
        r = paired_compare(a, b)
        print(f"\n=== {args.name_a} vs {args.name_b} | metric={args.metric} ===")
        print(f"  n 配对用例        : {r['n']}")
        print(f"  均值 {args.name_a} / {args.name_b}   : {r['mean_a']:.4f} / {r['mean_b']:.4f}")
        print(f"  均值差 (A-B)      : {r['mean_diff']:+.4f}")
        print(f"  95% bootstrap CI  : [{r['ci95'][0]:+.4f}, {r['ci95'][1]:+.4f}]")
        print(f"  Cohen's d (paired): {r['cohens_d']:+.3f}")
        if "wilcoxon_p" in r:
            print(f"  Wilcoxon p        : {fmt_p(r['wilcoxon_p'])}")
            print(f"  Wilcoxon r        : {r.get('wilcoxon_r', float('nan')):.3f}")
            print(f"  配对 t 检验 p     : {fmt_p(r.get('t_p'))}")
        else:
            print(f"  Wilcoxon z (近似) : {r.get('wilcoxon_z', float('nan')):.3f}  [{r.get('note', '')}]")
        if args.latex:
            print("\n" + latex_row(args.name_b, r, args.higher_better))
        return

    if args.baseline and args.compare:
        base = load_series(args.baseline, args.metric)
        rows, ps, names = [], [], []
        for path in args.compare:
            other = load_series(path, args.metric)
            r = paired_compare(base, other)
            name = os.path.splitext(os.path.basename(path))[0]
            rows.append((name, r))
            p = r.get("wilcoxon_p", r.get("t_p"))
            ps.append(1.0 if p is None or np.isnan(p) else p)
            names.append(name)
        adj = holm(np.array(ps))
        print(f"\n=== 多组比较 vs 基线 | metric={args.metric}（Holm 校正）===")
        for (name, r), p_raw, p_adj in zip(rows, ps, adj):
            print(f"  {name:<28} 差={r['mean_diff']:+.4f}  "
                  f"CI=[{r['ci95'][0]:+.4f}, {r['ci95'][1]:+.4f}]  "
                  f"d={r['cohens_d']:+.2f}  p={fmt_p(p_raw)}  p_holm={fmt_p(p_adj)}")
            if args.latex:
                print("    " + latex_row(name, r, args.higher_better))
        return

    ap.error("需指定 (--a 与 --b) 或 (--baseline 与 --compare)")


if __name__ == "__main__":
    sys.exit(main())
