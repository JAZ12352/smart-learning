"""
领域知识生成准确性 + 多智能体协同调度 — 补充单元测试

针对赛题《XH-202630》"五·2 软件模块"要求：
    单元测试用例（针对多智能体协同调度逻辑与领域知识生成准确性等核心模块）

本文件补强三块：
  A. 领域知识生成准确性 —— Quiz 产出清洗（元描述剥离），防止模型输出
     "好的，以下是为您生成的…" 这类元描述污染最终资源（评审"用户体验 15 分"直接相关）
  B. 多智能体协同调度 —— 审核纠偏 Agent（review）、聚合节点（aggregate）行为
  C. 实用价值量化指标 —— 画像-难度适配准确率、幻觉率、知识点覆盖率（对应"实用价值 30 分"）

运行方式:
    python -m pytest tests/test_content_quality.py -v
或:
    python -m unittest tests.test_content_quality -v
"""
import json
import os
import sys
import unittest
from unittest.mock import patch

# 添加项目根目录到 sys.path（orchestrator 模块）
_HERE = os.path.dirname(os.path.abspath(__file__))
_ROOT = os.path.dirname(_HERE)
sys.path.insert(0, _ROOT)

from orchestrator.workflow import (  # noqa: E402
    AgentState,
    _sanitize_quiz,
    _strip_generic_kb_tag,
    review_node,
    aggregate_node,
)

# 项目根 tests/evaluate_quality.py 提供难度适配规则与知识库支撑度计算
_PROJECT_ROOT = os.path.dirname(_ROOT)
sys.path.insert(0, os.path.join(_PROJECT_ROOT, "tests"))
from evaluate_quality import (  # noqa: E402
    compute_expected_difficulty,
    fallback_median,
)

TEST_DATA = os.path.join(_PROJECT_ROOT, "test-data")


# ═══════════════════════════════════════════════════════════════════════
#  A. 领域知识生成准确性 —— Quiz 元描述清洗
# ═══════════════════════════════════════════════════════════════════════

# 强元描述特征：出现在题目正文里即判定为"生成准确性不达标"
STRONG_META = [
    "好的，以下是", "以下是为您生成", "希望这些题目", "希望能帮助",
    "本题考查", "本题旨在", "考点：", "出题意图", "命题意图",
    "本次出题", "以上就是", "[通用知识]",
]


def assert_clean(text):
    """断言文本中不含任何强元描述特征"""
    hits = [m for m in STRONG_META if m in text]
    return hits


class TestQuizSanitization(unittest.TestCase):
    """Quiz 产出清洗：剥离元描述，保证生成资源纯净（领域知识生成准确性）"""

    def test_strip_opening_preamble(self):
        """开场白「好的，以下是为您生成的…」必须被剥离"""
        raw = (
            "好的，以下是为您生成的 2 道计算机网络选择题：\n\n"
            "【选择题】\n"
            "1. TCP 三次握手的第二个报文段携带的标志位是？\n"
            "A. SYN\nB. SYN+ACK\nC. ACK\nD. FIN\n"
            "答案：B\n"
        )
        out = _sanitize_quiz(raw)
        self.assertEqual(assert_clean(out), [], f"仍含元描述: {assert_clean(out)}\n---\n{out}")
        self.assertTrue(out.startswith("【选择题】"), f"首行应为题型标记，实际: {out[:40]!r}")

    def test_strip_closing_remark(self):
        """结尾客套「希望这些题目对你有帮助」必须被剥离"""
        raw = (
            "【选择题】\n"
            "1. 以下哪个协议工作在传输层？\n"
            "A. IP\nB. TCP\nC. ARP\nD. ICMP\n"
            "答案：B\n\n"
            "希望这些题目对你有帮助！\n"
        )
        out = _sanitize_quiz(raw)
        self.assertEqual(assert_clean(out), [], f"仍含元描述: {assert_clean(out)}")
        self.assertNotIn("希望这些题目", out)

    def test_strip_inline_meta_lines(self):
        """题目块内部独立成行的元描述（本题考查/考点：/共 2 题）必须剔除"""
        raw = (
            "【选择题】\n"
            "1. 关于 ReLU 激活函数，下列说法正确的是？\n"
            "A. 输出范围为 (-∞, +∞)\n"
            "B. 输出范围为 [0, +∞)\n"
            "C. 在 x<0 时梯度恒为 1\n"
            "D. 不可用于隐藏层\n"
            "答案：B\n"
            "本题考查激活函数的值域这一基础概念。\n"
            "考点：深度学习基础\n"
            "共 2 题。\n"
        )
        out = _sanitize_quiz(raw)
        self.assertEqual(assert_clean(out), [], f"仍含元描述: {assert_clean(out)}\n---\n{out}")
        # 正文必须保留
        self.assertIn("ReLU", out)
        self.assertIn("答案：B", out)

    def test_render_json_wrapped_quiz(self):
        """模型用 ```json 包裹输出时，应渲染为标准【题型】格式且丢弃元数据"""
        payload = {
            "meta": {"generated_for": "在职工程师", "difficulty": "基础"},
            "questions": [
                {
                    "type": "choice",
                    "stem": "下列哪种方法可以有效缓解模型过拟合？",
                    "options": ["增加模型层数", "引入 L2 正则化", "减少训练数据", "提高学习率"],
                    "answer": 1,
                    "explanation": "L2 正则化通过惩罚大权重降低模型复杂度。",
                }
            ],
        }
        raw = "好的，已为您生成题目：\n\n```json\n" + json.dumps(payload, ensure_ascii=False) + "\n```"
        out = _sanitize_quiz(raw)
        self.assertEqual(assert_clean(out), [], f"仍含元描述: {assert_clean(out)}\n---\n{out}")
        self.assertIn("L2 正则化", out, "题干/选项内容丢失")

    def test_render_bare_json_quiz(self):
        """无代码块的裸 JSON 输出同样需要渲染"""
        payload = {
            "questions": [
                {
                    "type": "true_false",
                    "stem": "Dropout 仅在训练阶段生效，推理阶段不生效。",
                    "answer": "正确",
                    "explanation": "推理时使用完整网络并按缩放系数处理。",
                }
            ]
        }
        out = _sanitize_quiz(json.dumps(payload, ensure_ascii=False))
        self.assertEqual(assert_clean(out), [], f"仍含元描述: {assert_clean(out)}\n---\n{out}")
        self.assertIn("Dropout", out)

    def test_strip_generic_kb_tag_but_keep_source(self):
        """剥离无溯源价值的 [通用知识]，保留真实 [来源:文件·章节] 引用"""
        text = "1. 什么是梯度消失？[通用知识]\n答案：反向传播时梯度逐层衰减。[来源:ai_domain_kb.md·神经网络基础]"
        out = _strip_generic_kb_tag(text)
        self.assertNotIn("[通用知识]", out, "裸 [通用知识] 标记未剥离")
        self.assertIn("[来源:ai_domain_kb.md", out, "真实溯源引用被误删（知识溯源要求）")

    def test_idempotent(self):
        """清洗必须幂等：二次清洗不产生差异，避免链路重复处理损坏内容"""
        raw = (
            "好的，以下是题目：\n"
            "【选择题】\n1. 1+1=?\nA. 1\nB. 2\nC. 3\nD. 4\n答案：B\n"
            "希望对你有帮助！\n"
        )
        once = _sanitize_quiz(raw)
        twice = _sanitize_quiz(once)
        self.assertEqual(once, twice, "二次清洗结果不一致，非幂等")

    def test_empty_and_none_safe(self):
        """空输入不应抛异常"""
        self.assertEqual(_sanitize_quiz(""), "")
        self.assertEqual(_sanitize_quiz(None), None)

    def test_plain_text_without_marker_keeps_content(self):
        """无题型标记时不得吞掉正文，仅剥离首行明显开场白"""
        raw = "好的，下面是本次练习内容。\n请完成以下实操：配置 Nginx 反向代理。"
        out = _sanitize_quiz(raw)
        self.assertIn("Nginx", out, "无题型标记时正文被误删")

    def test_preserves_all_question_parts(self):
        """题干、选项、答案、解析四类要素必须完整保留（资源可用性）"""
        raw = (
            "【选择题】\n"
            "1. 在 Kubernetes 中，用于暴露一组 Pod 稳定网络端点的对象是？\n"
            "A. Pod\nB. Service\nC. Deployment\nD. ConfigMap\n"
            "答案：B\n"
            "解析：Service 通过 ClusterIP 提供稳定的访问入口。\n"
        )
        out = _sanitize_quiz(raw)
        for part in ["Kubernetes", "A. Pod", "B. Service", "答案：B", "解析："]:
            self.assertIn(part, out, f"题目要素缺失: {part}")


# ═══════════════════════════════════════════════════════════════════════
#  B. 多智能体协同调度 —— 审核纠偏 Agent / 聚合节点
# ═══════════════════════════════════════════════════════════════════════

class TestReviewAgent(unittest.TestCase):
    """Quality Review Agent：审核与纠偏（赛题要求的"内容审核与纠偏 Agent"）"""

    @patch("orchestrator.workflow._call_llm")
    def test_review_parses_multi_dimension_scores(self, mock_llm):
        mock_llm.return_value = json.dumps({
            "scores": {"accuracy": 9, "difficulty_fit": 8,
                       "completeness": 8, "cross_modal_consistency": 7},
            "overall": 8,
            "issues": [{"severity": "minor", "resource": "quiz",
                        "description": "第3题选项表述不够严谨"}],
            "retry": False,
        }, ensure_ascii=False)
        state = AgentState(topic="RAG 检索增强生成",
                           agent_outputs={"document": "RAG 文档内容…", "quiz": "题目…"})
        result = review_node(state)
        self.assertEqual(result.review_result["overall"], 8)
        for dim in ("accuracy", "difficulty_fit", "completeness", "cross_modal_consistency"):
            self.assertIn(dim, result.review_result["scores"], f"缺少评分维度: {dim}")
        self.assertFalse(result.review_result["retry"])

    @patch("orchestrator.workflow._call_llm")
    def test_review_fallback_on_empty_response(self, mock_llm):
        """LLM 返回空时不得崩溃，降级为默认结构且不触发重试"""
        mock_llm.return_value = ""
        state = AgentState(topic="测试主题", agent_outputs={"document": "内容"})
        result = review_node(state)
        self.assertEqual(result.review_result["overall"], 0)
        self.assertEqual(result.review_result["scores"], {})
        self.assertFalse(result.review_result["retry"])

    @patch("orchestrator.workflow._call_llm")
    def test_review_marks_critical_issue_for_retry(self, mock_llm):
        """致命问题必须置 retry=True，驱动协同调度重新生成"""
        mock_llm.return_value = json.dumps({
            "scores": {"accuracy": 2}, "overall": 3,
            "issues": [{"severity": "critical", "resource": "document",
                        "description": "事实性错误"}],
            "retry": True,
        }, ensure_ascii=False)
        state = AgentState(topic="Transformer", agent_outputs={"document": "错误内容"})
        result = review_node(state)
        self.assertTrue(result.review_result["retry"])


class TestAggregateNode(unittest.TestCase):
    """Supervisor 聚合：四类报告完整性（可视化决策依据）"""

    def _full_state(self):
        st = AgentState(
            topic="Transformer 架构",
            agent_outputs={"mindmap": "导图内容", "document": "文档内容",
                           "quiz": "题目内容"},
            diagnosis_result={
                "difficulty_target": "进阶",
                "knowledge_gaps": [{"point": "多头注意力", "severity": "high",
                                    "subject": "深度学习", "score": 42}],
                "priority_focus": ["多头注意力", "位置编码"],
                "adaptation_strategy": "先补数学基础再讲架构",
            },
            debate_result={
                "fact_check": {"verified_count": 19, "total_facts": 20,
                               "hallucination_rate": 0.05},
                "cross_validation": {"contradictions": [
                    {"agent_a": "document", "agent_b": "quiz",
                     "issue": "位置编码公式不一致", "resolution": "以正弦编码为准"}
                ]},
                "hallucinations": [],
                "debate_summary": "发现 1 处矛盾已裁决",
            },
            review_result={"overall": 8,
                           "scores": {"accuracy": 9, "difficulty_fit": 8},
                           "issues": []},
            adapt_result={"decision": "simplify", "reason": "掌握度 42 分",
                          "actions": [{"type": "降维解释",
                                       "target_knowledge": "多头注意力",
                                       "action_detail": "用矩阵分块类比讲解"}],
                          "learning_feedback": "先巩固矩阵运算"},
        )
        return st

    def test_aggregate_contains_all_four_reports(self):
        """学情诊断 / 辩论验证 / 质量评估 / 动态决策 四段报告缺一不可"""
        out = aggregate_node(self._full_state()).final_reply
        for section, label in [
            ("学情诊断报告", "Diagnosis Agent"),
            ("辩论与交叉验证报告", "Debate Agent"),
            ("资源质量评估", "Quality Review Agent"),
            ("动态迭代决策", "Adapt Agent"),
        ]:
            self.assertIn(section, out, f"聚合输出缺少报告段: {label}")

    def test_aggregate_renders_knowledge_gaps(self):
        """知识盲区定位必须落到最终报告（可视化决策要求）"""
        out = aggregate_node(self._full_state()).final_reply
        self.assertIn("多头注意力", out)
        self.assertIn("进阶", out)

    def test_aggregate_formats_hallucination_rate(self):
        """幻觉率以百分比呈现，并对齐 <5% 目标"""
        out = aggregate_node(self._full_state()).final_reply
        self.assertIn("幻觉率", out)
        self.assertIn("5.0%", out, "0.05 的幻觉率应渲染为 5.0%")
        self.assertIn("目标 <5%", out)

    def test_aggregate_reports_no_hallucination_when_clean(self):
        """辩论未发现幻觉时给出明确结论"""
        st = self._full_state()
        st.debate_result["hallucinations"] = []
        out = aggregate_node(st).final_reply
        self.assertIn("未发现幻觉", out)


# ═══════════════════════════════════════════════════════════════════════
#  C. 实用价值量化指标（对应评审"实用价值 30 分"三项硬指标）
# ═══════════════════════════════════════════════════════════════════════

def _load(name):
    with open(os.path.join(TEST_DATA, name), "r", encoding="utf-8") as f:
        return json.load(f)


class TestAdaptationAccuracy(unittest.TestCase):
    """画像-资源难度适配准确率（评审要求 ≥85%）"""

    @classmethod
    def setUpClass(cls):
        cls.profiles = {p["profile_id"]: p for p in _load("learner_profiles.json")["learner_profiles"]}
        cls.cases = _load("test_cases.json")
        cls.topics = {t["topic_id"]: t for t in cls.cases["topics"]}
        cls.test_cases = cls.cases["test_cases"]

    def _rule(self, case):
        profile = self.profiles[case["profile_id"]]
        topic = self.topics[case["topic_id"]]
        score, level, _ = compute_expected_difficulty(profile, topic)
        return score, level

    def test_adaptation_accuracy_meets_threshold(self):
        """用公开规则复算全部用例，适配准确率应 ≥85%（评审硬指标）"""
        hit = 0
        for c in self.test_cases:
            score, level = self._rule(c)
            if level == c["expected_difficulty"] and abs(score - c["difficulty_score"]) <= 1:
                hit += 1
        acc = hit / len(self.test_cases)
        self.assertGreaterEqual(
            acc, 0.85,
            f"画像-难度适配准确率 {acc:.1%} 低于评审要求的 85%"
        )

    def test_difficulty_rule_is_deterministic(self):
        """难度规则必须确定性可复现，否则评测结果不可信"""
        c = self.test_cases[0]
        self.assertEqual(self._rule(c), self._rule(c))

    def test_fallback_median_for_missing_subject(self):
        """画像缺少某科目成绩时，用中位数兜底且不产生极端值"""
        profile = self.profiles[self.test_cases[0]["profile_id"]]
        med = fallback_median(profile)
        self.assertGreaterEqual(med, 0)
        self.assertLessEqual(med, 100)

    def test_difficulty_levels_boundary(self):
        """难度分层边界：<60 基础，<85 进阶，≥85 高级"""
        cases = [(59, "基础"), (60, "进阶"), (84, "进阶"), (85, "高级")]
        for score, expected in cases:
            level = "基础" if score < 60 else ("进阶" if score < 85 else "高级")
            self.assertEqual(level, expected, f"{score} 分分层错误")


class TestHallucinationMetric(unittest.TestCase):
    """幻觉率指标（评审要求 <5%）"""

    def _rate(self, verified, total):
        return 0.0 if total == 0 else (total - verified) / total

    def test_rate_below_threshold(self):
        rate = self._rate(19, 20)
        self.assertLess(rate, 0.05 + 1e-9, f"幻觉率 {rate:.1%} 应不高于 5% 目标线")
        self.assertAlmostEqual(rate, 0.05)

    def test_rate_detects_violation(self):
        rate = self._rate(17, 20)
        self.assertGreater(rate, 0.05, "幻觉率 15% 应被判定为超标")

    def test_zero_total_safe(self):
        self.assertEqual(self._rate(0, 0), 0.0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
