"""
验证有效性检验（论文 RQ4）
==========================
解决的问题：系统用 Debate Agent（与生成 Agent 同一模型族）判定自己的幻觉率，
存在**循环论证**风险，审稿人必然质疑。本工具提供两条独立锚点：

  1. 异构模型外部判定（external judge，不同厂商/模型族）
  2. 人工标注（≥2 名标注员，可算评分者间一致性）

以「知识点（core point）」为判定单元，三方判定对齐后计算
percent agreement / Cohen's kappa / Krippendorff's alpha。

流程
----
  # 1) 从采集结果中抽取待判定知识点片段（--blind 隐藏系统内部判定，避免锚定效应）
  python tests/verification_validity.py export \
      --from test-data/live_results.jsonl --out data/claims.csv --blind

  # 2) 异构模型外部判定
  export JUDGE_API_KEY=...
  python tests/verification_validity.py judge \
      --claims data/claims.jsonl \
      --endpoint https://api.openai.com/v1/chat/completions \
      --model gpt-4o-mini --out data/external_verdicts.jsonl

  # 3) 人工标注员填完 CSV 后，计算三方一致性
  python tests/verification_validity.py agree \
      --claims data/claims.jsonl \
      --external data/external_verdicts.jsonl \
      --human data/claims_annotated.csv --latex
"""
import argparse
import csv
import json
import os
import re
import sys
import time
from urllib import request as urlreq

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SNIPPET_WINDOW = 700

JUDGE_SYSTEM = (
    "You are a strict fact-checker for technical educational material in "
    "artificial intelligence and industrial AI. You will be given a topic, a "
    "specific knowledge point, and an excerpt of teaching material. Decide whether "
    "the excerpt contains a FACTUAL ERROR about that knowledge point "
    "(wrong definition, wrong formula, non-existent API, fabricated standard, "
    "incorrect numeric claim, or an assertion contradicted by mainstream practice). "
    "Judge only what is written; do not penalize brevity or style. "
    "Respond with JSON only: {\"verdict\": \"correct\" or \"hallucinated\", "
    "\"evidence\": \"short quote or paraphrase\", \"reason\": \"one sentence\"}"
)


# ────────────────────────── 片段抽取 ──────────────────────────

def keyword_of(point, max_len=6):
    parts = re.split(r"[（(与/、,，\-\s]+", point)
    cands = [p for p in parts if len(p) >= 2]
    return cands[0][:max_len] if cands else point[:max_len]


def make_snippet(text, point, window=SNIPPET_WINDOW):
    kw = keyword_of(point)
    idx = text.find(kw)
    if idx < 0:
        return text[:window * 2]
    lo = max(0, idx - window // 2)
    hi = min(len(text), idx + window)
    return ("..." if lo > 0 else "") + text[lo:hi] + ("..." if hi < len(text) else "")


def export_claims(src, out_csv, out_jsonl=None, blind=True, max_per_case=None):
    claims = []
    with open(src, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            r = json.loads(line)
            raw = r.get("_raw") or {}
            outputs = raw.get("outputs") or {}
            if not outputs:
                continue
            full_text = "\n".join(str(v) for v in outputs.values())
            hallus = (raw.get("debate") or {}).get("hallucinations") or []
            hallu_text = " || ".join(str(h.get("content", "")) for h in hallus)
            # 该用例的全部核心知识点：用覆盖/未覆盖集合 + 主题名无法还原，故从 coverage 明细取
            points = list((raw.get("covered_points") or [])) + list((raw.get("missed_points") or []))
            if max_per_case:
                points = points[:max_per_case]
            for i, p in enumerate(points):
                kw = keyword_of(p)
                internal = 1 if (kw and kw in hallu_text) else 0
                claims.append({
                    "claim_id": f"{r['case_id']}::p{i}",
                    "case_id": r["case_id"],
                    "topic": r["topic"],
                    "core_point": p,
                    "snippet": make_snippet(full_text, p),
                    "internal_verdict": internal,
                })

    os.makedirs(os.path.dirname(out_csv) or ".", exist_ok=True)
    with open(out_csv, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        header = ["claim_id", "case_id", "topic", "core_point", "snippet",
                  "internal_verdict", "human1_verdict", "human2_verdict", "note"]
        w.writerow(header)
        for c in claims:
            w.writerow([c["claim_id"], c["case_id"], c["topic"], c["core_point"],
                        c["snippet"].replace("\n", " "),
                        "" if blind else c["internal_verdict"], "", "", ""])
    if out_jsonl:
        with open(out_jsonl, "w", encoding="utf-8") as f:
            for c in claims:
                f.write(json.dumps(c, ensure_ascii=False) + "\n")
    print(f"[DONE] 导出 {len(claims)} 条待判定单元 -> {out_csv}")
    if out_jsonl:
        print(f"       机器可读副本 -> {out_jsonl}")


# ────────────────────────── 外部判定 ──────────────────────────

def judge(claims_path, out_path, endpoint, model, api_key, sleep=0.5, retries=3):
    done = set()
    if os.path.exists(out_path):
        with open(out_path, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip():
                    done.add(json.loads(line)["claim_id"])

    claims = [json.loads(l) for l in open(claims_path, encoding="utf-8") if l.strip()]
    pending = [c for c in claims if c["claim_id"] not in done]
    print(f"[INFO] 共 {len(claims)} 条，待判定 {len(pending)} 条（模型 {model}）")

    with open(out_path, "a", encoding="utf-8") as out:
        for i, c in enumerate(pending, 1):
            user = (f"Topic: {c['topic']}\nKnowledge point: {c['core_point']}\n\n"
                    f"Excerpt:\n{c['snippet']}")
            payload = json.dumps({
                "model": model,
                "messages": [{"role": "system", "content": JUDGE_SYSTEM},
                             {"role": "user", "content": user}],
                "temperature": 0,
            }).encode("utf-8")
            verdict = None
            for attempt in range(retries):
                try:
                    req = urlreq.Request(endpoint, data=payload,
                                         headers={"Content-Type": "application/json",
                                                  "Authorization": f"Bearer {api_key}"},
                                         method="POST")
                    with urlreq.urlopen(req, timeout=120) as resp:
                        body = json.loads(resp.read().decode("utf-8"))
                    content = body["choices"][0]["message"]["content"]
                    m = re.search(r"\{.*\}", content, re.S)
                    parsed = json.loads(m.group(0)) if m else {}
                    verdict = 1 if str(parsed.get("verdict", "")).lower().startswith("halluc") else 0
                    out.write(json.dumps({"claim_id": c["claim_id"], "verdict": verdict,
                                          "model": model, "reason": parsed.get("reason", ""),
                                          "evidence": parsed.get("evidence", "")},
                                         ensure_ascii=False) + "\n")
                    out.flush()
                    break
                except Exception as e:  # noqa: BLE001
                    print(f"  [retry {attempt + 1}] {c['claim_id']}: {type(e).__name__}: {e}")
                    time.sleep(3 * (2 ** attempt))
            if i % 20 == 0:
                print(f"  进度 {i}/{len(pending)}")
            time.sleep(sleep)
    print(f"[DONE] 外部判定写入 {out_path}")


# ────────────────────────── 一致性统计 ──────────────────────────

def percent_agreement(a, b):
    return sum(1 for x, y in zip(a, b) if x == y) / len(a)


def cohens_kappa(a, b):
    n = len(a)
    po = percent_agreement(a, b)
    pa = sum(a) / n
    pb = sum(b) / n
    pe = pa * pb + (1 - pa) * (1 - pb)
    return (po - pe) / (1 - pe) if pe < 1 else 1.0


def krippendorff_alpha(units):
    """Krippendorff's alpha（nominal）。units: list of list[int]，每 unit 可含 2+ 个评分。"""
    vals = sorted({v for u in units for v in u})
    idx = {v: i for i, v in enumerate(vals)}
    k = len(vals)
    o = [[0.0] * k for _ in range(k)]
    for u in units:
        m = len(u)
        if m < 2:
            continue
        for i in range(m):
            for j in range(m):
                if i == j:
                    continue
                o[idx[u[i]]][idx[u[j]]] += 1.0 / (m - 1)
    n = sum(sum(row) for row in o)
    if n == 0:
        return float("nan")
    do = sum(o[i][j] for i in range(k) for j in range(k) if i != j) / n
    n_c = [sum(o[i]) for i in range(k)]
    de = sum(n_c[i] * (n - n_c[i]) for i in range(k)) / (n * (n - 1)) if n > 1 else 0.0
    return 1 - do / de if de > 0 else float("nan")


def agree(claims_path, external_path, human_csv, latex=False):
    claims = {c["claim_id"]: c for c in
              (json.loads(l) for l in open(claims_path, encoding="utf-8") if l.strip())}
    ext = {}
    if external_path and os.path.exists(external_path):
        ext = {r["claim_id"]: r["verdict"] for r in
               (json.loads(l) for l in open(external_path, encoding="utf-8") if l.strip())}
    hum1, hum2 = {}, {}
    if human_csv and os.path.exists(human_csv):
        with open(human_csv, "r", encoding="utf-8-sig") as f:
            for row in csv.DictReader(f):
                v1 = row.get("human1_verdict", "").strip()
                v2 = row.get("human2_verdict", "").strip()
                if v1 != "":
                    hum1[row["claim_id"]] = 1 if str(v1).lower() not in ("0", "correct", "no", "false") else 0
                if v2 != "":
                    hum2[row["claim_id"]] = 1 if str(v2).lower() not in ("0", "correct", "no", "false") else 0

    def pairs(d1, d2):
        ks = sorted(set(d1) & set(d2))
        return [d1[k] for k in ks], [d2[k] for k in ks], ks

    results = []
    def add(name, d1, d2):
        if not d1 or not d2:
            results.append((name, None))
            return
        a, b, ks = pairs(d1, d2)
        if len(a) < 2:
            results.append((name, None))
            return
        results.append((name, {
            "n": len(a),
            "agreement": percent_agreement(a, b),
            "kappa": cohens_kappa(a, b),
            "alpha": krippendorff_alpha([[x, y] for x, y in zip(a, b)]),
        }))

    internal = {k: c["internal_verdict"] for k, c in claims.items()}
    add("Internal vs. human", internal, hum1)
    add("External vs. human", ext, hum1)
    add("Internal vs. external", internal, ext)
    add("Human--human", hum1, hum2)

    print("\n===== 幻觉判定一致性（RQ4）=====")
    for name, r in results:
        if r is None:
            print(f"  {name:<24} 数据不足，跳过")
            continue
        print(f"  {name:<24} n={r['n']:<5} agreement={r['agreement']:.1%}  "
              f"kappa={r['kappa']:.3f}  alpha={r['alpha']:.3f}")
        if latex:
            print(f"    {name} & {r['n']} & {r['agreement']:.1%} & {r['kappa']:.2f} \\\\")

    # 以人工为锚重算幻觉率
    if hum1 and internal:
        ks = sorted(set(hum1) & set(internal))
        print(f"\n  人工锚定幻觉率        : {sum(hum1[k] for k in ks) / len(ks):.2%} (n={len(ks)})")
        print(f"  系统内部幻觉率(同集合) : {sum(internal[k] for k in ks) / len(ks):.2%}")
        if ext:
            ks2 = sorted(set(hum1) & set(ext))
            print(f"  外部判定幻觉率        : {sum(ext[k] for k in ks2) / len(ks2):.2%} (n={len(ks2)})")
        print("\n  提示：若内部率显著低于人工锚定率，说明验证器存在漏检，"
              "论文中应以人工锚定值为主、内部值为辅。")


def main():
    ap = argparse.ArgumentParser(description="验证有效性检验")
    sub = ap.add_subparsers(dest="cmd", required=True)

    p1 = sub.add_parser("export", help="导出待判定单元")
    p1.add_argument("--from", dest="src", required=True)
    p1.add_argument("--out", required=True, help="人工标注 CSV")
    p1.add_argument("--jsonl", help="同时导出机器可读 JSONL")
    p1.add_argument("--no-blind", action="store_true", help="保留系统内部判定（会引入锚定效应）")
    p1.add_argument("--max-per-case", type=int)

    p2 = sub.add_parser("judge", help="异构模型外部判定")
    p2.add_argument("--claims", required=True)
    p2.add_argument("--out", required=True)
    p2.add_argument("--endpoint", required=True)
    p2.add_argument("--model", required=True)
    p2.add_argument("--api-key-env", default="JUDGE_API_KEY")

    p3 = sub.add_parser("agree", help="三方一致性统计")
    p3.add_argument("--claims", required=True)
    p3.add_argument("--external")
    p3.add_argument("--human")
    p3.add_argument("--latex", action="store_true")

    args = ap.parse_args()

    if args.cmd == "export":
        out_jsonl = args.jsonl or os.path.splitext(args.out)[0] + ".jsonl"
        export_claims(args.src, args.out, out_jsonl, blind=not args.no_blind,
                      max_per_case=args.max_per_case)
    elif args.cmd == "judge":
        key = os.environ.get(args.api_key_env, "")
        if not key:
            sys.exit(f"[ERROR] 环境变量 {args.api_key_env} 未设置")
        judge(args.claims, args.out, args.endpoint, args.model, key)
    elif args.cmd == "agree":
        agree(args.claims, args.external, args.human, args.latex)


if __name__ == "__main__":
    main()
