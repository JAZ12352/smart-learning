package com.mindskip.xzs.service;

import com.mindskip.xzs.service.MultiAgentService.AgentType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * MultiAgentService 单元测试 — JUnit 4
 * 测试 Agent 路由逻辑和提示词构建（无需 Spring 上下文）
 */
public class MultiAgentServiceTest {

    private MultiAgentService service;

    @Before
    public void setUp() {
        service = new MultiAgentService();
    }

    // ═══════════════════════════════════════════════════════════════
    //  AgentType 枚举测试
    // ═══════════════════════════════════════════════════════════════

    @Test
    public void fromKeyShouldMatchAllAgentTypes() {
        assertEquals(AgentType.SUPERVISOR, AgentType.fromKey("supervisor"));
        assertEquals(AgentType.DOC, AgentType.fromKey("document"));
        assertEquals(AgentType.MIND_MAP, AgentType.fromKey("mind_map"));
        assertEquals(AgentType.QUIZ, AgentType.fromKey("exercise"));
        assertEquals(AgentType.READING, AgentType.fromKey("extended_reading"));
        assertEquals(AgentType.VIDEO, AgentType.fromKey("video"));
        assertEquals(AgentType.CODE, AgentType.fromKey("code_example"));
    }

    @Test
    public void fromKeyShouldReturnNullForUnknownKey() {
        assertNull(AgentType.fromKey("unknown_agent"));
        assertNull(AgentType.fromKey(""));
    }

    @Test
    public void eachAgentShouldHaveNameAndDescription() {
        for (AgentType at : AgentType.values()) {
            assertNotNull(at.key, at.name);
            assertNotNull(at.name);
            assertNotNull(at.description);
            assertFalse(at.name.isEmpty());
            assertFalse(at.description.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Agent 路由测试
    // ═══════════════════════════════════════════════════════════════

    @Test
    public void shouldDefaultToDocAndMindMap() {
        List<AgentType> agents = service.getRecommendedAgents("任意教学内容");
        assertTrue(agents.contains(AgentType.DOC));
        assertTrue(agents.contains(AgentType.MIND_MAP));
        assertEquals(2, agents.size());
    }

    @Test
    public void shouldActivateQuizAgent() {
        String[] keywords = {"练习", "考试", "测试", "复习", "题目", "试卷"};
        for (String kw : keywords) {
            List<AgentType> agents = service.getRecommendedAgents(kw + " 内容");
            assertTrue("主题含'" + kw + "'应激活 Quiz Agent", agents.contains(AgentType.QUIZ));
        }
    }

    @Test
    public void shouldActivateCodeAgent() {
        String[] keywords = {"编程", "代码", "算法", "程序", "开发", "python", "java", "html"};
        for (String kw : keywords) {
            List<AgentType> agents = service.getRecommendedAgents(kw + " 教学");
            assertTrue("主题含'" + kw + "'应激活 Code Agent", agents.contains(AgentType.CODE));
        }
    }

    @Test
    public void shouldActivateReadingAgent() {
        String[] keywords = {"前沿", "进展", "文献", "研究", "综述", "趋势"};
        for (String kw : keywords) {
            List<AgentType> agents = service.getRecommendedAgents(kw + " 内容");
            assertTrue("主题含'" + kw + "'应激活 Reading Agent", agents.contains(AgentType.READING));
        }
    }

    @Test
    public void shouldActivateVideoAgent() {
        String[] keywords = {"视频", "动画", "演示", "实验", "展示"};
        for (String kw : keywords) {
            List<AgentType> agents = service.getRecommendedAgents(kw + " 内容");
            assertTrue("主题含'" + kw + "'应激活 Video Agent", agents.contains(AgentType.VIDEO));
        }
    }

    @Test
    public void shouldActivateMultipleAgents() {
        List<AgentType> agents = service.getRecommendedAgents("Python 编程练习与算法前沿研究");
        assertTrue(agents.contains(AgentType.DOC));
        assertTrue(agents.contains(AgentType.MIND_MAP));
        assertTrue(agents.contains(AgentType.CODE));
        assertTrue(agents.contains(AgentType.QUIZ));
        assertTrue(agents.contains(AgentType.READING));
    }

    @Test
    public void keyWordMatchingShouldBeCaseInsensitive() {
        List<AgentType> agents = service.getRecommendedAgents("PYTHON 编程");
        assertTrue(agents.contains(AgentType.CODE));
    }

    // ═══════════════════════════════════════════════════════════════
    //  提示词构建测试
    // ═══════════════════════════════════════════════════════════════

    @Test
    public void supervisorPromptShouldContainRoutingRules() {
        String prompt = service.buildSupervisorPrompt("导数");
        assertNotNull(prompt);
        assertTrue(prompt.contains("路由规则"));
        assertTrue(prompt.contains("JSON"));
    }

    @Test
    public void docAgentPromptShouldContainTenChapters() {
        String prompt = service.buildDocAgentPrompt("导数");
        assertNotNull(prompt);
        assertTrue(prompt.contains("概念定义"));
        assertTrue(prompt.contains("历史背景"));
        assertTrue(prompt.contains("核心原理"));
        assertTrue(prompt.contains("应用实例"));
        assertTrue(prompt.contains("常见误区"));
    }

    @Test
    public void mindMapAgentPromptShouldRequireEightDimensions() {
        String prompt = service.buildMindMapAgentPrompt("导数");
        assertNotNull(prompt);
        assertTrue(prompt.contains("8个维度"));
        assertTrue(prompt.contains("中心主题"));
    }

    @Test
    public void quizAgentPromptShouldContainFourTypes() {
        String prompt = service.buildQuizAgentPrompt("导数");
        assertNotNull(prompt);
        assertTrue(prompt.contains("选择题"));
        assertTrue(prompt.contains("判断题"));
        assertTrue(prompt.contains("填空题"));
        assertTrue(prompt.contains("简答题"));
    }

    @Test
    public void videoAgentPromptShouldContainSceneStructure() {
        String prompt = service.buildVideoAgentPrompt("导数");
        assertNotNull(prompt);
        assertTrue(prompt.contains("画面文字"));
        assertTrue(prompt.contains("语音讲解"));
        assertTrue(prompt.contains("开场引入"));
    }

    @Test
    public void codeAgentPromptShouldRequireRunnableCode() {
        String prompt = service.buildCodeAgentPrompt("排序算法");
        assertNotNull(prompt);
        assertTrue(prompt.contains("可运行"));
    }

    @Test
    public void allPromptsShouldBeNonEmpty() {
        String topic = "机器学习";
        assertFalse(service.buildSupervisorPrompt(topic).isEmpty());
        assertFalse(service.buildDocAgentPrompt(topic).isEmpty());
        assertFalse(service.buildMindMapAgentPrompt(topic).isEmpty());
        assertFalse(service.buildQuizAgentPrompt(topic).isEmpty());
        assertFalse(service.buildReadingAgentPrompt(topic).isEmpty());
        assertFalse(service.buildVideoAgentPrompt(topic).isEmpty());
        assertFalse(service.buildCodeAgentPrompt(topic).isEmpty());
    }
}
