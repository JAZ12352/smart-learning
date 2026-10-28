package com.mindskip.xzs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    public static final String MODEL_DEFAULT = "deepseek-v4-pro";

    /** 默认输出预算：短任务（判定、摘要等）足够用 */
    public static final int MAX_TOKENS_DEFAULT = 8192;
    /** 长文本生成预算：推理型模型需为 reasoning + 正文留出空间 */
    public static final int MAX_TOKENS_LONG = 32768;
    /** 自动加倍重试的上限 */
    public static final int MAX_TOKENS_CEILING = 65536;
    public static final String MODEL_CHAT = "deepseek-chat";
    public static final String MODEL_REASONER = "deepseek-reasoner";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ModelConfigService modelConfigService;

    public DeepSeekService(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    public boolean isConfigured() {
        return StringUtils.isNotBlank(modelConfigService.getActiveApiKey());
    }

    public String complete(List<Map<String, String>> messages) throws IOException {
        return complete(messages, modelConfigService.getActiveModel());
    }

    public String complete(List<Map<String, String>> messages, String model) throws IOException {
        return complete(messages, model, null);
    }

    /**
     * 指定输出预算的补全（沿用活跃模型与默认 temperature）。
     * 长文本生成（PPT 脚本、视频脚本、教学文档等）必须用大预算：
     * 推理型模型的 reasoning 开销会挤占正文，默认 8192 极易截断。
     */
    public String complete(List<Map<String, String>> messages, int maxTokens) throws IOException {
        return complete(messages, modelConfigService.getActiveModel(), null, maxTokens);
    }

    /**
     * 指定 temperature 的补全调用。temperature 传 null 表示沿用模型配置中的默认值。
     */
    public String complete(List<Map<String, String>> messages, String model, Double temperature) throws IOException {
        return complete(messages, model, temperature, MAX_TOKENS_DEFAULT);
    }

    /**
     * 指定 temperature 与输出预算的补全调用。temperature 传 null 表示沿用模型配置中的默认值。
     */
    public String complete(List<Map<String, String>> messages, String model, Double temperature, int maxTokens)
            throws IOException {
        validateConfigured();
        Map<String, Object> requestBody = buildRequestBody(messages, false, model);
        if (temperature != null) {
            requestBody.put("temperature", temperature);
        }
        requestBody.put("max_tokens", maxTokens > 0 ? maxTokens : MAX_TOKENS_DEFAULT);
        return callWithRetry(requestBody);
    }

    /**
     * 确定性补全（temperature=0）。
     *
     * 专供实验评测链路使用：默认 temperature=0.7 会让「种子样本生成」与「LLM-as-Judge 判定」
     * 都带采样随机性，同一系统状态连跑两次指标会抖动，实验报告将失去可复现性。
     * 注意 temperature=0 只能保证同一模型版本下尽可能稳定，模型升级仍可能改变输出。
     */
    public String completeDeterministic(List<Map<String, String>> messages) throws IOException {
        return complete(messages, modelConfigService.getActiveModel(), 0.0);
    }

    /**
     * 确定性补全（temperature=0），可指定更大的输出预算。
     *
     * 推理型模型（如 deepseek-v4-flash/pro）会先输出 reasoning_content 再输出正文，
     * 在生成长文档时极易出现「预算被推理过程耗尽、finish_reason=length、content 为空」。
     * 长文本生成链路必须显式调大 max_tokens，否则样本会成批失败。
     */
    public String completeDeterministic(List<Map<String, String>> messages, int maxTokens) throws IOException {
        return complete(messages, modelConfigService.getActiveModel(), 0.0, maxTokens);
    }

    /**
     * 发起请求并在瞬时故障时重试。
     *
     * 两类失败分别处理：
     * 1) 瞬时故障/偶发空响应 —— 原样重试一次；
     * 2) 输出被截断（finish_reason=length）—— 自动加倍 max_tokens 后重试，
     *    因为推理型模型的推理开销会挤占正文预算，原预算重试必然再次失败。
     */
    private String callWithRetry(Map<String, Object> requestBody) throws IOException {
        IOException lastError = null;
        int tokens = ((Number) requestBody.getOrDefault("max_tokens", MAX_TOKENS_DEFAULT)).intValue();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                requestBody.put("max_tokens", tokens);
                String responseBody = executeRequest(requestBody);
                JsonNode rootNode = objectMapper.readTree(responseBody);
                String content = extractContent(rootNode);
                if (StringUtils.isBlank(content)) {
                    String finishReason = rootNode.path("choices").path(0).path("finish_reason").asText("");
                    if ("length".equals(finishReason) && tokens < MAX_TOKENS_CEILING) {
                        int next = Math.min(tokens * 2, MAX_TOKENS_CEILING);
                        log.warn("DeepSeek 输出被截断（finish_reason=length），max_tokens {} → {}", tokens, next);
                        tokens = next;
                        throw new IOException("输出被截断，已提升 max_tokens 至 " + next);
                    }
                    throw new IOException("DeepSeek 返回内容为空");
                }
                return content.trim();
            } catch (IOException e) {
                lastError = e;
                log.warn("DeepSeek complete attempt {}/3 failed: {}", attempt, e.getMessage());
                if (attempt < 3) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("DeepSeek 调用被中断", ie);
                    }
                }
            }
        }
        throw lastError;
    }

    public void stream(List<Map<String, String>> messages, Consumer<String> onChunk) throws IOException {
        stream(messages, modelConfigService.getActiveModel(), onChunk);
    }

    public void stream(List<Map<String, String>> messages, String model, Consumer<String> onChunk) throws IOException {
        validateConfigured();
        String apiKey = modelConfigService.getActiveApiKey();
        String apiUrl = modelConfigService.getActiveApiUrl();

        Map<String, Object> requestBody = buildRequestBody(messages, true, model);
        log.info("DeepSeek stream request started, model={}, messages={}", model, messages.size());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity responseEntity = response.getEntity();
            if (statusCode >= 400) {
                String errorBody = responseEntity == null ? "" : EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                log.error("DeepSeek request failed, status={}, body={}", statusCode, errorBody);
                if (errorBody.contains("Insufficient Balance")) {
                    onChunk.accept("DeepSeek API 余额不足，请在模型设置中更换 API Key。");
                    return;
                }
                onChunk.accept("DeepSeek 调用失败，状态码 " + statusCode + "。请检查模型设置。");
                return;
            }
            if (responseEntity == null) {
                onChunk.accept("DeepSeek 响应为空。");
                return;
            }

            try (InputStream inputStream = responseEntity.getContent();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                boolean streamed = false;
                while ((line = reader.readLine()) != null) {
                    if (StringUtils.isBlank(line) || !line.startsWith("data: ")) {
                        continue;
                    }
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        JsonNode rootNode = objectMapper.readTree(data);
                        if (rootNode.has("error")) {
                            String errorMessage = rootNode.path("error").path("message").asText("DeepSeek API 错误");
                            log.error("DeepSeek API error: {}", errorMessage);
                            onChunk.accept("DeepSeek API 错误：" + errorMessage);
                            streamed = true;
                            break;
                        }
                        String content = extractContent(rootNode);
                        if (StringUtils.isNotBlank(content)) {
                            streamed = true;
                            onChunk.accept(content);
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to parse DeepSeek stream chunk: {}", ex.getMessage());
                    }
                }

                if (!streamed) {
                    onChunk.accept("DeepSeek 未返回可用的流式内容，请检查 API 与模型配置。");
                }
            }
        } catch (Exception e) {
            log.error("DeepSeek stream request failed", e);
            onChunk.accept("错误：" + e.getMessage());
        }
    }

    private void validateConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("DeepSeek API Key 未配置，请在模型设置中填写 API Key。");
        }
    }

    private Map<String, Object> buildRequestBody(List<Map<String, String>> messages, boolean stream, String model) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", StringUtils.defaultIfBlank(model, MODEL_DEFAULT));
        requestBody.put("messages", messages);
        requestBody.put("temperature", modelConfigService.getActiveTemperature());
        requestBody.put("max_tokens", MAX_TOKENS_DEFAULT);
        if (stream) {
            requestBody.put("stream", true);
        }
        return requestBody;
    }

    private String executeRequest(Map<String, Object> requestBody) throws IOException {
        String apiKey = modelConfigService.getActiveApiKey();
        String apiUrl = modelConfigService.getActiveApiUrl();

        // 必须显式设置超时：Apache HttpClient 默认全部不超时，一次连接挂起会永久阻塞调用线程
        // （实测曾导致实验判定阶段卡死 5 小时）。长文本 + 推理模型的响应时间可达数分钟，读超时需放宽。
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(15_000)
            .setConnectionRequestTimeout(15_000)
            .setSocketTimeout(600_000)
            .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity responseEntity = response.getEntity();
            String responseBody = responseEntity == null ? "" : EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            if (statusCode >= 400) {
                throw new IOException("DeepSeek 调用失败，状态码 " + statusCode + "，响应：" + responseBody);
            }
            return responseBody;
        }
    }

    private String extractContent(JsonNode rootNode) {
        JsonNode choicesNode = rootNode.path("choices");
        if (!choicesNode.isArray() || choicesNode.size() == 0) {
            return "";
        }
        JsonNode firstChoice = choicesNode.get(0);
        JsonNode deltaContentNode = firstChoice.path("delta").path("content");
        if (!deltaContentNode.isMissingNode() && !deltaContentNode.isNull()) {
            String content = deltaContentNode.asText("");
            return sanitizeChunk(content);
        }
        JsonNode reasoningNode = firstChoice.path("delta").path("reasoning_content");
        if (!reasoningNode.isMissingNode() && !reasoningNode.isNull()) {
            return reasoningNode.asText("");
        }
        JsonNode messageContentNode = firstChoice.path("message").path("content");
        if (!messageContentNode.isMissingNode() && !messageContentNode.isNull()) {
            String content = messageContentNode.asText("");
            return sanitizeContent(content);
        }
        return "";
    }

    /**
     * 流式chunk清理：去除控制字符
     */
    private String sanitizeChunk(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }

    /**
     * 最终内容清理：去除控制字符 + Markdown代码块标记 + JSON包装标记
     */
    public static String sanitizeContent(String text) {
        if (text == null) return "";
        return text
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
            .replaceAll("(?i)^```(?:json|markdown|html|xml|sql|python|java|javascript|css|yaml|text)?\\s*", "")
            .replaceAll("\\s*```$", "")
            .trim();
    }
}
