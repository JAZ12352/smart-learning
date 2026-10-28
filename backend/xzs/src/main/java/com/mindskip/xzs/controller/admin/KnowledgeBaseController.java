package com.mindskip.xzs.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.KbDocument;
import com.mindskip.xzs.domain.KnowledgeBase;
import com.mindskip.xzs.service.DeepSeekService;
import com.mindskip.xzs.service.KnowledgeBaseService;
import com.mindskip.xzs.service.PythonAgentClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/admin/knowledge-base")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final DeepSeekService deepSeekService;
    private final PythonAgentClient pythonAgentClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                   DeepSeekService deepSeekService,
                                   PythonAgentClient pythonAgentClient) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.deepSeekService = deepSeekService;
        this.pythonAgentClient = pythonAgentClient;
    }

    @GetMapping("/list")
    public RestResponse<List<Map<String, Object>>> getKnowledgeBaseList() {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getAllKnowledgeBases();
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeBase kb : knowledgeBases) {
            list.add(toKnowledgeBaseView(kb));
        }
        return RestResponse.ok(list);
    }

    @PostMapping("/create")
    public RestResponse<Map<String, Object>> createKnowledgeBase(@RequestBody Map<String, Object> requestData) {
        String name = text(requestData.get("name"));
        if (StringUtils.isBlank(name)) {
            return RestResponse.fail(2, "知识库名称不能为空");
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(text(requestData.get("description")));
        kb.setSubject(text(requestData.get("subject")));
        kb.setDocumentCount(0);
        kb.setCreateTime(new Date());
        kb.setModifyTime(new Date());
        kb.setDeleted(false);
        knowledgeBaseService.insert(kb);

        return RestResponse.ok(toKnowledgeBaseView(kb));
    }

    @DeleteMapping("/{id}")
    public RestResponse<Void> deleteKnowledgeBase(@PathVariable Integer id) {
        knowledgeBaseService.deleteById(id);
        return RestResponse.ok();
    }

    @GetMapping("/{kbId}/documents")
    public RestResponse<List<Map<String, Object>>> getDocuments(@PathVariable Integer kbId) {
        List<KbDocument> documents = knowledgeBaseService.getDocumentsByKbId(kbId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (KbDocument doc : documents) {
            list.add(toDocumentView(doc));
        }
        return RestResponse.ok(list);
    }

    @GetMapping("/document/{docId}/preview")
    public void previewDocument(@PathVariable Integer docId, HttpServletResponse response) throws IOException {
        KbDocument document = knowledgeBaseService.getDocumentById(docId);
        if (document == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("文档不存在");
            return;
        }
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(StringUtils.defaultIfBlank(document.getContent(), "当前文档暂无可预览的文本内容。"));
    }

    @PostMapping("/upload")
    public RestResponse<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file,
                                                            @RequestParam("kbId") Integer kbId) throws IOException {
        if (file.isEmpty()) {
            return RestResponse.fail(2, "文件不能为空");
        }

        KnowledgeBase kb = knowledgeBaseService.selectById(kbId);
        if (kb == null) {
            return RestResponse.fail(2, "知识库不存在");
        }

        String fileName = file.getOriginalFilename() == null ? "未命名文件" : file.getOriginalFilename();
        String extension = getFileExtension(fileName);

        KbDocument document = new KbDocument();
        document.setKbId(kbId);
        document.setName(fileName);
        document.setType(extension);
        document.setSize(file.getSize());
        document.setStatus("completed");
        document.setCreateTime(new Date());
        document.setContent(extractDocumentContent(file, extension));
        document.setDeleted(false);
        knowledgeBaseService.insertDocument(document);

        // Update document count
        int count = knowledgeBaseService.countDocuments(kbId);
        kb.setDocumentCount(count);
        kb.setModifyTime(new Date());
        knowledgeBaseService.updateByIdFilter(kb);

        // 异步同步到 ChromaDB 向量索引（Python 服务）
        if (document.getContent() != null && !document.getContent().isEmpty()) {
            new Thread(() -> {
                try {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("source", fileName);
                    meta.put("kb_id", kbId);
                    meta.put("type", extension);
                    pythonAgentClient.indexDocument(document.getId(), document.getContent(), meta);
                } catch (Exception e) {
                    log.warn("Async ChromaDB indexing failed for doc {}: {}", document.getId(), e.getMessage());
                }
            }).start();
        }

        return RestResponse.ok(toDocumentView(document));
    }

    @DeleteMapping("/document/{docId}")
    public RestResponse<Void> deleteDocument(@PathVariable Integer docId) {
        KbDocument doc = knowledgeBaseService.getDocumentById(docId);
        if (doc != null) {
            knowledgeBaseService.deleteDocument(docId);
            // Update document count
            int count = knowledgeBaseService.countDocuments(doc.getKbId());
            KnowledgeBase kb = knowledgeBaseService.selectById(doc.getKbId());
            if (kb != null) {
                kb.setDocumentCount(count);
                kb.setModifyTime(new Date());
                knowledgeBaseService.updateByIdFilter(kb);
            }
        }
        return RestResponse.ok();
    }

    @PostMapping("/document/{docId}/reprocess")
    public RestResponse<Void> reprocessDocument(@PathVariable Integer docId) {
        KbDocument document = knowledgeBaseService.getDocumentById(docId);
        if (document == null) {
            return RestResponse.fail(2, "文档不存在");
        }
        document.setStatus("completed");
        knowledgeBaseService.updateDocument(document);
        return RestResponse.ok();
    }

    @PostMapping("/import")
    public RestResponse<Map<String, Object>> importKnowledgeBase(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return RestResponse.fail(2, "文件不能为空");
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("导入知识库 - " + (file.getOriginalFilename() == null ? "" : file.getOriginalFilename()));
        kb.setDescription("由本地文件导入");
        kb.setSubject("other");
        kb.setDocumentCount(0);
        kb.setCreateTime(new Date());
        kb.setModifyTime(new Date());
        kb.setDeleted(false);
        knowledgeBaseService.insert(kb);

        return RestResponse.ok(toKnowledgeBaseView(kb));
    }

    @PostMapping("/search")
    public RestResponse<List<Map<String, Object>>> vectorSearch(@RequestBody Map<String, Object> requestData) {
        Integer kbId = toInteger(requestData.get("kbId"), null);
        String query = text(requestData.get("query"));
        Integer topK = toInteger(requestData.get("topK"), 5);
        return RestResponse.ok(searchDocuments(kbId, query, topK));
    }

    /**
     * 向量语义检索 — 优先使用 Python ChromaDB，回退关键词匹配。
     * 新增端点，供前端直接调用或 AI 对话中触发语义检索。
     */
    @PostMapping("/vector-search")
    public RestResponse<List<Map<String, Object>>> vectorSearchEnhanced(@RequestBody Map<String, Object> requestData) {
        Integer kbId = toInteger(requestData.get("kbId"), null);
        String query = text(requestData.get("query"));
        Integer topK = toInteger(requestData.get("topK"), 5);

        // 1. 优先 Python ChromaDB 向量检索
        List<Map<String, Object>> vectorResults = pythonAgentClient.searchKnowledge(query, topK);

        if (!vectorResults.isEmpty()) {
            log.info("Vector search hit: query='{}' → {} results", query.substring(0,
                Math.min(50, query.length())), vectorResults.size());
            // 标准化字段名，兼容前端
            for (Map<String, Object> item : vectorResults) {
                if (!item.containsKey("source")) {
                    item.put("source", item.getOrDefault("metadata", "向量检索"));
                }
                if (!item.containsKey("score")) {
                    item.put("score", item.getOrDefault("distance", 0.0));
                }
            }
            return RestResponse.ok(vectorResults);
        }

        // 2. 回退：关键词匹配 + 返回 mix 标记
        log.info("Vector search miss, fallback to keyword for: {}", query);
        List<Map<String, Object>> keywordResults = searchDocuments(kbId, query, topK);
        Map<String, Object> mixInfo = new HashMap<>();
        mixInfo.put("source", "关键词匹配（向量检索暂不可用）");
        mixInfo.put("score", 0.0);
        mixInfo.put("content", "请确认 Python 多智能体服务已启动以启用语义检索。");
        keywordResults.add(0, mixInfo);
        return RestResponse.ok(keywordResults);
    }

    @PostMapping("/rag-chat")
    public void ragChat(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        Integer kbId = toInteger(requestData.get("kbId"), null);
        String message = text(requestData.get("message"));

        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        if (kbId == null || StringUtils.isBlank(message)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"知识库或问题不能为空\"}");
            return;
        }

        List<Map<String, Object>> references = searchDocuments(kbId, message, 5);
        String userPrompt = buildRagPrompt(message, references);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", "你是一位高校学科知识库智能助教。请优先基于知识库中的教学文档内容严谨、准确地回答问题，需引用知识库来源文档。若知识库信息不足以支撑完整回答，请明确说明当前知识的局限性，并给出进一步获取相关学术资料的途径建议。"));
        messages.add(message("user", userPrompt));

        final boolean[] referencesSent = {false};

        try (PrintWriter writer = response.getWriter()) {
            deepSeekService.stream(messages, chunk -> {
                try {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("chunk", chunk);
                    if (!referencesSent[0]) {
                        payload.put("references", references);
                        referencesSent[0] = true;
                    }
                    writer.write(objectMapper.writeValueAsString(payload));
                    writer.write("\n");
                    writer.flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("RAG 问答失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write("{\"error\":\"" + safeError(e.getMessage()) + "\"}");
                writer.flush();
            }
        }
    }

    private List<Map<String, Object>> searchDocuments(Integer kbId, String query, Integer topK) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (kbId == null || StringUtils.isBlank(query)) {
            return results;
        }

        // 1. 优先尝试 Python ChromaDB 向量语义检索
        List<Map<String, Object>> vectorResults = pythonAgentClient.searchKnowledge(query, topK);
        if (!vectorResults.isEmpty()) {
            log.debug("Using Python vector search, got {} results", vectorResults.size());
            return vectorResults;
        }

        // 2. 回退：传统关键词匹配
        log.debug("Python vector search unavailable, using keyword matching fallback");
        String normalizedQuery = query.toLowerCase();
        List<KbDocument> documents = knowledgeBaseService.getDocumentsByKbId(kbId);
        for (KbDocument document : documents) {
            String searchableText = (document.getName() + "\n" + StringUtils.defaultString(document.getContent())).toLowerCase();
            double score = calculateScore(normalizedQuery, searchableText, document.getName().toLowerCase());
            if (score <= 0) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", document.getId());
            item.put("source", document.getName());
            item.put("score", Math.min(score, 0.99d));
            item.put("content", buildSnippet(document.getContent(), query));
            results.add(item);
        }

        results.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        if (results.size() > topK) {
            return new ArrayList<>(results.subList(0, topK));
        }
        return results;
    }

    private double calculateScore(String query, String content, String fileName) {
        double score = 0;
        if (content.contains(query)) {
            score += 0.65d;
        }
        if (fileName.contains(query)) {
            score += 0.2d;
        }
        String[] tokens = query.split("\\s+");
        for (String token : tokens) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            if (content.contains(token)) {
                score += 0.08d;
            }
        }
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                if (content.indexOf(c) >= 0) {
                    score += 0.03d;
                }
            }
        }
        return score;
    }

    private String buildSnippet(String content, String query) {
        if (StringUtils.isBlank(content)) {
            return "当前文档暂无可提取文本内容。";
        }
        int hitIndex = content.toLowerCase().indexOf(query.toLowerCase());
        if (hitIndex < 0) {
            return content.length() > 180 ? content.substring(0, 180) + "..." : content;
        }
        int start = Math.max(0, hitIndex - 60);
        int end = Math.min(content.length(), hitIndex + Math.max(query.length(), 20) + 120);
        String snippet = content.substring(start, end).trim();
        return start > 0 ? "..." + snippet : snippet;
    }

    private String buildRagPrompt(String question, List<Map<String, Object>> references) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于下面的知识库内容回答问题。\n\n");
        if (references.isEmpty()) {
            prompt.append("当前知识库中没有检索到直接命中的文档，请明确说明这一点，并给出谨慎的辅助建议。\n\n");
        } else {
            prompt.append("知识库参考片段：\n");
            for (int i = 0; i < references.size(); i++) {
                Map<String, Object> ref = references.get(i);
                prompt.append(i + 1)
                    .append(". 来源：").append(ref.get("source"))
                    .append("；相似度：").append(ref.get("score"))
                    .append("\n内容：").append(ref.get("content"))
                    .append("\n\n");
            }
        }
        prompt.append("用户问题：").append(question).append("\n\n");
        prompt.append("回答要求：\n");
        prompt.append("1. 先直接回答问题。\n");
        prompt.append("2. 明确指出答案主要依据了哪些知识库信息。\n");
        prompt.append("3. 如果知识库证据不足，要明确说“知识库中没有足够依据”。");
        return prompt.toString();
    }

    private Map<String, Object> toKnowledgeBaseView(KnowledgeBase kb) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", kb.getId());
        item.put("name", kb.getName());
        item.put("description", kb.getDescription());
        item.put("subject", kb.getSubject());
        item.put("documentCount", kb.getDocumentCount());
        item.put("createTime", kb.getCreateTime());
        item.put("updateTime", kb.getModifyTime());
        return item;
    }

    private Map<String, Object> toDocumentView(KbDocument doc) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", doc.getId());
        item.put("name", doc.getName());
        item.put("type", doc.getType());
        item.put("size", doc.getSize());
        item.put("status", doc.getStatus());
        item.put("uploadTime", doc.getCreateTime());
        return item;
    }

    private String extractDocumentContent(MultipartFile file, String extension) throws IOException {
        String normalizedExtension = StringUtils.defaultString(extension).toLowerCase();
        if ("txt".equals(normalizedExtension) || "md".equals(normalizedExtension) || "csv".equals(normalizedExtension)) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        if ("json".equals(normalizedExtension)) {
            String raw = new String(file.getBytes(), StandardCharsets.UTF_8);
            try {
                Object parsed = objectMapper.readValue(raw, Object.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
            } catch (Exception e) {
                return raw;
            }
        }
        if ("zip".equals(normalizedExtension)) {
            StringBuilder sb = new StringBuilder();
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(file.getInputStream())) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    String name = entry.getName().toLowerCase();
                    if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv") || name.endsWith(".json")) {
                        sb.append("=== ").append(entry.getName()).append(" ===\n");
                        byte[] buf = new byte[4096];
                        int len;
                        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                        while ((len = zis.read(buf)) > 0) { bos.write(buf, 0, len); }
                        String content = new String(bos.toByteArray(), StandardCharsets.UTF_8);
                        if (name.endsWith(".json")) {
                            try {
                                Object parsed = objectMapper.readValue(content, Object.class);
                                content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
                            } catch (Exception ignored) {}
                        }
                        sb.append(content).append("\n\n");
                    }
                    zis.closeEntry();
                }
            }
            return sb.length() > 0 ? sb.toString() : "ZIP 文件中未找到可解析的文本文件（支持 .txt/.md/.csv/.json）";
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        return "文件名：" + fileName + "。当前版本暂不解析该二进制格式的正文内容，但文档已经成功入库，可继续追问并结合文件名进行检索。";
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase();
    }

    private Integer toInteger(Object value, Integer defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private String safeError(String message) {
        return StringUtils.defaultString(message).replace("\"", "'");
    }
}
