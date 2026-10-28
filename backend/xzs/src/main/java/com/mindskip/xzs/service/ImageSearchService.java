package com.mindskip.xzs.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配图搜索与下载服务 — 根据关键词从网上搜索并下载课件配图
 *
 * 策略（按优先级自动降级）：
 *   1. Unsplash Source — https://source.unsplash.com/800x600/?keyword  (302重定向到真实图片)
 *   2. Lorem Picsum   — https://picsum.photos/800/600                     (随机高质量照片)
 *   3. 本地占位生成    — 纯色+图标文字                                       (终极兜底)
 *
 * 下载的图片缓存在 downloads/img_cache/ 目录，按关键词哈希命名。
 */
@Service
public class ImageSearchService {

    private static final Logger log = LoggerFactory.getLogger(ImageSearchService.class);
    private static final Path CACHE_DIR = Paths.get("downloads", "img_cache");

    // 缓存：已下载的图片路径
    private final Map<String, Path> cache = new ConcurrentHashMap<>();

    public ImageSearchService() {
        try {
            Files.createDirectories(CACHE_DIR);
            // 恢复已有缓存
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(CACHE_DIR, "*.png")) {
                for (Path p : ds) {
                    String name = p.getFileName().toString().replace(".png", "");
                    cache.put(name, p);
                }
            }
        } catch (IOException e) {
            log.warn("Image cache init failed: {}", e.getMessage());
        }
    }

    /**
     * 根据英文关键词搜索并下载图片
     * @param keywords  英文关键词（如 "classroom illustration, simple diagram"）
     * @param width     目标宽度
     * @param height    目标高度
     * @return 图片字节数组，失败返回 null
     */
    public byte[] searchAndDownload(String keywords, int width, int height) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return null;
        }

        // 提取主要关键词
        String mainKeyword = extractMainKeyword(keywords);
        String cacheKey = mainKeyword + "_" + width + "x" + height;

        // 检查缓存
        Path cached = cache.get(cacheKey);
        if (cached != null && Files.exists(cached)) {
            try {
                return Files.readAllBytes(cached);
            } catch (IOException e) {
                log.warn("Cache read failed for {}: {}", cacheKey, e.getMessage());
            }
        }

        byte[] imageBytes = null;

        // 策略1: Unsplash Source
        imageBytes = tryUnsplash(mainKeyword, width, height);
        if (imageBytes != null && imageBytes.length > 1024) {
            log.info("Image downloaded via Unsplash: {}", mainKeyword);
            cacheImage(cacheKey, imageBytes);
            return imageBytes;
        }

        // 策略2: Lorem Picsum
        imageBytes = tryPicsum(width, height);
        if (imageBytes != null && imageBytes.length > 1024) {
            log.info("Image downloaded via Picsum (fallback): {}", mainKeyword);
            cacheImage(cacheKey, imageBytes);
            return imageBytes;
        }

        // 策略3: 生成纯色占位图
        log.info("Generating placeholder for: {}", mainKeyword);
        imageBytes = generatePlaceholder(mainKeyword, width, height);
        cacheImage(cacheKey, imageBytes);
        return imageBytes;
    }

    /**
     * 从脚本的配图关键词字段中解析出所有图片，返回 关键词→图片字节 的映射
     */
    public Map<String, byte[]> downloadSlideImages(String imageKeywords, int width, int height) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        if (imageKeywords == null || imageKeywords.trim().isEmpty()) {
            return result;
        }

        // 拆分为独立关键词（逗号、顿号、空格分隔）
        String[] parts = imageKeywords.split("[,，、\\s]+");
        for (String part : parts) {
            String kw = part.trim().replaceAll("[\\[\\]\"'（）\\(\\)]", "");
            if (kw.length() < 2 || kw.length() > 50) continue;
            // 跳过中文关键词（Unsplash 不支持中文）
            if (containsChinese(kw)) continue;

            byte[] img = searchAndDownload(kw, width, height);
            if (img != null && img.length > 1024) {
                result.put(kw, img);
                if (result.size() >= 2) break; // 每页最多2张图
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════
    //  图片源策略
    // ═══════════════════════════════════════════

    private byte[] tryUnsplash(String keyword, int width, int height) {
        try {
            String url = "https://source.unsplash.com/" + width + "x" + height + "/?" +
                         keyword.replace(" ", ",");
            return downloadImage(url);
        } catch (Exception e) {
            log.debug("Unsplash failed for '{}': {}", keyword, e.getMessage());
            return null;
        }
    }

    private byte[] tryPicsum(int width, int height) {
        try {
            // Lorem Picsum 返回随机高质量图片
            String url = "https://picsum.photos/" + width + "/" + height + "?random=" + System.currentTimeMillis();
            return downloadImage(url);
        } catch (Exception e) {
            log.debug("Picsum fallback failed: {}", e.getMessage());
            return null;
        }
    }

    private byte[] downloadImage(String url) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            get.setConfig(org.apache.http.client.config.RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(10000)
                .setRedirectsEnabled(true)
                .build());

            HttpResponse resp = client.execute(get);
            int status = resp.getStatusLine().getStatusCode();

            if (status == 200 || status == 302 || status == 301) {
                byte[] bytes = EntityUtils.toByteArray(resp.getEntity());
                if (bytes.length > 512) { // 至少512字节才认为是有效图片
                    return bytes;
                }
            }
        }
        return null;
    }

    /**
     * 生成纯色占位图 — 柔和的校园配色 + 图标文字
     */
    private byte[] generatePlaceholder(String label, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // 柔色背景（根据关键词哈希选色）
        Color[] palette = {
            new Color(0xF0, 0xF4, 0xF8),  // 淡蓝灰
            new Color(0xF2, 0xF7, 0xF2),  // 浅草绿
            new Color(0xFF, 0xFA, 0xF5),  // 奶白
            new Color(0xED, 0xE7, 0xF6),  // 淡紫灰
            new Color(0xFF, 0xF3, 0xE0),  // 暖橙灰
        };
        Color bgColor = palette[Math.abs(label.hashCode()) % palette.length];
        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);

        // 装饰圆
        g.setColor(new Color(0x4A, 0x90, 0xA4, 40));
        g.fillOval(width - 120, -40, 200, 160);
        g.fillOval(-60, height - 120, 160, 140);

        // 中心图标
        g.setColor(new Color(0x4A, 0x90, 0xA4, 100));
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        String icon = getIconForKeyword(label);
        FontMetrics fm = g.getFontMetrics();
        int iconW = fm.stringWidth(icon);
        g.drawString(icon, (width - iconW) / 2, height / 2 + 16);

        // 关键词文字
        g.setColor(new Color(0x3A, 0x3A, 0x3A, 120));
        g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        String text = label.length() > 20 ? label.substring(0, 18) + "…" : label;
        FontMetrics tfm = g.getFontMetrics();
        int textW = tfm.stringWidth(text);
        g.drawString(text, (width - textW) / 2, height / 2 + 60);

        g.dispose();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private String getIconForKeyword(String kw) {
        String lower = kw.toLowerCase();
        if (lower.contains("class") || lower.contains("school") || lower.contains("education")) return "🏫";
        if (lower.contains("math") || lower.contains("science") || lower.contains("physics")) return "🔬";
        if (lower.contains("book") || lower.contains("read") || lower.contains("library")) return "📚";
        if (lower.contains("computer") || lower.contains("tech") || lower.contains("digital")) return "💻";
        if (lower.contains("nature") || lower.contains("plant") || lower.contains("flower")) return "🌿";
        if (lower.contains("art") || lower.contains("draw") || lower.contains("design")) return "🎨";
        if (lower.contains("history") || lower.contains("ancient")) return "🏛";
        if (lower.contains("music") || lower.contains("sound")) return "🎵";
        if (lower.contains("sport") || lower.contains("game")) return "⚽";
        if (lower.contains("food") || lower.contains("cook")) return "🍳";
        if (lower.contains("map") || lower.contains("world") || lower.contains("earth")) return "🌍";
        if (lower.contains("diagram") || lower.contains("chart") || lower.contains("graph")) return "📊";
        if (lower.contains("people") || lower.contains("student") || lower.contains("teacher")) return "👥";
        if (lower.contains("idea") || lower.contains("light") || lower.contains("think")) return "💡";
        if (lower.contains("star") || lower.contains("space")) return "⭐";
        if (lower.contains("heart") || lower.contains("love") || lower.contains("care")) return "💝";
        return "📖";
    }

    // ═══════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════

    private void cacheImage(String key, byte[] bytes) {
        try {
            Path p = CACHE_DIR.resolve(key + ".png");
            Files.write(p, bytes);
            cache.put(key, p);
        } catch (IOException e) {
            log.debug("Cache write skipped: {}", e.getMessage());
        }
    }

    private String extractMainKeyword(String keywords) {
        // 取第一个逗号前的词作为主关键词
        String[] parts = keywords.split("[,，、]");
        for (String part : parts) {
            String cleaned = part.trim().replaceAll("[\\[\\]\"'（）\\(\\)]", "");
            if (!containsChinese(cleaned) && cleaned.length() >= 3) {
                return cleaned.toLowerCase().replaceAll("\\s+", "-");
            }
        }
        // 如果全是中文或太短，用原始字符串
        return keywords.trim().replaceAll("[^a-zA-Z0-9\\s]", "")
                     .replaceAll("\\s+", "-").toLowerCase();
    }

    private boolean containsChinese(String s) {
        return s.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
    }
}
