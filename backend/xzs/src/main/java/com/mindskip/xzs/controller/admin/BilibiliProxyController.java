package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.service.BilibiliService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * B站视频代理控制器
 * 为前端提供B站视频流地址解析服务
 */
@RestController
@RequestMapping("/api/admin/bilibili")
public class BilibiliProxyController {

    private static final Logger log = LoggerFactory.getLogger(BilibiliProxyController.class);

    @Autowired
    private BilibiliService bilibiliService;

    /**
     * 解析B站视频URL，返回播放所需的流地址和元数据
     *
     * 请求体: { "url": "https://www.bilibili.com/video/BVxxxxxx/?p=1", "quality": 80 }
     * 响应:   { code: 1, response: { bvid, cid, page, title, duration, playUrl, format, backupUrls[] } }
     */
    @PostMapping("/resolve")
    public RestResponse<Map<String, Object>> resolveVideo(@RequestBody Map<String, Object> request) {
        try {
            String url = String.valueOf(request.getOrDefault("url", ""));
            if (url.isEmpty()) {
                return RestResponse.fail(2, "视频URL不能为空");
            }

            int quality = 80; // 默认1080P
            Object qObj = request.get("quality");
            if (qObj instanceof Number) {
                quality = ((Number) qObj).intValue();
            }

            log.info("Resolving B站 video: url={}, quality={}", url, quality);

            Map<String, Object> result = bilibiliService.resolveVideo(url, quality);

            log.info("B站 video resolved: title={}, format={}", result.get("title"), result.get("format"));
            return RestResponse.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid B站 URL: {}", e.getMessage());
            return RestResponse.fail(3, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to resolve B站 video: {}", e.getMessage(), e);
            return RestResponse.fail(2, "视频解析失败: " + e.getMessage());
        }
    }

    /**
     * 获取视频可用清晰度列表（预留接口）
     */
    @PostMapping("/qualities")
    public RestResponse<Map<String, Object>> getQualities(@RequestBody Map<String, String> request) {
        try {
            String url = request.getOrDefault("url", "");
            if (url.isEmpty()) {
                return RestResponse.fail(2, "视频URL不能为空");
            }

            // 简单返回常用清晰度选项
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            java.util.List<Map<String, Object>> qualities = new java.util.ArrayList<>();

            addQuality(qualities, 120, "4K 超清");
            addQuality(qualities, 112, "1080P 高码率");
            addQuality(qualities, 80, "1080P 高清");
            addQuality(qualities, 64, "720P 高清");
            addQuality(qualities, 32, "480P 清晰");
            addQuality(qualities, 16, "360P 流畅");

            result.put("qualities", qualities);
            return RestResponse.ok(result);

        } catch (Exception e) {
            log.error("Failed to get qualities: {}", e.getMessage(), e);
            return RestResponse.fail(2, "获取清晰度列表失败: " + e.getMessage());
        }
    }

    private void addQuality(java.util.List<Map<String, Object>> list, int qn, String desc) {
        Map<String, Object> q = new java.util.LinkedHashMap<>();
        q.put("qn", qn);
        q.put("desc", desc);
        list.add(q);
    }
}
