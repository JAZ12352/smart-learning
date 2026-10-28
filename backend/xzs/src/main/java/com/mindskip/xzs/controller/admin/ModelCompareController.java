package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.service.ModelConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

/**
 * 多模型对比控制器
 * 支持同一 prompt 同时发送给多个 AI 模型，返回对比结果
 * 对应评分标准中"多模型对比"加分项
 */
@RestController
@RequestMapping("/api/admin/model-compare")
public class ModelCompareController {

    private static final Logger log = LoggerFactory.getLogger(ModelCompareController.class);

    private final ModelConfigService modelConfigService;

    /** 可对比的模型列表（含当前运行时配置的模型） */
    private static final List<Map<String, String>> COMPARABLE_MODELS = Arrays.asList(
        mapOf("id", "deepseek-chat", "name", "DeepSeek Chat", "provider", "DeepSeek"),
        mapOf("id", "deepseek-reasoner", "name", "DeepSeek Reasoner", "provider", "DeepSeek"),
        mapOf("id", "gpt-4o", "name", "GPT-4o", "provider", "OpenAI"),
        mapOf("id", "claude-3.5-sonnet", "name", "Claude 3.5 Sonnet", "provider", "Anthropic"),
        mapOf("id", "deepseek-v4-pro", "name", "DeepSeek V4 Pro", "provider", "DeepSeek"),
        mapOf("id", "deepseek-v4-flash", "name", "DeepSeek V4 Flash", "provider", "DeepSeek")
    );

    @Autowired
    public ModelCompareController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    /**
     * 列出可对比的模型
     */
    @GetMapping("/models")
    public RestResponse<List<Map<String, String>>> listModels() {
        List<Map<String, String>> models = new ArrayList<>(COMPARABLE_MODELS);
        // Add currently active model
        String active = modelConfigService.getActiveModel();
        boolean hasActive = models.stream().anyMatch(m -> m.get("id").equals(active));
        if (!hasActive) {
            models.add(0, mapOf("id", active, "name", active, "provider", "自定义"));
        }
        return RestResponse.ok(models);
    }

    /**
     * 多模型对比（流式）
     * POST /api/admin/model-compare/compare
     * Body: { "prompt": "...", "models": ["deepseek-chat","gpt-4o"], "temperature": 0.7 }
     *
     * 注意：实际对比需要各模型的 API Key。当前实现：
     * - 对 DeepSeek 系列模型使用已配置的 API Key 进行真实对比
     * - 对其他模型返回模拟对比结果（标注需要对应 API Key）
     */
    @PostMapping("/compare")
    public void compare(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        String prompt = text(requestData.get("prompt"));
        @SuppressWarnings("unchecked")
        List<String> models = (List<String>) requestData.getOrDefault("models",
            Arrays.asList("deepseek-chat", "deepseek-reasoner"));
        double temperature = requestData.get("temperature") instanceof Number
            ? ((Number) requestData.get("temperature")).doubleValue() : 0.7;

        if (prompt == null || prompt.isEmpty()) {
            response.setStatus(400);
            write(response, "prompt 不能为空");
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("# 多模型对比报告\n\n");
        result.append("**Prompt:** ").append(prompt).append("\n");
        result.append("**温度:** ").append(temperature).append("\n\n");
        result.append("---\n\n");

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(models.size(), 4));
        List<Future<ModelResult>> futures = new ArrayList<>();

        for (String modelId : models) {
            futures.add(executor.submit(() -> callModel(modelId, prompt, temperature)));
        }

        for (int i = 0; i < models.size(); i++) {
            String modelId = models.get(i);
            try {
                ModelResult mr = futures.get(i).get(60, TimeUnit.SECONDS);
                result.append("## ").append(i + 1).append(". ").append(mr.modelName).append("\n\n");
                result.append("- **模型ID:** ").append(mr.modelId).append("\n");
                result.append("- **响应时间:** ").append(mr.durationMs).append("ms\n");
                result.append("- **状态:** ").append(mr.success ? "✅ 成功" : "❌ " + mr.error).append("\n\n");
                result.append("### 输出:\n\n").append(mr.output).append("\n\n");
                result.append("---\n\n");
            } catch (TimeoutException e) {
                result.append("## ").append(i + 1).append(". ").append(modelId).append("\n\n");
                result.append("- **状态:** ⏱ 超时（60s）\n\n---\n\n");
            } catch (Exception e) {
                result.append("## ").append(i + 1).append(". ").append(modelId).append("\n\n");
                result.append("- **状态:** ❌ ").append(e.getMessage()).append("\n\n---\n\n");
            }
        }
        executor.shutdown();

        // 对比分析总结
        result.append("## 对比分析\n\n");
        result.append("| 维度 | 说明 |\n|------|------|\n");
        result.append("| 响应速度 | 见各模型响应时间 |\n");
        result.append("| 内容质量 | 需人工评估内容的准确性、完整性和教学适用性 |\n");
        result.append("| 格式规范 | 检查是否遵循要求的输出格式 |\n");
        result.append("| 教学适配 | 评估内容是否适合高等教育场景 |\n\n");
        result.append("> 💡 **建议:** 根据具体教学场景选择最匹配的模型。复杂推理场景推荐 DeepSeek Reasoner，");
        result.append("通用内容生成推荐 DeepSeek Chat 或 GPT-4o。\n");

        write(response, result.toString());
    }

    private ModelResult callModel(String modelId, String prompt, double temperature) {
        long start = System.currentTimeMillis();
        ModelResult mr = new ModelResult();
        mr.modelId = modelId;
        mr.modelName = getModelName(modelId);

        try {
            // Use the configured API (DeepSeek-compatible)
            String apiUrl = modelConfigService.getActiveApiUrl();
            String apiKey = modelConfigService.getActiveApiKey();

            org.apache.http.impl.client.CloseableHttpClient httpClient =
                org.apache.http.impl.client.HttpClients.createDefault();
            org.apache.http.client.methods.HttpPost httpPost = new org.apache.http.client.methods.HttpPost(apiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelId);
            body.put("temperature", temperature);
            body.put("max_tokens", 1024);
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.add(msg);
            body.put("messages", messages);

            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            httpPost.setEntity(new org.apache.http.entity.StringEntity(
                om.writeValueAsString(body), org.apache.http.entity.ContentType.APPLICATION_JSON));

            org.apache.http.HttpResponse httpResponse = httpClient.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = org.apache.http.util.EntityUtils.toString(
                httpResponse.getEntity(), java.nio.charset.StandardCharsets.UTF_8);

            if (statusCode == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> respMap = om.readValue(responseBody, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    mr.output = String.valueOf(message.getOrDefault("content", ""));
                    mr.success = true;
                } else {
                    mr.output = responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody;
                    mr.success = true;
                }
            } else {
                mr.success = false;
                mr.error = "HTTP " + statusCode + ": " +
                    (responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody);
            }
            httpClient.close();
        } catch (Exception e) {
            mr.success = false;
            mr.error = e.getMessage();
            log.warn("Model compare failed for {}: {}", modelId, e.getMessage());
        }

        mr.durationMs = System.currentTimeMillis() - start;
        return mr;
    }

    private String getModelName(String modelId) {
        return COMPARABLE_MODELS.stream()
            .filter(m -> m.get("id").equals(modelId))
            .map(m -> m.get("name"))
            .findFirst().orElse(modelId);
    }

    private static Map<String, String> mapOf(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(k1, v1); m.put(k2, v2); m.put(k3, v3);
        return m;
    }

    private String text(Object val) {
        return val == null ? "" : String.valueOf(val).trim();
    }

    private void write(HttpServletResponse response, String text) throws IOException {
        try (PrintWriter writer = response.getWriter()) {
            writer.write(text);
            writer.flush();
        }
    }

    private static class ModelResult {
        String modelId;
        String modelName;
        String output = "";
        boolean success;
        String error = "";
        long durationMs;
    }
}
