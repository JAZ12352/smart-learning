package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.DialogueSession;
import com.mindskip.xzs.domain.StudentProfile;
import com.mindskip.xzs.service.DeepSeekService;
import com.mindskip.xzs.service.DialogueSessionService;
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
@RequestMapping("/api/admin/student-profile")
public class StudentProfileController {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileController.class);

    private final StudentProfileService studentProfileService;
    private final DialogueSessionService dialogueSessionService;
    private final DeepSeekService deepSeekService;

    public StudentProfileController(StudentProfileService studentProfileService,
                                    DialogueSessionService dialogueSessionService,
                                    DeepSeekService deepSeekService) {
        this.studentProfileService = studentProfileService;
        this.dialogueSessionService = dialogueSessionService;
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/{userId}")
    public RestResponse<StudentProfile> getProfile(@PathVariable String userId) {
        int uid = safeParseInt(userId);
        StudentProfile profile = studentProfileService.getProfileByUserId(uid);
        if (profile == null) {
            profile = createEmptyProfile(uid);
        }
        return RestResponse.ok(profile);
    }

    @PostMapping("/{userId}/update")
    public RestResponse updateProfile(@PathVariable String userId, @RequestBody StudentProfile profile) {
        int uid = safeParseInt(userId);
        profile.setUserId(uid);
        StudentProfile existing = studentProfileService.getProfileByUserId(uid);
        if (existing == null) {
            profile.setProfileVersion(1);
            profile.setCreateTime(new Date());
            profile.setModifyTime(new Date());
            profile.setDeleted(false);
            studentProfileService.insert(profile);
        } else {
            profile.setId(existing.getId());
            profile.setProfileVersion(existing.getProfileVersion() == null ? 1 : existing.getProfileVersion() + 1);
            profile.setModifyTime(new Date());
            studentProfileService.updateByIdFilter(profile);
        }
        return RestResponse.ok();
    }

    @PostMapping("/{userId}/dialogue")
    public void dialogue(@PathVariable String userId, @RequestBody Map<String, Object> requestData,
                         HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        int uid = safeParseInt(userId);
        String userMessage = text(requestData.get("message"));
        if (StringUtils.isBlank(userMessage)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writePlain(response, "消息不能为空");
            return;
        }

        StudentProfile existingProfile = studentProfileService.getProfileByUserId(uid);
        boolean hasProfile = existingProfile != null && StringUtils.isNotBlank(existingProfile.getProfileSummary());

        // 计算已填充和未填充的维度
        List<String> unfilled = new ArrayList<>();
        if (existingProfile == null || StringUtils.isBlank(existingProfile.getKnowledgeLevel())) unfilled.add("知识基础");
        if (existingProfile == null || StringUtils.isBlank(existingProfile.getCognitiveStyle())) unfilled.add("认知风格");
        if (existingProfile == null || StringUtils.isBlank(existingProfile.getWeakPoints())) unfilled.add("薄弱点");
        if (existingProfile == null || StringUtils.isBlank(existingProfile.getLearningGoals())) unfilled.add("学习目标");
        if (existingProfile == null || StringUtils.isBlank(existingProfile.getLearningPace())) unfilled.add("学习节奏");
        if (existingProfile == null || StringUtils.isBlank(existingProfile.getInterests())) unfilled.add("兴趣方向");
        if (existingProfile == null || StringUtils.isBlank(existingProfile.getLearningStyle())) unfilled.add("学习风格");
        if (existingProfile == null || existingProfile.getMotivationLevel() == null || existingProfile.getMotivationLevel() <= 0) unfilled.add("动机水平(1-10分)");
        int filledCount = 8 - unfilled.size();
        String unfilledList = unfilled.isEmpty() ? "全部完成" : String.join("、", unfilled);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system",
            "你是学习画像助手，通过简短对话收集学生信息。\n\n" +
            "进度：" + filledCount + "/8。待填：" + unfilledList + "\n" +
            (hasProfile ? "已填：\n" + buildProfileText(existingProfile) + "\n" : "") +
            "\n规则：\n" +
            "1. 每次只问 1 个待填维度，问完跳下一个\n" +
            "2. 用户回答后先输出【维度名：值】，再问下一个问题\n" +
            "3. 不重复问已填维度\n" +
            "4. 动机水平让用户打 1-10 分\n" +
            "5. 8/8 时输出 30 字内画像总结\n" +
            "6. 回复极简：1 句提取结果 + 1 句提问，问题不超过 15 字，不要寒暄"));

        // 从请求中获取前端发来的对话历史
        Object rawMessages = requestData.get("messages");
        if (rawMessages instanceof List) {
            List<?> list = (List<?>) rawMessages;
            for (int i = 0; i < Math.min(list.size(), 20); i++) {
                Object item = list.get(i);
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    String role = String.valueOf(m.get("role"));
                    String content = m.get("content") == null ? "" : String.valueOf(m.get("content"));
                    if (("user".equals(role) || "assistant".equals(role)) && StringUtils.isNotBlank(content)) {
                        messages.add(message(role, content));
                    }
                }
            }
        }

        StringBuilder fullResponse = new StringBuilder();

        try (PrintWriter writer = response.getWriter()) {
            deepSeekService.stream(messages, chunk -> {
                writer.write(chunk);
                writer.flush();
                fullResponse.append(chunk);
            });
        } catch (Exception e) {
            log.error("Profile dialogue stream failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writePlain(response, e.getMessage());
            return;
        }

        saveDialogue(uid, userMessage, fullResponse.toString());
    }

    @PostMapping("/{userId}/explain")
    public void explainProfile(@PathVariable String userId, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        int uid = safeParseInt(userId);
        StudentProfile profile = studentProfileService.getProfileByUserId(uid);
        if (profile == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writePlain(response, "学生画像不存在");
            return;
        }

        List<Map<String, String>> messages = buildMessages(
            "你是一位高校学业指导与生涯规划专家。请深度解读大学生学习画像数据，基于高等教育人才培养规律和大学生发展理论，给出个性化的学习资源推荐理由、最优学习路径建议和学业辅导策略。输出需逻辑清晰、有理有据、可操作，兼顾学生的学术发展与综合素质提升。",
            "请解读以下学生画像：\n" + buildProfileText(profile)
        );

        try (PrintWriter writer = response.getWriter()) {
            deepSeekService.stream(messages, chunk -> {
                writer.write(chunk);
                writer.flush();
            });
        } catch (Exception e) {
            log.error("Profile explanation stream failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writePlain(response, e.getMessage());
        }
    }

    private int safeParseInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return 1; }
    }

    private StudentProfile createEmptyProfile(Integer userId) {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setKnowledgeLevel("");
        profile.setCognitiveStyle("");
        profile.setWeakPoints("");
        profile.setLearningGoals("");
        profile.setLearningPace("");
        profile.setInterests("");
        profile.setLearningStyle("");
        profile.setMotivationLevel(5);
        profile.setProfileSummary("");
        profile.setProfileVersion(0);
        return profile;
    }

    private void saveDialogue(Integer userId, String userMessage, String assistantMessage) {
        try {
            DialogueSession session = new DialogueSession();
            session.setUserId(userId);
            session.setSessionType("profile_building");
            session.setMessages("学生：" + userMessage + "\n画像Agent：" + assistantMessage);
            session.setStatus("active");
            session.setCreateTime(new Date());
            session.setModifyTime(new Date());
            dialogueSessionService.insertByFilter(session);
        } catch (Exception e) {
            log.warn("Failed to save dialogue session: {}", e.getMessage());
        }
    }

    private List<Map<String, String>> loadSessionHistory(String sessionId) {
        List<Map<String, String>> history = new ArrayList<>();
        try {
            DialogueSession session = dialogueSessionService.selectById(safeParseInt(sessionId));
            if (session != null && StringUtils.isNotBlank(session.getMessages())) {
                history.add(message("assistant", session.getMessages()));
            }
        } catch (Exception e) {
            log.warn("Failed to load dialogue history: {}", e.getMessage());
        }
        return history;
    }

    private String buildProfileText(StudentProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("知识基础：").append(defaultText(profile.getKnowledgeLevel(), "未评估")).append("\n");
        sb.append("认知风格：").append(defaultText(profile.getCognitiveStyle(), "未评估")).append("\n");
        sb.append("薄弱点：").append(defaultText(profile.getWeakPoints(), "未评估")).append("\n");
        sb.append("学习目标：").append(defaultText(profile.getLearningGoals(), "未评估")).append("\n");
        sb.append("学习节奏：").append(defaultText(profile.getLearningPace(), "未评估")).append("\n");
        sb.append("兴趣方向：").append(defaultText(profile.getInterests(), "未评估")).append("\n");
        sb.append("学习风格：").append(defaultText(profile.getLearningStyle(), "未评估")).append("\n");
        sb.append("动机水平：").append(profile.getMotivationLevel() == null ? "5" : profile.getMotivationLevel()).append("/10\n");
        sb.append("画像摘要：").append(defaultText(profile.getProfileSummary(), "暂无")).append("\n");
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

    private String defaultText(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String val = String.valueOf(value).trim();
        return StringUtils.isBlank(val) ? defaultValue : val;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
