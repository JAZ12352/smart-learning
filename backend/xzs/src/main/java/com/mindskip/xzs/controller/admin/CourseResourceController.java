package com.mindskip.xzs.controller.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindskip.xzs.base.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/courses")
public class CourseResourceController {

    private static final Logger log = LoggerFactory.getLogger(CourseResourceController.class);
    private static final String DATA_FILE = "course-data.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Map<String, Object>> courses = new ArrayList<>();

    /** 章节知识点 LLM 扩充（可选依赖：模型未配置时跳过，回落到原始数据） */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.mindskip.xzs.service.DeepSeekService deepSeekService;

    /** 扩充结果缓存：同一章节只生成一次，重复导出 PPT 不再重复调用模型 */
    private final Map<String, List<String>> enrichedPointsCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 章节知识点不足时，用 LLM 生成具体知识点补充，替代"本章核心概念与理论知识"这类空泛占位话术。
     * 模型不可用或失败时返回原列表（保持旧填充逻辑作为最终兜底）。
     */
    private List<String> enrichChapterPoints(String courseName, String chTitle, List<String> existing) {
        if (existing.size() >= 4 || deepSeekService == null || !deepSeekService.isConfigured()) {
            return existing;
        }
        String cacheKey = courseName + "::" + chTitle;
        List<String> cached = enrichedPointsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sys = new LinkedHashMap<>();
            sys.put("role", "system");
            sys.put("content", "你是高校课程内容专家。只输出知识点列表本身，不要任何开场白、说明或总结。");
            messages.add(sys);
            Map<String, String> user = new LinkedHashMap<>();
            user.put("role", "user");
            user.put("content", "请为高校课程《" + courseName + "》的章节《" + chTitle + "》列出 6 个核心知识点。"
                + "要求：\n1. 每行一个，以\"- \"开头\n2. 具体到概念、定理、方法或公式名称（如\"KMP 字符串匹配算法\"而非\"字符串匹配\"）\n"
                + "3. 覆盖基础定义、核心原理、典型方法、常见应用\n4. 禁止出现\"本章\"\"核心概念\"等泛泛表述");
            messages.add(user);
            String raw = deepSeekService.complete(messages);
            List<String> parsed = new ArrayList<>();
            for (String line : raw.split("\n")) {
                String p = line.trim().replaceAll("^[-*•]\\s*|^\\d+[.、）)]\\s*", "");
                if (p.length() >= 4 && p.length() <= 60 && !p.contains("以下是") && !p.contains("希望")) {
                    parsed.add(p);
                }
                if (parsed.size() >= 6) break;
            }
            if (parsed.size() >= 3) {
                enrichedPointsCache.put(cacheKey, parsed);
                log.info("Enriched chapter points for {} :: {} ({} points)", courseName, chTitle, parsed.size());
                return parsed;
            }
        } catch (Exception e) {
            log.warn("Chapter points enrichment failed for {} :: {}: {}", courseName, chTitle, e.getMessage());
        }
        return existing;
    }

    @PostConstruct
    public void init() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try {
                courses = objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
                log.info("Loaded {} courses from {}", courses.size(), DATA_FILE);
            } catch (Exception e) {
                log.warn("Failed to read {}, loading defaults: {}", DATA_FILE, e.getMessage());
            }
        }
        if (courses.isEmpty()) {
            seedDefaultCourses();
        }
    }

    private void seedDefaultCourses() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(DATA_FILE);
            if (is != null) {
                courses = objectMapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
                log.info("Seeded {} courses from classpath: {}", courses.size(), DATA_FILE);
                persist();
            }
        } catch (Exception e) {
            log.warn("Failed to load seed data: {}", e.getMessage());
        }
    }

    @GetMapping
    public RestResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        List<Map<String, Object>> result = courses;
        if (category != null && !category.isEmpty()) {
            result = result.stream()
                    .filter(c -> category.equals(c.get("category")))
                    .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            result = result.stream()
                    .filter(c -> {
                        String name = String.valueOf(c.getOrDefault("name", ""));
                        String desc = String.valueOf(c.getOrDefault("description", ""));
                        String school = String.valueOf(c.getOrDefault("school", ""));
                        return name.toLowerCase().contains(kw) || desc.toLowerCase().contains(kw) || school.toLowerCase().contains(kw);
                    })
                    .collect(Collectors.toList());
        }
        return RestResponse.ok(result);
    }

    @GetMapping("/{id}")
    public RestResponse<Map<String, Object>> getById(@PathVariable String id) {
        for (Map<String, Object> c : courses) {
            if (id.equals(c.get("id"))) {
                return RestResponse.ok(c);
            }
        }
        return RestResponse.fail(2, "课程不存在");
    }

    @GetMapping("/categories")
    public RestResponse<List<String>> getCategories() {
        Set<String> cats = new LinkedHashSet<>();
        for (Map<String, Object> c : courses) {
            String cat = String.valueOf(c.getOrDefault("category", "其他"));
            if (!"null".equals(cat)) cats.add(cat);
        }
        return RestResponse.ok(new ArrayList<>(cats));
    }

    @PostMapping("/import")
    public RestResponse<Map<String, Object>> importCourse(@RequestBody Map<String, Object> courseData) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        courseData.put("id", id);
        courseData.put("importTime", System.currentTimeMillis());
        courseData.put("source", "manual_import");

        // Ensure nested structures exist
        if (!courseData.containsKey("chapters")) courseData.put("chapters", new ArrayList<>());
        if (!courseData.containsKey("pptList")) courseData.put("pptList", new ArrayList<>());
        if (!courseData.containsKey("videoList")) courseData.put("videoList", new ArrayList<>());
        if (!courseData.containsKey("textbookResources")) courseData.put("textbookResources", new ArrayList<>());
        if (!courseData.containsKey("exercises")) courseData.put("exercises", new ArrayList<>());

        courses.add(courseData);
        persist();
        log.info("Course imported: {}", courseData.get("name"));
        return RestResponse.ok(courseData);
    }

    @PostMapping("/import-url")
    public RestResponse<Map<String, Object>> importFromUrl(@RequestBody Map<String, String> request) {
        String url = request.getOrDefault("url", "").trim();
        if (url.isEmpty()) {
            return RestResponse.fail(3, "URL不能为空");
        }
        // For real implementation, this would scrape/fetch course data from the URL
        // For now, we return a placeholder that the frontend can populate
        Map<String, Object> course = new LinkedHashMap<>();
        course.put("id", UUID.randomUUID().toString().substring(0, 8));
        course.put("sourceUrl", url);
        course.put("name", request.getOrDefault("name", "导入课程"));
        course.put("description", request.getOrDefault("description", "从 " + url + " 导入的课程资源"));
        course.put("school", request.getOrDefault("school", "第三方平台"));
        course.put("instructor", request.getOrDefault("instructor", "待确认"));
        course.put("credit", 3);
        course.put("category", request.getOrDefault("category", "其他"));
        course.put("status", "ongoing");
        course.put("studentCount", 0);
        course.put("rating", 0);
        course.put("resourceCount", 0);
        course.put("term", "导入学期");
        course.put("textbook", "待确认");
        course.put("importTime", System.currentTimeMillis());
        course.put("source", "url_import");
        course.put("chapters", new ArrayList<>());
        course.put("pptList", new ArrayList<>());
        course.put("videoList", new ArrayList<>());
        course.put("textbookResources", new ArrayList<>());
        course.put("exercises", new ArrayList<>());

        // Try to fetch title from URL
        try {
            java.net.URL urlObj = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String html = readStream(conn.getInputStream());
                java.util.regex.Matcher titleMatcher = java.util.regex.Pattern.compile("<title>([^<]+)</title>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
                if (titleMatcher.find()) {
                    String pageTitle = titleMatcher.group(1).trim();
                    if (pageTitle.length() > 3) {
                        course.put("name", pageTitle.replace(" - ", " · ").substring(0, Math.min(pageTitle.length(), 60)));
                    }
                }
            }
        } catch (Exception ignored) {
            // URL fetch is best-effort
        }

        courses.add(course);
        persist();
        log.info("Course imported from URL: {}", url);
        return RestResponse.ok(course);
    }

    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(@PathVariable String id) {
        boolean removed = courses.removeIf(c -> id.equals(c.get("id")));
        if (removed) {
            persist();
            return RestResponse.ok(null);
        }
        return RestResponse.fail(2, "课程不存在");
    }

    @PutMapping("/{id}")
    public RestResponse<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        for (int i = 0; i < courses.size(); i++) {
            if (id.equals(courses.get(i).get("id"))) {
                Map<String, Object> course = courses.get(i);
                for (Map.Entry<String, Object> entry : updates.entrySet()) {
                    if (entry.getValue() != null) {
                        course.put(entry.getKey(), entry.getValue());
                    }
                }
                persist();
                return RestResponse.ok(course);
            }
        }
        return RestResponse.fail(2, "课程不存在");
    }

    @PostMapping("/sync")
    public RestResponse<List<Map<String, Object>>> syncFromKnowledgeBase() {
        // Sync course data from the generated resources / knowledge base
        // For now, just return all courses
        return RestResponse.ok(courses);
    }

    /**
     * 获取课程嵌入信息 — 返回 embedUrl、iframe 代码片段和直接链接
     */
    @GetMapping("/{id}/embed")
    public RestResponse<Map<String, Object>> getEmbedInfo(@PathVariable String id) {
        for (Map<String, Object> c : courses) {
            if (id.equals(c.get("id"))) {
                Map<String, Object> result = new LinkedHashMap<>();
                String courseName = String.valueOf(c.getOrDefault("name", "课程"));

                // 构建嵌入URL（指向独立查看器）
                String baseUrl = String.format("/api/admin/courses/embed-viewer?courseId=%s", id);
                result.put("embedUrl", baseUrl);
                result.put("iframeCode", String.format(
                    "<iframe src=\"%s\" width=\"100%%\" height=\"600\" frameborder=\"0\" allowfullscreen></iframe>",
                    baseUrl));

                // 提取PPT和教材的直接链接
                List<Map<String, String>> pptUrls = new ArrayList<>();
                List<?> pptList = (List<?>) c.getOrDefault("pptList", Collections.emptyList());
                if (pptList != null) {
                    for (Object p : pptList) {
                        if (p instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> pm = (Map<String, Object>) p;
                            String url = String.valueOf(pm.getOrDefault("url", ""));
                            if (!url.isEmpty() && !"#".equals(url)) {
                                Map<String, String> item = new LinkedHashMap<>();
                                item.put("name", String.valueOf(pm.getOrDefault("name", "课件")));
                                item.put("url", url);
                                pptUrls.add(item);
                            }
                        }
                    }
                }

                List<Map<String, String>> tbUrls = new ArrayList<>();
                List<?> tbList = (List<?>) c.getOrDefault("textbookResources", Collections.emptyList());
                if (tbList != null) {
                    for (Object t : tbList) {
                        if (t instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> tm = (Map<String, Object>) t;
                            String url = String.valueOf(tm.getOrDefault("url", ""));
                            if (!url.isEmpty() && !"#".equals(url)) {
                                Map<String, String> item = new LinkedHashMap<>();
                                item.put("name", String.valueOf(tm.getOrDefault("name", "教材")));
                                item.put("url", url);
                                item.put("author", String.valueOf(tm.getOrDefault("author", "")));
                                tbUrls.add(item);
                            }
                        }
                    }
                }

                Map<String, Object> directUrls = new LinkedHashMap<>();
                directUrls.put("ppt", pptUrls);
                directUrls.put("textbook", tbUrls);

                result.put("directUrls", directUrls);
                result.put("courseName", courseName);
                result.put("courseId", id);
                return RestResponse.ok(result);
            }
        }
        return RestResponse.fail(2, "课程不存在");
    }

    /**
     * 独立嵌入查看器 — 纯 HTML 页面，可用于第三方 iframe 嵌入
     */
    @GetMapping(value = "/embed-viewer", produces = "text/html;charset=UTF-8")
    public String embedViewer(
            @RequestParam String courseId,
            @RequestParam(defaultValue = "ppt") String resourceType,
            @RequestParam(defaultValue = "0") int resourceIndex) {
        // 查找课程
        Map<String, Object> course = null;
        for (Map<String, Object> c : courses) {
            if (courseId.equals(c.get("id"))) {
                course = c;
                break;
            }
        }
        if (course == null) {
            return "<html><body><h2>课程未找到</h2></body></html>";
        }

        String courseName = String.valueOf(course.getOrDefault("name", "课程"));
        String resourceUrl = "";
        String resourceTitle = "";

        if ("ppt".equals(resourceType)) {
            List<?> pptList = (List<?>) course.getOrDefault("pptList", Collections.emptyList());
            if (pptList != null && resourceIndex < pptList.size()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ppt = (Map<String, Object>) pptList.get(resourceIndex);
                resourceUrl = String.valueOf(ppt.getOrDefault("url", ""));
                resourceTitle = String.valueOf(ppt.getOrDefault("name", "课件"));
            }
        } else if ("textbook".equals(resourceType)) {
            List<?> tbList = (List<?>) course.getOrDefault("textbookResources", Collections.emptyList());
            if (tbList != null && resourceIndex < tbList.size()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tb = (Map<String, Object>) tbList.get(resourceIndex);
                resourceUrl = String.valueOf(tb.getOrDefault("url", ""));
                resourceTitle = String.valueOf(tb.getOrDefault("name", "教材"));
            }
        }

        // 生成独立嵌入查看器 HTML
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>").append(escapeHtml(courseName)).append(" - ").append(escapeHtml(resourceTitle)).append("</title>\n");
        html.append("<style>\n");
        html.append("*{margin:0;padding:0;box-sizing:border-box}\n");
        html.append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Microsoft YaHei',sans-serif;background:#f5f7fa;overflow:hidden}\n");
        html.append(".embed-header{display:flex;align-items:center;justify-content:space-between;padding:10px 16px;background:#1e293b;color:#fff}\n");
        html.append(".embed-header h3{font-size:14px;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}\n");
        html.append(".embed-header span{font-size:11px;opacity:0.65}\n");
        html.append(".embed-body{position:relative;height:calc(100vh - 44px)}\n");
        html.append(".embed-body iframe{position:absolute;inset:0;width:100%;height:100%;border:none;background:#fff}\n");
        html.append(".embed-nav{display:flex;align-items:center;gap:8px;padding:8px 16px;background:#fff;border-bottom:1px solid #e8ecf1}\n");
        html.append(".embed-nav button{padding:4px 12px;border:1px solid #d0d5dd;border-radius:6px;background:#fff;cursor:pointer;font-size:12px;color:#475569;transition:all 0.2s}\n");
        html.append(".embed-nav button:hover{border-color:#4A90A4;color:#4A90A4}\n");
        html.append(".embed-nav button.active{background:#4A90A4;color:#fff;border-color:#4A90A4}\n");
        html.append(".embed-empty{display:flex;align-items:center;justify-content:center;height:100%;color:#94a3b8;font-size:14px}\n");
        html.append(".embed-powered{text-align:center;padding:6px;font-size:10px;color:#94a3b8;background:#fff;border-top:1px solid #e8ecf1}\n");
        html.append("@media(max-width:480px){.embed-nav{flex-wrap:wrap}}\n");
        html.append("</style>\n</head>\n<body>\n");

        // Header
        html.append("<div class=\"embed-header\">");
        html.append("<h3>").append(escapeHtml(courseName)).append("</h3>");
        html.append("<span>知汇贝客 · 智慧教学平台</span>");
        html.append("</div>\n");

        // Navigation tabs for PPT/textbook switching
        html.append("<div class=\"embed-nav\">");
        List<?> pptList = (List<?>) course.getOrDefault("pptList", Collections.emptyList());
        if (pptList != null && !pptList.isEmpty()) {
            for (int i = 0; i < pptList.size(); i++) {
                @SuppressWarnings("unchecked")
                Map<String, Object> p = (Map<String, Object>) pptList.get(i);
                String name = String.valueOf(p.getOrDefault("name", "PPT" + (i + 1)));
                String active = ("ppt".equals(resourceType) && i == resourceIndex) ? " class=\"active\"" : "";
                String navUrl = String.format("/api/admin/courses/embed-viewer?courseId=%s&resourceType=ppt&resourceIndex=%d", courseId, i);
                html.append("<button").append(active).append(" onclick=\"location.href='").append(navUrl).append("'\">📊 ").append(escapeHtml(name)).append("</button>\n");
            }
        }
        List<?> tbList = (List<?>) course.getOrDefault("textbookResources", Collections.emptyList());
        if (tbList != null && !tbList.isEmpty()) {
            for (int i = 0; i < tbList.size(); i++) {
                @SuppressWarnings("unchecked")
                Map<String, Object> t = (Map<String, Object>) tbList.get(i);
                String name = String.valueOf(t.getOrDefault("name", "教材" + (i + 1)));
                String active = ("textbook".equals(resourceType) && i == resourceIndex) ? " class=\"active\"" : "";
                String navUrl = String.format("/api/admin/courses/embed-viewer?courseId=%s&resourceType=textbook&resourceIndex=%d", courseId, i);
                html.append("<button").append(active).append(" onclick=\"location.href='").append(navUrl).append("'\">📖 ").append(escapeHtml(name)).append("</button>\n");
            }
        }
        html.append("</div>\n");

        // Body
        html.append("<div class=\"embed-body\">");
        if (!resourceUrl.isEmpty() && !"#".equals(resourceUrl)) {
            html.append("<iframe src=\"").append(escapeHtml(resourceUrl)).append("\" allowfullscreen></iframe>");
        } else {
            html.append("<div class=\"embed-empty\">📚 请从上方导航选择要预览的资源</div>");
        }
        html.append("</div>\n");

        // Powered by
        html.append("<div class=\"embed-powered\">Powered by 知汇贝客 · 智慧教学平台 | 可嵌入第三方教学平台</div>\n");

        // PostMessage for height auto-resize
        html.append("<script>\n");
        html.append("window.addEventListener('load',function(){if(window.parent!==window){window.parent.postMessage({type:'embed-ready',height:document.body.scrollHeight},'*');}\n");
        html.append("var observer=new ResizeObserver(function(){window.parent.postMessage({type:'embed-resize',height:document.body.scrollHeight},'*');});observer.observe(document.body);});\n");
        html.append("</script>\n");

        html.append("</body>\n</html>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private synchronized void persist() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(DATA_FILE), courses);
        } catch (IOException e) {
            log.warn("Failed to persist course data: {}", e.getMessage());
        }
    }

    private String readStream(java.io.InputStream is) throws java.io.IOException {
        java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        String result = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return result;
    }

    /**
     * 自动生成课程教学PPT（从章节知识点内容生成，不用第三方平台）
     * POST /api/admin/courses/{id}/generate-ppts
     * Body 可选: { slides: [...], download: true }
     */
    @PostMapping("/{id}/generate-ppts")
    public RestResponse<Map<String, Object>> generatePPTs(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> course = findById(id);
        if (course == null) {
            return new RestResponse<>(1, "课程不存在", null);
        }
        Map<String, Object> result = new LinkedHashMap<>();

        // 如果前端传了已生成的 slides 且请求下载 → 生成真实PPTX
        boolean download = request != null && Boolean.TRUE.equals(request.get("download"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> existingSlides = request != null
                ? (List<Map<String, Object>>) request.get("slides") : null;

        if (download && existingSlides != null && !existingSlides.isEmpty()) {
            try {
                String pptxPath = generatePptxFile(course, existingSlides);
                result.put("pptxUrl", pptxPath);
                result.put("slides", existingSlides);
                result.put("slideCount", existingSlides.size());
                return new RestResponse<>(1, "PPTX已生成", result);
            } catch (Exception e) {
                log.warn("PPTX generation failed: {}", e.getMessage());
                result.put("slides", existingSlides);
                result.put("slideCount", existingSlides.size());
                result.put("message", "PPTX生成失败，可使用在线预览");
                return new RestResponse<>(1, "已返回课件数据", result);
            }
        }

        // 从课程数据构建PPT slides
        List<Map<String, Object>> slides = buildSlidesFromCourse(course);
        result.put("slides", slides);
        result.put("slideCount", slides.size());
        result.put("generated", true);
        return new RestResponse<>(1, "已生成" + slides.size() + "页课件", result);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildSlidesFromCourse(Map<String, Object> course) {
        List<Map<String, Object>> slides = new ArrayList<>();
        String name = (String) course.getOrDefault("name", "课程");
        String school = (String) course.getOrDefault("school", "");
        String instructor = (String) course.getOrDefault("instructor", "");
        String textbook = (String) course.getOrDefault("textbook", "");
        Object periodObj = course.get("period");
        String period = periodObj != null ? periodObj.toString() + "学时" : "";

        // 封面
        Map<String, Object> cover = new LinkedHashMap<>();
        cover.put("title", name);
        cover.put("type", "cover");
        List<String> coverPoints = new ArrayList<>();
        if (!school.isEmpty()) coverPoints.add(school);
        if (!instructor.isEmpty()) coverPoints.add("主讲：" + instructor);
        if (!textbook.isEmpty()) coverPoints.add("教材：" + textbook);
        if (!period.isEmpty()) coverPoints.add("学时：" + period);
        cover.put("points", coverPoints);
        slides.add(cover);

        // 课程简介
        String desc = (String) course.getOrDefault("description", "");
        if (desc != null && !desc.isEmpty()) {
            Map<String, Object> intro = new LinkedHashMap<>();
            intro.put("title", "课程简介");
            intro.put("type", "intro");
            intro.put("points", Collections.singletonList(desc));
            slides.add(intro);
        }

        // 每章
        Object chaptersObj = course.get("chapters");
        Object chTitlesObj = course.get("chapterTitles");
        List<String> chTitles = new ArrayList<>();
        if (chTitlesObj instanceof List) {
            for (Object t : (List<?>) chTitlesObj) {
                if (t != null) chTitles.add(t.toString());
            }
        }
        Map<String, Object> chapters = null;
        if (chaptersObj instanceof Map) {
            chapters = (Map<String, Object>) chaptersObj;
        }

        for (int ci = 0; ci < chTitles.size(); ci++) {
            String chTitle = chTitles.get(ci);
            Map<String, Object> slide = new LinkedHashMap<>();
            slide.put("title", "第" + (ci + 1) + "章 " + chTitle);
            slide.put("type", "chapter");

            List<String> points = new ArrayList<>();
            if (chapters != null && chapters.get(chTitle) instanceof Map) {
                Map<String, Object> chData = (Map<String, Object>) chapters.get(chTitle);
                Object ptsObj = chData.get("points");
                if (ptsObj instanceof List) {
                    for (Object p : (List<?>) ptsObj) {
                        if (p != null) points.add(p.toString());
                    }
                }
            }
            if (points.isEmpty()) {
                // 章节无知识点数据时优先用 LLM 扩充，而非直接填占位话术
                points = enrichChapterPoints(name, chTitle, points);
            }
            if (points.isEmpty()) {
                points.add("本章核心概念与理论知识");
                points.add("重点公式推导与证明方法");
                points.add("典型例题详解与应用");
                points.add("常见误区辨析与思考");
            }
            slide.put("points", points);
            slides.add(slide);

            // 如果该章有 lessons
            if (chapters != null && chapters.get(chTitle) instanceof Map) {
                Map<String, Object> chData = (Map<String, Object>) chapters.get(chTitle);
                Object lessonsObj = chData.get("lessons");
                if (lessonsObj instanceof List) {
                    for (Object lObj : (List<?>) lessonsObj) {
                        if (lObj instanceof Map) {
                            Map<String, Object> lesson = (Map<String, Object>) lObj;
                            if ("video".equals(lesson.get("type"))) {
                                Map<String, Object> lSlide = new LinkedHashMap<>();
                                lSlide.put("title", lesson.getOrDefault("name", chTitle + " · 教学视频"));
                                lSlide.put("type", "lesson");
                                String duration = lesson.containsKey("desc") ? lesson.get("desc").toString() : "约20分钟";
                                List<String> lPoints = new ArrayList<>();
                                lPoints.add("时长：" + duration);
                                lPoints.add("类型：第三方平台教学视频（B站）");
                                Object url = lesson.get("url");
                                if (url != null && !url.toString().isEmpty()) {
                                    lPoints.add("在线观看：" + url);
                                    lSlide.put("notes", "教学视频链接：" + url);
                                } else {
                                    lPoints.add("包含知识点讲解与例题演示");
                                    lPoints.add("建议配合教材对应章节学习");
                                }
                                lSlide.put("points", lPoints);
                                slides.add(lSlide);
                            }
                        }
                    }
                }
            }
        }

        // 总结
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("title", "课程总结");
        summary.put("type", "summary");
        summary.put("points", Arrays.asList(
                "共 " + chTitles.size() + " 个章节",
                "涵盖所有核心知识点",
                "配合教学视频加深理解",
                "完成课后练习巩固所学"
        ));
        slides.add(summary);

        return slides;
    }

    private String generatePptxFile(Map<String, Object> course, List<Map<String, Object>> slides) throws Exception {
        // 使用 PptGenerationService 生成真实PPTX文件
        // 如果pptxService不可用，创建一个简单JSON文件作为回退
        try {
            com.mindskip.xzs.service.PptGenerationService pptxService =
                    org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext()
                            .getBean(com.mindskip.xzs.service.PptGenerationService.class);
            if (pptxService != null) {
                String name = (String) course.getOrDefault("name", "course");
                return pptxService.generateFromSlides(name, slides);
            }
        } catch (Exception e) {
            log.warn("PptGenerationService not available: {}", e.getMessage());
        }
        // 回退：保存为JSON文件
        String name = (String) course.getOrDefault("name", "course");
        String safeName = name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
        String fileName = safeName + "_课件.json";
        File outputDir = new File("downloads/ppts");
        if (!outputDir.exists()) outputDir.mkdirs();
        File jsonFile = new File(outputDir, fileName);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("courseName", name);
        wrapper.put("slides", slides);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, wrapper);
        return "/downloads/ppts/" + fileName;
    }

    private Map<String, Object> findById(String id) {
        for (Map<String, Object> c : courses) {
            if (id.equals(c.get("id"))) return c;
        }
        return null;
    }
}
