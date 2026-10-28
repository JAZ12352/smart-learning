package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.service.ModelConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/model-config")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    public ModelConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    /** 获取当前模型配置 */
    @GetMapping
    public RestResponse<Map<String, Object>> getConfig() {
        return RestResponse.ok(modelConfigService.getConfig());
    }

    /** 更新模型配置 */
    @PostMapping
    public RestResponse<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> config) {
        return RestResponse.ok(modelConfigService.updateConfig(config));
    }

    /** 测试 API 连接（type=video 测试视频模型） */
    @PostMapping("/test")
    public RestResponse<Map<String, Object>> testConnection(@RequestParam(required = false) String type) {
        if ("video".equals(type)) {
            return RestResponse.ok(modelConfigService.testVideoConnection());
        }
        return RestResponse.ok(modelConfigService.testConnection());
    }
}
