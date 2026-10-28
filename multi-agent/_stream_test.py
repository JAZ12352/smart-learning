"""验证 SSE 流式路径：run_collaboration_streaming 队列事件流（模拟 main.py 的 SSE 生成器）。"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from orchestrator.workflow import run_collaboration_streaming

print(">>> 启动流式协作（队列事件流，模拟 SSE）...")
q = run_collaboration_streaming("二叉树", context={"profile": {"年级": "大二"}}, request_id="stream-001")

event_count = 0
final_seen = False
error_seen = False
while True:
    kind, payload = q.get(timeout=600)
    if kind == "done":
        print("\n[done] 队列结束")
        break
    if kind == "event":
        event_count += 1
        t = payload.get("type", "?")
        agent = payload.get("agent", "")
        dur = payload.get("durationMs", "")
        print(f"  event#{event_count:02d} [{t:12s}] {agent:22s} {dur}")
        if t == "review_end":
            print(f"           overall={payload.get('overall')} scores={payload.get('scores')}")
    elif kind == "final":
        final_seen = True
        print(f"\n[final] reply长度={len(payload.get('reply',''))} 资源={payload.get('outputs')} review_overall={payload.get('review',{}).get('overall')}")
    elif kind == "error":
        error_seen = True
        print(f"\n[error] {payload}")

print(f"\n总计事件: {event_count}, final到达: {final_seen}, error: {error_seen}")
if final_seen and not error_seen:
    print("[SUCCESS] SSE 流式路径验证通过")
else:
    print("[FAIL] 流式路径异常")
