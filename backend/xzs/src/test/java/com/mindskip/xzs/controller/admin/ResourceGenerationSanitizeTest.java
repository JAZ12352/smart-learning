package com.mindskip.xzs.controller.admin;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 练习卷净化逻辑（sanitizeExercise）单元测试。
 *
 * 对应赛题「单元测试用例（针对……领域知识生成准确性等核心模块）」要求：
 * sanitizeExercise 是 Java 侧生成准确性的最后一道兜底——剔除阅卷元数据、
 * 卷首套话、结尾收尾语，并修复「答案/解析连写」问题。
 *
 * 说明：sanitizeExercise 为私有方法且无状态（不依赖任何注入字段），
 * 故以反射直接调用，不起 Spring 上下文，保持纯单元测试速度。
 */
public class ResourceGenerationSanitizeTest {

    private String sanitize(String raw) throws Exception {
        // 构造器注入 6 个服务依赖；sanitizeExercise 为纯字符串处理，不触碰任何字段，传 null 安全
        ResourceGenerationController controller = new ResourceGenerationController(
                null, null, null, null, null, null);
        Method m = ResourceGenerationController.class
                .getDeclaredMethod("sanitizeExercise", String.class);
        m.setAccessible(true);
        return (String) m.invoke(controller, raw);
    }

    // ── 元数据剔除 ──────────────────────────────────────────────

    @Test
    public void 评分标准行必须被剔除() throws Exception {
        String raw = "【选择题】\n1. 1+1=?\nA. 1\nB. 2\n答案：B\n评分标准：本题共5分，答对得5分\n";
        String out = sanitize(raw);
        assertFalse("阅卷元数据「评分标准」不得出现在学生练习卷中", out.contains("评分标准"));
    }

    @Test
    public void 本题考查行必须被剔除() throws Exception {
        String raw = "【选择题】\n1. ReLU 的输出范围是？\nA. [0,+∞)\nB. (-∞,+∞)\n答案：A\n本题考查激活函数值域。\n";
        String out = sanitize(raw);
        assertFalse("「本题考查」元描述必须剔除", out.contains("本题考查"));
        assertTrue("题干必须保留", out.contains("ReLU"));
    }

    @Test
    public void 命题意图与考点行必须被剔除() throws Exception {
        String raw = "【选择题】\n1. 下列属于无监督学习的是？\nA. 回归\nB. K-Means\n答案：B\n命题意图：考察聚类算法\n考点：机器学习基础\n";
        String out = sanitize(raw);
        assertFalse(out.contains("命题意图"));
        assertFalse(out.contains("考点："));
    }

    // ── 卷首套话 / 结尾收尾语 ────────────────────────────────────

    @Test
    public void 卷首套话必须被剥离() throws Exception {
        String raw = "好的，以下是为您生成的练习题，希望对你有所帮助：\n\n【选择题】\n1. 2+2=?\nA. 3\nB. 4\n答案：B\n";
        String out = sanitize(raw);
        assertFalse("开场白必须剥离", out.startsWith("好的"));
        assertTrue("输出应从题型标记开始", out.startsWith("【选择题】"));
    }

    @Test
    public void 结尾收尾语必须被剥离() throws Exception {
        String raw = "【选择题】\n1. 2+2=?\nA. 3\nB. 4\n答案：B\n希望这些题目对你有帮助！\n";
        String out = sanitize(raw);
        assertFalse("结尾收尾语必须剥离", out.contains("希望"));
        assertTrue("答案行必须保留", out.contains("答案：B"));
    }

    // ── 答案/解析连写修复 ────────────────────────────────────────

    @Test
    public void 答案与解析连写必须拆行() throws Exception {
        String raw = "【选择题】\n1. TCP 属于哪一层协议？\nA. 网络层\nB. 传输层\n答案：B解析：TCP 是传输层协议。\n";
        String out = sanitize(raw);
        assertTrue("答案与解析应各占一行", out.contains("答案：B\n解析："));
    }

    @Test
    public void 选项与答案连写必须拆行() throws Exception {
        String raw = "【选择题】\n1. 测试\nA. 甲\nB. 乙答案：B\n";
        String out = sanitize(raw);
        assertTrue("选项与答案连写应拆行", out.contains("B. 乙\n答案：B"));
    }

    // ── 正常内容保护（防误伤）────────────────────────────────────

    @Test
    public void 正常题干与选项不得被误删() throws Exception {
        String raw = "【选择题】\n"
                + "1. 在 Kubernetes 中，Service 的作用是？\n"
                + "A. 存储 Pod 日志\nB. 提供稳定访问入口\nC. 调度 Pod\nD. 管理密钥\n"
                + "答案：B\n"
                + "解析：Service 通过 ClusterIP 提供稳定网络端点。\n";
        String out = sanitize(raw);
        for (String part : new String[]{"Kubernetes", "A. 存储 Pod 日志", "B. 提供稳定访问入口",
                "答案：B", "解析：Service"}) {
            assertTrue("正常内容被误删: " + part, out.contains(part));
        }
    }

    @Test
    public void 干净输入应保持原样() throws Exception {
        String raw = "【判断题】\n1. Dropout 在推理阶段不生效。\n答案：正确\n";
        String out = sanitize(raw);
        assertEquals("已干净的输入不应被改动",
                raw.replace("\r\n", "\n").trim(), out.trim());
    }

    @Test
    public void 空输入安全() throws Exception {
        assertEquals("", sanitize(""));
        assertEquals(null, sanitize(null));
    }
}
