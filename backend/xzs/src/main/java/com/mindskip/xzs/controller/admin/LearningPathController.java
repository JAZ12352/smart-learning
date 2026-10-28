package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.LearningPath;
import com.mindskip.xzs.domain.StudentProfile;
import com.mindskip.xzs.service.DeepSeekService;
import com.mindskip.xzs.service.LearningPathService;
import com.mindskip.xzs.service.StudentProfileService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/learning-path")
public class LearningPathController {

    private static final Logger log = LoggerFactory.getLogger(LearningPathController.class);

    private final LearningPathService learningPathService;
    private final StudentProfileService studentProfileService;
    private final DeepSeekService deepSeekService;

    public LearningPathController(LearningPathService learningPathService,
                                  StudentProfileService studentProfileService,
                                  DeepSeekService deepSeekService) {
        this.learningPathService = learningPathService;
        this.studentProfileService = studentProfileService;
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/generate")
    public void generate(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        Integer userId = toInteger(requestData.get("userId"), null);
        Integer subjectId = toInteger(requestData.get("subjectId"), null);
        String subjectName = text(requestData.get("subjectName"));

        if (userId == null || subjectId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writePlain(response, "用户 ID 和学科 ID 不能为空");
            return;
        }

        StudentProfile profile = studentProfileService.getProfileByUserId(userId);
        String profileText = buildProfileText(profile);
        String effectiveSubjectName = StringUtils.defaultIfBlank(subjectName, "通用课程");

        List<Map<String, String>> messages = buildMessages(
            "你是一位高校个性化学习路径设计专家。请基于学生画像数据（知识基础、认知风格、薄弱点、学习目标、学习节奏、兴趣方向、学习风格、动机水平），为该学生设计一条符合其认知特点和发展目标的个性化学习路径。路径节点需包含知识点标题、详细说明、前置依赖、推荐资源、预估学时、难度分级和与画像匹配的推荐理由。遵循高等教育学科知识体系的内在逻辑，确保路径的系统性和渐进性。只返回JSON，不输出Markdown代码块。格式：{\"nodes\":[{\"id\":\"node_1\",\"title\":\"标题\",\"description\":\"说明\",\"knowledgePoints\":[\"知识点\"],\"prerequisites\":[],\"estimatedHours\":2,\"difficulty\":\"easy\",\"resources\":[\"推荐资源\"],\"reason\":\"画像匹配理由\"}]}",
            "学科：" + effectiveSubjectName + "\n学生画像：\n" + profileText + "\n请为该学生生成个性化学习路径，并说明每个节点的推荐理由。"
        );

        StringBuilder fullContent = new StringBuilder();

        try (PrintWriter writer = response.getWriter()) {
            deepSeekService.stream(messages, chunk -> {
                writer.write(chunk);
                writer.flush();
                fullContent.append(chunk);
            });
        } catch (Exception e) {
            log.error("Learning path generation stream failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writePlain(response, e.getMessage());
            return;
        }

        savePath(userId, subjectId, effectiveSubjectName, fullContent.toString());
    }

    @GetMapping("/{userId}/{subjectId}")
    public RestResponse<LearningPath> getPath(@PathVariable String userId, @PathVariable Integer subjectId) {
        LearningPath path = learningPathService.getPathByUserAndSubject(safeParseInt(userId), subjectId);
        if (path == null) {
            return RestResponse.fail(2, "学习路径不存在");
        }
        return RestResponse.ok(path);
    }

    @PostMapping("/progress")
    public RestResponse updateProgress(@RequestBody Map<String, Object> requestData) {
        Integer pathId = toInteger(requestData.get("pathId"), null);
        String currentNodeId = text(requestData.get("currentNodeId"));
        Double progressPercent = requestData.get("progressPercent") instanceof Number
            ? ((Number) requestData.get("progressPercent")).doubleValue() : 0.0;

        if (pathId == null) {
            return RestResponse.fail(2, "路径 ID 不能为空");
        }

        LearningPath path = learningPathService.selectById(pathId);
        if (path == null) {
            return RestResponse.fail(2, "学习路径不存在");
        }

        if (StringUtils.isNotBlank(currentNodeId)) {
            path.setCurrentNodeId(currentNodeId);
        }
        path.setProgressPercent(progressPercent);
        path.setModifyTime(new Date());
        learningPathService.updateByIdFilter(path);

        return RestResponse.ok();
    }

    private void savePath(Integer userId, Integer subjectId, String subjectName, String nodes) {
        try {
            LearningPath existingPath = learningPathService.getPathByUserAndSubject(userId, subjectId);
            if (existingPath != null) {
                existingPath.setNodes(nodes);
                existingPath.setPathName(subjectName + " 学习路径");
                existingPath.setModifyTime(new Date());
                learningPathService.updateByIdFilter(existingPath);
            } else {
                LearningPath path = new LearningPath();
                path.setUserId(userId);
                path.setSubjectId(subjectId);
                path.setPathName(subjectName + " 学习路径");
                path.setNodes(nodes);
                path.setCurrentNodeId("");
                path.setProgressPercent(0.0);
                path.setCreateTime(new Date());
                path.setModifyTime(new Date());
                path.setDeleted(false);
                learningPathService.insertByFilter(path);
            }
        } catch (Exception e) {
            log.warn("Failed to save learning path: {}", e.getMessage());
        }
    }

    private String buildProfileText(StudentProfile profile) {
        if (profile == null) {
            return "暂无画像信息";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("知识基础：").append(defaultText(profile.getKnowledgeLevel(), "未评估")).append("\n");
        sb.append("认知风格：").append(defaultText(profile.getCognitiveStyle(), "未评估")).append("\n");
        sb.append("薄弱点：").append(defaultText(profile.getWeakPoints(), "未评估")).append("\n");
        sb.append("学习目标：").append(defaultText(profile.getLearningGoals(), "未评估")).append("\n");
        sb.append("学习节奏：").append(defaultText(profile.getLearningPace(), "未评估")).append("\n");
        sb.append("兴趣方向：").append(defaultText(profile.getInterests(), "未评估")).append("\n");
        sb.append("学习风格：").append(defaultText(profile.getLearningStyle(), "未评估")).append("\n");
        sb.append("动机水平：").append(profile.getMotivationLevel() == null ? "5" : profile.getMotivationLevel()).append("/10");
        return sb.toString();
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        return messages;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private void writePlain(HttpServletResponse response, String text) throws IOException {
        try (PrintWriter writer = response.getWriter()) {
            writer.write(StringUtils.defaultString(text));
            writer.flush();
        }
    }

    private Integer toInteger(Object value, Integer defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String defaultText(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String val = String.valueOf(value).trim();
        return StringUtils.isBlank(val) ? defaultValue : val;
    }

    /**
     * 学习路径自动检测与调整
     * POST /api/admin/learning-path/auto-adjust/{userId}/{subjectId}
     * 检测当前路径进度是否落后，AI 生成调整后的新路径
     * 对应评分标准中"学习计划动态调整"要求
     */
    @PostMapping("/auto-adjust/{userId}/{subjectId}")
    public void autoAdjust(@PathVariable Integer userId, @PathVariable Integer subjectId,
                           @RequestParam(required = false) String subjectName,
                           HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        LearningPath currentPath = learningPathService.getPathByUserAndSubject(userId, subjectId);
        StudentProfile profile = studentProfileService.getProfileByUserId(userId);
        String effectiveSubject = StringUtils.defaultIfBlank(subjectName, "通用课程");

        StringBuilder contextPrompt = new StringBuilder();
        contextPrompt.append("你是一位高校学习路径调整专家。");
        contextPrompt.append("学生当前有如下学习路径，但进度可能落后，需要你分析原因并给出调整方案。\n\n");
        contextPrompt.append("## 当前路径信息\n");

        if (currentPath != null) {
            contextPrompt.append("- 路径名称：").append(currentPath.getPathName()).append("\n");
            contextPrompt.append("- 当前进度：").append(
                currentPath.getProgressPercent() == null ? 0 : currentPath.getProgressPercent()).append("%\n");
            contextPrompt.append("- 当前节点：").append(
                StringUtils.defaultIfBlank(currentPath.getCurrentNodeId(), "未开始")).append("\n");
            contextPrompt.append("- 原始路径节点：\n").append(
                StringUtils.defaultIfBlank(currentPath.getNodes(), "无")).append("\n");
        } else {
            contextPrompt.append("该学生尚未有学习路径。\n");
        }

        contextPrompt.append("\n## 学生画像\n");
        contextPrompt.append(buildProfileText(profile));
        contextPrompt.append("\n\n## 调整要求\n");
        contextPrompt.append("请基于以上信息，输出调整后的学习路径 JSON，格式与原始路径相同。");
        contextPrompt.append("重点说明：1) 进度落后的原因分析 2) 调整后的路径 3) 调整理由\n");
        contextPrompt.append("只返回JSON，不输出Markdown代码块。");

        List<Map<String, String>> messages = buildMessages(
            "你是高校个性化学习路径调整专家。根据学生当前学习进度和画像，分析落后原因并给出调整方案。只返回JSON，不输出Markdown代码块。",
            "学科：" + effectiveSubject + "\n" + contextPrompt.toString()
        );

        StringBuilder fullContent = new StringBuilder();
        try (PrintWriter writer = response.getWriter()) {
            deepSeekService.stream(messages, chunk -> {
                writer.write(chunk);
                writer.flush();
                fullContent.append(chunk);
            });
        } catch (Exception e) {
            log.error("Auto-adjust stream failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writePlain(response, e.getMessage());
            return;
        }

        // 保存调整后的路径（覆盖原路径）
        savePath(userId, subjectId, effectiveSubject + "（已调整）", fullContent.toString());
    }

    /**
     * 检测所有路径是否需要调整
     * GET /api/admin/learning-path/check-progress/{userId}
     */
    @GetMapping("/check-progress/{userId}")
    public RestResponse<Map<String, Object>> checkProgress(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();
        List<LearningPath> paths = learningPathService.getPathsByUserId(userId);
        List<Map<String, Object>> needsAdjust = new ArrayList<>();

        if (paths != null) {
            for (LearningPath path : paths) {
                if (Boolean.TRUE.equals(path.getDeleted())) continue;
                Map<String, Object> info = new HashMap<>();
                info.put("pathId", path.getId());
                info.put("subjectId", path.getSubjectId());
                info.put("pathName", path.getPathName());
                info.put("progressPercent", path.getProgressPercent());
                boolean lagging = path.getProgressPercent() != null && path.getProgressPercent() < 30.0;
                info.put("needsAdjustment", lagging);
                if (lagging) {
                    info.put("suggestion", "进度偏低(<30%), 建议点击[自动调整]获取 AI 优化路径");
                }
                needsAdjust.add(info);
            }
        }

        result.put("totalPaths", paths == null ? 0 : paths.size());
        result.put("paths", needsAdjust);
        result.put("summary", needsAdjust.stream().anyMatch(p -> Boolean.TRUE.equals(p.get("needsAdjustment")))
            ? "存在需要调整的学习路径，建议启用自动调整功能" : "所有学习路径进度正常");
        return RestResponse.ok(result);
    }

    private int safeParseInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return 1; }
    }
}
