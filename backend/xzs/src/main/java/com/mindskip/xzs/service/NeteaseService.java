package com.mindskip.xzs.service;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网易公开课（open.163.com）视频解析服务
 * 从 theater 页面抓取带签名的 mp4 直链地址，供前端内嵌播放。
 * 签名 URL 有时效性，故缓存 TTL 设置较短。
 */
@Service
public class NeteaseService {

    private static final Logger log = LoggerFactory.getLogger(NeteaseService.class);

    private static final String THEATER_URL = "https://open.163.com/newview/movie/theater";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String REFERER = "https://open.163.com/";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 分钟，签名 URL 时效短

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 解析网易公开课视频，返回可直接播放的 mp4 直链地址。
     *
     * @param pid 课程 id（theater URL 中的 pid 参数）
     * @param mid 视频 id（theater URL 中的 mid 参数）
     * @return 可播放的 mp4 地址
     */
    public String resolvePlayUrl(String pid, String mid) throws Exception {
        if (pid == null || pid.isEmpty() || mid == null || mid.isEmpty()) {
            throw new IllegalArgumentException("pid/mid 不能为空");
        }

        String cacheKey = pid + "_" + mid;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.playUrl;
        }

        String pageUrl = THEATER_URL + "?pid=" + pid + "&mid=" + mid;
        String html = httpGet(pageUrl);

        String playUrl = extractMp4Url(html, mid);
        if (playUrl == null || playUrl.isEmpty()) {
            throw new IllegalArgumentException("未能从网易公开课页面提取视频地址（mid=" + mid + "）");
        }

        cache.put(cacheKey, new CacheEntry(playUrl, CACHE_TTL_MS));
        log.info("Netease resolved: pid={}, mid={}, playUrl={}", pid, mid, playUrl.substring(0, Math.min(80, playUrl.length())));

        return playUrl;
    }

    /**
     * 解析并返回完整元数据（供 /resolve JSON 接口使用）
     */
    public Map<String, Object> resolveVideo(String pid, String mid) throws Exception {
        String playUrl = resolvePlayUrl(pid, mid);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pid", pid);
        result.put("mid", mid);
        result.put("playUrl", playUrl);
        result.put("format", "mp4");
        return result;
    }

    /**
     * 从 theater/free 页面 HTML 中提取指定 mid 对应的 mp4 直链。
     * 网易公开课有两种页面结构，这里做兼容处理：
     *   A) 当前视频对象含字段 mp4HdUrl/mp4SdUrl: 对象以含 &mid= 或 _MID 的 webUrl/shortWebUrl 标记，向后截取找 mp4HdUrl/mp4SdUrl。
     *   B) 老式结构: 标题后跟 "MID","https:\u002F\u002Fflv0...mp4"。
     */
    private String extractMp4Url(String html, String mid) {
        String playUrl = null;

        // 方案 A：定位当前视频对象起点（其 webUrl/shortWebUrl 含 &mid=<mid> 或 _MID）
        int objStart = indexOfEither(html,
                "&mid=" + mid + "\"",
                "&mid=" + mid + "#",
                "&mid=" + mid + "&",
                "_" + mid + ".html",
                "\"mid\":\"" + mid + "\"");
        if (objStart >= 0) {
            // 从对象起点向后截取一段，找 mp4HdUrl 或 mp4SdUrl 或 mp4ShdUrl 或 mp4ShareUrl
            int scanEnd = Math.min(html.length(), objStart + 6000);
            String seg = html.substring(objStart, scanEnd);
            playUrl = findUrlField(seg, "mp4HdUrl");
            if (playUrl == null) playUrl = findUrlField(seg, "mp4SdUrl");
            if (playUrl == null) playUrl = findUrlField(seg, "mp4ShdUrl");
            if (playUrl == null) playUrl = findUrlField(seg, "mp4ShareUrl");
            if (playUrl == null) playUrl = findMp4NearMostClear(seg);
        }

        // 兜底：方案 A 起点未命中时，改为在全文各字段出现处附近查找 mid（兼容更分散的页面结构）
        if (playUrl == null) {
            String[] fields = {"mp4HdUrl", "mp4SdUrl", "mp4ShdUrl", "mp4ShareUrl"};
            for (String field : fields) {
                String cand = findFieldNearMid(html, field, mid);
                if (cand != null) { playUrl = cand; break; }
            }
            if (playUrl == null) {
                String cand = findMostClearNearMid(html, mid);
                if (cand != null) playUrl = cand;
            }
        }

        // 方案 B：老式 "MID","https:...mp4" 结构
        if (playUrl == null) {
            Pattern p = Pattern.compile("\"" + Pattern.quote(mid) + "\",\"(https:[^\"]+?\\.mp4)\"");
            Matcher m = p.matcher(html);
            if (m.find()) {
                playUrl = unescape(m.group(1));
            }
        }

        return playUrl;
    }

    /** 在全文搜索字段出现处，若该字段附近 2600 字符内含本 mid 则取其 mp4 直链 */
    private String findFieldNearMid(String html, String field, String mid) {
        String key = field + ":";
        int from = 0;
        while (true) {
            int j = html.indexOf(key, from);
            if (j < 0) return null;
            int windowStart = Math.max(0, j - 2600);
            int windowEnd = Math.min(html.length(), j + 700);
            if (html.substring(windowStart, windowEnd).contains(mid)) {
                String seg = html.substring(Math.max(0, j - 100));
                String found = findUrlField(seg, field);
                if (found != null) return found;
            }
            from = j + key.length();
        }
    }

    /** mostClearMp4Info 出现处附近 2600 字符内含本 mid 时提取 */
    private String findMostClearNearMid(String html, String mid) {
        String key = "mostClearMp4Info:{";
        int from = 0;
        while (true) {
            int j = html.indexOf(key, from);
            if (j < 0) return null;
            int windowStart = Math.max(0, j - 2600);
            int windowEnd = Math.min(html.length(), j + 1200);
            if (html.substring(windowStart, windowEnd).contains(mid)) {
                String tail = html.substring(j);
                Pattern p = Pattern.compile("mostClearMp4Info:\\{\"((?:https?://|https?:\\\\u002F\\\\u002F)[^\"]+?\\.mp4)\"");
                Matcher m = p.matcher(tail);
                if (m.find()) return unescape(m.group(1));
            }
            from = j + key.length();
        }
    }

    /**
     * 在 html 中定位第一个命中任一子串的位置
     */
    private int indexOfEither(String html, String... needles) {
        int best = Integer.MAX_VALUE;
        for (String n : needles) {
            int idx = html.indexOf(n);
            if (idx >= 0 && idx < best) {
                best = idx;
            }
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    /**
     * 从片段中提取字段 mp4HdUrl:"https:...mp4" 形式的直链。
     * 兼容两种编码：URL 已还原的 https://...mp4 与仍为 \\u002F 转义的 https:\u002F\u002F...mp4。
     */
    private String findUrlField(String seg, String field) {
        Pattern p = Pattern.compile(
                "(" + Pattern.quote(field) + "):\"((?:https?://|https?:\\\\u002F\\\\u002F)[^\"]+?\\.mp4)\"");
        Matcher m = p.matcher(seg);
        if (!m.find()) {
            // 极少数无引号包裹情况
            p = Pattern.compile(
                    "(" + Pattern.quote(field) + "):\"?((?:https?://|https?:\\\\u002F\\\\u002F)[^,\"]+?\\.mp4)\"?");
            m = p.matcher(seg);
            if (!m.find()) return null;
            return unescape(m.group(2));
        }
        return unescape(m.group(2));
    }

    /**
     * 从 mostClearMp4Info:{ "https:...mp4":... } 中提取 mp4，兼容转义与已还原两种 URL。
     */
    private String findMp4NearMostClear(String seg) {
        int idx = seg.indexOf("mostClearMp4Info:{");
        if (idx < 0) return null;
        String tail = seg.substring(idx);
        Pattern p = Pattern.compile(
                "mostClearMp4Info:\\{\"((?:https?://|https?:\\\\u002F\\\\u002F)[^\"]+?\\.mp4)\"");
        Matcher m = p.matcher(tail);
        if (!m.find()) return null;
        return unescape(m.group(1));
    }

    /**
     * 还原 JS/JSON 转义：\u002F -> / ，\u0026 -> &
     */
    private String unescape(String raw) {
        String escSlash = "\\" + "u002F";
        String escAmp = "\\" + "u0026";
        return raw.replace(escSlash, "/").replace(escAmp, "&");
    }

    private String httpGet(String url) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(15_000)
                .setConnectionRequestTimeout(15_000)
                .setSocketTimeout(30_000)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", USER_AGENT);
            httpGet.setHeader("Referer", REFERER);
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

            return httpClient.execute(httpGet, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    throw new RuntimeException("网易公开课页面返回 HTTP " + status);
                }
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            });
        }
    }

    private static class CacheEntry {
        private final String playUrl;
        private final long expireAt;

        CacheEntry(String playUrl, long ttlMs) {
            this.playUrl = playUrl;
            this.expireAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
