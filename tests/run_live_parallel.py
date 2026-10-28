"""
在线端到端评测 · 并发采集器
==========================
用途：以受控并发调用 multi-agent 服务跑完整闭环，替代
`evaluate_quality.py --live-all` 的串行执行（单条约 8-9 分钟，61 条串行需约 9 小时）。

特性
----
1. 线程池并发（默认 4），显著降低墙钟时间；
2. 失败自动重试（指数退避），对 DeepSeek 429/超时友好；
3. 断点续跑：结果逐条追加写入 JSONL，重跑自动跳过已完成用例；
4. 复用 evaluate_quality.py 的指标口径，产出同样的
   test-data/eval_report.json 与 docs/量化评测报告.md。

用法
----
  # 先启动 multi-agent 服务（8100）
  python tests/run_live_parallel.py --all --concurrency 4
  python tests/run_live_parallel.py --n 12 --concurrency 3
  python tests/run_live_parallel.py --all --out test-data/live_results_v2.jsonl
"""
import argparse
import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tests"))

import evaluate_quality as eq  # noqa: E402

DEFAULT_OUT = os.path.join(ROOT, "test-data", "live_results.jsonl")


def load_done(path):
    done = {}
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    r = json.loads(line)
                    done[r["case_id"]] = r
                except json.JSONDecodeError:
                    continue
    return done


def live_run_case_full(case, profile, topic, timeout=1500, ablation=None):
    """与 eq.live_run_case 指标口径完全一致，但额外保留 debate/review/plan 明细。

    保留明细是「外部交叉验证」与「人工标注」的前提：只有拿到 Debate Agent
    逐条 claim 的判定，才能把它与人工/异构模型的判定做一致性比较（论文 RQ4）。
    """
    from urllib import request as urlreq
    payload = json.dumps({
        "topic": topic["name"],
        # 传完整画像（含 modifiers 定标修正项），与 evaluate_quality.py 口径一致
        "context": {"student_profile": profile, "domain": "人工智能"},
        "request_id": case["case_id"],
        "ablation": ablation or {},
    }).encode("utf-8")
    req = urlreq.Request(
        f"{eq.MA_URL}/api/v1/collaboration/run",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urlreq.urlopen(req, timeout=timeout) as resp:
        body = json.loads(resp.read().decode("utf-8"))

    plan = body.get("plan") or {}
    system_difficulty = plan.get("difficulty", "")
    outputs = body.get("outputs") or {}
    doc_text = " ".join(str(v) for v in outputs.values())

    covered, missed = [], []
    for point in topic["core_points"]:
        kws = eq.extract_keywords(point)
        if any(kw in doc_text for kw in kws):
            covered.append(point)
        else:
            missed.append(point)

    debate = body.get("debate") or {}
    review = body.get("review") or {}
    review_scores = review.get("scores") or {}
    hallucination_rate = debate.get("hallucination_rate")
    # 消融组（no_debate）不自报幻觉率，保持 None，交由外部判定器统一测量
    if hallucination_rate is None and not debate.get("skipped"):
        n_hallu = len(debate.get("hallucinations") or [])
        n_points = max(len(topic["core_points"]), 1)
        hallucination_rate = round(n_hallu / n_points, 4)

    result = {
        "case_id": case["case_id"],
        "profile_id": case["profile_id"],
        "topic": topic["name"],
        "expected_difficulty": case["expected_difficulty"],
        "system_difficulty": system_difficulty,
        "difficulty_match": system_difficulty == case["expected_difficulty"],
        "coverage": {
            "covered": len(covered),
            "total": len(topic["core_points"]),
            "rate": round(len(covered) / len(topic["core_points"]), 4),
            "missed": missed,
        },
        "hallucination_rate": hallucination_rate,
        "hallucination_skipped": bool(debate.get("skipped")),
        "ablation": ablation or {},
        "debate_prevented": debate.get("hallucination_prevented"),
        "review_scores": {k: review_scores.get(k) for k in
                          ("accuracy", "difficulty_fit", "completeness", "cross_modal_consistency")
                          if k in review_scores},
        "review_overall": review.get("overall"),
        "agents_planned": plan.get("agents") or [],
    }
    result["_raw"] = {
        "plan": plan,
        "debate": {
            "hallucinations": debate.get("hallucinations") or [],
            "fact_check": debate.get("fact_check") or {},
            "cross_validation": debate.get("cross_validation") or {},
            "hallucination_rate": debate.get("hallucination_rate"),
            "debate_summary": debate.get("debate_summary", ""),
        },
        "review": {
            "scores": review.get("scores") or {},
            "overall": review.get("overall"),
            "issues": review.get("issues") or [],
        },
        "covered_points": covered,
        "missed_points": missed,
        "outputs_char_len": {k: len(str(v)) for k, v in outputs.items()},
        # 保存全文供外部判定 / 人工标注取片段（约 +40KB/条，可用 --no-raw 关闭）
        "outputs": {k: str(v) for k, v in outputs.items()},
    }
    return result


def run_one(case, profiles, topics, retries=3, save_raw=True, ablation=None):
    """单条用例，带指数退避重试。"""
    last_err = None
    for attempt in range(retries):
        try:
            if save_raw:
                r = live_run_case_full(case, profiles[case["profile_id"]],
                                       topics[case["topic_id"]], ablation=ablation)
            else:
                r = eq.live_run_case(case, profiles[case["profile_id"]], topics[case["topic_id"]])
                r["ablation"] = ablation or {}
            r["_attempt"] = attempt + 1
            return r, None
        except Exception as e:  # noqa: BLE001 - 采集阶段不因单条失败中断
            last_err = e
            wait = 5 * (2 ** attempt)
            print(f"  [retry {attempt + 1}/{retries}] {case['case_id']} 失败: {type(e).__name__}: {e}; 等待 {wait}s")
            time.sleep(wait)
    return None, last_err


def main():
    ap = argparse.ArgumentParser(description="并发在线评测采集")
    ap.add_argument("--all", action="store_true", help="跑全量用例")
    ap.add_argument("--n", type=int, default=0, help="跑 N 条分层抽样用例")
    ap.add_argument("--concurrency", type=int, default=4, help="并发度")
    ap.add_argument("--out", default=DEFAULT_OUT, help="结果 JSONL 路径")
    ap.add_argument("--retries", type=int, default=3, help="单条最大重试次数")
    ap.add_argument("--no-raw", action="store_true",
                    help="不保存 debate/review 明细（省空间，但无法做外部交叉验证）")
    ap.add_argument("--no-report", action="store_true", help="只采集，不生成报告")
    ap.add_argument("--ablation", default="",
                    help="消融开关，逗号分隔，如 no_debate 或 no_rag,no_retry")
    args = ap.parse_args()

    ablation = {k: True for k in args.ablation.split(",") if k.strip()}

    if not args.all and not args.n:
        ap.error("需指定 --all 或 --n N")

    profiles, topics, cases = eq.load_profiles_and_cases()
    done = load_done(args.out)
    print(f"[INFO] 已存在结果 {len(done)} 条（断点续跑）")

    if args.all:
        subset = list(cases)
    else:
        subset = eq.pick_live_subset(cases, args.n)

    pending = [c for c in subset if c["case_id"] not in done]
    print(f"[INFO] 目标 {len(subset)} 条，待跑 {len(pending)} 条，并发 {args.concurrency}")

    results = list(done.values())
    t0 = time.time()
    if pending:
        with ThreadPoolExecutor(max_workers=args.concurrency) as ex:
            futures = {ex.submit(run_one, c, profiles, topics, args.retries,
                                 not args.no_raw, ablation): c for c in pending}
            for i, fut in enumerate(as_completed(futures), 1):
                case = futures[fut]
                r, err = fut.result()
                if r is None:
                    print(f"  [{i}/{len(pending)}] {case['case_id']} 最终失败: {err}")
                    continue
                with open(args.out, "a", encoding="utf-8") as f:
                    f.write(json.dumps(r, ensure_ascii=False) + "\n")
                results.append(r)
                elapsed = (time.time() - t0) / 60
                print(f"  [{i}/{len(pending)}] {case['case_id']} 期望={r['expected_difficulty']} "
                      f"系统={r['system_difficulty']} 覆盖={r['coverage']['covered']}/{r['coverage']['total']} "
                      f"幻觉率={r['hallucination_rate']:.2%} | 已用 {elapsed:.1f} 分钟")

    print(f"\n[INFO] 采集完成，共 {len(results)} 条，用时 {(time.time() - t0) / 60:.1f} 分钟")

    if args.no_report:
        return

    # ── 汇总指标（口径与 evaluate_quality.py 完全一致）──
    ok, _bad = eq.self_check_cases(profiles, topics, cases)
    kb = eq.kb_grounding_metrics(topics)
    matched = sum(1 for r in results if r["difficulty_match"])
    cov_covered = sum(r["coverage"]["covered"] for r in results)
    cov_total = sum(r["coverage"]["total"] for r in results)
    hall_rates = [r["hallucination_rate"] for r in results
                  if r.get("hallucination_rate") is not None]
    hall_sum = sum(hall_rates)
    n_hall = len(hall_rates)
    confusion = {"基础": {}, "进阶": {}, "高级": {}}
    for r in results:
        confusion[r["expected_difficulty"]][r["system_difficulty"] or "?"] = \
            confusion[r["expected_difficulty"]].get(r["system_difficulty"] or "?", 0) + 1
    n = len(results) or 1

    kb_files = sorted(os.listdir(os.path.join(ROOT, "multi-agent", "knowledge_base")))
    report = {
        "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "concurrency": args.concurrency,
        "scale": {
            "profiles": len(profiles), "topics": len(topics), "cases": len(cases),
            "kb_files": len(kb_files), "kb_chunks": eq.get_kb_collection().count(),
        },
        "dataset_self_check": {"consistent": ok, "total": len(cases),
                               "consistency": round(ok / len(cases), 4)},
        "kb_grounding": kb,
        "metrics": {
            "adaptation": {"accuracy": round(matched / n, 4), "matched": matched,
                           "total": len(results), "confusion": confusion},
            "coverage": {"covered": cov_covered, "total": cov_total,
                         "overall_rate": round(cov_covered / cov_total, 4) if cov_total else 0.0},
            "hallucination": {"rate": round(hall_sum / n_hall, 4) if n_hall else None,
                              "n_measured": n_hall, "n_total": len(results),
                              "note": "各用例幻觉率的均值（Debate Agent 判定）；"
                                      "消融组不自报，需外部判定器统一测量"},
        },
        "live_results": results,
    }
    tag = "_".join(sorted(ablation)) or "full"

    # ── 安全闸门：失败/空跑绝不允许覆盖已发布的正式报告 ──
    # 教训：曾因密钥欠费导致 61 条全失败（results=[]），但报告仍以 "full" 标签
    # 直接覆盖了 docs/量化评测报告.md，把此前的真实数值（如 33.3%/72.2%）清零。
    # 规则：正式口径（tag=full）且采集结果不足 90% 时，只写 *_FAILED 旁路文件，
    # 保留既有报告不动，并打印醒目提示，由人工确认后决定是否替换。
    expected_n = len(subset)
    _full_ok = (len(results) >= max(1, int(expected_n * 0.9)))
    if tag == "full" and not _full_ok:
        report_path = os.path.join(ROOT, "test-data", f"eval_report_{tag}_FAILED.json")
        md_path = os.path.join(ROOT, "docs", f"量化评测报告_{tag}_FAILED.md")
        print(f"\n[!! 安全闸门] 本次仅采集 {len(results)}/{expected_n} 条，未达 90% 阈值，"
              f"已拒绝覆盖正式报告。\n"
              f"     正式报告 docs/量化评测报告.md 与 test-data/eval_report_full.json 保持不变。\n"
              f"     本次结果写入旁路文件：{md_path}\n"
              f"     请排查失败原因后重跑，通过闸门后才会更新正式报告。")
    else:
        report_path = os.path.join(ROOT, "test-data", f"eval_report_{tag}.json")
        md_path = (os.path.join(ROOT, "docs", "量化评测报告.md") if tag == "full"
                   else os.path.join(ROOT, "docs", f"量化评测报告_{tag}.md"))

    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    with open(md_path, "w", encoding="utf-8") as f:
        f.write(eq.render_md(report))

    m = report["metrics"]
    print("\n===== 核心指标 =====")
    print(f"  画像-难度适配准确率 : {m['adaptation']['accuracy']:.1%}  (n={m['adaptation']['total']})")
    print(f"  核心知识点覆盖率   : {m['coverage']['overall_rate']:.1%}")
    hr = m["hallucination"]["rate"]
    print(f"  幻觉率             : {hr:.2%}" if hr is not None else "  幻觉率             : 未自报（消融组，需外部判定）")
    print(f"\n[DONE] 报告: {md_path}\n       {report_path}")


if __name__ == "__main__":
    main()
