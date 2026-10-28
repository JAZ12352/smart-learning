package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.GeneratedResource;
import com.mindskip.xzs.service.DeepSeekService;
import com.mindskip.xzs.service.PptGenerationService;
import com.mindskip.xzs.service.ResourceGenerationService;
import com.mindskip.xzs.service.AgnesVideoService;
import com.mindskip.xzs.service.MultiAgentService;
import com.mindskip.xzs.service.PythonAgentClient;
import com.mindskip.xzs.service.MultiAgentService.AgentType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/admin/resource")
public class ResourceGenerationController {

    private static final Logger log = LoggerFactory.getLogger(ResourceGenerationController.class);
    private final DeepSeekService deepSeekService;
    private final ResourceGenerationService resourceGenerationService;
    private final PptGenerationService pptGenerationService;
    private final AgnesVideoService agnesVideoService;
    private final MultiAgentService multiAgentService;
    private final PythonAgentClient pythonAgentClient;

    public ResourceGenerationController(DeepSeekService deepSeekService,
                                        ResourceGenerationService resourceGenerationService,
                                        PptGenerationService pptGenerationService,
                                        AgnesVideoService agnesVideoService,
                                        MultiAgentService multiAgentService,
                                        PythonAgentClient pythonAgentClient) {
        this.deepSeekService = deepSeekService;
        this.resourceGenerationService = resourceGenerationService;
        this.pptGenerationService = pptGenerationService;
        this.agnesVideoService = agnesVideoService;
        this.multiAgentService = multiAgentService;
        this.pythonAgentClient = pythonAgentClient;
    }

    @PostMapping("/generate")
    public void generate(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String resourceType = text(requestData.get("resourceType"));
        String topic = text(requestData.get("topic"));
        int userId = getInt(requestData.get("userId"), 0);
        List<Map<String, String>> messages = buildMessages(buildSystemPrompt(resourceType), buildUserPrompt(resourceType, topic, requestData));

        // 思维导图：非流式获取完整响应，按双换行拆段取最后一段（跳过AI回声）
        if ("mind_map".equals(resourceType)) {
            String saved = null;
            try (PrintWriter writer = response.getWriter()) {
                String full = deepSeekService.complete(messages, DeepSeekService.MAX_TOKENS_LONG);
                if (full != null && !full.trim().isEmpty()) {
                    // 按连续换行拆段，AI回声通常是短段落，真实思维导图是最后的大段
                    String[] paragraphs = full.split("\\n\\s*\\n");
                    // 取最后一个以 - 或中心主题开头的段落
                    for (int i = paragraphs.length - 1; i >= 0; i--) {
                        String p = paragraphs[i].trim();
                        if (p.startsWith("-") || p.startsWith("中心主题") || p.startsWith("#")) {
                            writer.write(p);
                            writer.flush();
                            saved = p;
                            break;
                        }
                    }
                    // 兜底：输出整个响应
                    if (saved == null) {
                        writer.write(full);
                        writer.flush();
                        saved = full;
                    }
                }
            } catch (Exception e) { log.error("MindMap generation failed", e); }
            persistResource(userId, resourceType, topic, saved);
            return;
        }

        // 练习卷：非流式获取完整响应，净化后一次性输出。
        // 流式直播只能做「首个标记前丢弃」，中间/结尾的元描述（本题考查、过渡语、收尾语）会漏出到页面，
        // 故 exercise 改为整卷生成 → sanitizeExercise → 一次性写出，保证用户看到的与入库的同样干净。
        if ("exercise".equals(resourceType)) {
            String saved = null;
            try (PrintWriter writer = response.getWriter()) {
                String full = deepSeekService.complete(messages, DeepSeekService.MAX_TOKENS_LONG);
                saved = sanitizeExercise(StringUtils.defaultString(full));
                writer.write(saved);
                writer.flush();
            } catch (Exception e) { log.error("Exercise generation failed", e); }
            persistResource(userId, resourceType, topic, saved);
            return;
        }

        // 内置硬过滤：找到期望的第一个内容标记，在此之前的所有内容全部丢弃
        final String[] contentMarkers = getContentStartMarkers(resourceType);
        final StringBuilder buffer = new StringBuilder();
        final StringBuilder fullContent = new StringBuilder();
        final boolean[] contentStarted = {false};

        try (PrintWriter writer = response.getWriter()) {
            deepSeekService.stream(messages, chunk -> {
                if (contentStarted[0]) {
                    writer.write(chunk);
                    writer.flush();
                    fullContent.append(chunk);
                    return;
                }
                buffer.append(chunk);
                // 检查是否已出现内容标记
                for (String marker : contentMarkers) {
                    int idx = buffer.indexOf(marker);
                    if (idx >= 0) {
                        String afterMarker = buffer.substring(idx);
                        writer.write(afterMarker);
                        writer.flush();
                        fullContent.append(afterMarker);
                        contentStarted[0] = true;
                        return;
                    }
                }
            });
            if (!contentStarted[0] && buffer.length() > 0) {
                String finalContent = forceStripMeta(buffer.toString(), contentMarkers);
                writer.write(finalContent);
                writer.flush();
                fullContent.append(finalContent);
            }
        } catch (Exception e) { log.error("Generation failed", e); }
        String persisted = fullContent.toString();
        // 练习卷专用净化：剔除阅卷元数据与套话，修复答案/解析连写
        if ("exercise".equals(StringUtils.defaultString(resourceType))) {
            persisted = sanitizeExercise(persisted);
        }
        persistResource(userId, resourceType, topic, persisted);
    }

    /**
     * 练习卷净化 —— 提示词已明令禁止元描述，此处做最后一道兜底。
     *
     * 模型（尤其是推理型模型）仍可能漏出三类内容：
     * 1) 阅卷元数据：评分标准/赋分/要点X分/本题考查 —— 属教师内部信息，出现在学生练习卷里是硬伤；
     * 2) 卷首套话与结尾收尾语；
     * 3) 「答案：C解析：」连写 —— 可读性差，需拆成两行。
     */
    private String sanitizeExercise(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        String text = raw.replace("\r\n", "\n").replace("\r", "\n");

        // ① 「答案：X解析：」→ 拆行，保证答案与解析各占一行
        text = text.replaceAll("(?m)^(答案[:：][^\\n]*?)(解析[:：])", "$1\n$2");
        text = text.replaceAll("(答案[:：][^\\n]{0,40}?)(解析[:：])", "$1\n$2");
        // 题号与「答案」连写时同样拆行，如 "D.提高训练数据质量答案：C"
        text = text.replaceAll("(?m)([^\\n])(答案[:：])", "$1\n$2");

        // ② 逐行剔除元描述
        String[] metaPrefixes = {
            "评分标准", "赋分标准", "分值分配", "得分点", "评分细则", "评分说明",
            "本题考查", "本题主要", "考查目标", "考查内容", "命题意图", "出题思路", "设计说明",
            "难度标注", "知识点：", "考点：", "题目说明"
        };
        StringBuilder kept = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String t = line.trim();
            boolean drop = false;
            for (String p : metaPrefixes) {
                if (t.startsWith(p) || t.startsWith("**" + p) || t.startsWith("- " + p)) {
                    // 仅当该行是「元信息行」而非正常题干：元信息行通常较短且含分值/考查等词
                    if (t.length() < 80 && (t.contains("分") || t.contains("考查") || t.contains("考点")
                            || t.contains("说明") || t.contains("意图") || t.contains("思路"))) {
                        drop = true;
                        break;
                    }
                }
            }
            if (!drop) kept.append(line).append("\n");
        }
        text = kept.toString();

        // ③ 剥掉卷首套话：从第一个题型标记开始
        int firstMark = -1;
        String[] marks = {"【选择题】", "【判断题】", "【填空题】", "【简答题】", "【单选题】", "【多选题】"};
        for (String m : marks) {
            int i = text.indexOf(m);
            if (i >= 0 && (firstMark < 0 || i < firstMark)) firstMark = i;
        }
        if (firstMark > 0) text = text.substring(firstMark);

        // ④ 剥掉结尾收尾语
        text = text.replaceAll("(?s)\n[^\n]*?(希望[^\n]{0,30}|以上题目[^\n]{0,30}|祝[^\n]{0,20}学习[^\n]{0,20})\\s*$", "");

        return text.replaceAll("\n{3,}", "\n\n").trim();
    }

    /**
     * 将生成结果持久化到 t_generated_resource，形成「生成→入库→查看→删除」的完整闭环。
     * 对应赛题「全流程闭环系统」要求：多智能体协同生成的个性化资源可被保存、检索与复用。
     */
    private void persistResource(int userId, String resourceType, String topic, String content) {
        if (content == null || content.trim().isEmpty()) return;
        try {
            GeneratedResource record = new GeneratedResource();
            record.setUserId(userId);
            record.setResourceType(resourceType);
            record.setTitle(topic);
            record.setContent(content);
            record.setContentFormat("markdown");
            record.setSourceAgent(AgentType.fromKey(resourceType) != null
                ? AgentType.fromKey(resourceType).name : resourceType);
            record.setCreateTime(new Date());
            record.setDeleted(false);
            resourceGenerationService.insert(record);
            log.info("Resource persisted: id={}, type={}, topic={}, length={}",
                record.getId(), resourceType, topic, content.length());
        } catch (Exception e) {
            log.warn("Failed to persist generated resource: {}", e.getMessage());
        }
    }

    /**
     * 生成历史列表（按用户维度）
     */
    @GetMapping("/list")
    public RestResponse<List<GeneratedResource>> listResources(@RequestParam(required = false) Integer userId) {
        int uid = userId == null ? 0 : userId;
        return RestResponse.ok(resourceGenerationService.getResourcesByUserId(uid));
    }

    /**
     * 资源详情
     */
    @GetMapping("/{id}")
    public RestResponse<GeneratedResource> getResourceDetail(@PathVariable Integer id) {
        return RestResponse.ok(resourceGenerationService.selectById(id));
    }

    /**
     * 删除资源（软删除）
     */
    @DeleteMapping("/{id}")
    public RestResponse<Integer> deleteResource(@PathVariable Integer id) {
        GeneratedResource record = resourceGenerationService.selectById(id);
        if (record == null) {
            return RestResponse.fail(3, "资源不存在");
        }
        record.setDeleted(true);
        resourceGenerationService.updateByIdFilter(record);
        return RestResponse.ok(1);
    }

    /**
     * 每种内容类型的「内容开始标记」— 只有检测到这些标记才认为内容真正开始了。
     * 在此之前的一切输出（无论什么文字）都会被无条件丢弃。
     */
    private String[] getContentStartMarkers(String type) {
        switch (StringUtils.defaultString(type)) {
            case "mind_map":
                // 需要 BOTH 中心主题 AND 一个真实维度标记同时出现
                // 防止 AI 回声文本中的 "-中心主题" 被误判（回声后面跟的是说明文字而非维度标记）
                return new String[]{
                    "-中心主题：", "- 中心主题：",
                };
            case "video":
                return new String[]{"## 开场引入", "##开场引入", "## 核心概念", "##核心概念",
                    "## 原理讲解", "##原理讲解", "## 实例演示", "##实例演示",
                    "## 方法总结", "##方法总结", "## 课后思考", "##课后思考",
                    "开场引入", "核心概念", "## 引入", "##引入", "一、开场", "1. 开场"};
            case "exercise":
                // 练习/试卷 — 只匹配题型标题标记，不匹配裸词（防元描述中的「选择题」等被误触发）
                return new String[]{"【选择题】", "【判断题】", "【填空题】", "【简答题】",
                    "【单选题】", "【多选题】",
                    "一、选择题", "一、 选择题", "一、单选题", "一、 单选题",
                    "一、判断题", "一、 判断题",
                    "**选择题**", "**判断题**", "**填空题**", "**简答题**",
                    "### 选择题", "### 判断题", "### 填空题", "### 简答题",
                    "## 练习", "# 练习", "### 一、",
                    "二、", "三、", "四、",
                    "1.", "1、", "1．", "1）", "**1.**", "### 1."};
            case "extended_reading":
                return new String[]{"# ", "## ", "一、", "1."};
            case "ppt":
                return new String[]{"第1页", "第 1 页", "第1页·", "第一页"};
            case "code_example":
                // 代码 — 任何代码相关标记都接受
                return new String[]{"//", "/*", "```", "/**", "package ", "import ", "#include", "<!DOCTYPE",
                    "def ", "class ", "public class", "function ", "const ", "let ", "var ",
                    "import {", "from ", "require(", "print(", "console.", "System.out",
                    "if ", "for ", "while ", "# 代码", "## 代码", "```python", "```java",
                    "```javascript", "```cpp", "```c", "```html", "```css"};
            case "document":
                return new String[]{"# ", "## ", "一、", "1."};
            default:
                return new String[]{"# ", "## ", "- ", "一、"};
        }
    }

    /** 元描述裁剪 — 流结束后还没找到内容标记时作为最后手段，只剥离明确的AI应答前缀 */
    private String forceStripMeta(String raw, String[] markers) {
        if (raw == null || raw.isEmpty()) return "";
        String[] lines = raw.split("\n");
        // 跳过前几行明显的AI应答前缀（"好的，我来为你生成...", "以下是..."等）
        int startIdx = 0;
        for (int i = 0; i < Math.min(8, lines.length); i++) {
            String t = lines[i].trim();
            if (t.isEmpty()) { startIdx = i + 1; continue; }
            // 跳过AI应答前缀和提示词复读
            if (t.length() < 60 && (
                t.startsWith("好的") || t.equals("OK") || t.equals("好的，") ||
                t.startsWith("以下") || t.startsWith("让我") || t.startsWith("我将") ||
                t.startsWith("我来") || t.equals("请查收") || t.startsWith("收到") ||
                t.startsWith("明白") || t.startsWith("当然") || t.startsWith("没问题") ||
                t.contains("为您生成") || t.contains("为你生成") || t.contains("请稍等") ||
                t.contains("正在生成") || t.contains("下面是") || t.startsWith("这是") ||
                t.contains("总字数不少于") || t.contains("内容要充实") || t.contains("按照规范") ||
                t.contains("铁律") || t.contains("你的第一个字") || t.contains("超强约束") ||
                t.contains("一个一级维度") || t.contains("个维度用") || t.contains("空格缩进") ||
                t.contains("8个维度") || t.contains("八个维度") ||
                t.contains("视频脚本") || t.contains("镜头") || t.contains("分镜") ||
                t.equals("```") || t.equals("```markdown") || t.equals("```text") ||
                t.startsWith("注意：") || t.startsWith("注：") || t.startsWith("备注："))) {
                startIdx = i + 1;
                continue;
            }
            // 一旦遇到看起来像内容的东西（足够长或有结构标记），停止跳过
            if (t.length() >= 20 || t.startsWith("- ") || t.startsWith("#") || t.startsWith("**")) {
                break;
            }
        }
        if (startIdx >= lines.length) return raw;
        StringBuilder result = new StringBuilder();
        for (int j = startIdx; j < lines.length; j++) {
            result.append(lines[j]).append("\n");
        }
        return result.toString();
    }

    // ═══════════════════════════════════════════════════════════════
    //  多智能体编排 API — Supervisor 协调多个 Agent 生成资源包
    // ═══════════════════════════════════════════════════════════════

    /**
     * 多智能体批量生成 — 输入一个教学主题，自动激活全栈 Agent 并生成综合资源包。
     * 流程：优先 Python Supervisor 编排 → 回退 Java 直连 DeepSeek
     */
    @PostMapping("/multi-agent-generate")
    public void multiAgentGenerate(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String topic = text(requestData.get("topic"));
        String[] agentTypes = parseAgentTypes(requestData.get("agentTypes"));

        if (topic.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writePlain(response, "教学主题不能为空");
            return;
        }

        // 如果未指定 agent，默认激活所有 6 个
        if (agentTypes.length == 0) {
            agentTypes = new String[]{"document", "mind_map", "exercise", "extended_reading", "video", "code_example"};
        }

        PrintWriter writer = response.getWriter();
        writer.write("══════════════ 多智能体资源生成 ══════════════\n");
        writer.write("教学主题：" + topic + "\n");
        writer.write("激活智能体：" + String.join("、", agentTypes) + "\n");

        // ═══ 优先尝试 Python Supervisor 统一编排 ═══
        if (pythonAgentClient.isAvailable()) {
            writer.write("编排模式：Python Supervisor 多智能体协同\n\n");
            writer.flush();

            List<String> agentList = Arrays.asList(agentTypes);
            Map<String, Object> pythonResult = pythonAgentClient.multiGenerateResource(topic, agentList, requestData);

            if (pythonResult != null && !pythonResult.isEmpty()) {
                // 输出 Supervisor 汇总
                Object summary = pythonResult.get("summary");
                if (summary != null) {
                    writer.write("📋 Supervisor 汇总：\n" + summary + "\n\n");
                }

                // 输出各 Agent 结果
                @SuppressWarnings("unchecked")
                Map<String, Object> results = (Map<String, Object>) pythonResult.get("results");
                if (results != null) {
                    int completed = 0;
                    for (String agentType : agentTypes) {
                        writer.write("─────────────────────────────────────\n");
                        AgentType at = AgentType.fromKey(agentType);
                        String agentName = at != null ? at.name : agentType;
                        writer.write("🤖 " + agentName + " 开始工作...\n");
                        writer.write("─────────────────────────────────────\n");

                        Object content = results.get(agentType);
                        if (content != null && StringUtils.isNotBlank(String.valueOf(content))) {
                            writer.write(String.valueOf(content).trim() + "\n\n");
                            completed++;
                        } else {
                            writer.write("（" + agentName + " 未能生成有效内容）\n\n");
                        }
                        writer.flush();
                    }

                    Object successCount = pythonResult.get("success_count");
                    Object totalAgents = pythonResult.get("total_agents");
                    writer.write("═══════════════════════════════════════════\n");
                    writer.write("✅ 多智能体协作完成：成功 " + (successCount != null ? successCount : completed)
                        + "/" + (totalAgents != null ? totalAgents : agentTypes.length) + " 个\n");
                    writer.write("🔗 编排引擎：Python Supervisor (LangGraph)\n");
                    writer.write("═══════════════════════════════════════════\n");
                    writer.flush();
                    return;
                }
            }
            writer.write("⚠ Python Supervisor 返回异常，回退到本地直连模式...\n\n");
            writer.flush();
        } else {
            writer.write("编排模式：Java 本地直连（Python 服务未启动）\n\n");
            writer.flush();
        }

        // ═══ 回退：Java 本地直连 DeepSeek ═══
        int completed = 0;
        long startTime = System.currentTimeMillis();

        for (String agentType : agentTypes) {
            try {
                AgentType at = AgentType.fromKey(agentType);
                if (at == null) {
                    writer.write("⚠ 未知智能体类型：" + agentType + "，已跳过\n");
                    writer.flush();
                    continue;
                }

                writer.write("─────────────────────────────────────\n");
                writer.write("🤖 " + at.name + " 开始工作...\n");
                writer.write("─────────────────────────────────────\n");
                writer.flush();

                List<Map<String, String>> messages = buildMessages(
                    buildSystemPrompt(agentType),
                    buildUserPrompt(agentType, topic, requestData)
                );
                String result = deepSeekService.complete(messages, DeepSeekService.MAX_TOKENS_LONG);

                if (result != null && !result.trim().isEmpty()) {
                    String out = result.trim();
                    if ("exercise".equals(agentType)) {
                        out = sanitizeExercise(out);  // 兜底：练习卷直连同样要净化元描述
                    }
                    writer.write(out + "\n\n");
                    completed++;
                } else {
                    writer.write("（" + at.name + " 未能生成有效内容）\n\n");
                }
                writer.flush();

            } catch (Exception e) {
                log.error("Agent {} failed", agentType, e);
                writer.write("❌ " + agentType + " 生成失败：" + e.getMessage() + "\n\n");
                writer.flush();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        writer.write("═══════════════════════════════════════════\n");
        writer.write("✅ 多智能体协作完成：共激活 " + agentTypes.length + " 个智能体，成功 " + completed + " 个\n");
        writer.write("🔗 编排引擎：Java 本地直连 (fallback)\n");
        writer.write("⏱ 总耗时：" + (duration / 1000.0) + " 秒\n");
        writer.write("═══════════════════════════════════════════\n");
        writer.flush();
    }

    /**
     * 获取推荐的智能体列表 — 根据教学主题关键词匹配
     */
    @PostMapping("/recommend-agents")
    public RestResponse<Map<String, Object>> recommendAgents(@RequestBody Map<String, Object> requestData) {
        String topic = text(requestData.get("topic"));
        if (topic.isEmpty()) {
            return RestResponse.fail(3, "教学主题不能为空");
        }

        List<AgentType> recommended = multiAgentService.getRecommendedAgents(topic);
        List<Map<String, Object>> agentList = new ArrayList<>();
        for (AgentType at : recommended) {
            Map<String, Object> agent = new HashMap<>();
            agent.put("key", at.key);
            agent.put("name", at.name);
            agent.put("description", at.description);
            agentList.add(agent);
        }

        // 全部 agent 列表
        List<Map<String, Object>> allAgents = new ArrayList<>();
        for (AgentType at : AgentType.values()) {
            if (at == AgentType.SUPERVISOR) continue;
            Map<String, Object> agent = new HashMap<>();
            agent.put("key", at.key);
            agent.put("name", at.name);
            agent.put("description", at.description);
            allAgents.add(agent);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("topic", topic);
        result.put("recommendedAgents", agentList);
        result.put("allAgents", allAgents);
        return RestResponse.ok(result);
    }

    /**
     * 单个 Agent 生成（优先 Python Supervisor 路由，回退直连 DeepSeek）
     */
    @PostMapping("/agent-generate")
    public void agentGenerate(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String topic = text(requestData.get("topic"));
        String agentType = text(requestData.get("agentType"));
        String resourceType = text(requestData.get("resourceType"));

        // 优先使用 agentType 路由
        String effectiveType = agentType.isEmpty() ? (resourceType.isEmpty() ? "document" : resourceType) : agentType;

        AgentType at = AgentType.fromKey(effectiveType);
        if (at == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writePlain(response, "未知智能体类型：" + effectiveType + "。可用类型：document, mind_map, exercise, extended_reading, video, code_example");
            return;
        }

        PrintWriter writer = response.getWriter();
        writer.write("🤖 " + at.name + " — " + at.description + "\n");
        writer.write("═══════════════════════════════════════════\n");

        // ═══ 优先尝试 Python Supervisor 编排 ═══
        if (pythonAgentClient.isAvailable()) {
            writer.write("编排引擎：Python Supervisor (LangGraph)\n");
            writer.flush();

            Map<String, Object> pythonResult = pythonAgentClient.generateResource(topic, effectiveType, requestData);
            if (pythonResult != null && pythonResult.containsKey("reply")) {
                String reply = String.valueOf(pythonResult.get("reply"));
                if (StringUtils.isNotBlank(reply) && !reply.startsWith("生成失败")) {
                    writer.write(reply);
                    writer.flush();
                    return;
                }
            }
            writer.write("⚠ Python Supervisor 返回异常，回退直连 DeepSeek...\n");
            writer.write("─────────────────────────────────────\n");
            writer.flush();
        } else {
            writer.write("编排引擎：Java 本地直连 (Python 服务未启动)\n");
            writer.flush();
        }

        // ═══ 回退：直连 DeepSeek ═══
        List<Map<String, String>> messages = buildMessages(
            buildSystemPrompt(effectiveType),
            buildUserPrompt(effectiveType, topic, requestData)
        );

        try {
            if ("exercise".equals(effectiveType)) {
                // 练习卷：整卷生成后净化再输出（流式直播无法逐块剔除元描述）
                String full = deepSeekService.complete(messages, DeepSeekService.MAX_TOKENS_LONG);
                writer.write(sanitizeExercise(StringUtils.defaultString(full)));
                writer.flush();
            } else {
                deepSeekService.stream(messages, chunk -> { writer.write(chunk); writer.flush(); });
            }
        } catch (Exception e) {
            log.error("Agent {} generation failed", effectiveType, e);
            writer.write("\n\n❌ 生成失败：" + e.getMessage());
            writer.flush();
        }
    }

    /**
     * 生成真实PPTX文件 — AI生成脚本 → 解析 → POI输出.pptx → 返回下载URL + 结构化幻灯片
     */
    @PostMapping("/generate-pptx")
    public RestResponse<Map<String, Object>> generatePptx(@RequestBody Map<String, Object> requestData) {
        String topic = text(requestData.get("topic"));
        if (topic.isEmpty()) {
            return RestResponse.fail(3, "课题名称不能为空");
        }

        try {
            // 1. 调用DeepSeek生成PPT脚本（长文本：推理模型需大预算，否则脚本被截断、页数缺失）
            List<Map<String, String>> messages = buildMessages(
                buildSystemPrompt("ppt"), buildUserPrompt("ppt", topic, requestData)
            );
            String scriptText = deepSeekService.complete(messages, DeepSeekService.MAX_TOKENS_LONG);

            if (scriptText == null || scriptText.trim().isEmpty()) {
                return RestResponse.fail(5, "AI 生成PPT内容为空，请重试");
            }

            // 2. 在服务端解析为结构化幻灯片（供前端展示 + PPTX生成统一数据源）
            List<Map<String, Object>> slides = parseSlides(scriptText);

            // 3. 用同一份结构化数据生成真实.pptx文件 → 保证页面展示和下载内容一致
            Path pptxPath = pptGenerationService.generatePptx(slides, topic);

            // 4. 返回下载URL + 结构化幻灯片数据
            String fileName = pptxPath.getFileName().toString();
            Map<String, Object> result = new HashMap<>();
            result.put("downloadUrl", "/api/admin/resource/download/" + fileName);
            result.put("fileName", fileName);
            result.put("fileSize", Files.size(pptxPath));
            result.put("slides", slides);       // 结构化幻灯片数组
            result.put("slideCount", slides.size());
            result.put("topic", topic);
            return RestResponse.ok(result);

        } catch (Exception e) {
            log.error("PPTX generation failed for topic: {}", topic, e);
            return RestResponse.fail(2, "PPT生成失败: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Agnes Video V2.0 — 真实 MP4 视频生成
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/generate-video-agnes")
    public RestResponse<Map<String, Object>> generateVideoAgnes(@RequestBody Map<String, Object> requestData) {
        String topic = text(requestData.get("topic"));
        if (topic.isEmpty()) {
            return RestResponse.fail(3, "课题名称不能为空");
        }

        if (!agnesVideoService.isConfigured()) {
            return RestResponse.fail(2, "Agnes Video API 未配置（请在 agnes-config.json 中设置 apiKey）");
        }

        try {
            // 1. 先用 AI 生成视频脚本
            List<Map<String, String>> messages = buildMessages(
                buildSystemPrompt("video"), buildUserPrompt("video", topic, requestData)
            );
            String videoScript = deepSeekService.complete(messages, DeepSeekService.MAX_TOKENS_LONG);

            if (videoScript == null || videoScript.trim().isEmpty()) {
                return RestResponse.fail(5, "AI 生成视频脚本为空，请重试");
            }

            // 2. 构建 Agnes 视频生成 prompt（脚本 + 视觉指令）
            String agnesPrompt = buildAgnesVideoPrompt(videoScript, topic);

            // 3. 提交到 Agnes Video V2.0 — 教学动画视频
            // 视频时长（秒）：前端可调，5秒或10秒
            // Agnes V2.0 要求 num_frames = 8*n+1，上限 249 帧 @ 24fps ≈ 10.4 秒
            int durationSeconds = getInt(requestData.get("duration"), 5);
            if (durationSeconds < 5) durationSeconds = 5;
            if (durationSeconds > 10) durationSeconds = 10;
            // 计算满足 8*n+1 的帧数：5秒→121帧(8*15+1)，10秒→241帧(8*30+1)
            int targetFrames = durationSeconds * 24;
            int numFrames = 8 * Math.round((targetFrames - 1) / 8.0f) + 1;
            if (numFrames > 249) numFrames = 249; // 不超过模型最大帧数
            Map<String, Object> options = new HashMap<>();
            options.put("num_frames", numFrames);
            options.put("frame_rate", 24);
            options.put("size", "1280x720");

            Map<String, Object> submitResult = agnesVideoService.submitVideoGeneration(agnesPrompt, options);

            if (Boolean.TRUE.equals(submitResult.get("success"))) {
                Map<String, Object> result = new HashMap<>();
                result.put("taskId", submitResult.get("taskId"));
                result.put("status", submitResult.getOrDefault("status", "submitted"));
                result.put("duration", durationSeconds);
                result.put("numFrames", numFrames);
                result.put("scriptPreview", videoScript.substring(0, Math.min(videoScript.length(), 300)));
                log.info("Agnes video task submitted: taskId={}, topic={}", submitResult.get("taskId"), topic);
                return RestResponse.ok(result);
            } else {
                String errorDetail = String.valueOf(submitResult.getOrDefault("error", "未知错误"));
                String responseBody = String.valueOf(submitResult.getOrDefault("response", ""));
                log.error("Agnes video submit failed: error={}, response={}", errorDetail, responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody);
                return RestResponse.fail(2, "视频生成任务提交失败: " + errorDetail);
            }
        } catch (Exception e) {
            log.error("Agnes video generation failed for topic: {}", topic, e);
            return RestResponse.fail(2, "视频生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/video-status-agnes")
    public RestResponse<Map<String, Object>> queryVideoStatusAgnes(@RequestParam String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return RestResponse.fail(3, "taskId 不能为空");
        }

        try {
            Map<String, Object> statusResult = agnesVideoService.queryVideoStatus(taskId, null);
            Map<String, Object> result = new HashMap<>();
            result.put("status", statusResult.getOrDefault("status", "unknown"));
            result.put("taskId", taskId);

            if (Boolean.TRUE.equals(statusResult.get("success"))) {
                if (statusResult.containsKey("videoUrl")) {
                    result.put("videoUrl", statusResult.get("videoUrl"));
                }
                if (statusResult.containsKey("response")) {
                    result.put("response", statusResult.get("response"));
                }
            }
            if (statusResult.containsKey("error")) {
                result.put("error", statusResult.get("error"));
            }
            return RestResponse.ok(result);
        } catch (Exception e) {
            log.error("Agnes video status query failed: {}", taskId, e);
            return RestResponse.fail(2, "查询视频状态失败: " + e.getMessage());
        }
    }

    /** 将 AI 视频脚本转换为 Agnes Video V2.0 的可视化教学 prompt
     *  核心原则：Agnes 是文本→视频模型，但无法可靠渲染中文文字。
     *  所以 prompt 必须全部用英文描述画面场景、动画和图示，
     *  不包含任何中文文字渲染指令。中文教学内容由 TTS 语音承载。
     */
    private String buildAgnesVideoPrompt(String script, String topic) {
        StringBuilder prompt = new StringBuilder();
        // 整体风格（英文，不含中文）
        prompt.append("Educational animation video about '").append(topic).append("'. ");
        prompt.append("Style: clean modern classroom presentation with animated infographics, ");
        prompt.append("bright simple diagrams, soft blue and white background, ");
        prompt.append("professional teaching style, smooth camera motion, 24fps. ");
        prompt.append("NO actual text characters or writing in the video. ");
        prompt.append("Use visual diagrams, icons, and illustrations instead of text. ");

        // 提取场景描述 — 只使用英文关键词描述图示和动画，不渲染文字
        String[] lines = script.split("\\n");
        int sceneCount = 0;
        StringBuilder currentVisual = new StringBuilder();

        for (String line : lines) {
            String l = line.trim();
            if (l.isEmpty()) continue;

            // 检测 ## 场景标题
            if (l.startsWith("## ")) {
                if (currentVisual.length() > 0 && sceneCount < 6) {
                    prompt.append("Scene ").append(sceneCount + 1).append(": ");
                    prompt.append(currentVisual.toString().trim()).append(" ");
                    sceneCount++;
                }
                currentVisual = new StringBuilder();
                continue;
            }

            // 跳过 【画面文字】【语音讲解】等标签行
            if (l.startsWith("【")) continue;

            // 跳过画面文字内容（这些是中文，不适合让模型渲染）
            if (l.contains("画面文字")) continue;

            // 跳过语音讲解内容（由 TTS 承载）
            if (l.contains("语音讲解")) continue;

            // 其他中文内容行 → 转为英文视觉描述
            if (l.length() > 5 && !l.startsWith("```")) {
                // 提取核心概念关键词，用英文描述视觉呈现方式
                String cleaned = l.replaceAll("^[\\-*\\d.、#]+\\s*", "").trim();
                if (cleaned.length() > 3 && !cleaned.contains("禁忌") && !cleaned.contains("格式")) {
                    // 检查是否含有中文，有则用通用视觉描述替代
                    if (cleaned.matches(".*[\\u4e00-\\u9fff]+.*")) {
                        // 中文行 → 转为英文视觉描述
                        String[] keywords = cleaned.split("[，。、；：\\s]+");
                        String enDesc = "Showing animated diagram with ";
                        for (String kw : keywords) {
                            if (kw.length() >= 2 && kw.matches(".*[\\u4e00-\\u9fff]+.*")) {
                                enDesc += "illustration of this concept, ";
                            }
                        }
                        currentVisual.append(enDesc);
                    } else {
                        currentVisual.append(cleaned).append(". ");
                    }
                }
            }
        }

        // 最后一个场景
        if (currentVisual.length() > 0 && sceneCount < 6) {
            prompt.append("Scene ").append(sceneCount + 1).append(": ");
            prompt.append(currentVisual.toString().trim()).append(" ");
            sceneCount++;
        }

        // 兜底
        if (sceneCount == 0) {
            prompt.append("Educational animation about '").append(topic)
                  .append("' with animated diagrams, visual metaphors, ")
                  .append("and step-by-step visual explanations. ");
            prompt.append("Clean educational style, no text characters in the video.");
        }

        return prompt.toString();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Agnes Image 2.1 Flash — 思维导图图片生成
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/generate-mindmap-image")
    public RestResponse<Map<String, Object>> generateMindmapImage(@RequestBody Map<String, Object> requestData) {
        String topic = text(requestData.get("topic"));
        String mindmapJson = text(requestData.get("mindmapJson"));
        if (mindmapJson.isEmpty()) {
            mindmapJson = topic;
        }

        if (!agnesVideoService.isConfigured()) {
            return RestResponse.fail(2, "Agnes Image API 未配置");
        }

        try {
            String imagePrompt = "Beautiful mind map illustration about '" + topic +
                "'. Clean hierarchical layout with connected nodes, soft pastel colors, " +
                "white background, professional educational diagram style. " +
                "Content structure: " + mindmapJson.substring(0, Math.min(mindmapJson.length(), 500));

            Map<String, Object> options = new HashMap<>();
            options.put("size", "1024x1024");
            options.put("model", "agnes-image-2.0-flash");  // 已确认模型中存在

            Map<String, Object> submitResult = agnesVideoService.submitImageGeneration(imagePrompt, options);

            if (Boolean.TRUE.equals(submitResult.get("success"))) {
                Map<String, Object> result = new HashMap<>();
                result.put("taskId", submitResult.get("taskId"));
                result.put("status", submitResult.getOrDefault("status", "submitted"));
                if (submitResult.containsKey("imageUrl")) {
                    result.put("imageUrl", submitResult.get("imageUrl"));
                }
                return RestResponse.ok(result);
            } else {
                return RestResponse.fail(2, "图片生成任务提交失败: " +
                    submitResult.getOrDefault("error", "未知错误"));
            }
        } catch (Exception e) {
            log.error("Agnes image generation failed for topic: {}", topic, e);
            return RestResponse.fail(2, "图片生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/mindmap-image-status")
    public RestResponse<Map<String, Object>> queryMindmapImageStatus(@RequestParam String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return RestResponse.fail(3, "taskId 不能为空");
        }

        try {
            Map<String, Object> statusResult = agnesVideoService.queryImageStatus(taskId, null);
            Map<String, Object> result = new HashMap<>();
            result.put("status", statusResult.getOrDefault("status", "unknown"));
            result.put("taskId", taskId);

            if (Boolean.TRUE.equals(statusResult.get("success"))) {
                if (statusResult.containsKey("imageUrl")) {
                    result.put("imageUrl", statusResult.get("imageUrl"));
                }
            }
            if (statusResult.containsKey("error")) {
                result.put("error", statusResult.get("error"));
            }
            return RestResponse.ok(result);
        } catch (Exception e) {
            log.error("Agnes image status query failed: {}", taskId, e);
            return RestResponse.fail(2, "查询图片状态失败: " + e.getMessage());
        }
    }

    /**
     * 服务端解析AI脚本文本 → 结构化幻灯片列表
     * 每个slide: { title, type, points, layout, imageKeywords, notes }
     */
    private List<Map<String, Object>> parseSlides(String text) {
        List<Map<String, Object>> slides = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return slides;

        // 按「第X页」分割
        String[] parts = text.split("(?=第\\s*\\d+\\s*页)");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            Map<String, Object> slide = new LinkedHashMap<>();
            slide.put("title", "");
            slide.put("type", "");
            slide.put("points", new ArrayList<String>());
            slide.put("imageKeywords", new ArrayList<String>());
            slide.put("notes", "");

            // 提取页面标题 (## 开头)
            java.util.regex.Matcher titleM = java.util.regex.Pattern.compile("##\\s*(.+?)(?:\\n|$)")
                    .matcher(trimmed);
            if (titleM.find()) slide.put("title", titleM.group(1).trim());

            // 提取类型
            java.util.regex.Matcher typeM = java.util.regex.Pattern.compile("[类型|类别][：:]\\s*(.+?)(?:\\n|$)")
                    .matcher(trimmed);
            if (typeM.find()) slide.put("type", typeM.group(1).trim());

            // 提取配图关键词 [k1, k2, k3]
            java.util.regex.Matcher imgM = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]")
                    .matcher(trimmed);
            if (imgM.find()) {
                String[] kws = imgM.group(1).split("[,，]");
                @SuppressWarnings("unchecked")
                List<String> kwList = (List<String>) slide.get("imageKeywords");
                for (String kw : kws) {
                    String k = kw.trim();
                    if (!k.isEmpty()) kwList.add(k);
                }
            }

            // 提取讲解备注
            java.util.regex.Matcher noteM = java.util.regex.Pattern.compile(
                    "【讲解备注】[：:]?\\s*(.+?)(?:\\n|$)", java.util.regex.Pattern.DOTALL)
                    .matcher(trimmed);
            if (noteM.find()) {
                String note = noteM.group(1).trim();
                // 截取到下一个标签之前
                int endIdx = note.indexOf("【");
                if (endIdx > 0) note = note.substring(0, endIdx).trim();
                slide.put("notes", note);
            }

            // 提取要点（· 或 - 开头，或 要点N：格式）
            @SuppressWarnings("unchecked")
            List<String> points = (List<String>) slide.get("points");
            String[] lines = trimmed.split("\\n");
            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty()) continue;

                // 匹配 · 或 - 开头的要点
                java.util.regex.Matcher bulletM = java.util.regex.Pattern.compile("^[·\\-]\\s*(.+)").matcher(l);
                if (bulletM.find()) {
                    String pt = bulletM.group(1).trim();
                    if (!isPptMetaLine(pt)) points.add(pt);
                    continue;
                }

                // 匹配 要点N：格式
                java.util.regex.Matcher keyM = java.util.regex.Pattern.compile("^要点\\d+[：:]\\s*(.+)").matcher(l);
                if (keyM.find()) {
                    points.add(keyM.group(1).trim());
                    continue;
                }

                // 匹配编号列表 1. 2. 等
                java.util.regex.Matcher numM = java.util.regex.Pattern.compile("^\\d+[\\.\\)、]\\s*(.+)").matcher(l);
                if (numM.find()) {
                    String pt = numM.group(1).trim();
                    if (!isPptMetaLine(pt) && pt.length() > 5) points.add(pt);
                }
            }

            // 兜底标题
            if (slide.get("title").toString().isEmpty()) {
                String firstLine = trimmed.split("\\n")[0].replaceAll("^第\\s*\\d+\\s*页[：:\\s]*", "").trim();
                if (!firstLine.isEmpty() && firstLine.length() < 80) slide.put("title", firstLine);
            }
            if (slide.get("title").toString().isEmpty() && !slide.get("type").toString().isEmpty()) {
                slide.put("title", slide.get("type").toString());
            }

            // 只添加有实质内容的幻灯片
            if (!slide.get("title").toString().isEmpty() || !points.isEmpty()) {
                slides.add(slide);
            }
        }
        return slides;
    }

    /** 判断一行是否为元数据标签（非要点内容） — 用于 PPT 解析 */
    private boolean isPptMetaLine(String line) {
        return java.util.regex.Pattern.compile(
            "^(类型|配图|页面布局|页面内容|讲解备注|【|输出要求|风格|配色|课程名称|适用年级|副标题|致命禁忌|整体风格|内容节奏|语言表达|辅助理解|示意图|PPT结构|建议|字体)").matcher(line).find()
            || line.length() <= 3;
    }

    @PostMapping("/evaluate")
    public void evaluate(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        String type = text(requestData.get("resourceType"));
        String title = text(requestData.get("title"));
        String content = text(requestData.get("content"));
        List<Map<String, String>> messages = buildMessages(
            "你是一位高校教学质量评估专家。请从学术准确性（内容是否正确无讹）、逻辑清晰度（结构是否条理分明）、学科相关性（是否贴合课程目标）、教学有效性（是否有助于学生理解和掌握）四个维度评价教学资源。先输出「质量评分：X.X」（满分10分），再给出具体改进建议。",
            "资源类型：" + type + "\n标题：" + title + "\n\n内容：\n" + content
        );
        try (PrintWriter writer = response.getWriter()) { deepSeekService.stream(messages, chunk -> { writer.write(chunk); writer.flush(); }); }
        catch (Exception e) { log.error("Evaluation failed", e); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  全局输出禁令：所有模块共享，禁止一切非教学内容的元描述
    // ═══════════════════════════════════════════════════════════════
    private static final String NO_META =
        "【铁律】你只输出教学内容本身。严格禁止：\n" +
        "· 任何开场白/问候/过渡语（\"好的\"、\"以下是\"、\"下面我来\"等）\n" +
        "· 描述你正在做什么（\"我为您生成\"、\"根据要求\"、\"以下是\"等）\n" +
        "· 前言/后记/总结/参考资料等非教学章节\n" +
        "· 对读者的称呼（\"你\"、\"您\"、\"同学们请注意\"除外）\n" +
        "· 结尾的客套话（\"希望以上\"、\"如需更多\"、\"欢迎继续\"等）\n" +
        "· 任何元描述、说明、标注、占位符（\"此处可添加\"等）\n" +
        "直接从第一个知识点开始输出纯粹的教学内容。";

    // ═══════════════════════════════════════════════════════════════
    //  多智能体系统提示词（由 MultiAgentService 提供专业提示词）
    // ═══════════════════════════════════════════════════════════════
    private String buildSystemPrompt(String type) {
        String base;
        switch (StringUtils.defaultString(type)) {
            // ── 文档 ── Doc Agent
            case "document":
                base = multiAgentService.buildDocAgentPrompt("");  // 主题在 user prompt 中补充
                break;

            // ── 思维导图 ── MindMap Agent
            case "mind_map":
                base = multiAgentService.buildMindMapAgentPrompt("");
                break;

            // ── 练习题 ── Quiz Agent
            case "exercise":
                base = multiAgentService.buildQuizAgentPrompt("");
                break;

            // ── 拓展阅读 ── Reading Agent
            case "extended_reading":
                base = multiAgentService.buildReadingAgentPrompt("");
                break;

            // ── PPT课件（保持原有的专用PPT格式提示词） ──
            case "ppt":
                base = "你是一位资深课堂演示设计师，请为以下【主题】制作一份适配校园场景的课堂演示文稿。\n\n" +
                    "【整体风格】\n" +
                    "■ 风格定位：清新简洁、通俗易懂，适合师生课堂使用和学生课堂展示\n" +
                    "■ 配色原则：柔和护眼，优先使用浅色系背景 + 深色文字，避免刺眼荧光色；重点用温和暖色标记，不用大红大绿\n" +
                    "■ 字体要求：标题清晰醒目、正文大小适中（联想教室投影和屏幕阅读场景），段落留有呼吸空间\n" +
                    "■ 内容节奏：逻辑由浅入深、层层递进，每个知识点充分拆分细化，避免信息堆砌\n" +
                    "■ 语言表达：通俗易懂、贴近学生生活，规避晦涩专业术语；必须解释时附带简短说明或打个生活中的比方\n" +
                    "■ 辅助理解：适量搭配简单示意图、对比表格、流程图来辅助文字说明，降低认知负担\n\n" +
                    "【致命禁忌】\n" +
                    "❌ 禁止占位描述（如\"此处可添加XX例题\"）——必须写出完整文字\n" +
                    "❌ 禁止空泛概括（如\"拓展延伸：引导学生思考\"）——必须写出具体内容\n" +
                    "❌ 禁止用晦涩术语堆砌而不加解释\n" +
                    "❌ 禁止长篇连续文字不分段——每页信息密度合理，优先用分点/卡片呈现\n\n" +
                    "【每页输出格式】\n" +
                    "第X页\n" +
                    "## 页面标题（简练亲切，6～14字）\n" +
                    "- 类型：封面/目录/学习目标/知识回顾/新知讲解/重难点突破/例题精讲/课堂练习/思考题/小结/拓展延伸/课后任务/结束页\n" +
                    "- 配图关键词：[英文搜索词1, 英文搜索词2, 英文搜索词3]\n" +
                    "- 页面内容：\n" +
                    "  · 要点（每点20～35字，说清一个知识点，语言通俗；每页3～6个要点，宁多勿缺）\n" +
                    "- 【讲解备注】：口语化讲解词（60～200字），像老师在对学生说话一样自然\n\n" +
                    "【内容完整性硬要求】\n" +
                    "■ 必须覆盖主题的核心概念、关键原理、典型方法、实际应用与常见误区，不得遗漏主干知识\n" +
                    "■ 页数按主题复杂度自适应：概念型主题12页起步，方法/算法/工程类主题14～18页，新知讲解页可扩展为多页递进\n" +
                    "■ 最后一页结束页之前必须有小结页，小结需覆盖本次讲解的全部要点脉络\n\n" +
                    "【PPT结构顺序 — 自适应清新校园框架】\n" +
                    "第1页·封面 → 第2页·学习目标 → 第3页·情境导入 → 第4页·知识回顾 →\n" +
                    "第5-N页·新知讲解（基础→核心→进阶，复杂主题拆为多页，每页一个子主题） →\n" +
                    "重难点突破 → 例题精讲 → 思考题+练习 → 课堂小结 → 结束页\n\n" +
                    "从\"第1页\"封面开始连续输出完整PPT脚本，直到结束页为止，不要任何前言后记；" +
                    "输出被截断等同于任务失败，请确保完整输出所有页面。";
                break;

            // ── 代码示例 ── Code Agent
            case "code_example":
                base = multiAgentService.buildCodeAgentPrompt("");
                break;

            // ── 教学视频 ── Video Agent
            case "video":
                base = multiAgentService.buildVideoAgentPrompt("");
                break;

            default:
                base = "你是高等教育课程资源开发专家。请根据教学需求生成高质量学习资源。直接输出内容本身。" + NO_META;
                break;
        }
        return base;
    }

    private String buildUserPrompt(String type, String topic, Map<String, Object> requestData) {
        switch (StringUtils.defaultString(type)) {
            case "document":
                return String.format("教学主题：《%s》\n\n请按照 Doc Agent 的课程文档规范，输出完整的10章节教学文档（从概念定义到知识关联）。", topic);
            case "mind_map":
                return String.format("《%s》\n\n请直接输出以下格式的思维导图（从下一行开始，第一个字必须是\"-\"）：\n- 中心主题：%s", topic, topic);
            case "exercise":
                return String.format("教学主题：《%s》\n\n【超强约束】\n"
                    + "· 你的回复第一个字符必须是题型标记【选择题】，在此之前不允许任何文字；\n"
                    + "· 生成选择题3道(4选项) + 判断题2道 + 填空题2道 + 简答题1道，每题含答案与详细解析；\n"
                    + "· 题型分区标题固定为【选择题】【判断题】【填空题】【简答题】，分区之间不要写过渡语；\n\n"
                    + "【严禁出现的元描述 — 出现即判失败】\n"
                    + "· 卷首套话：\"以下是一份…\"、\"本练习卷…\"、\"已生成\"、\"好的\"、\"根据您的要求\"；\n"
                    + "· 阅卷元数据：评分标准、赋分标准、分值分配、得分点、要点X分、本题考查、考查目标、命题意图、出题思路、难度标注；\n"
                    + "· 解析里只写\"为什么对/为什么错\"的知识性说明，不得写\"本题考查…\"这类出题意图；\n"
                    + "· 简答题答案只给答案要点与解析，禁止附\"评分标准：要点①3分…\"这类教师阅卷信息；\n"
                    + "· 结尾收尾语：\"希望…\"、\"以上题目…\"、\"祝学习进步\"等一律禁止，最后一题解析结束立刻停止。\n\n"
                    + "【排版硬性要求】每道小题的\"答案\"与\"解析\"必须各占一行：先换行写\"答案：X\"，再换行写\"解析：…\"，禁止把答案和解析连写在同一行。", topic);
            case "extended_reading":
                return String.format("教学主题：《%s》\n\n【超强约束】你的回复第一个字必须是\"#\"，以\"# 一、\"或\"## 一、\"作为整段内容的绝对开头。按照 Reading Agent 的拓展阅读规范，输出7个章节的完整阅读材料。直接开始，不要前言。", topic);
            case "ppt":
                return String.format("【教学主题】：%s\n\n【超强约束】你的回复第一个字必须是\"第\"，以\"第1页\"作为整段输出的绝对开头。请按自适应清新校园框架（复杂主题14～18页）从第1页封面开始连续输出完整PPT脚本，直至结束页，中途不得省略任何页面。", topic);
            case "code_example":
                return String.format("教学主题：《%s》\n\n【超强约束】你的回复第一个字必须是\"/\"，以\"//\"注释行作为开头。按照 Code Agent 的教学代码规范，输出至少3个难度递进的教学代码示例（含运行环境、完整代码、注释、预期输出）。不要任何非代码的文字。", topic);
            case "video":
                return String.format("教学主题：《%s》\n\n" +
                    "请生成一份完整的3分钟教学视频讲解稿。内容必须覆盖该知识点的所有核心内容，不能遗漏。\n" +
                    "每节包含【画面文字】和【语音讲解】，两者内容不同：画面是精炼要点，语音是详细口语讲解。\n" +
                    "必须详细展开以下内容：概念定义→原理推导→公式解析→实例演示→方法总结→常见问题→课后思考。\n" +
                    "语音讲解总字数不少于1500字，确保能讲满3分钟。以\"## 开场引入\"开头，禁止元描述。", topic);
            default:
                return String.format("教学内容：《%s》\n请直接输出高质量学习资源。", topic);
        }
    }

    private List<Map<String, String>> buildMessages(String system, String user) {
        List<Map<String, String>> msgs = new ArrayList<>();
        msgs.add(msg("system", system));
        msgs.add(msg("user", user));
        return msgs;
    }

    private Map<String, String> msg(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role); m.put("content", content);
        return m;
    }

    private String text(Object o) { return o == null ? "" : o.toString().trim(); }

    private int getInt(Object o, int defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString().trim()); } catch (Exception e) { return defaultValue; }
    }

    /** 解析前端传入的 agentTypes 数组 */
    private String[] parseAgentTypes(Object rawTypes) {
        if (rawTypes instanceof List) {
            List<String> types = new ArrayList<>();
            for (Object item : (List<?>) rawTypes) {
                String s = String.valueOf(item).trim();
                if (!s.isEmpty()) types.add(s);
            }
            return types.toArray(new String[0]);
        }
        if (rawTypes instanceof String) {
            String s = ((String) rawTypes).trim();
            if (!s.isEmpty()) return s.split("[,，\\s]+");
        }
        return new String[0];
    }

    private void writePlain(HttpServletResponse response, String text) throws IOException {
        try (PrintWriter writer = response.getWriter()) {
            writer.write(StringUtils.defaultString(text));
            writer.flush();
        }
    }
}
