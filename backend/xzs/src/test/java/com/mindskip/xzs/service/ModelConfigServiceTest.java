package com.mindskip.xzs.service;

import com.mindskip.xzs.XzsApplication;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * ModelConfigService 集成测试 — JUnit 4 + Spring Test
 * 测试 AI 模型配置的读写、更新和验证
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = XzsApplication.class)
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModelConfigServiceTest {

    @Autowired
    private ModelConfigService modelConfigService;

    @Test
    public void step01_shouldHaveDefaultConfig() {
        Map<String, Object> config = modelConfigService.getConfig();
        assertNotNull(config);
        assertTrue(config.containsKey("apiKey"));
        assertTrue(config.containsKey("apiUrl"));
        assertTrue(config.containsKey("model"));
        assertTrue(config.containsKey("temperature"));
        assertTrue(config.containsKey("defaultModel"));
    }

    @Test
    public void step02_apiKeyShouldBeMasked() {
        Map<String, Object> config = modelConfigService.getConfig();
        String apiKey = (String) config.get("apiKey");
        assertNotNull(apiKey);
        assertTrue(apiKey.contains("****"));
        String rawKey = (String) config.get("apiKeyRaw");
        assertNotNull(rawKey);
        assertNotEquals(apiKey, rawKey);
    }

    @Test
    public void step03_shouldReturnValidModel() {
        String model = modelConfigService.getActiveModel();
        assertNotNull(model);
        assertFalse(model.isEmpty());
    }

    @Test
    public void step04_shouldReturnValidUrl() {
        String url = modelConfigService.getActiveApiUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("https://"));
    }

    @Test
    public void step05_temperatureShouldBeInRange() {
        Double temp = modelConfigService.getActiveTemperature();
        assertNotNull(temp);
        assertTrue(temp >= 0);
        assertTrue(temp <= 2);
    }

    @Test
    public void step06_updateConfigShouldUpdateModel() {
        String original = modelConfigService.getActiveModel();
        Map<String, Object> newConfig = modelConfigService.getConfig();
        newConfig.put("model", "deepseek-v4-flash");
        modelConfigService.updateConfig(newConfig);
        assertEquals("deepseek-v4-flash", modelConfigService.getActiveModel());
        // restore
        newConfig.put("model", original);
        modelConfigService.updateConfig(newConfig);
    }

    @Test
    public void step07_shouldRejectInvalidTemperature() {
        Double original = modelConfigService.getActiveTemperature();
        Map<String, Object> newConfig = modelConfigService.getConfig();
        newConfig.put("temperature", "999");
        modelConfigService.updateConfig(newConfig);
        assertEquals(original, modelConfigService.getActiveTemperature());
    }

    @Test
    public void step08_shouldRejectMaskedKey() {
        String original = modelConfigService.getActiveApiKey();
        Map<String, Object> newConfig = modelConfigService.getConfig();
        newConfig.put("apiKey", "sk-****abcd");
        modelConfigService.updateConfig(newConfig);
        assertEquals(original, modelConfigService.getActiveApiKey());
    }

    @Test
    public void step09_shouldNotOverrideWithEmptyString() {
        String original = modelConfigService.getActiveModel();
        Map<String, Object> newConfig = modelConfigService.getConfig();
        newConfig.put("model", "");
        modelConfigService.updateConfig(newConfig);
        assertEquals(original, modelConfigService.getActiveModel());
    }
}
