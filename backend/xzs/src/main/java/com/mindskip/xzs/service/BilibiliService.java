package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.BilibiliUrlInfo;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B站视频解析服务
 * 负责解析B站视频URL、调用B站API获取视频流地址
 */
@Service
public class BilibiliService {

    private static final Logger log = LoggerFactory.getLogger(BilibiliService.class);
    private static final String BILIBILI_VIEW_API = "https://api.bilibili.com/x/web-interface/view";
    private static final String BILIBILI_PLAYURL_API = "https://api.bilibili.com/x/player/playurl";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String REFERER = "https://www.bilibili.com";

    // 缓存：key = bvid/aid_p页码, value = CacheEntry
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    // 缓存过期时间：30分钟
    private static final long CACHE_TTL_MS = 30 * 60 * 1000;

    /**
     * 解析B站视频URL，提取AV号/BV号和页码
     */
    public BilibiliUrlInfo parseUrl(String url) {
        BilibiliUrlInfo info = new BilibiliUrlInfo();
        info.setOriginalUrl(url);

        if (url == null || url.trim().isEmpty()) {
            info.setValid(false);
            return info;
        }

        // 匹配BV号: bilibili.com/video/BVxxxxxx
        Pattern bvPattern = Pattern.compile("bilibili\\.com/video/(BV[a-zA-Z0-9]+)");
        Matcher bvMatcher = bvPattern.matcher(url);
        if (bvMatcher.find()) {
            info.setBvid(bvMatcher.group(1));
            info.setValid(true);
        }

        // 匹配AV号: bilibili.com/video/av12345
        Pattern avPattern = Pattern.compile("bilibili\\.com/video/av(\\d+)");
        Matcher avMatcher = avPattern.matcher(url);
        if (avMatcher.find()) {
            info.setAid(avMatcher.group(1));
            info.setValid(true);
        }

        // 如果没有匹配到任何格式，尝试b23.tv短链接域名
        if (!info.isValid()) {
            Pattern b23Pattern = Pattern.compile("b23\\.tv/([a-zA-Z0-9]+)");
            Matcher b23Matcher = b23Pattern.matcher(url);
            if (b23Matcher.find()) {
                // b23.tv短链接需要额外解析，暂时标记为有效（后续可扩展）
                info.setValid(true);
                // 保留原始URL，后续通过重定向获取真实地址
            }
        }

        // 匹配页码: ?p=N 或 &p=N
        Pattern pagePattern = Pattern.compile("[?&]p=(\\d+)");
        Matcher pageMatcher = pagePattern.matcher(url);
        if (pageMatcher.find()) {
            info.setPage(Integer.parseInt(pageMatcher.group(1)));
        }

        return info;
    }

    /**
     * 获取视频基本信息（包含cid、分P列表等）
     * API: https://api.bilibili.com/x/web-interface/view?bvid=xxx 或 ?aid=xxx
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getVideoInfo(String aid, String bvid) throws Exception {
        StringBuilder apiUrl = new StringBuilder(BILIBILI_VIEW_API);
        if (bvid != null && !bvid.isEmpty()) {
            apiUrl.append("?bvid=").append(bvid);
        } else if (aid != null && !aid.isEmpty()) {
            apiUrl.append("?aid=").append(aid);
        } else {
            throw new IllegalArgumentException("aid和bvid不能同时为空");
        }

        String responseJson = httpGet(apiUrl.toString());
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> result = mapper.readValue(responseJson, Map.class);

        return result;
    }

    /**
     * 获取视频流播放地址
     * API: https://api.bilibili.com/x/player/playurl?bvid=xxx&cid=xxx&qn=80&fnval=1&fourk=1
     *
     * @param bvid    BV号
     * @param cid    视频分P的cid
     * @param quality 清晰度: 80=1080P, 64=720P, 32=480P, 16=360P
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPlayUrl(String bvid, String cid, int quality) throws Exception {
        StringBuilder apiUrl = new StringBuilder(BILIBILI_PLAYURL_API);
        apiUrl.append("?bvid=").append(bvid);
        apiUrl.append("&cid=").append(cid);
        apiUrl.append("&qn=").append(quality);
        apiUrl.append("&fnval=1");   // FLV格式
        apiUrl.append("&fourk=1");   // 支持4K

        String responseJson = httpGet(apiUrl.toString());
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> result = mapper.readValue(responseJson, Map.class);

        return result;
    }

    /**
     * 一站式解析：给定B站视频页面URL，返回播放所需的全部信息
     *
     * @param url     B站视频页面URL
     * @param quality 清晰度
     * @return 包含 bvid, cid, page, title, duration, playUrl, backupUrls 的Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> resolveVideo(String url, int quality) throws Exception {
        // 1. 解析URL
        BilibiliUrlInfo urlInfo = parseUrl(url);
        if (!urlInfo.isValid()) {
            throw new IllegalArgumentException("不是有效的B站视频链接: " + url);
        }

        // 2. 构造缓存key
        String cacheKey = (urlInfo.getBvid() != null ? urlInfo.getBvid() : "av" + urlInfo.getAid())
                + "_p" + urlInfo.getPage();

        // 3. 检查缓存
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("Bilibili cache hit: {}", cacheKey);
            return cached.getData();
        }

        // 4. 调用B站view API获取视频信息（含cid）
        Map<String, Object> viewResult = getVideoInfo(urlInfo.getAid(), urlInfo.getBvid());

        Object codeObj = viewResult.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
        if (code != 0) {
            String message = String.valueOf(viewResult.getOrDefault("message", "未知错误"));
            throw new RuntimeException("B站视频信息获取失败 (code=" + code + "): " + message);
        }

        Map<String, Object> data = (Map<String, Object>) viewResult.get("data");
        if (data == null) {
            throw new RuntimeException("B站API返回数据为空");
        }

        // 5. 从pages中找到对应页码的cid
        List<Map<String, Object>> pages = (List<Map<String, Object>>) data.get("pages");
        String cid = null;
        String videoTitle = String.valueOf(data.getOrDefault("title", "未知视频"));
        int duration = 0;

        if (pages != null && !pages.isEmpty()) {
            for (Map<String, Object> page : pages) {
                int pageNum = page.get("page") instanceof Number
                        ? ((Number) page.get("page")).intValue() : 0;
                if (pageNum == urlInfo.getPage()) {
                    cid = String.valueOf(page.get("cid"));
                    duration = page.get("duration") instanceof Number
                            ? ((Number) page.get("duration")).intValue() : 0;
                    String part = String.valueOf(page.getOrDefault("part", ""));
                    if (!part.isEmpty()) {
                        videoTitle += " - " + part;
                    }
                    break;
                }
            }
            // 如果指定页码没找到，使用第一P
            if (cid == null) {
                Map<String, Object> firstPage = pages.get(0);
                cid = String.valueOf(firstPage.get("cid"));
                duration = firstPage.get("duration") instanceof Number
                        ? ((Number) firstPage.get("duration")).intValue() : 0;
            }
        }

        if (cid == null) {
            throw new RuntimeException("无法获取视频cid，视频可能已被删除");
        }

        // 6. 调用B站playurl API获取流地址
        String bvid = String.valueOf(data.getOrDefault("bvid", urlInfo.getBvid()));
        if (bvid == null || "null".equals(bvid)) {
            bvid = urlInfo.getBvid();
        }

        Map<String, Object> playResult = getPlayUrl(bvid, cid, quality);

        codeObj = playResult.get("code");
        code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
        if (code != 0) {
            String message = String.valueOf(playResult.getOrDefault("message", "未知错误"));
            throw new RuntimeException("获取视频流地址失败 (code=" + code + "): " + message);
        }

        Map<String, Object> playData = (Map<String, Object>) playResult.get("data");
        if (playData == null) {
            throw new RuntimeException("B站playurl API返回数据为空");
        }

        // 7. 提取durl中的视频流地址
        List<Map<String, Object>> durl = (List<Map<String, Object>>) playData.get("durl");
        if (durl == null || durl.isEmpty()) {
            // 尝试dash格式
            Map<String, Object> dash = (Map<String, Object>) playData.get("dash");
            if (dash != null) {
                List<Map<String, Object>> video = (List<Map<String, Object>>) dash.get("video");
                if (video != null && !video.isEmpty()) {
                    Map<String, Object> firstVideo = video.get(0);
                    String dashUrl = String.valueOf(firstVideo.getOrDefault("baseUrl", firstVideo.getOrDefault("base_url", "")));
                    if (!dashUrl.isEmpty() && !dashUrl.equals("null")) {
                        // DASH格式FLV → 构造结果
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("bvid", bvid);
                        result.put("cid", cid);
                        result.put("page", urlInfo.getPage());
                        result.put("title", videoTitle);
                        result.put("duration", duration);
                        result.put("playUrl", dashUrl);
                        result.put("backupUrls", new ArrayList<String>());
                        result.put("format", "dash");
                        cache.put(cacheKey, new CacheEntry(result, CACHE_TTL_MS));
                        return result;
                    }
                }
            }
            throw new RuntimeException("未获取到可用的视频流地址");
        }

        // 8. 构造返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bvid", bvid);
        result.put("cid", cid);
        result.put("page", urlInfo.getPage());
        result.put("title", videoTitle);
        result.put("duration", duration);
        result.put("playUrl", String.valueOf(durl.get(0).getOrDefault("url", "")));
        result.put("format", "flv");

        List<String> backupUrls = new ArrayList<>();
        for (int i = 0; i < durl.size(); i++) {
            Object bu = durl.get(i).get("backup_url");
            if (bu instanceof List) {
                for (Object u : (List<?>) bu) {
                    backupUrls.add(String.valueOf(u));
                }
            }
            // 也把其他分段URL加入备用
            if (i > 0) {
                String segUrl = String.valueOf(durl.get(i).getOrDefault("url", ""));
                if (!segUrl.isEmpty() && !"null".equals(segUrl)) {
                    backupUrls.add(segUrl);
                }
            }
        }
        result.put("backupUrls", backupUrls);

        // 9. 缓存结果
        cache.put(cacheKey, new CacheEntry(result, CACHE_TTL_MS));
        log.info("Bilibili resolved: {} -> cid={}, cached as {}", url, cid, cacheKey);

        return result;
    }

    /**
     * 带Referer和UA的HTTP GET请求
     */
    private String httpGet(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", USER_AGENT);
            httpGet.setHeader("Referer", REFERER);
            httpGet.setHeader("Accept", "application/json, text/plain, */*");
            httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");

            return httpClient.execute(httpGet, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    throw new RuntimeException("B站API返回HTTP " + status);
                }
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            });
        }
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final Map<String, Object> data;
        private final long expireAt;

        CacheEntry(Map<String, Object> data, long ttlMs) {
            // 防御性拷贝，避免外部修改影响缓存
            this.data = new LinkedHashMap<>(data);
            this.expireAt = System.currentTimeMillis() + ttlMs;
        }

        Map<String, Object> getData() {
            return new LinkedHashMap<>(data);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
