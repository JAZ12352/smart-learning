package com.mindskip.xzs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 第三方资源代理服务
 * 支持从多个第三方平台获取 PPT 课件和电子课本资源
 *
 * 架构: 每个平台有独立的解析策略（URL匹配 → 内容提取 → 缓存）
 * 扩展: 新增平台只需添加一条 PLATFORM 配置即可
 */
@Service
public class ThirdPartyResourceService {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyResourceService.class);
    private static final Path CACHE_DIR = Paths.get("downloads", "third_party");
    private static final int MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 60000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    private final ObjectMapper objectMapper = new ObjectMapper();
    // 缓存: URL哈希 → 资源信息
    private final Map<String, CacheEntry> resourceCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60 * 60 * 1000; // 1小时

    // ── 平台定义 ──
    // 每个平台配置: name, domain patterns, type hints, request headers
    private static final List<PlatformDef> PLATFORMS = new ArrayList<>();

    static {
        // ── 产业技能平台（对齐赛题受众：企业内训 / 转岗 / 工程师进阶）──

        // B站技术区（岗位技能视频首选）
        PLATFORMS.add(new PlatformDef(
            "B站技术区", "bilibili.com",
            Arrays.asList("video"),
            "https://search.bilibili.com/all?keyword=%s"
        ));

        // Hugging Face（大模型课程与模型库）
        PLATFORMS.add(new PlatformDef(
            "Hugging Face", "huggingface.co",
            Arrays.asList("doc", "pdf", "video"),
            "https://huggingface.co/search/full-text?q=%s&type=course"
        ));

        // DataWhale 开源学习社区
        PLATFORMS.add(new PlatformDef(
            "DataWhale", "github.com/datawhalechina",
            Arrays.asList("doc", "pdf"),
            "https://github.com/search?q=datawhale+%s&type=repositories"
        ));

        // 飞桨 AI Studio（免费GPU实训）
        PLATFORMS.add(new PlatformDef(
            "飞桨AI Studio", "aistudio.baidu.com",
            Arrays.asList("doc", "notebook", "dataset"),
            "https://aistudio.baidu.com/aistudio/projectoverview?keyword=%s"
        ));

        // 阿里云天池（产业数据集与竞赛）
        PLATFORMS.add(new PlatformDef(
            "阿里云天池", "tianchi.aliyun.com",
            Arrays.asList("dataset", "competition", "doc"),
            "https://tianchi.aliyun.com/search?q=%s"
        ));

        // Microsoft Learn（MLOps/AI 工程认证路径）
        PLATFORMS.add(new PlatformDef(
            "Microsoft Learn", "learn.microsoft.com",
            Arrays.asList("doc", "certification"),
            "https://learn.microsoft.com/en-us/search/?terms=%s"
        ));

        // Kaggle Learn（速成证书与竞赛）
        PLATFORMS.add(new PlatformDef(
            "Kaggle Learn", "kaggle.com",
            Arrays.asList("notebook", "dataset"),
            "https://www.kaggle.com/search?q=%s"
        ));

        // ── 理论补充平台（先修基础课）──

        // 中国大学 MOOC
        PLATFORMS.add(new PlatformDef(
            "中国大学MOOC", "icourse163.org",
            Arrays.asList("ppt", "pdf", "video"),
            "https://www.icourse163.org/search.htm?search=%s"
        ));

        // 国家高等教育智慧教育平台
        PLATFORMS.add(new PlatformDef(
            "智慧教育平台", "smartedu.cn",
            Arrays.asList("ppt", "pdf", "textbook"),
            "https://higher.smartedu.cn/"
        ));

        // 通用 — 直接 URL 或网盘链接
        PLATFORMS.add(new PlatformDef(
            "通用URL", null,  // null = 匹配所有未识别的URL
            Arrays.asList("ppt", "pdf", "doc", "docx", "txt", "html"),
            null
        ));
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(CACHE_DIR);
            log.info("ThirdPartyResourceService initialized, cache dir: {}", CACHE_DIR.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create cache directory", e);
        }
    }

    // ══════════════════════════════════════════════
    //  公开 API
    // ══════════════════════════════════════════════

    /**
     * 识别 URL 归属的平台
     */
    public PlatformDef detectPlatform(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        String lower = url.toLowerCase();
        for (PlatformDef p : PLATFORMS) {
            if (p.domain != null && lower.contains(p.domain)) {
                return p;
            }
        }
        // 返回通用平台
        return PLATFORMS.get(PLATFORMS.size() - 1);
    }

    /**
     * 获取所有支持的平台列表
     */
    public List<Map<String, Object>> getSupportedPlatforms() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PlatformDef p : PLATFORMS) {
            if (p.domain == null) continue; // 跳过通用平台
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p.name);
            m.put("domain", p.domain);
            m.put("supportedTypes", p.supportedTypes);
            m.put("searchUrl", p.searchTemplate);
            list.add(m);
        }
        return list;
    }

    /**
     * 搜索第三方资源
     * @param keyword 搜索关键词
     * @param platform 平台名称（可选，null=搜索全部）
     * @param resourceType 资源类型（可选：ppt, pdf, textbook）
     */
    public Map<String, Object> search(String keyword, String platform, String resourceType) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword);
        result.put("searchTime", System.currentTimeMillis());

        List<Map<String, Object>> items = new ArrayList<>();

        // 构建搜索目标平台列表
        List<PlatformDef> targets = new ArrayList<>();
        if (platform != null && !platform.isEmpty()) {
            for (PlatformDef p : PLATFORMS) {
                if (p.name.equals(platform) || (p.domain != null && p.domain.contains(platform))) {
                    targets.add(p);
                }
            }
        }
        if (targets.isEmpty()) {
            // 搜索所有非通用平台
            for (PlatformDef p : PLATFORMS) {
                if (p.domain != null) targets.add(p);
            }
        }

        for (PlatformDef p : targets) {
            try {
                List<Map<String, Object>> platformResults = searchPlatform(p, keyword, resourceType);
                items.addAll(platformResults);
            } catch (Exception e) {
                log.warn("Search failed for platform {}: {}", p.name, e.getMessage());
                // 继续搜索其他平台
            }
        }

        result.put("total", items.size());
        result.put("items", items);
        result.put("searchedPlatforms", targets.size());
        return result;
    }

    /**
     * 获取单个资源的详细信息（下载、解析元数据）
     * @param resourceUrl 资源URL
     * @param forceRefresh 是否强制刷新缓存
     */
    public Map<String, Object> fetchResource(String resourceUrl, boolean forceRefresh) throws Exception {
        String cacheKey = sha256(resourceUrl).substring(0, 12);

        // 检查缓存
        if (!forceRefresh) {
            CacheEntry cached = resourceCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.info("ThirdParty cache hit: {}", cacheKey);
                Map<String, Object> result = new LinkedHashMap<>(cached.data);
                result.put("cached", true);
                return result;
            }
        }

        PlatformDef platform = detectPlatform(resourceUrl);
        if (platform == null) {
            throw new IllegalArgumentException("无法识别该URL所属的平台: " + resourceUrl);
        }

        log.info("Fetching resource from {}: {}", platform.name, resourceUrl);

        // 0. 快速判断：URL是否为直接文件下载链接
        boolean isDirectFile = isDirectFileUrl(resourceUrl);

        // 1. 探测文件信息（HEAD 请求）
        FileInfo fileInfo;
        try {
            fileInfo = probeFile(resourceUrl);
        } catch (Exception e) {
            // HEAD请求失败，判断为平台页面URL
            if (!isDirectFile) {
                return buildPreviewResult(resourceUrl, platform);
            }
            throw e;
        }

        // 非直接文件URL或探测失败 → 返回预览模式
        if (!isDirectFile && !isResourceContentType(fileInfo.contentType)) {
            return buildPreviewResult(resourceUrl, platform);
        }

        // 2. 下载资源到本地缓存
        String localFileName = cacheKey + "_" + sanitizeFileName(fileInfo.fileName);
        Path localPath = CACHE_DIR.resolve(localFileName);

        try {
            if (!Files.exists(localPath)) {
                downloadFile(resourceUrl, localPath);
            }
        } catch (Exception e) {
            // 下载失败 → 回退到预览模式
            log.warn("Download failed for {}, falling back to preview: {}", resourceUrl, e.getMessage());
            return buildPreviewResult(resourceUrl, platform);
        }

        // 3. 提取元数据
        Map<String, Object> metadata = extractMetadata(localPath, fileInfo, platform);

        // 4. 构造返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", resourceUrl);
        result.put("platform", platform.name);
        result.put("fileName", fileInfo.fileName);
        result.put("fileSize", Files.size(localPath));
        result.put("contentType", fileInfo.contentType);
        result.put("resourceType", metadata.getOrDefault("resourceType", classifyResource(fileInfo.fileName)));
        result.put("localUrl", "/api/admin/third-party/download/" + localFileName);
        result.put("metadata", metadata);
        result.put("cached", false);
        result.put("fetchTime", System.currentTimeMillis());
        result.put("previewMode", "download");  // 本地文件

        // 缓存
        resourceCache.put(cacheKey, new CacheEntry(result, CACHE_TTL_MS));

        return result;
    }

    /**
     * 判断URL是否为直接文件下载链接（非平台页面URL）
     */
    private boolean isDirectFileUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        // 常见文件扩展名
        if (lower.endsWith(".pdf") || lower.endsWith(".ppt") || lower.endsWith(".pptx")
            || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".xls")
            || lower.endsWith(".xlsx") || lower.endsWith(".txt") || lower.endsWith(".zip")
            || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".mp4")) {
            return true;
        }
        // 常见直接下载域名
        return lower.contains("raw.githubusercontent.com")
            || lower.contains("github.com") && (lower.contains("/raw/") || lower.endsWith("/raw"))
            || lower.contains("qiniucdn.com") || lower.contains("cloudfront.net");
    }

    /**
     * 判断Content-Type是否为可下载的资源文件类型
     */
    private boolean isResourceContentType(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.contains("pdf") || ct.contains("powerpoint") || ct.contains("presentation")
            || ct.contains("msword") || ct.contains("document") || ct.contains("spreadsheet")
            || ct.contains("image/") || ct.contains("video/") || ct.contains("audio/")
            || ct.contains("application/zip") || ct.contains("octet-stream");
    }

    /**
     * 构建预览模式返回（不可下载时，返回Office Online Viewer预览URL）
     */
    private Map<String, Object> buildPreviewResult(String resourceUrl, PlatformDef platform) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", resourceUrl);
        result.put("platform", platform.name);
        result.put("fileName", extractFileNameFromUrl(resourceUrl));
        result.put("fileSize", 0L);
        result.put("contentType", "text/html");
        result.put("resourceType", "外部预览");
        result.put("previewMode", "online");  // 在线预览
        try {
            String encodedUrl = java.net.URLEncoder.encode(resourceUrl, "UTF-8");
            result.put("previewUrl", "https://view.officeapps.live.com/op/embed.aspx?src=" + encodedUrl);
        } catch (java.io.UnsupportedEncodingException e) {
            result.put("previewUrl", "https://view.officeapps.live.com/op/embed.aspx?src=" + resourceUrl);
        }
        result.put("metadata", new LinkedHashMap<String, Object>() {{
            put("resourceType", "外部平台资源");
            put("format", "html");
            put("viewable", true);
            put("platform", platform.name);
            put("note", "该资源来自第三方平台，通过在线查看器预览");
        }});
        result.put("cached", false);
        result.put("fetchTime", System.currentTimeMillis());
        return result;
    }

    /**
     * 批量获取资源（支持多个URL并行下载）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchBatch(List<String> urls, boolean forceRefresh) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (String url : urls) {
            try {
                results.add(fetchResource(url, forceRefresh));
            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("url", url);
                err.put("error", e.getMessage());
                results.add(err);
            }
        }
        return results;
    }

    /**
     * 下载已缓存的资源文件
     */
    public Path getCachedFile(String fileName) {
        Path filePath = CACHE_DIR.resolve(fileName).normalize();
        if (!filePath.startsWith(CACHE_DIR)) {
            throw new SecurityException("路径越权: " + fileName);
        }
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + fileName);
        }
        return filePath;
    }

    /**
     * 列出已缓存的第三方资源
     */
    public List<Map<String, Object>> listCachedResources() {
        List<Map<String, Object>> files = new ArrayList<>();
        try {
            Files.list(CACHE_DIR).filter(Files::isRegularFile).forEach(f -> {
                try {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", f.getFileName().toString());
                    info.put("size", Files.size(f));
                    info.put("lastModified", Files.getLastModifiedTime(f).toMillis());
                    info.put("type", classifyResource(f.getFileName().toString()));
                    info.put("url", "/api/admin/third-party/download/" + f.getFileName().toString());
                    files.add(info);
                } catch (IOException ignored) {}
            });
        } catch (IOException e) {
            log.warn("Failed to list cached resources: {}", e.getMessage());
        }
        // 按修改时间降序
        files.sort((a, b) -> Long.compare(
            (long) b.getOrDefault("lastModified", 0L),
            (long) a.getOrDefault("lastModified", 0L)
        ));
        return files;
    }

    /**
     * 删除缓存的资源
     */
    public boolean deleteCachedResource(String fileName) {
        try {
            Path filePath = CACHE_DIR.resolve(fileName).normalize();
            if (!filePath.startsWith(CACHE_DIR)) return false;
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete cached resource: {}", fileName);
            return false;
        }
    }

    /**
     * 获取指定平台的热门搜索建议
     */
    public List<String> getSearchSuggestions(String platform, String keyword) {
        // 为不同平台返回搜索建议（可扩展为调用平台API）
        List<String> suggestions = new ArrayList<>();
        String[] prefixes = {"课件", "电子课本", "教学", "精品课程", "期末复习"};
        for (String prefix : prefixes) {
            suggestions.add((keyword != null ? keyword + " " : "") + prefix);
        }
        return suggestions;
    }

    // ══════════════════════════════════════════════
    //  内部方法
    // ══════════════════════════════════════════════

    /**
     * 在单个平台上搜索资源
     * 通过构造搜索URL + HTML解析来获取结果列表
     */
    private List<Map<String, Object>> searchPlatform(PlatformDef platform, String keyword, String resourceType) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        if (platform.searchTemplate == null) return results;

        String searchUrl = platform.searchTemplate.replace("%s", URLEncoder.encode(keyword, "UTF-8"));

        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(searchUrl);
            httpGet.setHeader("User-Agent", USER_AGENT);
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            // 设置 Referer 避免部分平台的反爬
            httpGet.setHeader("Referer", "https://www.google.com/");

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                String html = EntityUtils.toString(response.getEntity(), "UTF-8");
                results = parseSearchResults(html, platform, keyword, resourceType);
            } else if (statusCode == 302 || statusCode == 301) {
                // 重定向 → 标记为需要手动访问
                Map<String, Object> redirect = new LinkedHashMap<>();
                redirect.put("title", platform.name + " - " + keyword);
                redirect.put("url", searchUrl);
                redirect.put("platform", platform.name);
                redirect.put("type", "redirect");
                redirect.put("note", "该平台需要直接访问，已为您构造搜索链接");
                results.add(redirect);
            } else {
                log.debug("Platform {} returned HTTP {} for search '{}'", platform.name, statusCode, keyword);
            }
        }

        return results;
    }

    /**
     * 从HTML中提取搜索结果
     * 使用正则匹配常见的资源链接模式
     */
    private List<Map<String, Object>> parseSearchResults(String html, PlatformDef platform, String keyword, String resourceType) {
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        // 通用模式: 从HTML中提取链接（href + 标题文本）
        // 匹配 <a ... href="..." ...>标题</a>
        Pattern linkPattern = Pattern.compile(
            "<a[^>]+href=\"([^\"]+)\"[^>]*>([^<]{5,200})</a>",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = linkPattern.matcher(html);

        int maxResults = 20;
        while (matcher.find() && results.size() < maxResults) {
            String href = matcher.group(1).trim();
            String title = matcher.group(2).trim().replaceAll("<[^>]+>", "").replaceAll("\\s+", " ");

            // 过滤：只保留资源类型的链接
            if (!isResourceLink(href, resourceType)) continue;
            if (seenUrls.contains(href)) continue;
            if (title.length() < 3) continue;

            // 补全相对URL
            String fullUrl = resolveUrl(href, platform);

            seenUrls.add(href);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", title);
            item.put("url", fullUrl);
            item.put("platform", platform.name);
            item.put("type", classifyResource(title));
            item.put("source", "search");
            results.add(item);
        }

        // 如果没有匹配到链接，返回一个直接访问提示
        if (results.isEmpty()) {
            Map<String, Object> direct = new LinkedHashMap<>();
            direct.put("title", "在" + platform.name + "中搜索: " + keyword);
            direct.put("url", platform.searchTemplate != null ?
                platform.searchTemplate.replace("%s", keyword) : "");
            direct.put("platform", platform.name);
            direct.put("type", "search_link");
            direct.put("note", "请直接访问该链接查看搜索结果");
            results.add(direct);
        }

        return results;
    }

    /**
     * 探测远程文件信息（通过HEAD请求，避免完整下载）
     */
    private FileInfo probeFile(String url) throws Exception {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpHead httpHead = new HttpHead(url);
            httpHead.setHeader("User-Agent", USER_AGENT);
            httpHead.setHeader("Accept", "*/*");

            HttpResponse response = httpClient.execute(httpHead);
            int status = response.getStatusLine().getStatusCode();

            FileInfo info = new FileInfo();
            info.statusCode = status;
            info.contentType = "application/octet-stream";

            if (response.containsHeader("Content-Type")) {
                info.contentType = response.getFirstHeader("Content-Type").getValue();
            }
            if (response.containsHeader("Content-Length")) {
                info.contentLength = Long.parseLong(response.getFirstHeader("Content-Length").getValue());
            }
            if (response.containsHeader("Content-Disposition")) {
                String cd = response.getFirstHeader("Content-Disposition").getValue();
                // 从 Content-Disposition 提取文件名
                Matcher fnMatch = Pattern.compile("filename[*]?=\"?([^\";]+)\"?").matcher(cd);
                if (fnMatch.find()) {
                    info.fileName = fnMatch.group(1);
                }
            }

            if (info.fileName == null) {
                info.fileName = extractFileNameFromUrl(url);
            }

            return info;
        } catch (Exception e) {
            // HEAD 请求失败时回退到 URL 提取
            FileInfo fallback = new FileInfo();
            fallback.fileName = extractFileNameFromUrl(url);
            fallback.contentType = "application/octet-stream";
            return fallback;
        }
    }

    /**
     * 下载文件到本地
     */
    private void downloadFile(String url, Path localPath) throws Exception {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", USER_AGENT);
            httpGet.setHeader("Accept", "*/*");
            httpGet.setHeader("Referer", new java.net.URL(url).getProtocol() + "://" + new java.net.URL(url).getHost() + "/");

            HttpResponse response = httpClient.execute(httpGet);
            int status = response.getStatusLine().getStatusCode();

            if (status == 200) {
                try (InputStream is = response.getEntity().getContent()) {
                    Files.copy(is, localPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } else if (status == 302 || status == 301) {
                // 跟随重定向
                String redirectUrl = response.getFirstHeader("Location").getValue();
                log.info("Following redirect: {} -> {}", url, redirectUrl);
                downloadFile(redirectUrl, localPath);
            } else {
                throw new IOException("HTTP " + status + " when downloading: " + url);
            }
        }
    }

    /**
     * 从下载的文件中提取元数据
     */
    private Map<String, Object> extractMetadata(Path localPath, FileInfo fileInfo, PlatformDef platform) {
        Map<String, Object> meta = new LinkedHashMap<>();
        String fileName = fileInfo.fileName.toLowerCase();
        String contentType = fileInfo.contentType;

        // 根据文件类型提取不同元数据
        if (fileName.endsWith(".pdf")) {
            meta.put("resourceType", "电子课本");
            meta.put("format", "pdf");
            meta.put("viewable", true);
        } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
            meta.put("resourceType", "PPT课件");
            meta.put("format", fileName.substring(fileName.lastIndexOf('.') + 1));
            meta.put("viewable", true);
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            meta.put("resourceType", "文档");
            meta.put("format", fileName.substring(fileName.lastIndexOf('.') + 1));
            meta.put("viewable", false);
        } else {
            meta.put("resourceType", "其他资源");
            meta.put("format", fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "unknown");
            meta.put("viewable", false);
        }

        meta.put("platform", platform.name);
        meta.put("fileSize", fileInfo.contentLength > 0 ? fileInfo.contentLength : fileInfo(localPath));

        // 尝试通过文件名推断更多信息
        String baseName = fileName.replaceAll("\\.[^.]+$", "");
        meta.put("inferredTitle", baseName.replaceAll("[-_]", " "));

        return meta;
    }

    /**
     * 判断链接是否为资源类型
     */
    private boolean isResourceLink(String href, String resourceType) {
        String lower = href.toLowerCase();

        // 排除明显不是资源的链接
        if (lower.startsWith("javascript:") || lower.startsWith("#") || lower.startsWith("mailto:")) {
            return false;
        }

        // 如果指定了资源类型，优先匹配
        if (resourceType != null && !resourceType.isEmpty()) {
            switch (resourceType.toLowerCase()) {
                case "ppt":
                    return lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.contains("ppt");
                case "pdf":
                case "textbook":
                    return lower.endsWith(".pdf") || lower.contains("pdf") || lower.contains("textbook");
                case "doc":
                    return lower.endsWith(".doc") || lower.endsWith(".docx") || lower.contains("doc");
            }
        }

        // 通用匹配：常见资源扩展名
        return lower.endsWith(".ppt") || lower.endsWith(".pptx")
            || lower.endsWith(".pdf")
            || lower.endsWith(".doc") || lower.endsWith(".docx")
            || lower.contains("/ppt/") || lower.contains("/pdf/")
            || lower.contains("download") || lower.contains("resource")
            || lower.contains("course") || lower.contains("textbook");
    }

    /**
     * 将相对URL补全为绝对URL
     */
    private String resolveUrl(String href, PlatformDef platform) {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (platform.domain == null) return href;

        String scheme = "https";
        String host = platform.domain;

        if (href.startsWith("//")) {
            return scheme + ":" + href;
        }
        if (href.startsWith("/")) {
            return scheme + "://" + host + href;
        }
        return scheme + "://" + host + "/" + href;
    }

    /**
     * 从URL提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        try {
            String path = new java.net.URL(url).getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.isEmpty() || name.length() > 200) name = "resource";
            int qIdx = name.indexOf('?');
            if (qIdx > 0) name = name.substring(0, qIdx);
            // 如果没有扩展名，添加默认扩展名
            if (!name.contains(".")) name += ".bin";
            return name;
        } catch (Exception e) {
            return "resource.bin";
        }
    }

    /**
     * 根据文件名分类资源类型
     */
    private String classifyResource(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "PPT课件";
        if (lower.endsWith(".pdf")) return "电子课本";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "文档";
        if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".gif")) return "图片";
        if (lower.endsWith(".mp4") || lower.endsWith(".flv") || lower.endsWith(".avi")) return "视频";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "文本";
        return "其他";
    }

    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(CONNECT_TIMEOUT)
            .setSocketTimeout(READ_TIMEOUT)
            .setRedirectsEnabled(true)
            .setCircularRedirectsAllowed(false)
            .setMaxRedirects(5)
            .build();
        return HttpClients.custom()
            .setDefaultRequestConfig(config)
            .setUserAgent(USER_AGENT)
            .disableCookieManagement()  // 避免被平台追踪
            .build();
    }

    private String sanitizeFileName(String name) {
        // 保留中文、英文、数字、点、下划线、连字符
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

    private long fileInfo(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    // ══════════════════════════════════════════════
    //  数据类
    // ══════════════════════════════════════════════

    /**
     * 平台定义
     */
    public static class PlatformDef {
        public final String name;
        public final String domain;       // 域名关键词，null = 通用
        public final List<String> supportedTypes;
        public final String searchTemplate; // null = 不支持搜索

        PlatformDef(String name, String domain, List<String> supportedTypes, String searchTemplate) {
            this.name = name;
            this.domain = domain;
            this.supportedTypes = supportedTypes != null ? supportedTypes : Collections.emptyList();
            this.searchTemplate = searchTemplate;
        }
    }

    /**
     * 文件元信息（HEAD探测结果）
     */
    private static class FileInfo {
        String fileName = "resource";
        String contentType = "application/octet-stream";
        long contentLength = -1;
        int statusCode = 0;
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final Map<String, Object> data;
        final long expireAt;

        CacheEntry(Map<String, Object> data, long ttlMs) {
            this.data = new LinkedHashMap<>(data);
            this.expireAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
