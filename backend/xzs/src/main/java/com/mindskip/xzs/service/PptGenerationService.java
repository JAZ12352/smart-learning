package com.mindskip.xzs.service;

import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 真实PPTX生成服务 — 校园清新风格，12页差异化布局
 * 配色与系统提示词完全一致：柔和蓝绿 + 奶白/浅草绿/淡蓝灰背景，柔和橙点缀
 */
@Service
public class PptGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PptGenerationService.class);
    private static final Path OUTPUT_DIR = Paths.get("downloads");
    private static final double SLIDE_W = 960;
    private static final double SLIDE_H = 540;
    // 统一边距
    private static final double MARGIN = 50;
    private static final double CONTENT_W = SLIDE_W - MARGIN * 2;

    private final ImageSearchService imageSearchService;

    public PptGenerationService(ImageSearchService imageSearchService) {
        this.imageSearchService = imageSearchService;
    }

    // ═══════════════════════════════════════════
    //  校园清新配色方案（与系统提示词一致）
    // ═══════════════════════════════════════════
    private static final Color C_PRIMARY   = new Color(0x4A, 0x90, 0xA4);  // 柔和蓝绿 — 标题、顶条
    private static final Color C_WOOD      = new Color(0x8B, 0x73, 0x55);  // 温暖木色 — 备选主色
    private static final Color C_ORANGE    = new Color(0xE8, 0x95, 0x6A);  // 柔和橙 — 编号、装饰
    private static final Color C_PURPLE    = new Color(0x9B, 0x8E, 0xC4);  // 淡紫 — 辅助点缀
    private static final Color C_CREAM     = new Color(0xFF, 0xFA, 0xF5);  // 奶白 — 主背景
    private static final Color C_GREEN_BG  = new Color(0xF2, 0xF7, 0xF2);  // 浅草绿 — 情境/回顾页
    private static final Color C_BLUE_BG   = new Color(0xF0, 0xF4, 0xF8);  // 淡蓝灰 — 新知/例题页
    private static final Color C_TEXT      = new Color(0x3A, 0x3A, 0x3A);  // 深灰 — 正文
    private static final Color C_WHITE     = new Color(0xFF, 0xFF, 0xFF);
    private static final Color C_LIGHT_LINE= new Color(0xE8, 0xE4, 0xDF);  // 分隔线
    private static final Color C_CARD_BG   = new Color(0xFF, 0xFF, 0xFF);  // 卡片白
    private static final Color C_PRIMARY_30= new Color(0x4A, 0x90, 0xA4, 0x4D); // 半透明主色

    // 背景色轮换（新知讲解3页用不同背景）
    private static final Color[] BG_ROTATION = { C_CREAM, C_BLUE_BG, C_GREEN_BG };

    static {
        try { Files.createDirectories(OUTPUT_DIR); } catch (IOException e) {
            throw new RuntimeException("Cannot create PPT output directory", e);
        }
    }

    /**
     * 根据结构化幻灯片数据生成PPTX（与前端展示数据完全一致）
     * 这是推荐方法 — 保证页面展示和下载文件内容统一
     */
    /**
     * 从课程自动生成的slides列表生成PPTX文件（简化接口）
     */
    public String generateFromSlides(String courseName, List<Map<String, Object>> slidesList) {
        try {
            Path path = generatePptx(slidesList, courseName);
            return "/api/admin/resource/download/" + path.getFileName().toString();
        } catch (Exception e) {
            log.warn("generateFromSlides failed: {}", e.getMessage());
            throw new RuntimeException("PPTX generation failed: " + e.getMessage());
        }
    }

    public Path generatePptx(List<Map<String, Object>> slidesList, String topic) throws IOException {
        XMLSlideShow ppt = new XMLSlideShow();
        ppt.setPageSize(new java.awt.Dimension((int) SLIDE_W, (int) SLIDE_H));

        // 转换结构化 Map → SlideData
        List<SlideData> slides = new ArrayList<>();
        for (Map<String, Object> slideMap : slidesList) {
            SlideData data = new SlideData();
            data.title = text(slideMap.get("title"));
            data.type = text(slideMap.get("type"));
            @SuppressWarnings("unchecked")
            List<String> pts = (List<String>) slideMap.get("points");
            if (pts != null) data.points.addAll(pts);
            @SuppressWarnings("unchecked")
            List<String> kws = (List<String>) slideMap.get("imageKeywords");
            if (kws != null && !kws.isEmpty()) {
                data.imageKeywords = String.join(", ", kws);
            }
            data.notes = text(slideMap.get("notes"));
            slides.add(data);
        }

        if (slides.isEmpty()) {
            slides.add(new SlideData("封面", topic, Collections.singletonList(topic)));
        }

        // 为每页下载配图
        for (SlideData data : slides) {
            if (data.imageKeywords != null && !data.imageKeywords.isEmpty()) {
                data.downloadedImages = imageSearchService.downloadSlideImages(data.imageKeywords, 400, 300);
            }
        }

        // 构建每页
        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = ppt.createSlide();
            SlideData data = slides.get(i);
            String type = (data.type != null ? data.type.trim() : "");

            if (i == 0 || type.contains("封面")) {
                createCampusCover(slide, data);
            } else if (type.contains("学习目标")) {
                createLearningGoal(slide, data);
            } else if (type.contains("情境导入")) {
                createScenario(slide, data);
            } else if (type.contains("知识回顾")) {
                createKnowledgeReview(slide, data);
            } else if (type.contains("重难点")) {
                createKeyPointBreakthrough(slide, data);
            } else if (type.contains("例题") || type.contains("案例")) {
                createExampleSlide(slide, data);
            } else if (type.contains("练习") || type.contains("思考题")) {
                createExerciseSlide(slide, data);
            } else if (type.contains("小结")) {
                createSummarySlide(slide, data);
            } else if (type.contains("结束")) {
                createCampusEnd(slide, data);
            } else if (type.contains("目录")) {
                createCampusToc(slide, data);
            } else {
                int knowledgeIndex = countKnowledgePagesBefore(slides, i);
                createNewKnowledge(slide, data, knowledgeIndex);
            }
        }

        String fileName = sanitizeFileName(topic) + "_" + System.currentTimeMillis() + ".pptx";
        Path outputPath = OUTPUT_DIR.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            ppt.write(fos);
        }
        ppt.close();
        log.info("PPTX generated: {} ({} slides)", outputPath, slides.size());
        return outputPath;
    }

    /**
     * 从脚本原文生成PPTX（向后兼容，内部会先解析）
     */
    public Path generatePptx(String scriptText, String topic) throws IOException {
        // 使用内置解析器 → 再委托给结构化方法
        List<SlideData> rawSlides = parseScript(scriptText);
        List<Map<String, Object>> structuredSlides = new ArrayList<>();
        for (SlideData s : rawSlides) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", s.title != null ? s.title : "");
            m.put("type", s.type != null ? s.type : "");
            m.put("points", s.points);
            if (s.imageKeywords != null) {
                m.put("imageKeywords", Arrays.asList(s.imageKeywords.split(",\\s*")));
            }
            m.put("notes", "");
            structuredSlides.add(m);
        }
        return generatePptx(structuredSlides, topic);
    }

    // ═══════════════════════════════════════════
    //  12种校园风格页面构建方法
    // ═══════════════════════════════════════════

    /** 第1页·封面 — 主色背景 + 居中大字 */
    private void createCampusCover(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_PRIMARY);
        // 装饰圆
        addOval(slide, SLIDE_W - 200, -80, 320, 240, new Color(0xFF, 0xFF, 0xFF, 25));
        addOval(slide, -80, SLIDE_H - 160, 240, 200, new Color(0xFF, 0xFF, 0xFF, 12));

        String title = data.title != null && !data.title.isEmpty()
                ? data.title : (data.points.isEmpty() ? "课件" : data.points.get(0));
        addTextBox(slide, MARGIN, 160, CONTENT_W, 90, title, 40.0, C_WHITE, true);

        String sub = data.points.size() > 1 ? data.points.get(1) : "校园课堂演示文稿";
        addTextBox(slide, MARGIN, 260, CONTENT_W, 40, sub, 18.0, C_ORANGE, false);

        addTextBox(slide, MARGIN, SLIDE_H - 50, CONTENT_W, 24,
                "校园课堂演示文稿", 12.0, new Color(0xFF, 0xFF, 0xFF, 150), false);
    }

    /** 第2页·学习目标 — 奶白背景 + 3列卡片 */
    private void createLearningGoal(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_CREAM);
        addRect(slide, 0, 0, SLIDE_W, 4, C_PRIMARY);

        addTextBox(slide, MARGIN, 30, CONTENT_W, 36, "学完这节课，你将能……", 24.0, C_PRIMARY, true);

        double cardW = 260, gap = 30;
        double totalW = cardW * 3 + gap * 2;
        double startX = (SLIDE_W - totalW) / 2;

        for (int i = 0; i < Math.min(3, data.points.size()); i++) {
            double cx = startX + i * (cardW + gap), cy = 90;
            Color accent = i == 0 ? C_ORANGE : (i == 1 ? C_PRIMARY : C_PURPLE);

            addRect(slide, cx, cy, cardW, 380, C_WHITE);
            addRect(slide, cx, cy, cardW, 4, accent);
            String num = String.format("0%d", i + 1);
            addTextBox(slide, cx + 20, cy + 20, 40, 32, num, 24.0, accent, true);
            addTextBox(slide, cx + 20, cy + 64, cardW - 40, 290, data.points.get(i), 16.0, C_TEXT, false);
        }

        addTextBox(slide, MARGIN, SLIDE_H - 32, 200, 18, "学习目标", 10.0, C_PRIMARY, false);
    }

    /** 第3页·情境导入 — 浅草绿背景 + 场景框 + 问题 */
    private void createScenario(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_GREEN_BG);
        addRect(slide, 0, 0, SLIDE_W, 4, C_PRIMARY);
        addTextBox(slide, MARGIN, 26, CONTENT_W, 36, "情境导入", 24.0, C_PRIMARY, true);

        byte[] imgBytes = getFirstImage(data);
        boolean hasImg = imgBytes != null;
        double imgW = hasImg ? 200 : 0;
        double boxW = CONTENT_W - (hasImg ? imgW + 24 : 0);

        // 白色场景框
        double boxY = 78, boxH = 280;
        addRect(slide, MARGIN, boxY, boxW, boxH, C_WHITE);
        addRect(slide, MARGIN, boxY, 5, boxH, C_ORANGE);

        String story = data.points.size() > 0 ? String.join("\n",
                data.points.subList(0, Math.min(2, data.points.size()))) : "请思考以下场景……";
        addTextBox(slide, MARGIN + 24, boxY + 16, boxW - 48, boxH - 32, story, 16.0, C_TEXT, false);

        if (hasImg) {
            double imgX = MARGIN + boxW + 24, imgY = boxY;
            addRect(slide, imgX - 3, imgY - 3, imgW + 6, boxH + 6, C_LIGHT_LINE);
            if (!addImageToSlide(slide, imgBytes, imgX, imgY, imgW, boxH)) {
                addRect(slide, imgX, imgY, imgW, boxH, C_BLUE_BG);
                addTextBox(slide, imgX + 10, imgY + boxH / 2 - 14, imgW - 20, 28, "🖼", 16.0, C_PRIMARY, true);
            }
        }

        // 引导问题
        addRect(slide, MARGIN, 378, 90, 28, C_ORANGE);
        addTextBox(slide, MARGIN + 6, 380, 78, 24, "想一想", 14.0, C_WHITE, true);

        String q1 = data.points.size() > 2 ? data.points.get(2) : "这和已学知识有什么联系？";
        addTextBox(slide, MARGIN, 416, CONTENT_W, 28, "❓ " + q1, 15.0, C_TEXT, false);

        String q2 = data.points.size() > 3 ? data.points.get(3) : "你能用自己的话解释吗？";
        addTextBox(slide, MARGIN, 450, CONTENT_W, 28, "❓ " + q2, 15.0, C_TEXT, false);

        addTextBox(slide, MARGIN, SLIDE_H - 30, 200, 18, "情境导入", 10.0, C_PRIMARY, false);
    }

    /** 第4页·知识回顾 — 奶白背景 + 回顾卡片 */
    private void createKnowledgeReview(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_CREAM);
        addRect(slide, 0, 0, SLIDE_W, 4, C_PRIMARY);
        addTextBox(slide, MARGIN, 26, CONTENT_W, 36, "知识回顾", 24.0, C_PRIMARY, true);

        int count = Math.min(data.points.size(), 3);
        double cardH = count <= 2 ? 140 : 110;
        double startY = 80;

        for (int i = 0; i < count; i++) {
            double cy = startY + i * (cardH + 20);
            addRect(slide, MARGIN, cy, CONTENT_W, cardH, C_WHITE);
            addRect(slide, MARGIN, cy, 5, cardH, C_PRIMARY);

            String num = String.format("%02d", i + 1);
            addTextBox(slide, MARGIN + 24, cy + 18, 36, 30, num, 20.0, C_PRIMARY, true);
            addTextBox(slide, MARGIN + 80, cy + 16, CONTENT_W - 100, cardH - 32,
                    data.points.get(i), 16.0, C_TEXT, false);
        }
        addTextBox(slide, MARGIN, SLIDE_H - 30, 200, 18, "知识回顾", 10.0, C_PRIMARY, false);
    }

    /** 第5-7页·新知讲解 — 背景轮换，干净列表+配图 */
    private void createNewKnowledge(XSLFSlide slide, SlideData data, int knowledgeIndex) {
        Color bg = BG_ROTATION[knowledgeIndex % 3];
        setSlideBackground(slide, bg);
        addRect(slide, 0, 0, SLIDE_W, 4, C_PRIMARY);

        String label = knowledgeIndex == 0 ? "基础概念" : (knowledgeIndex == 1 ? "核心内容" : "进阶理解");
        addTextBox(slide, MARGIN, 24, CONTENT_W, 34, data.title != null ? data.title : label, 24.0, C_PRIMARY, true);

        int pointCount = data.points.size();
        if (pointCount == 0) return;

        byte[] imageBytes = getFirstImage(data);
        boolean hasImage = imageBytes != null;
        // 有图时左侧留空间给内容，右侧放图
        double contentRight = hasImage ? SLIDE_W - MARGIN - 220 : SLIDE_W - MARGIN;

        double y = 76, itemGap = 14;
        for (int i = 0; i < pointCount; i++) {
            double itemH = Math.min(60, (SLIDE_H - 120) / pointCount);
            Color dotColor = i % 3 == 0 ? C_ORANGE : (i % 3 == 1 ? C_PRIMARY : C_PURPLE);

            // 小圆点
            addOval(slide, MARGIN + 6, y + 8, 14, 14, dotColor);
            // 编号
            addTextBox(slide, MARGIN + 6, y + 8, 14, 14, String.valueOf(i + 1), 10.0, C_WHITE, true);
            // 文字
            addTextBox(slide, MARGIN + 32, y + 4, contentRight - MARGIN - 40, itemH,
                    data.points.get(i), 15.0, C_TEXT, false);
            // 分隔线
            if (i < pointCount - 1) {
                addRect(slide, MARGIN + 32, y + itemH, contentRight - MARGIN - 40, 0.5, C_LIGHT_LINE);
            }
            y += itemH + itemGap;
        }

        // 右侧配图
        if (hasImage) {
            double imgX = SLIDE_W - MARGIN - 200;
            double imgY = 76, imgH = SLIDE_H - 130;
            addRect(slide, imgX - 3, imgY - 3, 206, imgH + 6, C_LIGHT_LINE);
            if (!addImageToSlide(slide, imageBytes, imgX, imgY, 200, imgH)) {
                addRect(slide, imgX, imgY, 200, imgH, C_BLUE_BG);
                addTextBox(slide, imgX + 10, imgY + imgH / 2 - 14, 180, 28, "🖼 示意图", 14.0, C_PRIMARY, true);
            }
        }

        addTextBox(slide, MARGIN, SLIDE_H - 30, 200, 18, label, 10.0, C_PRIMARY, false);
    }

    /** 第8页·重难点突破 — 步骤拆解 + 误区提醒 */
    private void createKeyPointBreakthrough(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_CREAM);
        addRect(slide, 0, 0, SLIDE_W, 4, C_ORANGE);
        addTextBox(slide, MARGIN, 24, 500, 34, data.title != null ? data.title : "重难点突破", 24.0, C_ORANGE, true);

        int stepCount = Math.min(data.points.size(), 5);
        double stepY = 76, stepH = (SLIDE_H - 120) / Math.max(stepCount, 3);

        for (int i = 0; i < stepCount; i++) {
            double sy = stepY + i * stepH;
            if (i < stepCount - 1) {
                addRect(slide, MARGIN + 17, sy + 26, 2, stepH - 18, C_PRIMARY);
            }
            addOval(slide, MARGIN + 8, sy + 6, 20, 20, C_ORANGE);
            addTextBox(slide, MARGIN + 8, sy + 6, 20, 20, String.valueOf(i + 1), 11.0, C_WHITE, true);
            addTextBox(slide, MARGIN + 44, sy + 2, CONTENT_W - 260, stepH - 8,
                    data.points.get(i), 15.0, C_TEXT, false);
        }

        // 误区提醒框
        double tipW = 220, tipX = SLIDE_W - MARGIN - tipW;
        addRect(slide, tipX, 76, tipW, SLIDE_H - 120, new Color(0xFF, 0xF0, 0xE6));
        addRect(slide, tipX, 76, 4, SLIDE_H - 120, C_ORANGE);
        addTextBox(slide, tipX + 16, 90, tipW - 32, 24, "⚠ 常见误区", 14.0, C_ORANGE, true);
        addTextBox(slide, tipX + 16, 122, tipW - 32, SLIDE_H - 190,
                data.points.size() > stepCount ? data.points.get(stepCount) : "注意区分相似概念", 12.0, C_TEXT, false);

        addTextBox(slide, MARGIN, SLIDE_H - 30, 200, 18, "重难点突破", 10.0, C_ORANGE, false);
    }

    /** 第9页·例题精讲 — 题干→思路→解答 清晰分区 */
    private void createExampleSlide(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_BLUE_BG);
        addRect(slide, 0, 0, SLIDE_W, 4, C_PRIMARY);
        addTextBox(slide, MARGIN, 24, 500, 34, data.title != null ? data.title : "例题精讲", 24.0, C_PRIMARY, true);

        // 题干
        double qY = 72, qH = 130;
        addRect(slide, MARGIN, qY, CONTENT_W, qH, C_WHITE);
        addRect(slide, MARGIN, qY, 5, qH, C_PRIMARY);
        addTextBox(slide, MARGIN + 24, qY + 10, 100, 26, "📝 题干", 15.0, C_PRIMARY, true);
        String question = data.points.size() > 0 ? data.points.get(0) : "题目内容";
        addTextBox(slide, MARGIN + 24, qY + 42, CONTENT_W - 48, qH - 52, question, 15.0, C_TEXT, false);

        // 思路 + 解答 双栏
        double colW = (CONTENT_W - 30) / 2, colY = 218, colH = 260;
        // 思路
        addRect(slide, MARGIN, colY, colW, colH, C_WHITE);
        addRect(slide, MARGIN, colY, 4, colH, C_PURPLE);
        addTextBox(slide, MARGIN + 20, colY + 10, colW - 40, 24, "💡 思路点拨", 14.0, C_PURPLE, true);
        addTextBox(slide, MARGIN + 20, colY + 42, colW - 40, colH - 52,
                data.points.size() > 1 ? data.points.get(1) : "分析关键信息", 13.0, C_TEXT, false);
        // 解答
        double col2X = MARGIN + colW + 30;
        addRect(slide, col2X, colY, colW, colH, C_WHITE);
        addRect(slide, col2X, colY, 4, colH, C_ORANGE);
        addTextBox(slide, col2X + 20, colY + 10, colW - 40, 24, "✏ 分步解答", 14.0, C_ORANGE, true);
        addTextBox(slide, col2X + 20, colY + 42, colW - 40, colH - 52,
                data.points.size() > 2 ? data.points.get(2) : "解题步骤", 13.0, C_TEXT, false);

        addTextBox(slide, MARGIN, SLIDE_H - 28, 200, 18, "例题精讲", 10.0, C_PRIMARY, false);
    }

    /** 第10页·思考题+课堂练习 — 2×2卡片 */
    private void createExerciseSlide(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_GREEN_BG);
        addRect(slide, 0, 0, SLIDE_W, 4, C_PRIMARY);
        addTextBox(slide, MARGIN, 24, 500, 34, data.title != null ? data.title : "思考与练习", 24.0, C_PRIMARY, true);

        int count = Math.min(data.points.size(), 4);
        double cardW = (CONTENT_W - 30) / 2, cardH = 190;
        String[] labels = {"💬 思考题", "💬 思考题", "✍ 练习题", "✍ 练习题"};

        for (int i = 0; i < count; i++) {
            int row = i / 2, col = i % 2;
            double cx = MARGIN + col * (cardW + 30);
            double cy = 76 + row * (cardH + 20);

            addRect(slide, cx, cy, cardW, cardH, C_WHITE);
            Color tagColor = i < 2 ? C_PURPLE : C_ORANGE;
            addRect(slide, cx, cy, cardW, 32, tagColor);
            addTextBox(slide, cx + 14, cy + 5, cardW - 28, 22, labels[i] + " " + (i + 1), 13.0, C_WHITE, true);
            addTextBox(slide, cx + 16, cy + 42, cardW - 32, cardH - 50, data.points.get(i), 14.0, C_TEXT, false);
        }

        addTextBox(slide, MARGIN, SLIDE_H - 28, 200, 18, "思考与练习", 10.0, C_PRIMARY, false);
    }

    /** 第11页·课堂小结 — 要点横向卡片 */
    private void createSummarySlide(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_CREAM);
        addRect(slide, 0, 0, SLIDE_W, 4, C_PRIMARY);
        addTextBox(slide, MARGIN, 24, 500, 34, "课堂小结", 24.0, C_PRIMARY, true);
        addTextBox(slide, MARGIN, 60, 400, 22, "一起来回顾今天学到的知识吧 ✨", 14.0, C_ORANGE, false);

        int count = Math.min(data.points.size(), 6);
        if (count == 0) return;

        double cardW = (CONTENT_W - 60) / count - 12;
        double cardY = 100, cardH = SLIDE_H - 140;

        for (int i = 0; i < count; i++) {
            double cx = MARGIN + i * (cardW + 12);
            Color barColor = i % 3 == 0 ? C_ORANGE : (i % 3 == 1 ? C_PRIMARY : C_PURPLE);

            addRect(slide, cx, cardY, cardW, cardH, C_WHITE);
            addRect(slide, cx, cardY, cardW, 4, barColor);
            String num = String.format("0%d", i + 1);
            addTextBox(slide, cx + 10, cardY + 14, cardW - 20, 28, num, 20.0, barColor, true);
            addTextBox(slide, cx + 10, cardY + 48, cardW - 20, cardH - 60, data.points.get(i), 13.0, C_TEXT, false);

            if (i < count - 1) {
                addTextBox(slide, cx + cardW - 2, cardY + cardH / 2 - 12, 14, 24, "→", 16.0, C_PRIMARY, false);
            }
        }

        addTextBox(slide, MARGIN, SLIDE_H - 28, 200, 18, "课堂小结", 10.0, C_PRIMARY, false);
    }

    /** 第12页·结束页 — 温暖鼓励 + 主色背景 */
    private void createCampusEnd(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_PRIMARY);
        addOval(slide, SLIDE_W - 200, -80, 300, 260, new Color(0xFF, 0xFF, 0xFF, 15));
        addOval(slide, -80, SLIDE_H - 160, 240, 200, new Color(0xFF, 0xFF, 0xFF, 10));

        String msg = !data.points.isEmpty() ? data.points.get(0) : "今天的课程就到这里啦～";
        String next = data.points.size() > 1 ? data.points.get(1) : "下节课见！";

        addTextBox(slide, MARGIN, 170, CONTENT_W, 60, msg, 32.0, C_WHITE, true);
        addRect(slide, MARGIN, 250, 60, 3, C_ORANGE);
        addTextBox(slide, MARGIN, 270, CONTENT_W, 40, next, 20.0, C_ORANGE, false);
        addTextBox(slide, MARGIN, SLIDE_H - 50, CONTENT_W, 24, "感谢聆听 · 期待下次再见", 12.0,
                new Color(0xFF, 0xFF, 0xFF, 130), false);
    }

    /** 目录页（可选） */
    private void createCampusToc(XSLFSlide slide, SlideData data) {
        setSlideBackground(slide, C_CREAM);
        addRect(slide, 0, 0, 6, SLIDE_H, C_PRIMARY);
        addTextBox(slide, MARGIN, 32, 300, 44, "目  录", 32.0, C_PRIMARY, true);

        double y = 110;
        for (int i = 0; i < data.points.size() && i < 8; i++) {
            addTextBox(slide, MARGIN + 30, y, 40, 28, String.format("%02d", i + 1), 22.0, C_PRIMARY, true);
            addTextBox(slide, MARGIN + 80, y + 2, CONTENT_W - 100, 26, data.points.get(i), 16.0, C_TEXT, false);
            if (i < data.points.size() - 1) {
                addRect(slide, MARGIN + 80, y + 34, CONTENT_W - 100, 0.5, C_LIGHT_LINE);
            }
            y += 48;
        }
    }

    // ═══════════════════════════════════════════
    //  POI 辅助绘图方法
    // ═══════════════════════════════════════════

    /** 将图片字节嵌入到幻灯片指定位置，返回 true 表示成功 */
    private boolean addImageToSlide(XSLFSlide slide, byte[] imageBytes, double x, double y, double w, double h) {
        if (imageBytes == null || imageBytes.length < 512) return false;
        try {
            XSLFPictureData picData = slide.getSlideShow().addPicture(
                imageBytes, org.apache.poi.sl.usermodel.PictureData.PictureType.PNG);
            XSLFPictureShape pic = slide.createPicture(picData);
            pic.setAnchor(new Rectangle((int) x, (int) y, (int) w, (int) h));
            return true;
        } catch (Exception e) {
            log.debug("Failed to embed image: {}", e.getMessage());
            return false;
        }
    }

    /** 获取 slide 的第一张图片关键词的图片字节 */
    private byte[] getFirstImage(SlideData data) {
        if (data.downloadedImages == null || data.downloadedImages.isEmpty()) return null;
        return data.downloadedImages.values().iterator().next();
    }

    private void setSlideBackground(XSLFSlide slide, Color color) {
        XSLFBackground bg = slide.getBackground();
        bg.setFillColor(color);
    }

    private XSLFTextBox addTextBox(XSLFSlide slide, double x, double y, double w, double h,
                                    String text, double fontSize, Color color, boolean bold) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle((int) x, (int) y, (int) w, (int) h));
        XSLFTextParagraph p = box.addNewTextParagraph();
        p.setTextAlign(TextParagraph.TextAlign.LEFT);
        XSLFTextRun run = p.addNewTextRun();
        run.setText(sanitizeText(text));
        run.setFontSize(fontSize);
        run.setFontColor(color);
        run.setFontFamily("Microsoft YaHei");
        run.setBold(bold);
        return box;
    }

    private void addRect(XSLFSlide slide, double x, double y, double w, double h, Color color) {
        XSLFAutoShape rect = slide.createAutoShape();
        rect.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        rect.setAnchor(new Rectangle((int) x, (int) y, (int) w, (int) h));
        rect.setFillColor(color);
    }

    private void addOval(XSLFSlide slide, double x, double y, double w, double h, Color color) {
        XSLFAutoShape oval = slide.createAutoShape();
        oval.setShapeType(org.apache.poi.sl.usermodel.ShapeType.ELLIPSE);
        oval.setAnchor(new Rectangle((int) x, (int) y, (int) w, (int) h));
        oval.setFillColor(color);
    }

    // ═══════════════════════════════════════════
    //  脚本解析（增强版 — 识别校园风12种页面类型）
    // ═══════════════════════════════════════════

    private List<SlideData> parseScript(String text) {
        List<SlideData> slides = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return slides;

        // 按"第X页"分割
        String[] parts = text.split("(?=第\\s*\\d+\\s*页)");
        for (String part : parts) {
            if (part.trim().isEmpty()) continue;

            SlideData data = new SlideData();

            // 1. 提取页面标题 (## 开头的行)
            Matcher titleMatcher = Pattern.compile("##\\s*(.+?)(?:\\n|$)").matcher(part);
            if (titleMatcher.find()) {
                data.title = titleMatcher.group(1).trim();
            }

            // 2. 提取类型（"类型：" 或 "- 类型："）
            Matcher typeMatcher = Pattern.compile("类型[：:]\\s*(.+?)(?:\\n|$)").matcher(part);
            if (typeMatcher.find()) {
                data.type = typeMatcher.group(1).trim();
            }

            // 2.5 提取配图关键词（"- 配图关键词：[...]" 或 "配图关键词：[...]"）
            Matcher imgKwMatcher = Pattern.compile("配图关键词[：:]\\s*\\[?\\s*(.+?)(?:\\]|\\n|$)").matcher(part);
            if (imgKwMatcher.find()) {
                String raw = imgKwMatcher.group(1).trim();
                // 只保留英文/数字/逗号/空格，去掉中文和括号
                data.imageKeywords = raw.replaceAll("[一-鿿（）\\(\\)\\[\\]\"']", "")
                                       .replaceAll("\\s+", " ").trim();
            }

            // 3. 提取要点 — 多种格式
            //   · 要点文字
            //   - 要点文字（排除元数据行）
            //   页面内容：之后的 · 或数字列表
            Matcher pointMatcher = Pattern.compile("(?:^|\\n)\\s*[·]\\s*(.+?)(?:\\n|$)").matcher(part);
            while (pointMatcher.find()) {
                String point = pointMatcher.group(1).trim();
                if (isContentPoint(point)) {
                    data.points.add(point);
                }
            }

            // 也提取 "- " 开头的要点（排除元数据标签）
            Matcher dashMatcher = Pattern.compile("(?:^|\\n)\\s*[-]\\s*(.+?)(?:\\n|$)").matcher(part);
            while (dashMatcher.find()) {
                String point = dashMatcher.group(1).trim();
                if (isContentPoint(point)) {
                    // 避免重复（可能已被 · 匹配）
                    if (!data.points.contains(point)) {
                        data.points.add(point);
                    }
                }
            }

            // 4. 如果没提取到标题，用页面类型作为标题
            if (data.title == null && data.type != null) {
                data.title = data.type;
            }

            // 5. 兜底：如果还是没有标题，取第一行非页面号文字
            if (data.title == null) {
                Matcher firstLine = Pattern.compile("^\\s*(.+?)(?:\\n|$)").matcher(part);
                if (firstLine.find()) {
                    String line = firstLine.group(1).trim();
                    if (!line.matches("第\\s*\\d+\\s*页.*") && line.length() < 60) {
                        data.title = line;
                    }
                }
            }

            if (data.title != null || !data.points.isEmpty()) {
                slides.add(data);
            }
        }
        return slides;
    }

    /** 判断是否为内容要点（排除元数据标签行） */
    private boolean isContentPoint(String text) {
        if (text.length() < 2) return false;
        String[] excludedPrefixes = {
            "类型", "配图关键词", "配图", "页面布局", "讲解备注", "【讲解备注】",
            "输出要求", "风格", "配色", "字体", "内容节奏", "语言表达", "辅助理解",
            "致命禁忌", "建议", "整体风格", "示意图", "PPT结构"
        };
        for (String prefix : excludedPrefixes) {
            if (text.startsWith(prefix)) return false;
        }
        return true;
    }

    /** 计算在当前页面之前有多少个"新知讲解"类型的页面 */
    private int countKnowledgePagesBefore(List<SlideData> slides, int currentIndex) {
        int count = 0;
        for (int i = 0; i < currentIndex; i++) {
            String t = slides.get(i).type;
            if (t != null && t.contains("新知讲解")) {
                count++;
            }
        }
        return count;
    }

    // ═══════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════

    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "")
                   .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                   .trim();
    }

    private String truncateText(String text, int maxLen) {
        text = sanitizeText(text);
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }

    private String sanitizeFileName(String name) {
        // 先转为拼音/字母数字 + 下划线，避免中文文件名导致代理传输异常
        String clean = name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_\\-]", "_").replaceAll("\\s+", "_");
        // 如果包含中文，使用时间戳命名，仅保留前缀的第一个字母数字段
        if (clean.matches(".*[\\u4e00-\\u9fa5].*")) {
            // 提取第一个字母数字块作为前缀
            String prefix = clean.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
            if (prefix.length() > 30) prefix = prefix.substring(0, 30);
            if (prefix.isEmpty()) prefix = "course";
            return prefix;
        }
        return clean.substring(0, Math.min(clean.length(), 40));
    }

    // ═══════════════════════════════════════════
    //  数据结构
    // ═══════════════════════════════════════════

    private static class SlideData {
        String type;
        String title;
        List<String> points = new ArrayList<>();
        String imageKeywords;                        // 配图关键词（英文，逗号分隔）
        String notes;                                // 讲解备注
        Map<String, byte[]> downloadedImages = new LinkedHashMap<>();  // 关键词→图片字节

        SlideData() {}
        SlideData(String type, String title, List<String> points) {
            this.type = type;
            this.title = title;
            this.points = points;
        }
    }

    private static String text(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
