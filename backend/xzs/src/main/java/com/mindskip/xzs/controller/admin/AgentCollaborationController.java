package com.mindskip.xzs.controller.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.StudentProfile;
import com.mindskip.xzs.service.MultiAgentService;
import com.mindskip.xzs.service.MultiAgentService.AgentType;
import com.mindskip.xzs.service.PythonAgentClient;
import com.mindskip.xzs.service.StudentProfileService;

/**
 * Agent 协作状态 API — 查看多智能体系统运行状态和编排任务。
 * 展示 Python Supervisor 与各子 Agent 的协作关系。
 */
@RestController
@RequestMapping("/api/admin/agent")
public class AgentCollaborationController {

    private static final Logger log = LoggerFactory.getLogger(AgentCollaborationController.class);

    private final PythonAgentClient pythonAgentClient;
    private final MultiAgentService multiAgentService;
    private final StudentProfileService studentProfileService;

    public AgentCollaborationController(PythonAgentClient pythonAgentClient,
                                         MultiAgentService multiAgentService,
                                         StudentProfileService studentProfileService) {
        this.pythonAgentClient = pythonAgentClient;
        this.multiAgentService = multiAgentService;
        this.studentProfileService = studentProfileService;
    }

    /**
     * 获取所有 Agent 的状态信息。
     * 包括 Python Supervisor 可用性和各子 Agent 的配置。
     */
    @GetMapping("/status")
    public RestResponse<Map<String, Object>> getAgentStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Python 服务状态
        boolean pythonAvailable = pythonAgentClient.isAvailable();
        status.put("pythonServiceAvailable", pythonAvailable);
        status.put("orchestrationMode", pythonAvailable ? "Supervisor 统一编排 (LangGraph)" : "Java 本地直连");

        // Python Agent 列表
        List<Map<String, Object>> pythonAgents = new ArrayList<>();
        Map<String, Object> supervisorInfo = new LinkedHashMap<>();
        supervisorInfo.put("name", "Supervisor Agent");
        supervisorInfo.put("key", "supervisor");
        supervisorInfo.put("role", "意图分类、任务路由、质量审查、输出合成");
        supervisorInfo.put("model", "deepseek-v4-pro");
        supervisorInfo.put("temperature", 0.3);
        supervisorInfo.put("available", pythonAvailable);
        pythonAgents.add(supervisorInfo);

        String[] pyAgentNames = {"Paper Agent", "Question Agent", "Student Agent",
            "Profile Agent", "Resource Agent", "Path Agent", "QA Agent", "Teaching Agent"};
        String[] pyAgentKeys = {"paper", "question", "student", "profile", "resource", "path", "qa", "teaching"};
        String[] pyAgentRoles = {"智能组卷", "练习题推荐", "学情分析", "学生画像", "多模态资源编排",
            "学习路径规划", "智能辅导答疑", "教案设计"};
        double[] pyTemperatures = {0.5, 0.5, 0.5, 0.5, 0.5, 0.4, 0.7, 0.7};

        for (int i = 0; i < pyAgentNames.length; i++) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", pyAgentNames[i]);
            info.put("key", pyAgentKeys[i]);
            info.put("role", pyAgentRoles[i]);
            info.put("model", "deepseek-v4-pro");
            info.put("temperature", pyTemperatures[i]);
            info.put("available", pythonAvailable);
            pythonAgents.add(info);
        }
        status.put("pythonAgents", pythonAgents);

        // Java Agent 列表
        List<Map<String, Object>> javaAgents = new ArrayList<>();
        for (AgentType at : AgentType.values()) {
            if (at == AgentType.SUPERVISOR) continue;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", at.name);
            info.put("key", at.key);
            info.put("description", at.description);
            info.put("model", "deepseek-v4-pro (直连 DeepSeek)");
            info.put("available", true);  // Java Agent 始终可用
            javaAgents.add(info);
        }
        status.put("javaAgents", javaAgents);

        // 编排工作流说明
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("entry", "Supervisor 意图分类");
        workflow.put("routing", "意图匹配 → 路由到专业 Agent");
        workflow.put("execution", "Agent 执行 + 工具调用 (MySQL / ChromaDB)");
        workflow.put("synthesis", "Supervisor 质量审查 + 结果合成");
        workflow.put("framework", "LangGraph StateGraph");
        status.put("workflow", workflow);

        Map<String, String> toolRegistry = new HashMap<>();
        toolRegistry.put("sql_query", "MySQL 只读查询（Paper/Question/Student/QA Agent）");
        toolRegistry.put("knowledge_search", "ChromaDB 向量检索（Question/QA/Teaching Agent）");
        status.put("toolRegistry", toolRegistry);

        return RestResponse.ok(status);
    }

    /**
     * 提交编排任务到 Python Supervisor。
     * 用于验证多智能体协作功能。
     */
    @PostMapping("/orchestrate")
    public RestResponse<Map<String, Object>> orchestrateTask(@RequestBody Map<String, Object> requestData) {
        String topic = String.valueOf(requestData.getOrDefault("topic", ""));
        String message = String.valueOf(requestData.getOrDefault("message", ""));

        if (topic.isEmpty() && message.isEmpty()) {
            return RestResponse.fail(3, "topic 或 message 不能为空");
        }

        if (!pythonAgentClient.isAvailable()) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("mode", "java_direct");
            fallback.put("activatedAgents", multiAgentService.getRecommendedAgents(
                topic.isEmpty() ? message : topic).stream()
                .map(at -> at.key).toArray());
            fallback.put("note", "Python 服务不可用，使用 Java 本地关键词路由");
            return RestResponse.ok(fallback);
        }

        // 调用 Python 资源生成端点作为编排测试
        List<String> agentTypes = new ArrayList<>();
        for (AgentType at : multiAgentService.getRecommendedAgents(topic.isEmpty() ? message : topic)) {
            agentTypes.add(at.key);
        }

        Map<String, Object> result = pythonAgentClient.multiGenerateResource(
            topic.isEmpty() ? message : topic, agentTypes, requestData);

        if (result == null) {
            return RestResponse.fail(2, "Python Supervisor 编排失败");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "python_supervisor");
        response.put("topic", topic.isEmpty() ? message : topic);
        response.put("activatedAgents", agentTypes);
        response.put("successCount", result.getOrDefault("success_count", 0));
        response.put("summary", result.getOrDefault("summary", ""));
        return RestResponse.ok(response);
    }

    /**
     * SSE 流式推送多智能体协作的真实执行事件。
     * 前端通过 fetch 读取 text/event-stream，每条 data: 是一个 JSON 事件。
     * 事件类型：plan_start/plan_end/agent_start/agent_end/review_start/review_end/retry/final/error。
     *
     * 请求体：{ "topic": "教学主题", "context": { "profile": {...} } }
     */
    @PostMapping("/stream-collaboration")
    public void streamCollaboration(@RequestBody Map<String, Object> requestData,
                                     HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        String topic = String.valueOf(requestData.getOrDefault("topic", ""));
        if (topic.isEmpty()) {
            try (PrintWriter writer = response.getWriter()) {
                writer.write("data: {\"type\":\"error\",\"message\":\"topic 不能为空\"}\n\n");
                writer.write("data: [DONE]\n\n");
                writer.flush();
            }
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> context = requestData.get("context") instanceof Map
            ? (Map<String, Object>) requestData.get("context") : new HashMap<>();

        // ── 自动注入学生画像数据（PDF要求：整合学习者先验知识画像）──
        if (!context.containsKey("profile") || context.get("profile") == null) {
            Map<String, Object> profile = loadDefaultProfile();
            if (profile != null) {
                context.put("profile", profile);
                context.put("userId", profile.get("userId"));
                log.info("自动注入学生画像: userId={}, knowledgeLevel={}",
                    profile.get("userId"), profile.get("knowledgeLevel"));
            }
        }

        String requestId = String.valueOf(requestData.getOrDefault("requestId", "req_" + System.currentTimeMillis()));

        try (PrintWriter writer = response.getWriter()) {
            pythonAgentClient.streamCollaboration(topic, context, requestId, eventData -> {
                writer.write("data: " + eventData + "\n\n");
                writer.flush();
            });
            writer.write("data: [DONE]\n\n");
            writer.flush();
        } catch (Exception e) {
            log.error("stream-collaboration proxy failed", e);
            try {
                PrintWriter errWriter = response.getWriter();
                errWriter.write("data: {\"type\":\"error\",\"message\":\""
                    + (e.getMessage() == null ? "unknown" : e.getMessage().replace("\"", "'"))
                    + "\"}\n\n");
                errWriter.write("data: [DONE]\n\n");
                errWriter.flush();
            } catch (Exception ignored) { }
        }
    }

    /**
     * 加载默认学生画像（依次尝试 userId=2,3,4，取第一个可用画像）。
     * 用于协作工作流自动注入学情画像数据。
     */
    private Map<String, Object> loadDefaultProfile() {
        int[] testUserIds = {2, 3, 4};
        for (int uid : testUserIds) {
            try {
                StudentProfile p = studentProfileService.getProfileByUserId(uid);
                if (p != null && p.getKnowledgeLevel() != null) {
                    Map<String, Object> profile = new LinkedHashMap<>();
                    profile.put("userId", uid);
                    profile.put("knowledgeLevel", p.getKnowledgeLevel());
                    profile.put("cognitiveStyle", p.getCognitiveStyle());
                    profile.put("weakPoints", p.getWeakPoints());
                    profile.put("learningGoals", p.getLearningGoals());
                    profile.put("learningPace", p.getLearningPace());
                    profile.put("learningStyle", p.getLearningStyle());
                    profile.put("interests", p.getInterests());
                    profile.put("motivationLevel", p.getMotivationLevel());
                    profile.put("educationBackground", p.getEducationBackground());
                    profile.put("occupationTarget", p.getOccupationTarget());
                    profile.put("theoryTestResult", p.getTheoryTestResult());
                    profile.put("profileSummary", p.getProfileSummary());
                    return profile;
                }
            } catch (Exception e) {
                log.warn("获取用户 {} 画像失败: {}", uid, e.getMessage());
            }
        }
        log.warn("未找到任何学生画像数据，协作工作流将以无画像模式运行");
        return null;
    }
}
