package com.mindskip.xzs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindskip.xzs.domain.GeneratedResource;
import com.mindskip.xzs.domain.StudentProfile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可复现实验评测引擎
 *
 * 将实验报告中的核心指标从「静态基准值」升级为「真实可复现的实测值」：
 *   · 知识准确率：LLM-as-Judge —— 对抽样生成资源做 RAG 检索增强的事实性核验
 *   · 幻觉率：由知识准确率换算（100 − 准确率），是同一测量的另一面，不重复计分
 *   · 画像-资源难度匹配准确率：逐条按样本所属学习者的画像档位，比对 Judge 判定的资源难度档位
 *   · 知识点覆盖率：正文对垂直知识库 7 大子领域专属概念词的覆盖比例
 *
 * 可复现性保障：种子生成与 Judge 判定均使用 temperature=0 的确定性调用（见 completeDeterministic）。
 *
 * 已知局限（应在实验报告中如实说明，不宜对外宣称超过此范围）：
 *   1. 种子样本由本系统生成、再由本系统判定，且与 Judge 共用同一知识库做接地，
 *      属于自证式评测，准确率天然偏高，理想情况应以人工标注 gold set 校准；
 *   2. temperature=0 只能保证同一模型版本下的输出稳定，模型升级仍可能改变结果；
 *   3. 采样路径随数据量切换（真实资源 ≥15 条走真实样本，否则播种），
 *      两条路径的样本来源不同，指标不做跨路径比较。
 *
 * 对应赛题「实用价值 30 分」的量化验收要求。
 */
@Service
public class ExperimentEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentEvaluationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ResourceGenerationService resourceGenerationService;
    private final DeepSeekService deepSeekService;
    private final PythonAgentClient pythonAgentClient;
    private final StudentProfileService studentProfileService;

    public ExperimentEvaluationService(ResourceGenerationService resourceGenerationService,
                                       DeepSeekService deepSeekService,
                                       PythonAgentClient pythonAgentClient,
                                       StudentProfileService studentProfileService) {
        this.resourceGenerationService = resourceGenerationService;
        this.deepSeekService = deepSeekService;
        this.pythonAgentClient = pythonAgentClient;
        this.studentProfileService = studentProfileService;
    }

    /**
     * 垂直领域知识库 7 大子领域及其专属概念词（用于覆盖率判定）。
     *
     * 刻意不包含子领域名称本身（如「深度学习」「强化学习」）：种子样本的标题形如
     * 「深度学习-反向传播」，若把领域名当作关键词，覆盖率会被标题直接刷满、
     * 恒等于 100%，与生成内容的质量完全脱钩。
     * 同时剔除跨领域泛词（如「图像」「卷积」「策略」「环境」），避免子领域之间互相顶替。
     */
    private static final String[][] DOMAINS = {
        {"机器学习基础", "监督学习,无监督学习,决策树,支持向量机,聚类,过拟合,交叉验证,召回率"},
        {"深度学习", "反向传播,损失函数,梯度下降,激活函数,优化器,正则化,批归一化"},
        {"神经网络", "感知机,前馈,循环神经网络,长短期记忆,门控循环,残差连接,池化"},
        {"自然语言处理", "词向量,词嵌入,文本分类,情感分析,命名实体,分词,语义角色"},
        {"计算机视觉", "目标检测,图像分割,语义分割,特征提取,数据增强,感受野,边缘检测"},
        {"强化学习", "马尔可夫,奖励函数,策略梯度,Q学习,价值函数,贝尔曼,探索"},
        {"大语言模型", "Transformer,自注意力,多头注意力,预训练,微调,上下文学习,提示词"}
    };

    /** 单个子领域需命中的专属概念词数量下限，避免某个词被顺带提及即计为覆盖 */
    private static final int COVERAGE_MIN_HITS = 2;

    /** 种子样本主题（覆盖全部 7 大子领域，每个子领域 2-3 个具体主题） */
    private static final String[] SEED_TOPICS = {
        "机器学习基础-监督学习", "机器学习基础-无监督学习", "机器学习基础-模型评估",
        "深度学习-反向传播", "深度学习-卷积神经网络", "深度学习-优化器",
        "神经网络-RNN与LSTM", "神经网络-注意力机制",
        "自然语言处理-词向量", "自然语言处理-文本分类", "自然语言处理-命名实体识别",
        "计算机视觉-目标检测", "计算机视觉-图像分割",
        "强化学习-Q学习", "强化学习-策略梯度",
        "大语言模型-Transformer", "大语言模型-预训练微调"
    };

    /**
     * 负样本探针：{标题, 含公认事实错误的正文}。
     *
     * 存在的理由：种子样本由本系统生成、又由本系统判定，且与 Judge 共用同一知识库做接地，
     * 属于自证式评测——若 Judge 只是无脑放行，准确率必然虚高且无从察觉。
     * 混入这些「已知答案」的错误样本后，如果连此类低级事实错误都抓不出来，
     * 就说明判定环节失效，本次的知识准确率不宜直接采信。
     *
     * 标题刻意选取知识库能检索到正确内容的主题，使探针走完整判定链路（含 RAG 检索），
     * 检验的是「拿到正确参考资料后能否发现内容是错的」，而非「检索不到时的猜测能力」。
     */
    private static final String[][] NEGATIVE_CONTROLS = {
        {"反向传播", "反向传播（Backpropagation）是神经网络中的一种前向计算过程。"
            + "它沿网络正向传播输入信号并直接得到预测输出，整个过程中不涉及任何梯度或导数的计算，也不需要链式法则。"},
        {"卷积神经网络", "卷积神经网络（CNN）只能处理一维的序列数据，例如文本和时间序列。"
            + "由于卷积核的结构限制，卷积操作无法应用于二维的图像数据，因此 CNN 在图像识别任务中基本没有用武之地。"},
        {"监督学习", "监督学习是一种不需要标注数据的学习范式。"
            + "训练时模型通过自行聚类发现数据中的内在规律，因此训练集中是否包含标签对模型训练没有任何影响。"},
        {"Transformer", "Transformer 架构完全建立在循环神经网络之上，它通过对序列逐个时刻递归处理来实现并行计算。"
            + "由于保留了 RNN 的时序依赖，Transformer 无法并行处理序列中的不同位置。"},
        {"强化学习", "强化学习的优化目标是最大化智能体在当前一步获得的即时奖励，"
            + "因此完全不需要考虑未来长期回报的累积，折扣因子在强化学习算法中也没有任何实际意义。"}
    };

    /** 负样本探针检出率下限：低于此值说明判定偏松，准确率指标不可信 */
    private static final double NEGATIVE_RECALL_FLOOR = 80.0;

    public Map<String, Object> runExperiment() {
        // 门禁：LLM 未配置时直接拒绝，杜绝「默认放行」造成的虚高指标
        if (!deepSeekService.isConfigured()) {
            throw new IllegalStateException("DeepSeek API 未配置（请在 .env 设置 DEEPSEEK_API_KEY 或在「模型配置」页启用），无法进行 LLM-as-Judge 实测");
        }
        long start = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 抽样资源（不足则自动生成种子样本）
        List<GeneratedResource> samples = sampleResources();

        // 2. LLM-as-Judge：知识准确率 + 难度档位
        List<Map<String, Object>> details = new ArrayList<>();
        int accurateCount = 0;
        int matchedCount = 0;
        int matchedTotal = 0;      // 能确定目标档位的样本数（画像缺失者排除出分母）
        int judgeFailedCount = 0;
        String learnerTier = resolveLearnerTier();   // 全局档位：仅用于种子样本比对与结果展示
        Map<Integer, String> tierByUser = resolveTierByUser();

        for (GeneratedResource res : samples) {
            Map<String, Object> judge = judgeResource(res);
            boolean accurate = Boolean.TRUE.equals(judge.get("accurate"));
            boolean judgeFailed = Boolean.TRUE.equals(judge.get("judgeFailed"));
            if (judgeFailed) judgeFailedCount++;
            // 判定不可用时的兜底档位为「未知」，不得用「进阶」兜底，否则会与目标档位意外匹配
            String difficulty = String.valueOf(judge.getOrDefault("difficulty", "未知"));
            if (accurate) accurateCount++;

            // 难度匹配：按样本所属学习者各自的画像档位比对，而非用一个全局档位衡量全部样本。
            // 三个画像的档位本就不同，全局比对会让真实样本路径下的匹配率被结构性压到三分之一左右。
            String expectedTier = expectedTierFor(res, tierByUser, learnerTier);
            if (expectedTier != null) {
                matchedTotal++;
                if (expectedTier.equals(difficulty)) matchedCount++;
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("title", res.getTitle());
            detail.put("type", res.getResourceType());
            detail.put("accurate", accurate);
            detail.put("difficulty", difficulty);
            detail.put("expectedTier", expectedTier == null ? "（画像缺失，不计入匹配率）" : expectedTier);
            detail.put("reason", judge.get("reason"));
            detail.put("judgeFailed", judgeFailed);
            details.add(detail);
        }

        int n = samples.size();
        double accuracy = n == 0 ? 0 : round(accurateCount * 100.0 / n);
        double matching = matchedTotal == 0 ? 0 : round(matchedCount * 100.0 / matchedTotal);
        double hallucination = round(Math.max(0, 100.0 - accuracy));
        double coverage = computeCoverage(samples);

        // 3. 负样本探针：判定环节的可靠性自检（只判定、不写库、不计入准确率分母）
        Map<String, Object> negativeControls = runNegativeControls();
        result.put("negativeControls", negativeControls);

        result.put("sampleCount", n);
        result.put("learnerTier", learnerTier);
        result.put("tierByUser", tierByUser);
        result.put("matchedTotal", matchedTotal);
        result.put("judgeFailedCount", judgeFailedCount);

        List<String> warnings = new ArrayList<>();
        if (n > 0 && judgeFailedCount * 2 >= n) {
            warnings.add("超过半数样本的 LLM 判定失败，本次结果按保守原则计为不通过，可靠性有限，请检查模型配置后重跑");
        }
        double probeRecall = ((Number) negativeControls.get("recall")).doubleValue();
        double probeFloor = ((Number) negativeControls.get("floor")).doubleValue();
        if (probeRecall < probeFloor) {
            warnings.add("负样本探针检出率 " + probeRecall + "% 低于下限 " + probeFloor + "%（"
                + negativeControls.get("caught") + "/" + negativeControls.get("total")
                + "），判定环节偏松，知识准确率与幻觉率不宜直接采信");
        }
        if (n > 0 && matchedTotal < n) {
            warnings.add("有 " + (n - matchedTotal) + " 条样本缺少对应的学习者画像，"
                + "已排除出难度匹配率分母（有效样本 " + matchedTotal + "/" + n + " 条）");
        }
        if (!warnings.isEmpty()) {
            result.put("warning", String.join("；", warnings));
        }
        result.put("details", details);
        result.put("durationMs", System.currentTimeMillis() - start);
        result.put("metrics", buildMetrics(accuracy, hallucination, matching, coverage));
        return result;
    }

    /**
     * 负样本探针校验：把已知含事实错误的内容送进同一条判定链路，统计被判为「不准确」的比例。
     *
     * 探针仅参与判定，不写入数据库，不污染真实资源，也不计入知识准确率的分母。
     * 其目的是回答一个准确率本身回答不了的问题：这个数字之所以高，
     * 是因为内容真的对，还是因为 Judge 什么都不判错。
     */
    private Map<String, Object> runNegativeControls() {
        List<Map<String, Object>> items = new ArrayList<>();
        int caught = 0;
        int failed = 0;
        for (String[] probe : NEGATIVE_CONTROLS) {
            GeneratedResource res = new GeneratedResource();
            res.setTitle(probe[0]);
            res.setContent(probe[1]);
            res.setResourceType("document");
            Map<String, Object> judge = judgeResource(res);
            if (Boolean.TRUE.equals(judge.get("judgeFailed"))) failed++;
            // 判为「不准确」即视为成功检出这条人为植入的错误
            boolean flagged = !Boolean.TRUE.equals(judge.get("accurate"));
            if (flagged) caught++;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", probe[0]);
            item.put("content", probe[1]);
            item.put("caught", flagged);
            item.put("reason", judge.get("reason"));
            items.add(item);
        }
        int total = NEGATIVE_CONTROLS.length;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("caught", caught);
        out.put("failed", failed);
        out.put("recall", total == 0 ? 0 : round(caught * 100.0 / total));
        out.put("floor", NEGATIVE_RECALL_FLOOR);
        out.put("items", items);
        return out;
    }

    private List<Map<String, Object>> buildMetrics(double accuracy, double hallucination, double matching, double coverage) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(metric("knowledge_accuracy", "知识准确率", accuracy, 95, "%", "higher_better",
            "LLM-as-Judge：对抽样生成资源做 RAG 检索增强的事实性核验，判定与权威知识库一致的比例"));
        metrics.add(metric("matching_accuracy", "画像-资源难度匹配准确率", matching, 85, "%", "higher_better",
            "逐条按样本所属学习者的画像档位，与 LLM-as-Judge 判定的资源难度档位求匹配率；画像缺失的样本不计入分母"));
        metrics.add(metric("coverage", "知识点覆盖率", coverage, 90, "%", "higher_better",
            "以 7 大子领域的专属概念词统计正文覆盖情况，单个子领域需命中 ≥" + COVERAGE_MIN_HITS
                + " 个概念词方计为覆盖；仅统计正文内容，不含标题"));
        metrics.add(metric("hallucination_rate", "幻觉率", hallucination, 5, "%", "lower_better",
            "由知识准确率换算（100 − 准确率），与知识准确率为同一测量的两面，不重复计分；"
                + "阈值口径与「准确率 ≥95%」等价，故取 ≤5% 而非 <5%，避免同一测量给出相反结论"));
        return metrics;
    }

    /**
     * direction 为 higher_better / lower_better，决定达标判定方向与前端的阈值展示符号。
     * 达标判定必须与互补指标的口径自洽：准确率取 ≥95%，则换算出的幻觉率只能取 ≤5%，
     * 否则在边界（如 20 个样本错 1 个）会出现「准确率 95% 达标、幻觉率 5% 未达标」的自相矛盾。
     */
    private Map<String, Object> metric(String key, String name, double value, double target, String unit,
                                       String direction, String method) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("name", name);
        m.put("value", value);
        m.put("target", target);
        m.put("unit", unit);
        m.put("direction", direction);
        m.put("pass", "lower_better".equals(direction) ? value <= target : value >= target);
        m.put("method", method);
        return m;
    }

    /** 抽样：优先使用真实学习者（user 2/3/4）已生成资源，按标题去重取最新；不足 15 条则清理历史评测种子后重新播种 */
    private List<GeneratedResource> sampleResources() {
        List<GeneratedResource> all = new ArrayList<>();
        for (int uid : new int[]{2, 3, 4}) {
            List<GeneratedResource> r = resourceGenerationService.getResourcesByUserId(uid);
            if (r != null) all.addAll(r);
        }
        // 过滤低质量资源（空壳/草稿）
        all.removeIf(r -> r.getContent() == null || r.getContent().trim().length() < 200);
        // 全局按 id 倒序 + 标题去重（保留最新），避免历史版本与跨用户重复样本污染指标
        all.sort(Comparator.comparing(GeneratedResource::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        List<GeneratedResource> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (GeneratedResource r : all) {
            String key = r.getTitle() == null ? "id:" + r.getId() : r.getTitle().trim();
            if (seen.add(key)) deduped.add(r);
        }
        if (deduped.size() >= 15) return deduped.subList(0, Math.min(deduped.size(), 20));
        // user_id=0 的资源仅由本服务播种写入；清理陈旧种子，按当前学习者档位与知识库重新生成
        resourceGenerationService.deleteByUserId(0);
        return generateSeedResources();
    }

    /** 生成种子样本资源，确保评测始终可运行（深度按学习者档位校准，事实按知识库接地） */
    private List<GeneratedResource> generateSeedResources() {
        String tier = resolveLearnerTier();
        List<GeneratedResource> seeds = new ArrayList<>();
        for (String topic : SEED_TOPICS) {
            try {
                String content = generateDocument(topic, tier);
                if (StringUtils.isBlank(content)) continue;
                GeneratedResource r = new GeneratedResource();
                r.setUserId(0);
                r.setResourceType("document");
                r.setTitle(topic);
                r.setContent(content);
                r.setContentFormat("markdown");
                r.setSourceAgent("Doc Agent");
                r.setCreateTime(new Date());
                r.setDeleted(false);
                resourceGenerationService.insert(r);
                seeds.add(r);
                log.info("Experiment seed resource generated: {} ({} chars)", topic, content.length());
            } catch (Exception e) {
                log.warn("Seed generation failed for {}: {}", topic, e.getMessage());
            }
        }
        return seeds;
    }

    /** RAG 接地 + 档位校准的种子文档生成：事实对齐权威知识库（压幻觉），深度对齐学习者档位（保匹配） */
    private String generateDocument(String topic, String tier) throws Exception {
        StringBuilder groundTruth = new StringBuilder();
        try {
            String query = topic.replace("-", " ").replace("—", " ").trim();
            List<Map<String, Object>> hits = pythonAgentClient.searchKnowledge(query, 3);
            if (hits != null) {
                for (Map<String, Object> h : hits) {
                    Object c = h.get("content");
                    if (c != null) groundTruth.append(c).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Seed grounding retrieval failed for {}: {}", topic, e.getMessage());
        }
        String depthRule;
        if ("基础".equals(tier)) {
            depthRule = "面向入门学习者：以概念定义、直观示例为主，公式点到即止。";
        } else if ("高级".equals(tier)) {
            depthRule = "面向高水平学习者：覆盖前沿方法、复杂系统设计与跨领域综合应用。";
        } else {
            depthRule = "面向进阶学习者：必须包含核心公式的数学表达与推导过程、模型/算法结构与原理细节（如损失函数、优化机制、典型网络结构），不得停留在概念罗列。";
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(msg("system", "你是课程文档生成专家。直接输出结构化教学文档，不要任何开场白、客套话或元描述。"
            + "所有事实性表述（尤其是术语分类、模型归属、公式）必须与「权威知识库片段」一致；片段未涵盖且你不确定的内容一律不写，严禁编造。"));
        messages.add(msg("user", "权威知识库片段：\n" + (groundTruth.length() == 0 ? "（无检索结果，仅依据公认学科知识撰写，确保事实准确）" : groundTruth.toString())
            + "\n\n任务：为教学主题《" + topic + "》生成一份完整的教学文档。"
            + "\n深度要求：" + depthRule
            + "\n结构：概念定义、核心原理（含公式推导）、关键特征、应用实例、常见误区。使用 Markdown 标题组织。"));
        // 确定性解码：默认 temperature=0.7 会让每次播种的内容不同，指标随之抖动。
        // 推理型模型会先消耗 reasoning tokens，长文档必须用大输出预算，否则正文被截断为空、样本整条丢失。
        return deepSeekService.completeDeterministic(messages, DeepSeekService.MAX_TOKENS_LONG);
    }

    /** LLM-as-Judge：结合 RAG 检索的权威知识库片段，对生成资源做事实性核验 + 难度档位判定 */
    private Map<String, Object> judgeResource(GeneratedResource res) {
        // 保守兜底：判定不可用一律计为「不准确 + 难度未知」，绝不默认放行，避免指标虚高
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("accurate", false);
        fallback.put("difficulty", "未知");
        fallback.put("reason", "LLM 判定不可用（模型调用/解析失败），按保守原则计为不通过");
        fallback.put("judgeFailed", true);
        try {
            // RAG 检索权威知识库 ground-truth（标题拆词，提高专名命中率）
            String query = res.getTitle() == null ? "" : res.getTitle().replace("-", " ").replace("—", " ").trim();
            StringBuilder groundTruth = new StringBuilder();
            List<Map<String, Object>> hits = pythonAgentClient.searchKnowledge(query, 2);
            if (hits != null) {
                for (Map<String, Object> h : hits) {
                    Object c = h.get("content");
                    if (c != null) groundTruth.append(c).append("\n");
                }
            }

            String content = res.getContent() == null ? "" : res.getContent();
            if (content.length() > 1200) content = content.substring(0, 1200);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(msg("system", "你是教学资源评测专家。任务：核验「生成内容」的事实准确性并判定难度档位。判定规则："
                + "① 若「权威知识库片段」与生成内容主题相关，以片段为准核验事实；"
                + "② 若片段为空或与主题无关，必须基于通用学科知识判断内容准确性，不得因「片段不相关/无检索结果」直接判 inaccurate；"
                + "③ 难度档位标准：基础=概念定义与入门介绍；进阶=原理推导、公式、模型细节；高级=前沿方法、复杂系统设计与跨领域综合。"
                + "只输出一个 JSON，不要任何其它文字：{\"accurate\": true|false, \"difficulty\": \"基础|进阶|高级\", \"reason\": \"一句话说明\"}"));
            messages.add(msg("user", "权威知识库片段：\n" + (groundTruth.length() == 0 ? "（无检索结果，请基于通用学科知识判断）" : groundTruth.toString()) + "\n\n生成内容：\n" + content));

            // 确定性解码：判定环节若带随机性，同一批样本两次运行的结论会不一致
            String raw = deepSeekService.completeDeterministic(messages);
            return parseJudge(raw, fallback);
        } catch (Exception e) {
            log.warn("Judge failed for {}: {}", res.getTitle(), e.getMessage());
            return fallback;
        }
    }

    private Map<String, Object> parseJudge(String raw, Map<String, Object> fallback) {
        if (raw == null) return fallback;
        try {
            int s = raw.indexOf('{');
            int e = raw.lastIndexOf('}');
            if (s >= 0 && e > s) {
                JsonNode node = MAPPER.readTree(raw.substring(s, e + 1));
                Map<String, Object> out = new LinkedHashMap<>();
                // 判定字段缺失时按保守原则处理：accurate 缺省 false、difficulty 缺省「未知」
                out.put("accurate", node.has("accurate") && !node.get("accurate").isNull() && node.get("accurate").asBoolean());
                out.put("difficulty", node.has("difficulty") && !node.get("difficulty").isNull() ? node.get("difficulty").asText() : "未知");
                out.put("reason", node.has("reason") && !node.get("reason").isNull() ? node.get("reason").asText() : "");
                out.put("judgeFailed", false);
                return out;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    /**
     * 全局难度档位：3 组画像知识分的平均档位。
     * 仅用于种子样本的目标档位比对与结果展示；真实样本必须逐用户取档位（见 resolveTierByUser）。
     */
    private String resolveLearnerTier() {
        double sum = 0;
        int count = 0;
        for (int uid : new int[]{2, 3, 4}) {
            StudentProfile p = studentProfileService.getProfileByUserId(uid);
            if (p == null) continue;
            double avg = avgKnowledgeScore(p.getKnowledgeLevel());
            if (avg > 0) { sum += avg; count++; }
        }
        if (count == 0) return "进阶";
        return tierOf(sum / count);
    }

    /** 逐用户解析画像对应的难度档位（基础/进阶/高级）；画像缺失或无知识分的用户不出现在结果中 */
    private Map<Integer, String> resolveTierByUser() {
        Map<Integer, String> tiers = new LinkedHashMap<>();
        for (int uid : new int[]{2, 3, 4}) {
            StudentProfile p = studentProfileService.getProfileByUserId(uid);
            if (p == null) continue;
            double score = avgKnowledgeScore(p.getKnowledgeLevel());
            if (score > 0) tiers.put(uid, tierOf(score));
        }
        return tiers;
    }

    private String tierOf(double score) {
        return score < 65 ? "基础" : (score > 85 ? "高级" : "进阶");
    }

    /**
     * 样本的目标难度档位：真实资源（user 2/3/4）取该用户自己的画像档位，
     * 种子资源（user_id=0）取播种时使用的全局档位；画像缺失返回 null，由调用方排除出分母。
     */
    private String expectedTierFor(GeneratedResource res, Map<Integer, String> tierByUser, String seedTier) {
        Integer uid = res.getUserId();
        if (uid == null || uid == 0) return seedTier;
        return tierByUser.get(uid);
    }

    /** 解析画像知识水平 JSON（{"高等数学":75,...}），返回平均分 */
    private double avgKnowledgeScore(String knowledgeLevel) {
        if (StringUtils.isBlank(knowledgeLevel)) return 0;
        try {
            JsonNode node = MAPPER.readTree(knowledgeLevel);
            if (!node.isObject()) return 0;
            double sum = 0;
            int n = 0;
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                JsonNode v = it.next();
                if (v.isNumber()) { sum += v.asDouble(); n++; }
            }
            return n == 0 ? 0 : sum / n;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 知识点覆盖率：正文覆盖的 7 大子领域比例。
     *
     * 只统计正文内容，不统计标题：种子样本的标题形如「深度学习-反向传播」，本身就把
     * 7 大子领域的名字写进去了，若计入标题则覆盖率恒等于 100%，与生成质量完全脱钩。
     * 每个子领域需命中 ≥ COVERAGE_MIN_HITS 个专属概念词才计为覆盖，避免单词顺带提及即得分。
     */
    private double computeCoverage(List<GeneratedResource> samples) {
        if (samples.isEmpty()) return 0;
        StringBuilder corpus = new StringBuilder();
        for (GeneratedResource r : samples) {
            corpus.append(r.getContent() == null ? "" : r.getContent()).append('\n');
        }
        String text = corpus.toString();
        int covered = 0;
        for (String[] domain : DOMAINS) {
            int hits = 0;
            for (String kw : domain[1].split(",")) {
                String k = kw.trim();
                if (!k.isEmpty() && text.contains(k)) {
                    hits++;
                    if (hits >= COVERAGE_MIN_HITS) { covered++; break; }
                }
            }
        }
        return round(covered * 100.0 / DOMAINS.length);
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private Map<String, String> msg(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
