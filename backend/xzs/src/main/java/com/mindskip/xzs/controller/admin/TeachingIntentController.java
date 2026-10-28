package com.mindskip.xzs.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.service.DeepSeekService;
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
@RequestMapping("/api/admin/teaching-intent")
public class TeachingIntentController {

    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeachingIntentController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping("/analyze")
    public void analyzeTeachingIntent(@RequestBody Map<String, Object> request, HttpServletResponse response) throws IOException {
        streamTextResponse(response, buildMessages(
            "你是一位高等教育课程教学设计专家。请将教师的教学意图深度拆解为结构化的教学设计方案，包含知识点体系、教学逻辑顺序、重难点分析、参考资料整合、知识融合策略及课件生成指令。面向本科及以上学历层次教学，注重学科前沿性与学术深度。",
            buildAnalyzePrompt(request)
        ));
    }

    @PostMapping("/test-analyze")
    public RestResponse<Map<String, Object>> testAnalyzeTeachingIntent(@RequestBody Map<String, Object> request) {
        try {
            String content = deepSeekService.complete(buildMessages(
                "你是一位高等教育课程教学设计专家。请将教师的教学意图深度拆解为结构化的教学设计方案，包含知识点体系、教学逻辑顺序、重难点分析、参考资料整合、知识融合策略及课件生成指令。面向本科及以上学历层次教学，注重学科前沿性与学术深度。",
                buildAnalyzePrompt(request)
            ));
            return RestResponse.ok(parseStructuredAnalysis(content));
        } catch (Exception e) {
            return RestResponse.fail(500, e.getMessage());
        }
    }

    @PostMapping("/generate-ppt")
    public void generatePPT(@RequestBody Map<String, Object> request, HttpServletResponse response) throws IOException {
        streamTextResponse(response, buildMessages(
            "你是一位高校多媒体教学设计专家。请将教学分析结果转化为结构完整、逻辑清晰、可直接用于高校课堂教学的PPT内容，至少包含封面、目录、教学目标、核心知识点、课堂活动、案例分析或练习、总结与思考题。",
            buildPptPrompt(request)
        ));
    }

    @PostMapping("/generate-word")
    public void generateWordLessonPlan(@RequestBody Map<String, Object> request, HttpServletResponse response) throws IOException {
        streamTextResponse(response, buildMessages(
            "你是一位高校教师教学发展培训专家。请生成一份完整、规范的高校课程教案，至少包含教学目标（知识/能力/素养三维目标）、学情分析、教学重难点、教学过程设计、课堂互动策略、板书或课件框架、课后作业与评价方式、教学反思要点。",
            buildWordPrompt(request)
        ));
    }

    @PostMapping("/generate-content")
    public RestResponse<Map<String, Object>> generateContent(@RequestBody Map<String, Object> request) {
        try {
            String content = deepSeekService.complete(buildMessages(
                "你是一位高校教学创新设计专家。请围绕教学内容生成丰富多样的课堂互动方案，包括可视化动画创意、课堂互动游戏和多元化教学方法，所有内容需贴合本科及以上层次教学实际场景。生成JSON格式。",
                buildDiverseContentPrompt(request)
            ));

            Map<String, Object> normalized = parseJsonOrWrap(content);
            return RestResponse.ok(normalized);
        } catch (Exception e) {
            return RestResponse.fail(500, e.getMessage());
        }
    }

    private void streamTextResponse(HttpServletResponse response, List<Map<String, String>> messages) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

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

    private String buildAnalyzePrompt(Map<String, Object> request) {
        String teachingIntent = text(request.get("teachingIntent"));
        List<Map<String, Object>> materials = extractReferenceMaterials(request.get("referenceMaterials"));

        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下教学意图和参考材料，输出一份结构化分析结果。\n\n");
        prompt.append("教学意图：\n").append(StringUtils.defaultIfBlank(teachingIntent, "暂无描述")).append("\n\n");
        if (!materials.isEmpty()) {
            prompt.append("参考材料：\n");
            for (int i = 0; i < materials.size(); i++) {
                Map<String, Object> item = materials.get(i);
                prompt.append(i + 1).append(". ")
                    .append(text(item.get("name")))
                    .append("（").append(text(item.get("type"))).append("）\n");
            }
            prompt.append("\n");
        }
        prompt.append("请严格按下面格式输出，保留编号标题：\n");
        prompt.append("1. 知识点清单：使用顿号分隔\n");
        prompt.append("2. 逻辑顺序：详细说明知识讲解路径\n");
        prompt.append("3. 重点：使用顿号分隔\n");
        prompt.append("4. 难点：使用顿号分隔\n");
        prompt.append("5. 参考资料分析：结合参考材料总结价值\n");
        prompt.append("6. 知识融合结果：说明如何融合教学意图、资料和课堂活动\n");
        prompt.append("7. 课件生成指令集：输出一份可直接用于生成课件的详细指令");
        return prompt.toString();
    }

    private String buildPptPrompt(Map<String, Object> request) {
        return new StringBuilder()
            .append("请根据以下教学信息生成一份可直接制作成 PPT 的完整内容。\n\n")
            .append("教学意图：\n").append(text(request.get("teachingIntent"))).append("\n\n")
            .append("分析结果：\n").append(formatAnalysisResult(request.get("analysisResult"))).append("\n\n")
            .append("输出要求：\n")
            .append("1. 使用 Markdown。\n")
            .append("2. 至少包含封面、目录、教学目标、核心知识点、课堂活动、案例或练习、总结。\n")
            .append("3. 每个一级标题下给出可直接展示在幻灯片里的要点内容。\n")
            .toString();
    }

    private String buildWordPrompt(Map<String, Object> request) {
        return new StringBuilder()
            .append("请根据以下教学信息生成一份完整的 Word 教案正文。\n\n")
            .append("教学意图：\n").append(text(request.get("teachingIntent"))).append("\n\n")
            .append("分析结果：\n").append(formatAnalysisResult(request.get("analysisResult"))).append("\n\n")
            .append("输出要求：\n")
            .append("1. 使用 Markdown。\n")
            .append("2. 至少包含教学目标、学情分析、重难点、教学流程、互动设计、板书设计、作业与评价。\n")
            .append("3. 内容要求具体、成体系、可直接落地。\n")
            .toString();
    }

    private String buildDiverseContentPrompt(Map<String, Object> request) {
        return new StringBuilder()
            .append("请根据以下教学信息生成课堂多样化内容，并且只返回 JSON，不要输出 Markdown 代码块。\n\n")
            .append("教学意图：\n").append(text(request.get("teachingIntent"))).append("\n\n")
            .append("分析结果：\n").append(formatAnalysisResult(request.get("analysisResult"))).append("\n\n")
            .append("JSON 格式要求：\n")
            .append("{\n")
            .append("  \"animationIdeas\": [{\n")
            .append("    \"title\": \"动画标题\",\n")
            .append("    \"description\": \"动画说明\",\n")
            .append("    \"animationCode\": \"animation-type-keyword\",\n")
            .append("    \"htmlCode\": \"完整的HTML5 Canvas动画代码，用<script>标签包裹JS，用canvas元素绘图，实现与教学主题相关的可视化动画效果\"\n")
            .append("  }],\n")
            .append("  \"interactiveGames\": [{\n")
            .append("    \"title\": \"游戏标题\",\n")
            .append("    \"description\": \"游戏说明\",\n")
            .append("    \"playable\": true,\n")
            .append("    \"gameCode\": \"game-type-keyword\",\n")
            .append("    \"htmlCode\": \"完整的HTML5互动游戏代码，包含HTML结构和<script>标签中的JS逻辑，实现选择题、填空题、拖拽匹配、记忆翻牌等浏览器内可直接运行的互动小游戏\"\n")
            .append("  }],\n")
            .append("  \"creativeMethods\": [\"具体创意方法1\", \"具体创意方法2\", \"具体创意方法3\"]\n")
            .append("}\n")
            .append("要求：\n")
            .append("1. htmlCode必须是完整的、可在浏览器直接运行的HTML代码片段\n")
            .append("2. 动画类用Canvas API实现教学可视化效果\n")
            .append("3. 游戏类实现浏览器内可交互的网页小游戏（选择题、拖拽、翻牌等）\n")
            .append("4. 所有代码要贴合教学场景\n")
            .append("5. 内容要具体、可执行、贴合课堂实际")
            .toString();
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

    private List<Map<String, Object>> extractReferenceMaterials(Object rawMaterials) {
        List<Map<String, Object>> materials = new ArrayList<>();
        if (!(rawMaterials instanceof List)) {
            return materials;
        }
        for (Object item : (List<?>) rawMaterials) {
            if (item instanceof Map) {
                materials.add((Map<String, Object>) item);
            }
        }
        return materials;
    }

    private String formatAnalysisResult(Object analysisResult) {
        if (!(analysisResult instanceof Map)) {
            return "暂无结构化分析结果";
        }
        Map<?, ?> map = (Map<?, ?>) analysisResult;
        StringBuilder builder = new StringBuilder();
        builder.append("知识点清单：").append(joinValue(map.get("knowledgePoints"))).append("\n");
        builder.append("逻辑顺序：").append(joinValue(map.get("logicalOrder"))).append("\n");
        builder.append("重点：").append(joinValue(map.get("keyPoints"))).append("\n");
        builder.append("难点：").append(joinValue(map.get("difficultPoints"))).append("\n");
        builder.append("参考资料分析：").append(joinValue(map.get("referenceAnalysis"))).append("\n");
        builder.append("知识融合结果：").append(joinValue(map.get("knowledgeIntegration"))).append("\n");
        builder.append("课件生成指令集：").append(joinValue(map.get("lessonPlanInstructions")));
        return builder.toString();
    }

    private String joinValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> items = new ArrayList<>();
            for (Object item : list) {
                if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    items.add(String.valueOf(item));
                }
            }
            return String.join("、", items);
        }
        return text(value);
    }

    private Map<String, Object> parseJsonOrWrap(String content) {
        try {
            String cleaned = content.replace("```json", "").replace("```", "").trim();
            if (cleaned.startsWith("{")) {
                return objectMapper.readValue(cleaned, Map.class);
            }
        } catch (Exception ignored) {
        }
        Map<String, Object> result = new HashMap<>();
        result.put("rawContent", content);
        result.put("content", content);
        return result;
    }

    private Map<String, Object> parseStructuredAnalysis(String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("knowledgePoints", splitSection(extractSection(content, "1. 知识点清单：", "2. 逻辑顺序：")));
        result.put("logicalOrder", extractSection(content, "2. 逻辑顺序：", "3. 重点："));
        result.put("keyPoints", splitSection(extractSection(content, "3. 重点：", "4. 难点：")));
        result.put("difficultPoints", splitSection(extractSection(content, "4. 难点：", "5. 参考资料分析：")));
        result.put("referenceAnalysis", extractSection(content, "5. 参考资料分析：", "6. 知识融合结果："));
        result.put("knowledgeIntegration", extractSection(content, "6. 知识融合结果：", "7. 课件生成指令集："));
        result.put("lessonPlanInstructions", extractSection(content, "7. 课件生成指令集：", null));
        return result;
    }

    private List<String> splitSection(String section) {
        List<String> values = new ArrayList<>();
        if (StringUtils.isBlank(section)) {
            return values;
        }
        String[] parts = section.replace("\n", "、").split("[、；;，,]");
        for (String part : parts) {
            String item = part.trim();
            if (StringUtils.isNotBlank(item)) {
                values.add(item);
            }
        }
        return values;
    }

    private String extractSection(String content, String startMarker, String endMarker) {
        int startIndex = content.indexOf(startMarker);
        if (startIndex < 0) {
            return "";
        }
        startIndex += startMarker.length();
        int endIndex = endMarker == null ? content.length() : content.indexOf(endMarker, startIndex);
        if (endIndex < 0) {
            endIndex = content.length();
        }
        return content.substring(startIndex, endIndex).trim();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
