package com.mindskip.xzs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Python 多智能体服务 HTTP 客户端。
 * 封装对 Python FastAPI (port 8100) 的所有调用：
 *   - 向量知识检索 (ChromaDB)
 *   - 知识库文档向量化入库
 *   - 资源生成（Supervisor 统一编排）
 *   - 多智能体批量生成
 *   - 健康检查
 *
 * 遵循项目现有的 Apache HttpClient 模式（与 DeepSeekService / AgnesVideoService 一致）。
 * 当 Python 服务不可用时，调用方应回退到 Java 本地实现。
 */
@Service
public class PythonAgentClient {

    private static final Logger log = LoggerFactory.getLogger(PythonAgentClient.class);

    @Value("${python.service.url:${PYTHON_SERVICE_URL:http://localhost:8100}}")
    private String configuredUrl;

    @Value("${python.service.enabled:true}")
    private boolean enabled;

    private String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.baseUrl = configuredUrl.replaceAll("/+$", "");
        log.info("PythonAgentClient initialized: baseUrl={}, enabled={}", baseUrl, enabled);
    }

    public boolean isAvailable() {
        if (!enabled) return false;
        try {
            Map<String, Object> health = healthCheck();
            return "ok".equals(health.get("status"));
        } catch (Exception e) {
            log.debug("Python service not available: {}", e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  知识库向量检索
    // ═══════════════════════════════════════════════════════════════

    /**
     * ChromaDB 向量语义检索知识库文档。
     * @param query   搜索查询
     * @param topK    返回结果数
     * @return 检索结果列表，Python 不可用时返回空列表
     */
    public List<Map<String, Object>> searchKnowledge(String query, int topK) {
        if (!enabled) return Collections.emptyList();

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("top_k", topK);

            Map<String, Object> response = post("/api/v1/knowledge/search", requestBody);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null) return Collections.emptyList();

            // 标准化字段名，兼容 Java 端的 SearchResult 格式
            for (Map<String, Object> item : results) {
                if (!item.containsKey("source") && item.containsKey("metadata")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> meta = (Map<String, Object>) item.get("metadata");
                    if (meta != null && meta.containsKey("source")) {
                        item.put("source", meta.get("source"));
                    }
                }
            }

            log.debug("Vector search returned {} results for query: {}", results.size(),
                query.length() > 50 ? query.substring(0, 50) + "..." : query);
            return results;

        } catch (Exception e) {
            log.warn("Python vector search failed, caller should fallback to keyword: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 将文档内容写入 ChromaDB 向量索引。
     * @param docId      文档 ID
     * @param content    文档文本内容
     * @param metadata   附加元数据（文件名、知识库ID等）
     * @return 是否成功
     */
    public boolean indexDocument(int docId, String content, Map<String, Object> metadata) {
        if (!enabled || StringUtils.isBlank(content)) return false;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("doc_id", docId);
            requestBody.put("content", content);
            requestBody.put("metadata", metadata != null ? metadata : new HashMap<>());

            Map<String, Object> response = post("/api/v1/knowledge/index", requestBody);
            boolean success = Boolean.TRUE.equals(response.get("success"));
            log.info("Document {} indexing {}: {}", docId, success ? "succeeded" : "failed",
                response.getOrDefault("message", ""));
            return success;

        } catch (Exception e) {
            log.warn("Python document indexing failed for docId={}: {}", docId, e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  资源生成（Supervisor 统一编排）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 通过 Python Supervisor 编排单个 Agent 生成资源。
     * @param topic       教学主题
     * @param agentType   智能体类型 (document/mind_map/exercise/extended_reading/video/code_example)
     * @param context     额外上下文（学科、学段等）
     * @return 生成结果；Python 不可用时返回 null
     */
    public Map<String, Object> generateResource(String topic, String agentType, Map<String, Object> context) {
        if (!isAvailable()) return null;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("topic", topic);
            requestBody.put("agent_type", agentType);
            requestBody.put("context", context != null ? context : new HashMap<>());

            return post("/api/v1/resource/generate", requestBody);

        } catch (Exception e) {
            log.warn("Python resource generation failed for topic='{}', agent={}: {}",
                topic, agentType, e.getMessage());
            return null;
        }
    }

    /**
     * 通过 Python Supervisor 编排多个 Agent 批量生成资源。
     * @param topic        教学主题
     * @param agentTypes   要激活的智能体类型列表
     * @param context      额外上下文
     * @return 生成结果集（含各 Agent 输出）；Python 不可用时返回 null
     */
    public Map<String, Object> multiGenerateResource(String topic, List<String> agentTypes,
                                                      Map<String, Object> context) {
        if (!isAvailable()) return null;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("topic", topic);
            requestBody.put("agent_types", agentTypes);
            requestBody.put("context", context != null ? context : new HashMap<>());

            return post("/api/v1/resource/multi-generate", requestBody);

        } catch (Exception e) {
            log.warn("Python multi-agent generation failed for topic='{}': {}", topic, e.getMessage());
            return null;
        }
    }

    /**
     * 流式资源生成 — 通过 SSE 回调逐块输出内容。
     */
    public void generateResourceStream(String topic, String agentType, Map<String, Object> context,
                                        Consumer<String> onChunk) {
        if (!isAvailable()) {
            onChunk.accept("Python 多智能体服务不可用，请检查服务是否启动。");
            return;
        }

        try {
            String url = baseUrl + "/api/v1/resource/generate";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("topic", topic);
            requestBody.put("agent_type", agentType);
            requestBody.put("context", context != null ? context : new HashMap<>());
            requestBody.put("stream", true);

            try (CloseableHttpClient httpClient = createHttpClient()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setEntity(new StringEntity(
                    objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                if (entity == null) return;

                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;
                            onChunk.accept(data);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Python resource stream generation failed", e);
            onChunk.accept("资源生成失败：" + e.getMessage());
        }
    }

    /**
     * 流式多智能体协作 — 通过 SSE 实时推送真实 Agent 执行事件。
     * 调用 Python /api/v1/collaboration/stream，逐行读取 SSE data: 并回调。
     * 每个 data 行是一个 JSON 事件（plan_start/agent_end/review_end/final 等）。
     *
     * @param topic     教学主题
     * @param context   上下文（可含 profile 学生画像）
     * @param requestId 请求追踪 ID
     * @param onEvent   每条 SSE data 负载的回调
     */
    public void streamCollaboration(String topic, Map<String, Object> context, String requestId,
                                     Consumer<String> onEvent) {
        if (!isAvailable()) {
            onEvent.accept("{\"type\":\"error\",\"message\":\"Python 多智能体服务不可用，请检查 8100 端口\"}");
            return;
        }

        try {
            String url = baseUrl + "/api/v1/collaboration/stream";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("topic", topic);
            requestBody.put("context", context != null ? context : new HashMap<>());
            if (requestId != null && !requestId.isEmpty()) {
                requestBody.put("request_id", requestId);
            }
            requestBody.put("stream", true);

            try (CloseableHttpClient httpClient = createHttpClient()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept", "text/event-stream");
                httpPost.setEntity(new StringEntity(
                    objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

                HttpResponse response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    onEvent.accept("{\"type\":\"error\",\"message\":\"Python 返回空响应 (" + statusCode + ")\"}");
                    return;
                }

                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;
                            onEvent.accept(data);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Python collaboration stream failed", e);
            onEvent.accept("{\"type\":\"error\",\"message\":\"协作流式生成失败："
                + (e.getMessage() == null ? "unknown" : e.getMessage().replace("\"", "'")) + "\"}");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Agent 状态查询
    // ═══════════════════════════════════════════════════════════════

    /**
     * 获取所有 Agent 的运行状态。
     */
    public Map<String, Object> getAgentStatus() {
        if (!isAvailable()) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("available", false);
            fallback.put("message", "Python 多智能体服务不可用");
            return fallback;
        }

        try {
            return get("/api/v1/health");
        } catch (Exception e) {
            log.warn("Failed to get agent status: {}", e.getMessage());
            return Collections.singletonMap("available", false);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  健康检查
    // ═══════════════════════════════════════════════════════════════

    public Map<String, Object> healthCheck() throws IOException {
        return get("/api/v1/health");
    }

    // ═══════════════════════════════════════════════════════════════
    //  HTTP 调用基方法
    // ═══════════════════════════════════════════════════════════════

    /**
     * 带超时的 HTTP 客户端。默认实现无任何超时，一次连接挂起会永久阻塞调用线程
     * （实测曾导致实验判定链路卡死数小时）。本地服务连接快，读超时放宽以容纳
     * 透传大模型的生成请求。
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(5_000)
            .setConnectionRequestTimeout(5_000)
            .setSocketTimeout(600_000)
            .build();
        return HttpClients.custom().setDefaultRequestConfig(config).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body) throws IOException {
        String url = baseUrl + path;
        log.debug("POST {}", url);

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(
                objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (statusCode >= 400) {
                throw new IOException("Python service returned " + statusCode + ": " +
                    (responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody));
            }

            if (StringUtils.isBlank(responseBody)) {
                return new HashMap<>();
            }

            return objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {});
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path) throws IOException {
        String url = baseUrl + path;
        log.debug("GET {}", url);

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(url);

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (statusCode >= 400) {
                throw new IOException("Python service returned " + statusCode);
            }

            if (StringUtils.isBlank(responseBody)) {
                return new HashMap<>();
            }

            return objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {});
        }
    }
}
