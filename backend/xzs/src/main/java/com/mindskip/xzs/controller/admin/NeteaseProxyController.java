package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.service.NeteaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 网易公开课（open.163.com）视频代理控制器
 * 为前端提供视频直链解析与内嵌播放支持
 */
@RestController
@RequestMapping("/api/admin/netease")
public class NeteaseProxyController {

    private static final Logger log = LoggerFactory.getLogger(NeteaseProxyController.class);

    @Autowired
    private NeteaseService neteaseService;

    /**
     * 302 重定向到可播放的 mp4 直链（供 <video src> 直接使用）
     * GET /api/admin/netease/play?pid=XXX&mid=YYY
     */
    @GetMapping("/play")
    public ResponseEntity<Void> play(@RequestParam String pid, @RequestParam String mid) {
        try {
            String playUrl = neteaseService.resolvePlayUrl(pid, mid);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, playUrl)
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Netease params: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to resolve Netease video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 返回 JSON 直链信息
     * POST /api/admin/netease/resolve   body: { "pid": "XXX", "mid": "YYY" }
     */
    @PostMapping("/resolve")
    public RestResponse<Map<String, Object>> resolve(@RequestBody Map<String, String> request) {
        try {
            String pid = request.getOrDefault("pid", "");
            String mid = request.getOrDefault("mid", "");

            // 也支持直接传 theater URL，自动解析 pid/mid
            String url = request.getOrDefault("url", "");
            if ((pid.isEmpty() || mid.isEmpty()) && !url.isEmpty()) {
                String[] pm = parsePidMid(url);
                pid = pm[0];
                mid = pm[1];
            }

            if (pid.isEmpty() || mid.isEmpty()) {
                return RestResponse.fail(2, "pid/mid 不能为空");
            }

            Map<String, Object> result = neteaseService.resolveVideo(pid, mid);
            return RestResponse.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid Netease request: {}", e.getMessage());
            return RestResponse.fail(3, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to resolve Netease video: {}", e.getMessage(), e);
            return RestResponse.fail(2, "视频解析失败: " + e.getMessage());
        }
    }

    /**
     * 从 theater URL 提取 pid/mid
     */
    private String[] parsePidMid(String url) {
        String pid = "";
        String mid = "";
        java.util.regex.Matcher pm = java.util.regex.Pattern.compile("[?&]pid=([A-Za-z0-9]+)").matcher(url);
        if (pm.find()) {
            pid = pm.group(1);
        }
        java.util.regex.Matcher mm = java.util.regex.Pattern.compile("[?&]mid=([A-Za-z0-9]+)").matcher(url);
        if (mm.find()) {
            mid = mm.group(1);
        }
        return new String[]{pid, mid};
    }
}
