package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.service.ThirdPartyResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 第三方资源代理控制器
 * 暴露 REST API 获取外部平台的 PPT 课件和电子课本
 *
 * 路径: /api/admin/third-party
 */
@RestController
@RequestMapping("/api/admin/third-party")
public class ThirdPartyResourceController {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyResourceController.class);
    private final ThirdPartyResourceService thirdPartyResourceService;

    public ThirdPartyResourceController(ThirdPartyResourceService thirdPartyResourceService) {
        this.thirdPartyResourceService = thirdPartyResourceService;
    }

    // ══════════════════════════════════════════
    //  平台信息
    // ══════════════════════════════════════════

    /**
     * 获取所有支持的第三方平台列表
     * GET /api/admin/third-party/platforms
     */
    @GetMapping("/platforms")
    public RestResponse<List<Map<String, Object>>> getPlatforms() {
        List<Map<String, Object>> platforms = thirdPartyResourceService.getSupportedPlatforms();
        return RestResponse.ok(platforms);
    }

    /**
     * 识别URL归属的平台
     * POST /api/admin/third-party/detect-platform
     * Body: { "url": "https://..." }
     */
    @PostMapping("/detect-platform")
    public RestResponse<Map<String, Object>> detectPlatform(@RequestBody Map<String, String> request) {
        String url = request.getOrDefault("url", "").trim();
        if (url.isEmpty()) {
            return RestResponse.fail(3, "URL不能为空");
        }

        ThirdPartyResourceService.PlatformDef platform = thirdPartyResourceService.detectPlatform(url);
        Map<String, Object> result = new LinkedHashMap<>();
        if (platform != null) {
            result.put("recognized", true);
            result.put("platform", platform.name);
            result.put("domain", platform.domain);
            result.put("supportedTypes", platform.supportedTypes);
        } else {
            result.put("recognized", false);
            result.put("platform", "未知平台");
        }
        result.put("url", url);
        return RestResponse.ok(result);
    }

    // ══════════════════════════════════════════
    //  搜索
    // ══════════════════════════════════════════

    /**
     * 搜索第三方资源
     * POST /api/admin/third-party/search
     * Body: { "keyword": "...", "platform": "百度文库" (可选), "resourceType": "ppt|pdf|textbook" (可选) }
     */
    @PostMapping("/search")
    public RestResponse<Map<String, Object>> search(@RequestBody Map<String, String> request) {
        String keyword = request.getOrDefault("keyword", "").trim();
        String platform = request.getOrDefault("platform", "").trim();
        String resourceType = request.getOrDefault("resourceType", "").trim();

        if (keyword.isEmpty()) {
            return RestResponse.fail(3, "搜索关键词不能为空");
        }

        try {
            Map<String, Object> results = thirdPartyResourceService.search(keyword, platform, resourceType);
            return RestResponse.ok(results);
        } catch (Exception e) {
            log.error("Third-party search failed: keyword={}", keyword, e);
            return RestResponse.fail(2, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取搜索建议
     * GET /api/admin/third-party/suggestions?keyword=...&platform=...
     */
    @GetMapping("/suggestions")
    public RestResponse<List<String>> getSuggestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String platform) {
        List<String> suggestions = thirdPartyResourceService.getSearchSuggestions(
            platform != null ? platform : "", keyword != null ? keyword : ""
        );
        return RestResponse.ok(suggestions);
    }

    // ══════════════════════════════════════════
    //  资源获取
    // ══════════════════════════════════════════

    /**
     * 获取/下载单个资源
     * POST /api/admin/third-party/fetch
     * Body: { "url": "https://...", "forceRefresh": false }
     */
    @PostMapping("/fetch")
    public RestResponse<Map<String, Object>> fetchResource(@RequestBody Map<String, Object> request) {
        String url = String.valueOf(request.getOrDefault("url", "")).trim();
        boolean forceRefresh = Boolean.parseBoolean(String.valueOf(request.getOrDefault("forceRefresh", "false")));

        if (url.isEmpty()) {
            return RestResponse.fail(3, "资源URL不能为空");
        }

        try {
            Map<String, Object> resource = thirdPartyResourceService.fetchResource(url, forceRefresh);
            return RestResponse.ok(resource);
        } catch (IllegalArgumentException e) {
            return RestResponse.fail(3, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch resource: {}", url, e);
            return RestResponse.fail(2, "获取资源失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取资源
     * POST /api/admin/third-party/fetch-batch
     * Body: { "urls": ["url1", "url2", ...], "forceRefresh": false }
     */
    @PostMapping("/fetch-batch")
    public RestResponse<Map<String, Object>> fetchBatch(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) request.getOrDefault("urls", Collections.emptyList());
        boolean forceRefresh = Boolean.parseBoolean(String.valueOf(request.getOrDefault("forceRefresh", "false")));

        if (urls.isEmpty()) {
            return RestResponse.fail(3, "URL列表不能为空");
        }

        try {
            List<Map<String, Object>> results = thirdPartyResourceService.fetchBatch(urls, forceRefresh);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("total", results.size());
            response.put("items", results);
            response.put("successCount", results.stream().filter(r -> !r.containsKey("error")).count());
            response.put("failCount", results.stream().filter(r -> r.containsKey("error")).count());
            return RestResponse.ok(response);
        } catch (Exception e) {
            log.error("Batch fetch failed", e);
            return RestResponse.fail(2, "批量获取失败: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    //  文件下载服务
    // ══════════════════════════════════════════

    /**
     * 下载已缓存的资源文件
     * GET /api/admin/third-party/download/{fileName}
     *
     * 支持 iframe 内嵌预览和直接下载
     * query param: ?disposition=inline|attachment (默认 attachment)
     */
    @GetMapping("/download/{fileName:.+}")
    public void serveFile(
            @PathVariable String fileName,
            @RequestParam(defaultValue = "attachment") String disposition,
            HttpServletResponse response) throws IOException {

        // URL 解码（支持中文文件名）
        try {
            fileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (Exception ignored) {}

        try {
            Path filePath = thirdPartyResourceService.getCachedFile(fileName);

            // MIME 类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = inferMimeType(fileName);
            }

            response.setContentType(contentType);
            response.setContentLengthLong(Files.size(filePath));
            response.setHeader("Content-Disposition", disposition + "; filename=\"" +
                new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1) + "\"");
            response.setHeader("Cache-Control", "public, max-age=86400");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
            response.setHeader("Access-Control-Allow-Origin", "*");

            try (OutputStream out = response.getOutputStream();
                 InputStream in = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
        } catch (SecurityException e) {
            response.sendError(403, "Forbidden: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            response.sendError(404, "Not Found: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to serve file: {}", fileName, e);
            response.sendError(500, "Internal Error");
        }
    }

    // ══════════════════════════════════════════
    //  缓存管理
    // ══════════════════════════════════════════

    /**
     * 列出所有缓存的第三方资源
     * GET /api/admin/third-party/cached
     */
    @GetMapping("/cached")
    public RestResponse<List<Map<String, Object>>> listCached() {
        List<Map<String, Object>> resources = thirdPartyResourceService.listCachedResources();
        return RestResponse.ok(resources);
    }

    /**
     * 删除缓存的资源
     * DELETE /api/admin/third-party/cached/{fileName}
     */
    @DeleteMapping("/cached/{fileName:.+}")
    public RestResponse<Map<String, Object>> deleteCached(@PathVariable String fileName) {
        try {
            fileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (Exception ignored) {}

        boolean deleted = thirdPartyResourceService.deleteCachedResource(fileName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", fileName);
        result.put("deleted", deleted);
        return RestResponse.ok(result);
    }

    /**
     * 清空所有缓存的资源
     * DELETE /api/admin/third-party/cached
     */
    @DeleteMapping("/cached")
    public RestResponse<Map<String, Object>> clearAllCached() {
        List<Map<String, Object>> all = thirdPartyResourceService.listCachedResources();
        int deleted = 0;
        for (Map<String, Object> item : all) {
            String name = String.valueOf(item.get("name"));
            if (thirdPartyResourceService.deleteCachedResource(name)) {
                deleted++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFound", all.size());
        result.put("deleted", deleted);
        return RestResponse.ok(result);
    }

    // ══════════════════════════════════════════
    //  辅助
    // ══════════════════════════════════════════

    private String inferMimeType(String fileName) {
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
        switch (ext) {
            case "pdf":  return "application/pdf";
            case "ppt":  return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "doc":  return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png":  return "image/png";
            case "gif":  return "image/gif";
            case "svg":  return "image/svg+xml";
            case "mp4":  return "video/mp4";
            case "mp3":  return "audio/mpeg";
            case "html":
            case "htm":  return "text/html";
            case "json": return "application/json";
            case "txt":  return "text/plain";
            case "zip":  return "application/zip";
            default:     return "application/octet-stream";
        }
    }
}
