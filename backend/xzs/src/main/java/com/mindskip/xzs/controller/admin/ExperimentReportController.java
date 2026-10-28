package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.LearningPath;
import com.mindskip.xzs.domain.StudentProfile;
import com.mindskip.xzs.service.LearningPathService;
import com.mindskip.xzs.service.PythonAgentClient;
import com.mindskip.xzs.service.StudentProfileService;
import com.mindskip.xzs.service.ExperimentEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可复现实验报告控制器
 *
 * 对应赛题评分标准「实用价值 30 分」：
 *   1) 提供 1 个垂直领域专业知识库
 *   2) 提供 3 组不同学习者画像的演示数据
 *   3) 生成的知识准确率 ≥ 95%（幻觉率 < 5%）
 *   4) 学习者画像-资源难度匹配准确率 ≥ 85%
 *   5) 知识点覆盖率 ≥ 90%
 *
 * 本接口实时汇聚系统真实运行数据（知识库、画像、学习路径），
 * 并对核心评测指标给出可复现的评测方法说明。
 */
@RestController
@RequestMapping("/api/admin/evaluation")
public class ExperimentReportController {

    private static final Logger log = LoggerFactory.getLogger(ExperimentReportController.class);

    private final StudentProfileService studentProfileService;
    private final LearningPathService learningPathService;
    private final PythonAgentClient pythonAgentClient;
    private final ExperimentEvaluationService experimentEvaluationService;

    public ExperimentReportController(StudentProfileService studentProfileService,
                                      LearningPathService learningPathService,
                                      PythonAgentClient pythonAgentClient,
                                      ExperimentEvaluationService experimentEvaluationService) {
        this.studentProfileService = studentProfileService;
        this.learningPathService = learningPathService;
        this.pythonAgentClient = pythonAgentClient;
        this.experimentEvaluationService = experimentEvaluationService;
    }

    /**
     * 运行可复现实验评测：LLM-as-Judge 实测知识准确率 / 难度匹配 / 覆盖率 / 幻觉率，
     * 并附负样本探针自检判定环节的可靠性。
     * 该接口会触发真实的大模型评测（20 样本 + 5 探针，temperature=0），约需 8~10 分钟。
     */
    @PostMapping("/run-experiment")
    public RestResponse<Map<String, Object>> runExperiment() {
        try {
            return RestResponse.ok(experimentEvaluationService.runExperiment());
        } catch (IllegalStateException e) {
            return RestResponse.fail(2, e.getMessage());
        } catch (Exception e) {
            log.error("run-experiment failed", e);
            return RestResponse.fail(2, "实验运行失败: " + e.getMessage());
        }
    }

    @GetMapping("/experiment-report")
    public RestResponse<Map<String, Object>> getExperimentReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("title", "领域知识个性化生成与多智能体协同决策系统 · 可复现实验报告");
        report.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        report.put("metrics", Collections.emptyList());
        report.put("knowledgeBase", buildKnowledgeBase());
        report.put("profiles", buildProfiles());
        report.put("learningPaths", buildLearningPaths());
        report.put("methodology", buildMethodology());
        return RestResponse.ok(report);
    }

    /** 垂直领域知识库（真实运行数据） */
    private Map<String, Object> buildKnowledgeBase() {
        Map<String, Object> kb = new LinkedHashMap<>();
        kb.put("name", "人工智能领域垂直知识库");
        kb.put("collection", "xzs_knowledge");
        kb.put("domain", "人工智能");
        kb.put("subDomains", Arrays.asList("机器学习基础", "深度学习", "神经网络", "自然语言处理", "计算机视觉", "强化学习", "大语言模型"));
        kb.put("docCount", resolveChromaDocCount());
        kb.put("vectorDb", "ChromaDB（离线确定性嵌入，全离线可复现）");
        kb.put("description", "为多智能体协同决策系统提供「人工智能」垂直领域的专业知识底座，覆盖高等教育 AI 专业人才培养与企业 AI 工程师内训场景");
        return kb;
    }

    /** 3 组差异化学习者画像（演示数据，来自 test-data.sql） */
    private List<Map<String, Object>> buildProfiles() {
        List<Map<String, Object>> profiles = new ArrayList<>();
        int[] userIds = {2, 3, 4};
        String[] labels = {"学习者 A", "学习者 B", "学习者 C"};
        for (int i = 0; i < userIds.length; i++) {
            StudentProfile p = studentProfileService.getProfileByUserId(userIds[i]);
            if (p == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", labels[i]);
            item.put("userId", p.getUserId());
            item.put("knowledgeLevel", p.getKnowledgeLevel());
            item.put("cognitiveStyle", p.getCognitiveStyle());
            item.put("weakPoints", p.getWeakPoints());
            item.put("learningGoals", p.getLearningGoals());
            item.put("learningPace", p.getLearningPace());
            item.put("learningStyle", p.getLearningStyle());
            item.put("motivationLevel", p.getMotivationLevel());
            profiles.add(item);
        }
        return profiles;
    }

    /** 个性化学习路径（真实运行数据） */
    private Map<String, Object> buildLearningPaths() {
        int total = 0;
        for (int uid : new int[]{2, 3, 4}) {
            List<LearningPath> paths = learningPathService.getPathsByUserId(uid);
            if (paths != null) total += paths.size();
        }
        Map<String, Object> lp = new LinkedHashMap<>();
        lp.put("total", total);
        lp.put("description", "基于知识点依赖图 + 拓扑排序 + 难度梯度生成的个性化学习路径");
        return lp;
    }

    /** 评测方法说明（可复现性；指标实测值由「运行实验」接口实时产生，不做静态宣称） */
    private String buildMethodology() {
        return "1) 知识准确率：LLM-as-Judge 实测——对抽样生成资源做 RAG 检索增强的事实性核验，"
            + "由大模型逐条判定「与权威知识库是否一致」；判定失败按保守原则计为不通过，不做默认放行。\n"
            + "   幻觉率 = 100% − 知识准确率，是同一测量的另一种表述，不重复计分；"
            + "阈值取 ≤5%，与「准确率 ≥95%」严格等价，避免同一测量给出相反结论。\n"
            + "2) 画像-资源难度匹配准确率：逐条按样本所属学习者的画像知识分映射到难度档位（基础/进阶/高级），"
            + "与 LLM-as-Judge 判定的资源难度档位比对求匹配率；画像缺失的样本排除出分母，不拉低指标。\n"
            + "3) 知识点覆盖率：以垂直知识库 7 大子领域的专属概念词为基准，单个子领域需在正文中命中 ≥2 个概念词方计为覆盖；"
            + "仅统计正文内容，不含标题，避免覆盖率被标题字段直接刷满而与生成质量脱钩。\n"
            + "4) 复现方式：点击「运行可复现评测」实时评测（样本取自系统真实生成资源，不足 15 条时自动生成种子样本并入库留痕）；"
            + "种子生成与判定均使用 temperature=0 的确定性调用；每次运行均返回样本明细"
            + "（逐条 accurate / difficulty / expectedTier / reason）与判定失败计数，全过程可审计。\n"
            + "5) 判定可靠性自检：每次运行会向同一条判定链路混入 5 条已知含公认事实错误的负样本探针"
            + "（只判定、不写库、不计入准确率分母），统计被识破的比例；检出率低于 80% 时判定偏松，"
            + "本次准确率与幻觉率将附带可靠性告警，不宜直接采信。\n"
            + "6) 已知局限：种子样本由本系统生成、并由本系统判定，且与 Judge 共用同一知识库做接地，"
            + "属自证式评测，准确率天然偏高（负样本探针可缓解但无法根除），严谨结论应以人工标注 gold set 校准。"
            + "采样路径随数据量切换（真实资源 ≥15 条走真实样本，否则播种），两条路径的指标不做跨路径比较。";
    }

    /** 从 Python 服务健康状态解析 ChromaDB 文档数 */
    private int resolveChromaDocCount() {
        try {
            Map<String, Object> health = pythonAgentClient.healthCheck();
            Object chroma = health.get("chromadb");
            if (chroma != null) {
                Matcher m = Pattern.compile("\\((\\d+)\\s*documents?\\)").matcher(String.valueOf(chroma));
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve ChromaDB doc count: {}", e.getMessage());
        }
        return 11; // 默认值：ai_domain_kb.md 切分后的文本块数量
    }
}
