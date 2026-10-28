package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin/resource")
public class ResourceProxyController {

    private static final Logger log = LoggerFactory.getLogger(ResourceProxyController.class);
    private static final Path DOWNLOAD_DIR = Paths.get("downloads");
    private static final Path IMAGE_DIR = DOWNLOAD_DIR.resolve("images");
    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    static {
        try {
            Files.createDirectories(DOWNLOAD_DIR);
            Files.createDirectories(IMAGE_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create download directories", e);
        }
    }

    /**
     * 下载远程资源到本地，返回本地访问URL
     */
    @PostMapping("/proxy")
    public RestResponse<Map<String, Object>> proxyResource(@RequestBody Map<String, String> request) {
        String remoteUrl = request.getOrDefault("url", "").trim();
        String fileName = request.getOrDefault("fileName", "").trim();

        if (remoteUrl.isEmpty() || remoteUrl.equals("#")) {
            return RestResponse.fail(3, "URL无效，请提供有效的资源链接");
        }

        // 拒绝GitHub tree/directory URLs（这些不是文件下载链接）
        if (remoteUrl.contains("github.com") && remoteUrl.contains("/tree/")) {
            return RestResponse.fail(3, "GitHub目录页面不支持直接下载，请提供raw文件链接");
        }

        try {
            // 生成本地文件名
            if (fileName.isEmpty()) {
                fileName = extractFileName(remoteUrl);
            }
            // 如果fileName没有合理的扩展名，从URL提取
            String ext = getExtension(fileName).toLowerCase();
            if (ext.isEmpty() || ext.length() > 5) {
                String urlExt = getExtension(extractFileName(remoteUrl));
                if (!urlExt.isEmpty() && urlExt.length() <= 5) {
                    fileName = fileName + "." + urlExt;
                }
            }
            String hash = sha256(remoteUrl).substring(0, 8);
            String localName = hash + "_" + sanitizeFileName(fileName);
            Path localPath = DOWNLOAD_DIR.resolve(localName);

            // 如果已下载过，直接返回
            if (Files.exists(localPath)) {
                Map<String, Object> result = new HashMap<>();
                result.put("localUrl", "/api/admin/resource/download/" + localName);
                result.put("fileName", fileName);
                result.put("fileSize", Files.size(localPath));
                result.put("cached", true);
                result.put("contentType", inferMimeType(fileName));
                return RestResponse.ok(result);
            }

            // 下载文件
            URL urlObj = new URL(remoteUrl);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            // 跟随重定向
            conn.setInstanceFollowRedirects(true);

            int contentLength = conn.getContentLength();
            if (contentLength > MAX_FILE_SIZE) {
                return RestResponse.fail(4, "文件过大（>" + (MAX_FILE_SIZE / 1024 / 1024) + "MB），无法下载");
            }

            // 检测实际内容类型，调整扩展名
            String responseContentType = conn.getContentType();
            if (responseContentType != null) {
                String actualExt = mimeTypeToExtension(responseContentType);
                if (actualExt != null && !actualExt.equals(getExtension(fileName).toLowerCase())) {
                    // 如果URL扩展名与实际内容类型不匹配，使用实际类型
                    if (getExtension(fileName).equalsIgnoreCase("pdf") || actualExt.equals("pdf")) {
                        // 保持PDF扩展名优先
                    } else if (!actualExt.equals("bin")) {
                        fileName = fileName.replaceAll("\\.[^.]+$", "") + "." + actualExt;
                    }
                }
            }

            try (InputStream is = conn.getInputStream()) {
                Files.copy(is, localPath, StandardCopyOption.REPLACE_EXISTING);
            }

            long fileSize = Files.size(localPath);
            if (fileSize == 0) {
                Files.deleteIfExists(localPath);
                return RestResponse.fail(5, "下载的文件为空");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("localUrl", "/api/admin/resource/download/" + localName);
            result.put("fileName", fileName);
            result.put("fileSize", fileSize);
            result.put("cached", false);
            result.put("contentType", inferMimeType(fileName));
            log.info("Resource downloaded: {} -> {} ({} bytes, type={})", remoteUrl, localName, fileSize, inferMimeType(fileName));
            return RestResponse.ok(result);

        } catch (java.net.SocketTimeoutException e) {
            log.error("Timeout downloading: {}", remoteUrl);
            return RestResponse.fail(6, "资源下载超时，请稍后重试");
        } catch (Exception e) {
            log.error("Failed to proxy resource: {}", remoteUrl, e);
            return RestResponse.fail(2, "资源下载失败: " + e.getMessage());
        }
    }

    /**
     * 搜索并下载图片（用于PPT配图）
     */
    @PostMapping("/search-images")
    public RestResponse<Map<String, Object>> searchImages(@RequestBody Map<String, String> request) {
        String keyword = request.getOrDefault("keyword", "").trim();
        int count = Math.min(Integer.parseInt(request.getOrDefault("count", "3")), 5);

        if (keyword.isEmpty()) {
            return RestResponse.fail(3, "搜索关键词不能为空");
        }

        List<Map<String, String>> images = new ArrayList<>();

        try {
            // 使用 Unsplash Source API（免费，无需API Key）
            // 或使用 DuckDuckGo 图片搜索
            images.addAll(searchUnsplash(keyword, count, images.size()));
            if (images.size() < count) {
                images.addAll(searchPlaceholderIcons(keyword, count - images.size(), images.size()));
            }
        } catch (Exception e) {
            log.warn("Image search failed for '{}', using placeholders: {}", keyword, e.getMessage());
            images = searchPlaceholderIcons(keyword, count, 0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("images", images);
        return RestResponse.ok(result);
    }

    /**
     * 上传本地文件（PPT/PDF/电子课本），存储到downloads目录并返回本地访问URL
     */
    @PostMapping("/upload")
    public RestResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return RestResponse.fail(3, "文件为空");
        }

        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null) {
                originalName = "upload.bin";
            }

            // 限制文件大小（50MB）
            if (file.getSize() > MAX_FILE_SIZE) {
                return RestResponse.fail(4, "文件过大（>" + (MAX_FILE_SIZE / 1024 / 1024) + "MB）");
            }

            // 限制文件类型
            String ext = getExtension(originalName).toLowerCase();
            String mimeType = inferMimeType(originalName);

            String hash = sha256(originalName + System.currentTimeMillis()).substring(0, 8);
            String localName = hash + "_" + sanitizeFileName(originalName);
            Path localPath = DOWNLOAD_DIR.resolve(localName);

            // 如果已存在，重新生成名字
            if (Files.exists(localPath)) {
                localName = sha256(originalName + System.nanoTime()).substring(0, 8) + "_" + sanitizeFileName(originalName);
                localPath = DOWNLOAD_DIR.resolve(localName);
            }

            file.transferTo(localPath.toFile());

            Map<String, Object> result = new HashMap<>();
            result.put("localUrl", "/api/admin/resource/download/" + localName);
            result.put("fileName", originalName);
            result.put("fileSize", Files.size(localPath));
            result.put("cached", false);
            result.put("contentType", mimeType);
            result.put("previewMode", "local");
            result.put("previewUrl", "/api/admin/resource/download/" + localName + "?disposition=inline");
            log.info("File uploaded: {} -> {} ({} bytes)", originalName, localName, Files.size(localPath));
            return RestResponse.ok(result);

        } catch (Exception e) {
            log.error("Failed to upload file", e);
            return RestResponse.fail(2, "文件上传失败: " + e.getMessage());
        }
    }

    private List<Map<String, String>> searchUnsplash(String keyword, int count, int startIdx) {
        List<Map<String, String>> images = new ArrayList<>();
        try {
            // Unsplash Source: https://source.unsplash.com/800x600/?keyword
            for (int i = 0; i < count && images.size() < count; i++) {
                String url = "https://source.unsplash.com/800x600/?" + URLEncoder.encode(keyword, "UTF-8") + "&sig=" + i;
                String hash = sha256(url).substring(0, 8);
                String localName = "unsplash_" + hash + ".jpg";
                Path localPath = IMAGE_DIR.resolve(localName);

                if (!Files.exists(localPath)) {
                    try {
                        downloadImageFromUrl(url, localPath);
                    } catch (Exception e) {
                        log.warn("Unsplash download failed: {}", e.getMessage());
                        continue;
                    }
                }

                Map<String, String> img = new HashMap<>();
                img.put("localUrl", "/api/admin/resource/image/" + localName);
                img.put("sourceUrl", url);
                img.put("alt", keyword);
                img.put("source", "Unsplash");
                images.add(img);
            }
        } catch (Exception e) {
            log.warn("Unsplash search failed: {}", e.getMessage());
        }
        return images;
    }

    private List<Map<String, String>> searchPlaceholderIcons(String keyword, int count, int startIdx) {
        List<Map<String, String>> images = new ArrayList<>();
        // 生成教学主题相关的SVG占位图
        String[] colors = {"#6366f1", "#8b5cf6", "#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#ec4899", "#06b6d4"};
        String[] icons = {"📚", "📐", "🔬", "💡", "📊", "🎯", "📝", "🌟", "🧩", "🎓"};

        for (int i = 0; i < count; i++) {
            String icon = icons[(startIdx + i) % icons.length];
            String color = colors[(startIdx + i) % colors.length];
            String svg = String.format(
                "<svg xmlns='http://www.w3.org/2000/svg' width='800' height='600' viewBox='0 0 800 600'>" +
                "<rect width='800' height='600' fill='%s' opacity='0.15'/>" +
                "<rect x='50' y='50' width='700' height='500' rx='20' fill='white' stroke='%s' stroke-width='3'/>" +
                "<text x='400' y='280' text-anchor='middle' font-size='120'>%s</text>" +
                "<text x='400' y='400' text-anchor='middle' font-size='28' fill='%s'>%s</text>" +
                "</svg>",
                color, color, icon, color, keyword
            );
            String hash = sha256(svg).substring(0, 8);
            String localName = "placeholder_" + hash + ".svg";
            Path localPath = IMAGE_DIR.resolve(localName);
            if (!Files.exists(localPath)) {
                try { Files.write(localPath, svg.getBytes(StandardCharsets.UTF_8)); } catch (IOException ignored) {}
            }

            Map<String, String> img = new HashMap<>();
            img.put("localUrl", "/api/admin/resource/image/" + localName);
            img.put("sourceUrl", "");
            img.put("alt", keyword);
            img.put("source", "Generated");
            images.add(img);
        }
        return images;
    }

    /**
     * 提供下载的本地文件（支持iframe嵌入查看）
     */
    @GetMapping("/download/{fileName:.+}")
    public void serveFile(@PathVariable String fileName,
                          @RequestParam(defaultValue = "attachment") String disposition,
                          HttpServletResponse response) throws IOException {
        // 接受 URL 编码的文件名（支持中文和特殊字符）
        try {
            fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
        } catch (Exception ignored) {}

        Path filePath = DOWNLOAD_DIR.resolve(fileName).normalize();
        if (!filePath.startsWith(DOWNLOAD_DIR)) {
            response.sendError(403, "Forbidden");
            return;
        }
        if (!Files.exists(filePath)) {
            // 尝试查找匹配的最新文件（文件名中时间戳可能已变化）
            try {
                final String baseName = fileName.replaceFirst("_\\d{13}\\.pptx$", "");
                final String searchExt = getExtension(fileName);
                File[] matches = DOWNLOAD_DIR.toFile().listFiles((dir, name) -> {
                    return name.startsWith(baseName) && name.endsWith("." + searchExt);
                });
                if (matches != null && matches.length > 0) {
                    // 取最新的文件
                    File latest = matches[0];
                    for (File f : matches) {
                        if (f.lastModified() > latest.lastModified()) {
                            latest = f;
                        }
                    }
                    filePath = latest.toPath();
                    log.info("Download fallback: {} → {}", fileName, latest.getName());
                } else {
                    response.sendError(404, "File not found: " + fileName);
                    return;
                }
            } catch (Exception ex) {
                response.sendError(404, "File not found: " + fileName);
                return;
            }
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            // 根据扩展名推断MIME类型
            contentType = inferMimeType(filePath.getFileName().toString());
        }
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(filePath));
        // 支持内嵌和附件两种模式
        // 文件名 URL 编码，避免中文导致 Express 代理 HPE_INVALID_HEADER_TOKEN
        String safeFileName = URLEncoder.encode(fileName.replaceAll("\\\\", "_"), "UTF-8")
            .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", disposition + "; filename*=UTF-8''" + safeFileName);
        response.setHeader("Cache-Control", "public, max-age=86400");
        // 允许同源iframe嵌入
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        // 允许跨域访问（同源请求不需要，但保留以备代理使用）
        response.setHeader("Access-Control-Allow-Origin", "*");

        try (OutputStream out = response.getOutputStream(); InputStream in = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    /**
     * 根据文件名扩展名推断MIME类型
     */
    private String inferMimeType(String fileName) {
        String ext = getExtension(fileName).toLowerCase();
        switch (ext) {
            case "pdf":  return "application/pdf";
            case "ppt":  return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "doc":  return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":  return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png":  return "image/png";
            case "gif":  return "image/gif";
            case "svg":  return "image/svg+xml";
            case "html":
            case "htm":  return "text/html";
            case "txt":  return "text/plain";
            case "json": return "application/json";
            case "xml":  return "application/xml";
            case "mp4":  return "video/mp4";
            case "mp3":  return "audio/mpeg";
            case "zip":  return "application/zip";
            default:     return "application/octet-stream";
        }
    }

    /**
     * 提供下载的本地图片
     */
    @GetMapping("/image/{fileName:.+}")
    public void serveImage(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        Path filePath = IMAGE_DIR.resolve(fileName).normalize();
        if (!filePath.startsWith(IMAGE_DIR)) {
            response.sendError(403, "Forbidden");
            return;
        }
        if (!Files.exists(filePath)) {
            response.sendError(404, "Image not found");
            return;
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "image/svg+xml";
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "public, max-age=86400");
        Files.copy(filePath, response.getOutputStream());
    }

    /**
     * 列出已下载的资源
     */
    @GetMapping("/downloads")
    public RestResponse<List<Map<String, Object>>> listDownloads() {
        List<Map<String, Object>> files = new ArrayList<>();
        try {
            Files.list(DOWNLOAD_DIR).filter(Files::isRegularFile).forEach(f -> {
                Map<String, Object> info = new HashMap<>();
                info.put("name", f.getFileName().toString());
                info.put("size", f.toFile().length());
                info.put("url", "/api/admin/resource/download/" + f.getFileName().toString());
                files.add(info);
            });
        } catch (IOException e) {
            log.warn("Failed to list downloads: {}", e.getMessage());
        }
        return RestResponse.ok(files);
    }

    // ====== 辅助方法 ======

    private void downloadImageFromUrl(String imageUrl, Path localPath) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        try (InputStream is = conn.getInputStream()) {
            Files.copy(is, localPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String extractFileName(String url) {
        try {
            String path = new URL(url).getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.isEmpty() || name.length() > 200) name = "resource";
            // 去掉查询参数
            int qIdx = name.indexOf('?');
            if (qIdx > 0) name = name.substring(0, qIdx);
            return name;
        } catch (Exception e) {
            return "resource";
        }
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx + 1) : "bin";
    }

    /**
     * 将MIME类型转换为文件扩展名
     */
    private String mimeTypeToExtension(String mimeType) {
        if (mimeType == null) return null;
        String mt = mimeType.toLowerCase().split(";")[0].trim();
        switch (mt) {
            case "application/pdf": return "pdf";
            case "application/vnd.ms-powerpoint": return "ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation": return "pptx";
            case "application/msword": return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": return "docx";
            case "application/vnd.ms-excel": return "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": return "xlsx";
            case "image/jpeg": return "jpg";
            case "image/png": return "png";
            case "image/gif": return "gif";
            case "image/svg+xml": return "svg";
            case "text/html": return "html";
            case "text/plain": return "txt";
            case "application/json": return "json";
            case "application/zip":
            case "application/x-zip-compressed": return "zip";
            case "application/octet-stream": return "bin";
            default: return null;
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.\\-_\\u4e00-\\u9fa5]", "_");
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
