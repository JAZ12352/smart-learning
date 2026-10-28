package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.service.DeepSeekService;
import com.mindskip.xzs.service.SubjectService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/deepSeek/api")
public class DeepSeekPager {

    private final DeepSeekService deepSeekService;
    private final SubjectService subjectService;

    public DeepSeekPager(DeepSeekService deepSeekService, SubjectService subjectService) {
        this.deepSeekService = deepSeekService;
        this.subjectService = subjectService;
    }

    @PostMapping("/chat")
    public RestResponse<Map<String, Object>> chat(@RequestBody Map<String, Object> requestData) {
        try {
            Integer questionType = toInteger(requestData.get("questionType"), 1);
            Integer gradeLevel = toInteger(requestData.get("gradeLevel"), 1);
            String subject = normalizeSubject(requestData.get("subjectId"));
            String typeName = questionTypeToText(questionType);

            // ── 第一步：AI 生成题干 + 选项 ──
            String step1Prompt = buildStep1Prompt(questionType, gradeLevel, subject, typeName);
            String step1Output = cleanupModelText(deepSeekService.complete(buildMessages(
                "你是一位高校课程考试命题专家。只输出题目内容，格式：题干：xxx\\n选项：\\nA) xxx\\nB) xxx... 题干和选项都不含答案提示。禁止开场白。",
                step1Prompt
            )));

            // ── 第二步：AI 分析正确答案 + 解析 ──
            String step2Prompt = buildStep2Prompt(questionType, step1Output);
            String step2Output = cleanupModelText(deepSeekService.complete(buildMessages(
                "你是一位高校学科专家。根据给定的题目和选项，给出正确答案和详细解析。只输出：答案：X\\n解析：... 禁止开场白。",
                step2Prompt
            )));

            // ── 组装结果 ──
            Map<String, Object> result = new HashMap<>();
            result.put("title", extractStem(step1Output));
            result.put("items", extractItems(step1Output, questionType));
            result.put("correct", extractAnswer(step2Output));
            result.put("analyze", extractAnalyze(step2Output));
            return RestResponse.ok(result);
        } catch (Exception e) {
            return RestResponse.fail(2, e.getMessage());
        }
    }

    private String buildStep1Prompt(Integer qType, Integer grade, String subject, String typeName) {
        String base = String.format("请为%d年级%s学科生成1道高质量%s。", grade, subject, typeName);
        if (qType == 1 || qType == 2) {
            return base + "\n输出格式:\n题干：<题目>\n选项：\nA) ...\nB) ...\nC) ...\nD) ...";
        } else if (qType == 3) {
            return base + "\n输出格式:\n题干：<陈述>\n选项：\nA) 正确\nB) 错误";
        } else if (qType == 4) {
            return base + "\n输出格式:\n题干：<含______的陈述>";
        } else {
            return base + "\n输出格式:\n题干：<问题>";
        }
    }

    private String buildStep2Prompt(Integer qType, String step1Output) {
        if (qType == 1 || qType == 2) {
            return "以下是一道选择题，请给出正确答案和详细解析：\n\n" + step1Output +
                "\n\n输出格式:\n答案：<字母>\n解析：<说明正确选项为何正确，错误选项为何错误>";
        } else if (qType == 3) {
            return "以下是一道判断题，请判断正误并解析：\n\n" + step1Output +
                "\n\n输出格式:\n答案：A(正确)或B(错误)\n解析：<判断依据>";
        } else if (qType == 4) {
            return "以下是一道填空题，请给出正确答案和解析：\n\n" + step1Output +
                "\n\n输出格式:\n答案：<正确答案>\n解析：<解题过程>";
        } else {
            return "以下是一道简答题，请给出参考答案和解析：\n\n" + step1Output +
                "\n\n输出格式:\n答案：<要点>\n解析：<知识拓展>";
        }
    }

    // ── 解析工具方法 ──
    private String extractStem(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "题干[：:]\\s*([\\s\\S]*?)(?=\\n选项[：:]|\\n答案[：:]|\\nA\\)|\\n解析[：:]|$)",
            java.util.regex.Pattern.DOTALL).matcher(text);
        if (m.find()) return m.group(1).trim();
        // fallback: 取前 3 行
        String[] lines = text.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 3); i++) {
            if (lines[i].matches("^\\s*[A-D][\\)）]") || lines[i].contains("答案：") || lines[i].contains("解析：")) break;
            sb.append(lines[i]).append("\n");
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractItems(String text, Integer qType) {
        List<Map<String, String>> items = new ArrayList<>();
        // 提取 A) ... B) ... 格式的选项
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "^\\s*([A-Za-z])[\\)）]\\s*(.+)$", java.util.regex.Pattern.MULTILINE).matcher(text);
        while (m.find()) {
            Map<String, String> item = new HashMap<>();
            item.put("prefix", m.group(1).toUpperCase());
            item.put("content", m.group(2).trim());
            items.add(item);
        }
        if (items.isEmpty() && (qType == 1 || qType == 2)) {
            // 默认 ABCD
            for (char c = 'A'; c <= 'D'; c++) {
                Map<String, String> item = new HashMap<>();
                item.put("prefix", String.valueOf(c));
                item.put("content", "");
                items.add(item);
            }
        }
        if (items.isEmpty() && qType == 3) {
            Map<String, String> a = new HashMap<>(); a.put("prefix", "A"); a.put("content", "正确"); items.add(a);
            Map<String, String> b = new HashMap<>(); b.put("prefix", "B"); b.put("content", "错误"); items.add(b);
        }
        return items;
    }

    private String extractAnswer(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "答案[：:]\\s*([^\\n]+)").matcher(text);
        if (m.find()) return m.group(1).trim();
        // fallback: 取第一行
        String line = text.split("\\n")[0].trim();
        return line.replaceAll("^[^A-Za-z0-9]*", "");
    }

    private String extractAnalyze(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "解析[：:]\\s*([\\s\\S]*?)$", java.util.regex.Pattern.DOTALL).matcher(text);
        if (m.find()) return m.group(1).trim();
        // fallback: 去掉答案行后的所有内容
        String[] parts = text.split("\\n", 2);
        return parts.length > 1 ? parts[1].replaceAll("^答案[：:][^\\n]*\\n?", "").trim() : "";
    }

    @PostMapping("/page")
    public void page(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        try (PrintWriter writer = response.getWriter()) {
            String systemPrompt = "你是一位高等教育智能教学助手。请按照教师的具体要求输出可直接应用于高校课堂教学的内容，内容需具备学术性、系统性和可操作性，适合本科及以上学历层次的教学场景。直接输出教学内容，不得包含任何元描述或过程性语句。";
            String userPrompt = buildPagePrompt(requestData);
            deepSeekService.stream(buildMessages(systemPrompt, userPrompt), chunk -> {
                writer.write(chunk);
                writer.flush();
            });
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(e.getMessage());
                writer.flush();
            }
        }
    }

    @PostMapping("/general-chat")
    public RestResponse<Map<String, Object>> generalChat(@RequestBody Map<String, Object> requestData) {
        try {
            List<Map<String, String>> messages = extractMessages(requestData);
            if (messages.isEmpty()) {
                return RestResponse.fail(2, "消息不能为空");
            }
            String reply = cleanupModelText(deepSeekService.complete(messages));
            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            return RestResponse.ok(result);
        } catch (Exception e) {
            return RestResponse.fail(2, e.getMessage());
        }
    }

    @PostMapping("/stream-chat")
    public void streamChat(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        List<Map<String, String>> messages = extractMessages(requestData);
        if (messages.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter writer = response.getWriter()) {
                writer.write("消息不能为空");
                writer.flush();
            }
            return;
        }

        try (PrintWriter writer = response.getWriter()) {
            deepSeekService.stream(messages, chunk -> {
                writer.write(chunk);
                writer.flush();
            });
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(e.getMessage());
                writer.flush();
            }
        }
    }

    private String buildPagePrompt(Map<String, Object> requestData) {
        Integer gradeLevel = toInteger(requestData.get("gradeLevel"), 1);
        String subject = normalizeSubject(requestData.get("subjectId"));
        String title = defaultText(requestData.get("title"), "未命名教案");
        String type = defaultText(requestData.get("type"), "图文教案");
        String content = defaultText(requestData.get("content"), "请围绕主题完整展开");
        boolean generatePpt = Boolean.TRUE.equals(requestData.get("generatePPT"));
        boolean generateContent = Boolean.TRUE.equals(requestData.get("generateContent"));

        if (generateContent) {
            return String.format(
                "请围绕以下教学主题生成多样化教学创意，并且只返回 JSON，不要输出 Markdown 代码块。\n" +
                    "年级：%d\n学科：%s\n主题：%s\n教案类型：%s\n教学目标：%s\n\n" +
                    "JSON 格式要求：\n" +
                    "{\n" +
                    "  \"animationIdeas\": [{\"title\":\"动画标题\",\"description\":\"动画说明\",\"animationCode\":\"animation-type-keyword\",\"htmlCode\":\"完整 HTML5 Canvas 动画代码\"}],\n" +
                    "  \"interactiveGames\": [{\"title\":\"游戏标题\",\"description\":\"游戏说明\",\"playable\":true,\"gameCode\":\"game-type-keyword\",\"htmlCode\":\"完整 HTML5 互动游戏代码\"}],\n" +
                    "  \"creativeMethods\": [\"具体创意方法1\", \"具体创意方法2\", \"具体创意方法3\"]\n" +
                    "}\n" +
                    "要求：代码必须可在浏览器直接运行，内容贴合%s学科和《%s》主题。",
                gradeLevel, subject, title, type, content, subject, title
            );
        }

        if (generatePpt) {
            return String.format(
                "为 %d 年级%s《%s》生成 PPT 完整内容大纲。教案类型：%s。教学目标：%s。\n" +
                    "包含：封面、目录、教学目标、核心知识点、课堂活动、总结与作业。输出 Markdown。",
                gradeLevel, subject, title, type, content
            );
        }

        return String.format(
            "请为 %d 年级%s《%s》生成一份完整教案。教案类型：%s。教学目标：%s。\n" +
                "请使用 Markdown 输出，至少包含：教学目标、重难点、知识讲解、教学过程、课堂互动、作业设计、教学反思建议。",
            gradeLevel, subject, title, type, content
        );
    }

    private List<Map<String, String>> extractMessages(Map<String, Object> requestData) {
        Object rawMessages = requestData.get("messages");
        List<Map<String, String>> messages = new ArrayList<>();

        if (rawMessages instanceof List) {
            List<?> list = (List<?>) rawMessages;
            for (Object item : list) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> source = (Map<?, ?>) item;
                String role = String.valueOf(source.get("role"));
                String content = source.get("content") == null ? "" : String.valueOf(source.get("content"));
                if (StringUtils.isNotBlank(role) && StringUtils.isNotBlank(content)) {
                    messages.add(message(role, content));
                }
            }
        }

        if (messages.isEmpty()) {
            String msgContent = requestData.get("message") == null ? "" : String.valueOf(requestData.get("message"));
            if (StringUtils.isNotBlank(msgContent)) {
                messages.add(message("user", msgContent));
            }
        }

        return messages;
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        return messages;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
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

    private String normalizeSubject(Object subjectValue) {
        String subject = defaultText(subjectValue, "通用学科").toLowerCase();
        // 支持数字ID查学科名
        try {
            int id = Integer.parseInt(subject);
            if (id > 0) {
                com.mindskip.xzs.domain.Subject s = subjectService.selectById(id);
                if (s != null && s.getName() != null) return s.getName();
            }
        } catch (NumberFormatException ignored) {}
        if ("math".equals(subject)) return "数学";
        if ("chinese".equals(subject)) return "语文";
        if ("english".equals(subject)) return "英语";
        if ("physics".equals(subject)) return "物理";
        if ("chemistry".equals(subject)) return "化学";
        if ("biology".equals(subject)) return "生物";
        if ("history".equals(subject)) return "历史";
        if ("geography".equals(subject)) return "地理";
        return subjectValue == null ? "通用学科" : String.valueOf(subjectValue);
    }

    private String questionTypeToText(Integer questionType) {
        if (questionType == null) {
            return "选择题";
        }
        switch (questionType) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "判断题";
            case 4: return "填空题";
            case 5: return "简答题";
            default: return "题目";
        }
    }

    private String cleanupModelText(String text) {
        return DeepSeekService.sanitizeContent(text);
    }

    private String defaultText(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.isBlank(text) ? defaultValue : text;
    }
}
