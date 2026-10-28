"""
多智能体协同调度逻辑与领域知识生成准确性 — 单元测试
针对 PDF 要求的核心模块进行测试：
1. 多智能体协同调度逻辑（工作流节点编排、Agent路由、重试机制）
2. 领域知识生成准确性（Agent提示词、产出格式、知识覆盖）
3. 辩论与交叉验证机制（幻觉识别、事实核验）
4. 动态迭代决策（降维/进阶触发逻辑）

运行方式: python -m pytest tests/test_workflow.py -v
或:        python -m unittest tests.test_workflow -v
"""
import json
import os
import sys
import unittest
from unittest.mock import patch, MagicMock

# 添加项目根目录到 sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from orchestrator.workflow import (
    AgentState, Intent, TaskComplexity, AgentMode,
    AGENT_PROMPTS, _AGENT_LABELS, _AGENT_ACTIONS,
    build_collaboration_workflow,
    plan_node, diagnosis_node, generate_node,
    debate_node, review_node, adapt_node, aggregate_node,
    decide_after_review, prepare_retry,
    _extract_json, _summarize, _now, _get_profile,
    classify_intent,
)


class TestAgentState(unittest.TestCase):
    """测试 AgentState 数据结构"""

    def test_default_state(self):
        state = AgentState()
        self.assertEqual(state.intent, Intent.GENERAL_CHAT)
        self.assertEqual(state.complexity, TaskComplexity.SIMPLE)
        self.assertEqual(state.mode, AgentMode.ROUTE)
        self.assertEqual(state.agent_outputs, {})
        self.assertEqual(state.diagnosis_result, {})
        self.assertEqual(state.debate_result, {})
        self.assertEqual(state.adapt_result, {})
        self.assertEqual(state.retry_count, 0)

    def test_state_with_topic(self):
        state = AgentState(topic="机器学习", request_id="test-001")
        self.assertEqual(state.topic, "机器学习")
        self.assertEqual(state.request_id, "test-001")


class TestAgentPrompts(unittest.TestCase):
    """测试 Agent 提示词完整性（领域知识生成准确性基础）"""

    REQUIRED_AGENTS = [
        "supervisor", "supervisor_plan",
        "mindmap", "document", "reading", "code", "guide", "quiz", "video",
        "review", "diagnosis", "debate", "adapt",
    ]

    def test_all_agent_prompts_exist(self):
        for agent_key in self.REQUIRED_AGENTS:
            self.assertIn(agent_key, AGENT_PROMPTS,
                          f"缺少 Agent 提示词: {agent_key}")
            self.assertTrue(len(AGENT_PROMPTS[agent_key]) > 50,
                           f"Agent 提示词过短: {agent_key}")

    def test_diagnosis_prompt_contains_required_dimensions(self):
        prompt = AGENT_PROMPTS["diagnosis"]
        self.assertIn("knowledge_gaps", prompt)
        self.assertIn("cognitive_profile", prompt)
        self.assertIn("difficulty_target", prompt)
        self.assertIn("priority_focus", prompt)
        self.assertIn("adaptation_strategy", prompt)

    def test_debate_prompt_contains_hallucination_detection(self):
        prompt = AGENT_PROMPTS["debate"]
        self.assertIn("幻觉", prompt)
        self.assertIn("交叉验证", prompt)
        self.assertIn("hallucination_rate", prompt)
        self.assertIn("事实性幻觉", prompt)
        self.assertIn("一致性幻觉", prompt)

    def test_adapt_prompt_contains_decision_rules(self):
        prompt = AGENT_PROMPTS["adapt"]
        self.assertIn("降维解释", prompt)
        self.assertIn("进阶挑战", prompt)
        self.assertIn("内容修正", prompt)
        self.assertIn("simplify", prompt)
        self.assertIn("challenge", prompt)

    def test_review_prompt_contains_quality_dimensions(self):
        prompt = AGENT_PROMPTS["review"]
        self.assertIn("accuracy", prompt)
        self.assertIn("difficulty_fit", prompt)
        self.assertIn("completeness", prompt)
        self.assertIn("cross_modal_consistency", prompt)

    def test_generate_agents_have_output_format_specs(self):
        for agent in ["mindmap", "document", "reading", "code", "quiz", "video"]:
            prompt = AGENT_PROMPTS[agent]
            self.assertTrue(
                "格式" in prompt or "输出" in prompt or "必须" in prompt,
                f"{agent} 提示词缺少输出格式规范"
            )

    def test_agent_labels_complete(self):
        for agent_key in ["mindmap", "document", "reading", "code", "guide",
                          "quiz", "video", "review", "supervisor",
                          "diagnosis", "debate", "adapt"]:
            self.assertIn(agent_key, _AGENT_LABELS)

    def test_supervisor_plan_supports_guide_agent_and_prior_knowledge(self):
        """赛题要求：先验知识画像（学历背景/理论测试结果）+ 实操指南资源形态"""
        prompt = AGENT_PROMPTS["supervisor_plan"]
        self.assertIn("guide", prompt)
        self.assertIn("学历背景", prompt)
        self.assertIn("理论测试结果", prompt)

    def test_guide_prompt_contains_operational_structure(self):
        """实操指南 Agent：分步骤流程 + 验收标准（赛题资源形态要求）"""
        prompt = AGENT_PROMPTS["guide"]
        self.assertIn("实操指南", prompt)
        self.assertIn("标准操作流程", prompt)
        self.assertIn("验收标准", prompt)
        self.assertIn("岗位", prompt)

    def test_debate_prompt_requires_kb_evidence_traceability(self):
        """高保真知识溯源：Debate 必须引用知识库来源"""
        prompt = AGENT_PROMPTS["debate"]
        self.assertIn("kb_evidence", prompt)
        self.assertIn("溯源", prompt)


class TestProfileContextNormalization(unittest.TestCase):
    """测试画像上下文键名兼容（profile / student_profile）"""

    def test_profile_key(self):
        state = AgentState(context={"profile": {"knowledgeLevel": 75}})
        self.assertEqual(_get_profile(state), {"knowledgeLevel": 75})

    def test_student_profile_key(self):
        state = AgentState(context={"student_profile": {"理论测试结果": {"Python": 65}}})
        self.assertEqual(_get_profile(state)["理论测试结果"]["Python"], 65)

    def test_missing_profile_returns_empty_dict(self):
        self.assertEqual(_get_profile(AgentState(context={})), {})
        self.assertEqual(_get_profile(AgentState()), {})

    def test_diagnosis_accepts_student_profile_key(self):
        with patch('orchestrator.workflow._call_llm', return_value=""):
            state = AgentState(
                topic="RAG",
                context={"student_profile": {"学历背景": "在职工程师", "理论测试结果": {"Python": 65}}},
            )
            result = diagnosis_node(state)
            self.assertIn("difficulty_target", result.diagnosis_result)

    def test_agent_actions_describe_behavior(self):
        for agent_key, action in _AGENT_ACTIONS.items():
            self.assertTrue(len(action) > 5, f"{agent_key} 动作描述过短")


class TestWorkflowGraph(unittest.TestCase):
    """测试多智能体协同调度逻辑（工作流图结构）"""

    def setUp(self):
        self.wf = build_collaboration_workflow()

    def test_workflow_compiles_successfully(self):
        self.assertIsNotNone(self.wf)

    def test_workflow_has_all_required_nodes(self):
        """验证工作流包含 PDF 要求的所有节点：分析-生成-校验-决策"""
        # LangGraph 编译后的图在 nodes 属性中包含所有节点
        graph_nodes = set(self.wf.nodes.keys())
        required = {"plan", "diagnosis", "generate", "debate",
                    "review", "adapt", "aggregate"}
        missing = required - graph_nodes
        self.assertEqual(missing, set(), f"工作流缺少节点: {missing}")

    def test_workflow_execution_order(self):
        """验证工作流包含「分析→生成→校验→决策」闭环所需节点"""
        # plan → diagnosis（分析）
        # diagnosis → generate（生成）
        # generate → debate（校验-辩论）
        # debate → review（校验-审核）
        # review → adapt（决策）
        # adapt → aggregate（输出）
        graph_nodes = set(self.wf.nodes.keys())
        self.assertIn("plan", graph_nodes)
        self.assertIn("diagnosis", graph_nodes)
        self.assertIn("generate", graph_nodes)
        self.assertIn("debate", graph_nodes)
        self.assertIn("review", graph_nodes)
        self.assertIn("adapt", graph_nodes)
        self.assertIn("aggregate", graph_nodes)


class TestExtractJson(unittest.TestCase):
    """测试 JSON 提取工具函数（多智能体产出解析）"""

    def test_extract_plain_json(self):
        text = '{"key": "value", "num": 42}'
        result = _extract_json(text)
        self.assertEqual(result["key"], "value")
        self.assertEqual(result["num"], 42)

    def test_extract_json_with_prefix(self):
        text = '这是结果：\n{"knowledge_gaps": [], "difficulty_target": "基础"}'
        result = _extract_json(text)
        self.assertEqual(result["difficulty_target"], "基础")

    def test_extract_json_with_code_block(self):
        text = '```json\n{"hallucination_rate": 0.03}\n```'
        result = _extract_json(text)
        self.assertEqual(result["hallucination_rate"], 0.03)

    def test_extract_invalid_returns_empty(self):
        result = _extract_json("这不是JSON")
        self.assertFalse(result)

    def test_extract_empty_returns_empty(self):
        result = _extract_json("")
        self.assertFalse(result)


class TestDecideAfterReview(unittest.TestCase):
    """测试审核后的路由决策（协同调度核心逻辑）"""

    def test_retry_when_review_score_low(self):
        state = AgentState()
        state.review_result = {
            "overall": 4,
            "scores": {"accuracy": 3, "difficulty_fit": 5,
                       "completeness": 4, "cross_modal_consistency": 4},
            "retry": True,
            "issues": [{"severity": "critical", "description": "严重错误"}],
        }
        state.retry_count = 0
        decision = decide_after_review(state)
        self.assertEqual(decision, "retry")

    def test_adapt_when_review_passes(self):
        state = AgentState()
        state.review_result = {
            "overall": 8,
            "scores": {"accuracy": 9, "difficulty_fit": 8,
                       "completeness": 8, "cross_modal_consistency": 7},
            "retry": False,
        }
        state.retry_count = 0
        decision = decide_after_review(state)
        self.assertEqual(decision, "adapt")

    def test_force_adapt_after_max_retry(self):
        state = AgentState()
        state.review_result = {"overall": 3, "retry": True}
        state.retry_count = 1  # 已重试1次（最大值）
        decision = decide_after_review(state)
        self.assertEqual(decision, "adapt")


class TestDiagnosisNode(unittest.TestCase):
    """测试学情诊断 Agent（PDF 智能体①）"""

    @patch('orchestrator.workflow._call_llm')
    @patch('orchestrator.workflow._run_one_agent')
    def test_diagnosis_extracts_knowledge_gaps(self, mock_run, mock_llm):
        mock_llm.return_value = json.dumps({
            "knowledge_gaps": [
                {"point": "反向传播", "severity": "high", "subject": "深度学习", "score": 35}
            ],
            "cognitive_profile": {"style": "visual", "pace": "slow", "motivation": 5},
            "difficulty_target": "基础",
            "priority_focus": ["反向传播", "梯度下降"],
            "adaptation_strategy": "多视觉化教学",
        })
        state = AgentState(
            topic="神经网络",
            context={"profile": {"knowledge_level": {"深度学习": 35}}},
        )
        result = diagnosis_node(state)
        self.assertIn("knowledge_gaps", result.diagnosis_result)
        self.assertEqual(result.diagnosis_result["difficulty_target"], "基础")
        self.assertIn("diagnosis", result.context)

    def test_diagnosis_fallback_on_empty_response(self):
        with patch('orchestrator.workflow._call_llm', return_value=""):
            state = AgentState(topic="测试", context={})
            result = diagnosis_node(state)
            self.assertIn("difficulty_target", result.diagnosis_result)
            self.assertEqual(result.diagnosis_result["difficulty_target"], "进阶")


class TestDebateNode(unittest.TestCase):
    """测试辩论与交叉验证 Agent（PDF 创新性核心）"""

    @patch('orchestrator.workflow._call_llm')
    def test_debate_identifies_hallucinations(self, mock_llm):
        mock_llm.return_value = json.dumps({
            "cross_validation": {
                "contradictions": [
                    {"agent_a": "document", "agent_b": "quiz",
                     "issue": "文档说ReLU范围[0,+∞)，题目答案写(-∞,+∞)",
                     "resolution": "ReLU范围为[0,+∞)，题目需修正"}
                ]
            },
            "hallucinations": [
                {"type": "事实性幻觉", "agent": "reading",
                 "content": "编造了不存在的2019年论文",
                 "correction": "该论文不存在", "severity": "major"}
            ],
            "fact_check": {"verified_count": 18, "total_facts": 20, "hallucination_rate": 0.1},
            "debate_summary": "发现1处矛盾和1处幻觉",
            "hallucination_prevented": True,
            "retry_recommended": True,
        })
        state = AgentState(
            topic="ReLU激活函数",
            agent_outputs={"document": "ReLU范围[0,+∞)", "quiz": "题目答案(-∞,+∞)"},
        )
        result = debate_node(state)
        self.assertGreater(len(result.debate_result["hallucinations"]), 0)
        self.assertTrue(result.debate_result["hallucination_prevented"])
        self.assertGreater(result.debate_result["fact_check"]["hallucination_rate"], 0)

    def test_debate_fallback_on_empty(self):
        with patch('orchestrator.workflow._call_llm', return_value=""):
            state = AgentState(topic="测试", agent_outputs={"doc": "内容"})
            result = debate_node(state)
            self.assertEqual(result.debate_result["fact_check"]["hallucination_rate"], 0.0)
            self.assertFalse(result.debate_result["hallucination_prevented"])


class TestAdaptNode(unittest.TestCase):
    """测试动态迭代决策 Agent（PDF 反馈机制）"""

    @patch('orchestrator.workflow._call_llm')
    def test_adapt_triggers_simplify_for_weak_student(self, mock_llm):
        mock_llm.return_value = json.dumps({
            "decision": "simplify",
            "reason": "学习者掌握度45分，需降维解释",
            "actions": [
                {"type": "降维解释", "target_knowledge": "反向传播",
                 "action_detail": "使用水管类比解释链式法则"}
            ],
            "learning_feedback": "别灰心，我们换个方式理解",
            "next_iteration_needed": True,
        })
        state = AgentState(
            topic="神经网络",
            context={"profile": {"knowledge_level": {"深度学习": 45}}},
            diagnosis_result={"difficulty_target": "基础"},
            review_result={"overall": 5},
            debate_result={"hallucination_prevented": False},
            learning_feedback={"answer_correct_rate": 0.4},
        )
        result = adapt_node(state)
        self.assertEqual(result.adapt_result["decision"], "simplify")
        self.assertGreater(len(result.adapt_result["actions"]), 0)

    @patch('orchestrator.workflow._call_llm')
    def test_adapt_triggers_challenge_for_strong_student(self, mock_llm):
        mock_llm.return_value = json.dumps({
            "decision": "challenge",
            "reason": "掌握度90分且正确率85%，触发进阶",
            "actions": [
                {"type": "进阶挑战", "target_knowledge": "Transformer",
                 "action_detail": "阅读Attention is All You Need并复现"}
            ],
            "learning_feedback": "你已掌握基础，来挑战前沿",
            "next_iteration_needed": False,
        })
        state = AgentState(
            topic="神经网络",
            learning_feedback={"answer_correct_rate": 0.85},
        )
        result = adapt_node(state)
        self.assertEqual(result.adapt_result["decision"], "challenge")


class TestIntentClassification(unittest.TestCase):
    """测试意图识别与路由（多智能体调度入口）"""

    def test_classify_intent_returns_state(self):
        with patch('orchestrator.workflow._call_llm', return_value=json.dumps({
            "intent": "collaboration", "complexity": "complex",
            "mode": "hierarchy", "plan": {"steps": [], "agents_needed": []},
            "summary": "多智能体协作",
        })):
            state = AgentState(user_message="帮我生成机器学习教学资源")
            result = classify_intent(state)
            self.assertEqual(result.intent, Intent.COLLABORATION)


class TestKnowledgeBaseIntegration(unittest.TestCase):
    """测试领域知识库切片完整性（PDF 测试数据要求）"""

    KB_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)),
                          "knowledge_base")

    REQUIRED_KB_FILES = [
        "ai_domain_kb.md",            # 核心理论切片
        "ai_engineering_practice.md", # 工程实操切片
        "llm_application_dev.md",     # 大模型应用开发切片
        "ai_industry_cases.md",       # 行业落地与岗位规范切片
    ]

    def test_kb_file_exists(self):
        self.assertTrue(os.path.exists(os.path.join(self.KB_DIR, "ai_domain_kb.md")),
                        "AI 领域知识库切片文件不存在")

    def test_all_vertical_kb_slices_exist(self):
        for fname in self.REQUIRED_KB_FILES:
            self.assertTrue(os.path.exists(os.path.join(self.KB_DIR, fname)),
                            f"缺少知识库垂直切片: {fname}")

    def test_kb_has_sufficient_content(self):
        total = 0
        for fname in self.REQUIRED_KB_FILES:
            path = os.path.join(self.KB_DIR, fname)
            if not os.path.exists(path):
                continue
            with open(path, "r", encoding="utf-8") as f:
                total += len(f.read())
        self.assertGreater(total, 10000,
                           f"知识库总内容过少（{total} 字符），应至少 10000 字符")

    def test_kb_covers_core_ai_domains(self):
        path = os.path.join(self.KB_DIR, "ai_domain_kb.md")
        if not os.path.exists(path):
            self.skipTest("知识库文件不存在")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        required_topics = ["机器学习", "深度学习", "神经网络", "Transformer"]
        for topic in required_topics:
            self.assertIn(topic, content, f"知识库缺少核心主题: {topic}")

    def test_engineering_kb_covers_deployment_and_training(self):
        """工程实操切片需覆盖部署与训练调参（岗位技能培训场景）"""
        path = os.path.join(self.KB_DIR, "ai_engineering_practice.md")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        for kw in ["ONNX", "TensorRT", "灰度发布", "混合精度", "DDP"]:
            self.assertIn(kw, content, f"工程实操切片缺少: {kw}")

    def test_llm_kb_covers_rag_and_hallucination(self):
        """大模型应用切片需覆盖 RAG 与幻觉防控（赛题核心创新点支撑）"""
        path = os.path.join(self.KB_DIR, "llm_application_dev.md")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        for kw in ["RAG", "引用溯源", "幻觉", "重排序", "LoRA"]:
            self.assertIn(kw, content, f"LLM 应用切片缺少: {kw}")

    def test_industry_kb_covers_job_skills(self):
        """行业切片需覆盖企业内训/转岗培训场景（受众校准）"""
        path = os.path.join(self.KB_DIR, "ai_industry_cases.md")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        for kw in ["视觉质检", "预测性维护", "转岗培训", "内训", "技能矩阵"]:
            self.assertIn(kw, content, f"行业切片缺少: {kw}")


class TestLearnerProfiles(unittest.TestCase):
    """测试差异化学习者画像数据（PDF 测试数据要求 ≥3 组）"""

    ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

    def _load_profiles_file(self):
        path = os.path.join(self.ROOT, "test-data", "learner_profiles.json")
        self.assertTrue(os.path.exists(path), "learner_profiles.json 不存在")
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)["learner_profiles"]

    def test_at_least_3_differentiated_profiles(self):
        profiles = self._load_profiles_file()
        self.assertGreaterEqual(len(profiles), 3,
                                "PDF 要求至少 3 组差异化学习者画像")

    def test_profiles_cover_career_backgrounds(self):
        """受众校准：必须覆盖在校生之外的转岗/在职/企业内训身份"""
        profiles = self._load_profiles_file()
        occupations = " ".join(p.get("岗位方向", "") for p in profiles)
        for kw in ["转岗", "在职", "内训", "在校"]:
            self.assertIn(kw, occupations, f"画像缺少职业身份覆盖: {kw}")

    def test_profiles_have_prior_knowledge_fields(self):
        """赛题指定先验知识字段：学历背景 / 岗位方向 / 理论测试结果"""
        for p in self._load_profiles_file():
            self.assertIn("学历背景", p, f"{p['real_name']} 缺少学历背景")
            self.assertIn("岗位方向", p, f"{p['real_name']} 缺少岗位方向")
            self.assertIn("理论测试结果", p, f"{p['real_name']} 缺少理论测试结果")
            self.assertGreater(len(p["理论测试结果"]), 0)

    def test_profiles_have_different_characteristics(self):
        profiles = self._load_profiles_file()
        styles = {p["认知风格"] for p in profiles}
        self.assertGreaterEqual(len(styles), 2, "画像认知风格应有差异化")
        paces = {p["学习节奏"] for p in profiles}
        self.assertGreaterEqual(len(paces), 2, "画像学习节奏应有差异化")


class TestQuantifiedTestCases(unittest.TestCase):
    """测试量化用例集与难度适配规则一致性（实用价值指标基础）"""

    ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    FALLBACK_CACHE = {}

    @classmethod
    def setUpClass(cls):
        cases_path = os.path.join(cls.ROOT, "test-data", "test_cases.json")
        profiles_path = os.path.join(cls.ROOT, "test-data", "learner_profiles.json")
        with open(cases_path, "r", encoding="utf-8") as f:
            cls.cases_data = json.load(f)
        with open(profiles_path, "r", encoding="utf-8") as f:
            cls.profiles = {p["profile_id"]: p for p in json.load(f)["learner_profiles"]}
        cls.topics = {t["topic_id"]: t for t in cls.cases_data["topics"]}

    def _fallback_median(self, profile):
        scores = sorted(v for v in profile["理论测试结果"].values())
        n = len(scores)
        mid = n // 2
        med = scores[mid] if n % 2 else (scores[mid - 1] + scores[mid]) / 2
        return int(med + 0.5)  # 四舍五入

    def _rule_difficulty(self, case):
        profile = self.profiles[case["profile_id"]]
        topic = self.topics[case["topic_id"]]
        mods = profile.get("modifiers", {})
        vals = []
        for subj in topic["subjects"]:
            s = profile["理论测试结果"].get(subj)
            vals.append(s if isinstance(s, (int, float)) else self._fallback_median(profile))
        base = int(sum(vals) / len(vals) + 0.5)
        final = max(0, min(100, base + mods.get("goal", 0) + mods.get("pace", 0) + mods.get("career", 0)))
        return final, ("基础" if final < 60 else ("进阶" if final < 85 else "高级"))

    def test_at_least_50_test_cases(self):
        """评审细则：测试用例不足 50 组会被扣分"""
        self.assertGreaterEqual(len(self.cases_data["test_cases"]), 50,
                                "测试用例应 ≥50 组")

    def test_cases_span_all_difficulty_levels(self):
        levels = {c["expected_difficulty"] for c in self.cases_data["test_cases"]}
        self.assertEqual(levels, {"基础", "进阶", "高级"}, f"用例难度覆盖不全: {levels}")

    def test_dataset_matches_published_rule(self):
        """数据集 expected_difficulty 必须与公开规则复算结果一致"""
        mismatches = []
        for c in self.cases_data["test_cases"]:
            score, level = self._rule_difficulty(c)
            if abs(score - c["difficulty_score"]) > 1 or level != c["expected_difficulty"]:
                mismatches.append(f"{c['case_id']}: dataset={c['expected_difficulty']}({c['difficulty_score']}) rule={level}({score})")
        self.assertEqual(mismatches, [], "用例与规则不一致:\n" + "\n".join(mismatches))

    def test_every_case_has_expected_resources(self):
        for c in self.cases_data["test_cases"]:
            self.assertGreaterEqual(len(c["expected_resources"]), 3,
                                    f"{c['case_id']} 资源形态少于 3 种（PDF 要求 ≥3）")


if __name__ == "__main__":
    unittest.main(verbosity=2)
