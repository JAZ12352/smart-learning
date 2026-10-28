package com.mindskip.xzs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigService.class);
    private static final String CONFIG_FILE = "model-config.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String defaultApiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String defaultApiUrl;

    @Value("${deepseek.model:deepseek-v4-pro}")
    private String defaultModel;

    @Value("${deepseek.temperature:0.7}")
    private Double defaultTemperature;

    private String activeApiKey;
    private String activeApiUrl;
    private String activeModel;
    private Double activeTemperature;

    // 视频模型配置
    private String videoApiKey;
    private String videoApiUrl;
    private String videoModel;

    @PostConstruct
    public void init() {
        activeApiKey = defaultApiKey;
        activeApiUrl = defaultApiUrl;
        activeModel = defaultModel;
        activeTemperature = defaultTemperature;

        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try {
                Map<String, Object> saved = objectMapper.readValue(file, Map.class);
                // 注意：空字符串不可覆盖环境变量注入的 key，否则 .env 会被空配置顶掉
                if (saved.containsKey("apiKey") && saved.get("apiKey") != null
                        && !String.valueOf(saved.get("apiKey")).trim().isEmpty()) {
                    activeApiKey = String.valueOf(saved.get("apiKey")).trim();
                }
                if (saved.containsKey("apiUrl") && saved.get("apiUrl") != null) {
                    activeApiUrl = String.valueOf(saved.get("apiUrl"));
                }
                if (saved.containsKey("model") && saved.get("model") != null) {
                    activeModel = String.valueOf(saved.get("model"));
                }
                if (saved.containsKey("temperature") && saved.get("temperature") != null) {
                    activeTemperature = Double.parseDouble(String.valueOf(saved.get("temperature")));
                }
                // 视频模型配置
                if (saved.containsKey("videoApiKey") && saved.get("videoApiKey") != null) {
                    videoApiKey = String.valueOf(saved.get("videoApiKey"));
                }
                if (saved.containsKey("videoApiUrl") && saved.get("videoApiUrl") != null) {
                    videoApiUrl = String.valueOf(saved.get("videoApiUrl"));
                }
                if (saved.containsKey("videoModel") && saved.get("videoModel") != null) {
                    videoModel = String.valueOf(saved.get("videoModel"));
                }
                log.info("Loaded model config from {}: model={}, apiUrl={}", CONFIG_FILE, activeModel, activeApiUrl);
            } catch (Exception e) {
                log.warn("Failed to read {}, using application defaults: {}", CONFIG_FILE, e.getMessage());
            }
        }

        // 如果 model-config.json 中没有视频配置，尝试从 agnes-config.json 加载
        if (videoApiKey == null || videoApiKey.isEmpty()) {
            File agnesFile = new File("agnes-config.json");
            if (agnesFile.exists()) {
                try {
                    Map<String, Object> agnesConfig = objectMapper.readValue(agnesFile, Map.class);
                    videoApiKey = agnesConfig.containsKey("apiKey") ? String.valueOf(agnesConfig.get("apiKey")) : null;
                    videoApiUrl = agnesConfig.containsKey("apiUrl") ? String.valueOf(agnesConfig.get("apiUrl")) : "https://apihub.agnes-ai.cn/v1";
                    videoModel = agnesConfig.containsKey("model") ? String.valueOf(agnesConfig.get("model")) : "agnes-video-v2.0";
                    log.info("Loaded video config from agnes-config.json: model={}, apiUrl={}", videoModel, videoApiUrl);
                } catch (Exception e) {
                    log.warn("Failed to read agnes-config.json: {}", e.getMessage());
                }
            }
        }
    }

    public Map<String, Object> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("apiKey", maskKey(activeApiKey));
        config.put("apiKeyRaw", activeApiKey);
        config.put("apiUrl", activeApiUrl);
        config.put("model", activeModel);
        config.put("temperature", activeTemperature);
        config.put("defaultModel", defaultModel);
        // 视频模型配置
        config.put("videoApiKey", maskKey(videoApiKey));
        config.put("videoApiKeyRaw", videoApiKey);
        config.put("videoApiUrl", videoApiUrl);
        config.put("videoModel", videoModel);
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
        if (newConfig.containsKey("temperature")) {
            try {
                double temp = Double.parseDouble(String.valueOf(newConfig.get("temperature")));
                if (temp >= 0 && temp <= 2) {
                    activeTemperature = temp;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        // 视频模型配置
        if (newConfig.containsKey("videoApiKey")) {
            String vKey = String.valueOf(newConfig.get("videoApiKey")).trim();
            if (!vKey.isEmpty() && !vKey.contains("*")) {
                videoApiKey = vKey;
            }
        }
        if (newConfig.containsKey("videoApiUrl")) {
            String vUrl = String.valueOf(newConfig.get("videoApiUrl")).trim();
            if (!vUrl.isEmpty()) {
                videoApiUrl = vUrl;
            }
        }
        if (newConfig.containsKey("videoModel")) {
            String vModel = String.valueOf(newConfig.get("videoModel")).trim();
            if (!vModel.isEmpty()) {
                videoModel = vModel;
            }
        }

        persistConfig();
        log.info("Model config updated: model={}, apiUrl={}, temp={}", activeModel, activeApiUrl, activeTemperature);
        return getConfig();
    }

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (org.apache.http.impl.client.CloseableHttpClient httpClient =
                 org.apache.http.impl.client.HttpClients.createDefault()) {
            org.apache.http.client.methods.HttpPost httpPost = new org.apache.http.client.methods.HttpPost(activeApiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + activeApiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", activeModel);
            List<Map<String, String>> msgs = new ArrayList<>();
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "hi");
            msgs.add(userMsg);
            body.put("messages", msgs);
            body.put("max_tokens", 5);
            body.put("temperature", 0);

            httpPost.setEntity(new org.apache.http.entity.StringEntity(
                objectMapper.writeValueAsString(body),
                org.apache.http.entity.ContentType.APPLICATION_JSON
            ));

            org.apache.http.HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = org.apache.http.util.EntityUtils.toString(
                response.getEntity(), java.nio.charset.StandardCharsets.UTF_8
            );

            if (statusCode == 200) {
                result.put("success", true);
                result.put("message", "连接成功，模型 " + activeModel + " 响应正常");
            } else {
                result.put("success", false);
                result.put("message", "连接失败，状态码 " + statusCode);
                result.put("detail", responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody);
            }
            result.put("statusCode", statusCode);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接异常：" + e.getMessage());
        }
        return result;
    }

    public String getActiveApiKey() {
        return activeApiKey;
    }

    public String getActiveApiUrl() {
        return activeApiUrl;
    }

    public String getActiveModel() {
        return activeModel;
    }

    public Double getActiveTemperature() {
        return activeTemperature;
    }

    public String getVideoApiKey() {
        return videoApiKey;
    }

    public String getVideoApiUrl() {
        return videoApiUrl != null ? videoApiUrl : "https://apihub.agnes-ai.cn/v1";
    }

    public String getVideoModel() {
        return videoModel != null ? videoModel : "agnes-video-v2.0";
    }

    public Map<String, Object> testVideoConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        String apiUrl = getVideoApiUrl();
        String apiKey = getVideoApiKey();
        String model = getVideoModel();

        if (apiKey == null || apiKey.isEmpty()) {
            result.put("success", false);
            result.put("message", "视频 API Key 未配置");
            return result;
        }

        try (org.apache.http.impl.client.CloseableHttpClient httpClient =
                 org.apache.http.impl.client.HttpClients.createDefault()) {
            // 提交一个最小帧数的测试请求（9 = 8*1+1）
            org.apache.http.client.methods.HttpPost httpPost = new org.apache.http.client.methods.HttpPost(apiUrl + "/videos");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("prompt", "test connection");
            body.put("num_frames", 9);
            body.put("frame_rate", 24);
            httpPost.setEntity(new org.apache.http.entity.StringEntity(
                objectMapper.writeValueAsString(body),
                org.apache.http.entity.ContentType.APPLICATION_JSON
            ));

            org.apache.http.HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = org.apache.http.util.EntityUtils.toString(
                response.getEntity(), java.nio.charset.StandardCharsets.UTF_8
            );

            if (statusCode == 200 || statusCode == 201) {
                result.put("success", true);
                result.put("message", "视频 API 连接成功，模型 " + model + " 可用");
            } else {
                result.put("success", false);
                result.put("message", "视频 API 返回错误，状态码 " + statusCode);
                result.put("detail", responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody);
            }
            result.put("statusCode", statusCode);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "视频 API 连接异常：" + e.getMessage());
        }
        return result;
    }

    private void persistConfig() {
        try {
            Map<String, Object> save = new LinkedHashMap<>();
            save.put("apiKey", activeApiKey);
            save.put("apiUrl", activeApiUrl);
            save.put("model", activeModel);
            save.put("temperature", activeTemperature);
            save.put("videoApiKey", videoApiKey);
            save.put("videoApiUrl", videoApiUrl);
            save.put("videoModel", videoModel);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(CONFIG_FILE), save);
        } catch (IOException e) {
            log.warn("Failed to persist model config: {}", e.getMessage());
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "****";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }
}
