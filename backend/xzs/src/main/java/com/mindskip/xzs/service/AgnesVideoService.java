package com.mindskip.xzs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AgnesVideoService {

    private static final Logger log = LoggerFactory.getLogger(AgnesVideoService.class);
    private static final String CONFIG_FILE = "agnes-config.json";
    private static final String DEFAULT_API_URL = "https://apihub.agnes-ai.cn/v1";
    private static final String DEFAULT_MODEL = "agnes-video-v2.0";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    private ModelConfigService modelConfigService;

    @Value("${agnes.api-key:}")
    private String defaultApiKey;

    private String activeApiKey;
    private String activeApiUrl;
    private String activeModel;

    @PostConstruct
    public void init() {
        activeApiKey = defaultApiKey;
        activeApiUrl = DEFAULT_API_URL;
        activeModel = DEFAULT_MODEL;

        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try {
                Map<String, Object> saved = objectMapper.readValue(file, Map.class);
                if (saved.containsKey("apiKey") && saved.get("apiKey") != null) {
                    activeApiKey = String.valueOf(saved.get("apiKey"));
                }
                if (saved.containsKey("apiUrl") && saved.get("apiUrl") != null) {
                    activeApiUrl = String.valueOf(saved.get("apiUrl"));
                }
                if (saved.containsKey("model") && saved.get("model") != null) {
                    activeModel = String.valueOf(saved.get("model"));
                }
                log.info("Loaded Agnes config: model={}, apiUrl={}", activeModel, activeApiUrl);
            } catch (Exception e) {
                log.warn("Failed to read Agnes config: {}", e.getMessage());
            }
        }
    }

    /**
     * 创建兼容 TLS 的 HttpClient（修复 Connection reset 问题）
     */
    private CloseableHttpClient createHttpClient() {
        try {
            // 创建信任所有证书的 SSLContext（解决 TLS 握手问题）
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建支持 TLS 1.2 的 SSL socket factory
            SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"},
                null,
                NoopHostnameVerifier.INSTANCE
            );

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslFactory)
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(20);
            cm.setDefaultMaxPerRoute(10);

            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setSocketTimeout(120000)
                .setConnectionRequestTimeout(30000)
                .build();

            return HttpClientBuilder.create()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        } catch (Exception e) {
            log.error("Failed to create custom HTTP client, falling back to default", e);
            return HttpClientBuilder.create().build();
        }
    }

    /**
     * 提交视频生成任务
     */
    public Map<String, Object> submitVideoGeneration(String prompt, Map<String, Object> options) throws IOException {
        String apiKey = resolveApiKey(options);
        String apiUrl = resolveApiUrl(options);
        String model = resolveModel(options);

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost httpPost = new HttpPost(apiUrl + "/videos");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("prompt", prompt);

            // 视频参数
            int numFrames = getIntOption(options, "num_frames", 121); // 默认约5秒 @ 24fps
            int frameRate = getIntOption(options, "frame_rate", 24);
            body.put("num_frames", numFrames);
            body.put("frame_rate", frameRate);

            if (options.containsKey("negative_prompt")) {
                body.put("negative_prompt", String.valueOf(options.get("negative_prompt")));
            }
            if (options.containsKey("size")) {
                body.put("size", String.valueOf(options.get("size")));
            }
            if (options.containsKey("seed")) {
                body.put("seed", getIntOption(options, "seed", 0));
            }

            httpPost.setEntity(new StringEntity(
                objectMapper.writeValueAsString(body),
                ContentType.APPLICATION_JSON
            ));

            org.apache.http.HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("statusCode", statusCode);

            if (statusCode == 200 || statusCode == 201) {
                JsonNode json = objectMapper.readTree(responseBody);
                result.put("success", true);
                if (json.has("task_id")) {
                    result.put("taskId", json.get("task_id").asText());
                } else if (json.has("id")) {
                    result.put("taskId", json.get("id").asText());
                }
                result.put("status", json.has("status") ? json.get("status").asText() : "submitted");
                result.put("response", responseBody);
            } else {
                result.put("success", false);
                result.put("error", "API error: " + statusCode);
                result.put("response", responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
            }
            return result;
        }
    }

    /**
     * 查询视频生成状态
     */
    public Map<String, Object> queryVideoStatus(String taskId, Map<String, Object> options) throws IOException {
        String apiKey = resolveApiKey(options);
        String apiUrl = resolveApiUrl(options);

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(apiUrl + "/videos/" + taskId);
            httpGet.setHeader("Authorization", "Bearer " + apiKey);
            httpGet.setHeader("Content-Type", "application/json");

            org.apache.http.HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("statusCode", statusCode);

            if (statusCode == 200) {
                JsonNode json = objectMapper.readTree(responseBody);
                result.put("success", true);
                String status = json.has("status") ? json.get("status").asText() : "unknown";
                result.put("status", status);

                // 兼容多种状态值（不同 API 版本可能返回不同命名）
                String statusLower = status.toLowerCase();
                if ("completed".equals(statusLower) || "succeeded".equals(statusLower)
                    || "done".equals(statusLower) || "ready".equals(statusLower)
                    || "finished".equals(statusLower) || "complete".equals(statusLower)) {
                    // 获取视频 URL（兼容多种响应格式）
                    String videoUrl = null;
                    // 实际 API 格式: metadata.url
                    if (json.has("metadata") && json.get("metadata").has("url")) {
                        videoUrl = json.get("metadata").get("url").asText();
                    } else if (json.has("video_url")) {
                        videoUrl = json.get("video_url").asText();
                    } else if (json.has("output") && json.get("output").has("video_url")) {
                        videoUrl = json.get("output").get("video_url").asText();
                    } else if (json.has("output") && json.get("output").has("url")) {
                        videoUrl = json.get("output").get("url").asText();
                    } else if (json.has("url")) {
                        videoUrl = json.get("url").asText();
                    } else if (json.has("result") && json.get("result").isTextual()) {
                        videoUrl = json.get("result").asText();
                    } else if (json.has("remixed_from_video_id")) {
                        videoUrl = json.get("remixed_from_video_id").asText();
                    }
                    result.put("videoUrl", videoUrl);
                    result.put("response", responseBody);
                    log.info("Agnes video completed: taskId={}, videoUrl={}", taskId, videoUrl);
                } else if ("failed".equals(statusLower) || "error".equals(statusLower)) {
                    String errorMsg = "Generation failed";
                    if (json.has("error")) {
                        errorMsg = json.get("error").isTextual() ? json.get("error").asText() : json.get("error").toString();
                    } else if (json.has("message")) {
                        errorMsg = json.get("message").asText();
                    }
                    result.put("error", errorMsg);
                    log.warn("Agnes video failed: taskId={}, error={}", taskId, errorMsg);
                } else {
                    // 未识别的状态 → 记录日志便于排查
                    log.info("Agnes video status: taskId={}, status={}, response={}",
                        taskId, status, responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody);
                }
            } else {
                result.put("success", false);
                result.put("error", "API error: " + statusCode);
            }
            return result;
        }
    }

    // ====== Agnes Image 2.1 Flash 图片生成 ======

    public Map<String, Object> submitImageGeneration(String prompt, Map<String, Object> options) throws IOException {
        String apiKey = resolveApiKey(options);
        String apiUrl = resolveApiUrl(options);
        String model = options != null && options.containsKey("model")
            ? String.valueOf(options.get("model")) : "agnes-image-2.1-flash";

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost httpPost = new HttpPost(apiUrl + "/images/generations");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("prompt", prompt);

            String size = getOption(options, "size", "1024x1024");
            body.put("size", size);
            body.put("n", 1);

            httpPost.setEntity(new StringEntity(
                objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));

            org.apache.http.HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("statusCode", statusCode);

            if (statusCode == 200 || statusCode == 201) {
                JsonNode json = objectMapper.readTree(responseBody);
                result.put("success", true);
                if (json.has("task_id")) {
                    result.put("taskId", json.get("task_id").asText());
                } else if (json.has("id")) {
                    result.put("taskId", json.get("id").asText());
                } else if (json.has("data") && json.get("data").isArray() && json.get("data").size() > 0) {
                    result.put("imageUrl", json.get("data").get(0).has("url")
                        ? json.get("data").get(0).get("url").asText() : null);
                    result.put("status", "completed");
                }
                result.put("status", json.has("status") ? json.get("status").asText() : "submitted");
                result.put("response", responseBody);
            } else {
                result.put("success", false);
                result.put("error", "API error: " + statusCode);
                result.put("response", responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
            }
            return result;
        }
    }

    public Map<String, Object> queryImageStatus(String taskId, Map<String, Object> options) throws IOException {
        String apiKey = resolveApiKey(options);
        String apiUrl = resolveApiUrl(options);

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(apiUrl + "/images/generations/" + taskId);
            httpGet.setHeader("Authorization", "Bearer " + apiKey);

            org.apache.http.HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("statusCode", statusCode);
            if (statusCode == 200) {
                JsonNode json = objectMapper.readTree(responseBody);
                result.put("success", true);
                String status = json.has("status") ? json.get("status").asText() : "unknown";
                result.put("status", status);
                String statusLower = status.toLowerCase();
                if ("completed".equals(statusLower) || "succeeded".equals(statusLower)
                    || "done".equals(statusLower) || "ready".equals(statusLower)
                    || "finished".equals(statusLower) || "complete".equals(statusLower)) {
                    String url = null;
                    if (json.has("output") && json.get("output").has("image_url")) {
                        url = json.get("output").get("image_url").asText();
                    } else if (json.has("data") && json.get("data").isArray() && json.get("data").size() > 0) {
                        url = json.get("data").get(0).has("url") ? json.get("data").get(0).get("url").asText() : null;
                    } else if (json.has("image_url")) {
                        url = json.get("image_url").asText();
                    }
                    result.put("imageUrl", url);
                    log.info("Agnes image completed: taskId={}, url={}", taskId, url);
                }
            } else {
                result.put("success", false);
                result.put("error", "API error: " + statusCode);
            }
            return result;
        }
    }

    public Map<String, Object> generateImageSync(String prompt, Map<String, Object> options, int maxPollSeconds) throws IOException, InterruptedException {
        Map<String, Object> submitResult = submitImageGeneration(prompt, options);
        if (Boolean.TRUE.equals(submitResult.get("success")) && submitResult.containsKey("imageUrl")) {
            return submitResult; // 同步返回
        }
        String taskId = (String) submitResult.get("taskId");
        if (taskId == null) return submitResult;

        int pollInterval = 3000;
        int maxPolls = (maxPollSeconds * 1000) / pollInterval;
        for (int i = 0; i < maxPolls; i++) {
            Thread.sleep(pollInterval);
            Map<String, Object> statusResult = queryImageStatus(taskId, options);
            if (Boolean.TRUE.equals(statusResult.get("success"))) {
                String status = (String) statusResult.get("status");
                if ("completed".equals(status) || "succeeded".equals(status)) return statusResult;
                if ("failed".equals(status)) return statusResult;
            }
        }
        Map<String, Object> timeout = new LinkedHashMap<>();
        timeout.put("success", false); timeout.put("taskId", taskId);
        timeout.put("error", "Image generation timed out");
        return timeout;
    }

    private String getOption(Map<String, Object> options, String key, String defaultValue) {
        if (options != null && options.containsKey(key)) return String.valueOf(options.get(key));
        return defaultValue;
    }

    /**
     * 同步方式生成视频（轮询直到完成）
     */
    public Map<String, Object> generateVideoSync(String prompt, Map<String, Object> options, int maxPollSeconds) throws IOException, InterruptedException {
        Map<String, Object> submitResult = submitVideoGeneration(prompt, options);
        if (!Boolean.TRUE.equals(submitResult.get("success"))) {
            return submitResult;
        }

        String taskId = (String) submitResult.get("taskId");
        if (taskId == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("error", "No task ID returned");
            return error;
        }

        log.info("Agnes video task submitted: taskId={}", taskId);

        int pollInterval = 5000; // 5秒轮询
        int maxPolls = (maxPollSeconds * 1000) / pollInterval;

        for (int i = 0; i < maxPolls; i++) {
            Thread.sleep(pollInterval);
            Map<String, Object> statusResult = queryVideoStatus(taskId, options);
            if (!Boolean.TRUE.equals(statusResult.get("success"))) {
                continue;
            }

            String status = (String) statusResult.get("status");
            if ("completed".equals(status) || "succeeded".equals(status)) {
                log.info("Agnes video generation completed: taskId={}", taskId);
                return statusResult;
            } else if ("failed".equals(status)) {
                log.warn("Agnes video generation failed: taskId={}", taskId);
                return statusResult;
            }

            log.debug("Agnes video still generating... taskId={}, status={}", taskId, status);
        }

        Map<String, Object> timeout = new LinkedHashMap<>();
        timeout.put("success", false);
        timeout.put("taskId", taskId);
        timeout.put("error", "Video generation timed out after " + maxPollSeconds + "s");
        return timeout;
    }

    public Map<String, Object> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("apiKey", maskKey(activeApiKey));
        config.put("apiKeyRaw", activeApiKey);
        config.put("apiUrl", activeApiUrl);
        config.put("model", activeModel);
        return config;
    }

    public Map<String, Object> updateConfig(Map<String, Object> newConfig) {
        if (newConfig.containsKey("apiKey")) {
            String key = String.valueOf(newConfig.get("apiKey")).trim();
            if (!key.isEmpty() && !key.contains("*")) {
                activeApiKey = key;
            }
        }
        if (newConfig.containsKey("apiUrl")) {
            String url = String.valueOf(newConfig.get("apiUrl")).trim();
            if (!url.isEmpty()) {
                activeApiUrl = url;
            }
        }
        if (newConfig.containsKey("model")) {
            String model = String.valueOf(newConfig.get("model")).trim();
            if (!model.isEmpty()) {
                activeModel = model;
            }
        }
        persistConfig();
        log.info("Agnes config updated: model={}, apiUrl={}", activeModel, activeApiUrl);
        return getConfig();
    }

    public boolean isConfigured() {
        if (modelConfigService != null && modelConfigService.getVideoApiKey() != null && !modelConfigService.getVideoApiKey().isEmpty()) {
            return true;
        }
        return activeApiKey != null && !activeApiKey.isEmpty();
    }

    private String resolveApiKey(Map<String, Object> options) {
        if (options != null && options.containsKey("apiKey")) {
            return String.valueOf(options.get("apiKey"));
        }
        // 优先从 ModelConfigService 获取（统一配置管理）
        if (modelConfigService != null && modelConfigService.getVideoApiKey() != null && !modelConfigService.getVideoApiKey().isEmpty()) {
            return modelConfigService.getVideoApiKey();
        }
        return activeApiKey;
    }

    private String resolveApiUrl(Map<String, Object> options) {
        if (options != null && options.containsKey("apiUrl")) {
            return String.valueOf(options.get("apiUrl"));
        }
        if (modelConfigService != null) {
            return modelConfigService.getVideoApiUrl();
        }
        return activeApiUrl;
    }

    private String resolveModel(Map<String, Object> options) {
        if (options != null && options.containsKey("model")) {
            return String.valueOf(options.get("model"));
        }
        if (modelConfigService != null) {
            return modelConfigService.getVideoModel();
        }
        return activeModel;
    }

    private int getIntOption(Map<String, Object> options, String key, int defaultValue) {
        if (options != null && options.containsKey(key)) {
            Object val = options.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(val));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private void persistConfig() {
        try {
            Map<String, Object> save = new LinkedHashMap<>();
            save.put("apiKey", activeApiKey);
            save.put("apiUrl", activeApiUrl);
            save.put("model", activeModel);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(CONFIG_FILE), save);
        } catch (IOException e) {
            log.warn("Failed to persist Agnes config: {}", e.getMessage());
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "****";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }
}
