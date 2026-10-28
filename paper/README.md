# 论文工程说明（IEEE TLT 目标稿）

Overleaf 可直接编译的 LaTeX 工程。**所有需要真实数据的位置都打成了红色的 `[TODO: ...]`，投稿前必须清零。**

---

## 1. 上传到 Overleaf

1. 把整个 `paper/` 目录打包为 ZIP（**包含 `main.tex` 在根目录**）
2. Overleaf → New Project → Upload Project → 选择 ZIP
3. Menu → Compiler 选 **pdfLaTeX**；Menu → Main document 确认是 `main.tex`
4. 首次编译：Recompile（bib 需要 `pdflatex → bibtex → pdflatex → pdflatex`，Overleaf 自动处理）

> 若 Overleaf 报 `file not found`，检查是否把 `paper/` 本身也打包进去了 —— `main.tex` 必须位于项目根。

---

## 2. 目录结构

```
paper/
├── main.tex                      # 主文件：宏包、标题、摘要、章节拼接
├── refs.bib                      # 参考文献
├── sections/
│   ├── 01-introduction.tex
│   ├── 02-related-work.tex
│   ├── 03-method.tex
│   ├── 04-experiments.tex
│   ├── 05-results.tex
│   ├── 06-discussion.tex
│   ├── 07-conclusion.tex
│   └── appendix-agent-prompts.tex
└── figures/
    └── architecture.tex          # TikZ 系统架构图
```

---

## 3. 换期刊

当前用 `\documentclass[journal,compsoc]{IEEEtran}`，适配 IEEE Transactions on Learning Technologies。

| 目标期刊 | 改动 |
|---|---|
| Elsevier（ESWA / KBS） | `\documentclass[review]{elsarticle}`；参考文献改 `\bibliographystyle{elsarticle-num}` |
| Springer（EAIT / ILE） | `\documentclass[sn-article]{sn-jnl}`；需补 `sn-mathphys` 等宏包 |
| Springer（EAIT / ILE） | 表格 `\toprule` 系列来自 booktabs，三套模板均兼容 |

无论换哪家：**先确认模板自带宏包清单**，再增删 preamble。

---

## 4. TODO 清零流程

```bash
grep -rn "TODO" paper/        # 应逐条清零
```

各 TODO 的数据来源：

| TODO 位置 | 数据来源 | 产出命令 |
|---|---|---|
| 摘要三项指标 | 主实验结果 | `python tests/run_live_parallel.py --all --concurrency 4` |
| §4.2 主题/画像/用例数 | 数据集 | `test-data/test_cases.json` |
| Table III 主结果 | 全量 + 消融 | `python tests/stats_tests.py --baseline ... --compare ... --latex` |
| Table IV 混淆矩阵 | `eval_report_full.json` → `metrics.adaptation.confusion` | 自动 |
| Table V 一致性 | 外部判定 + 人工标注 | `python tests/verification_validity.py agree --latex` |
| Table VI 人工评分 | 标注员 CSV | 手工整理 |
| Table VII 成本 | 采集日志 | 手工统计 |
| §4.1 RQ 表述 | 与最终 Table 对齐 | 手改 |

---

## 5. 写作要点（投稿前自检）

- [ ] 摘要中的每一个数字都能在正文表格里找到出处
- [ ] 贡献列表 C1–C4 与 §5 结果逐一对应，没有"声称了但没测"的贡献
- [ ] §6.3 威胁到效度**主动承认**没有学习增益实验（若确实没做）
- [ ] 所有引用 DOI 已核对（`refs.bib` 中部分条目为回忆重建，标了待核对）
- [ ] 图表编号无重复，正文交叉引用均能解析
- [ ] 术语统一：`dimension reduction`（降维解释）、`advanced challenge`（进阶挑战）

---

## 6. 封面信（cover letter）要点

1. 说明系统与常规 LLM-in-education 工作的区别：**验证前置 + 可复核的难度规则**
2. 强调可复现性：评测脚本、测试集、消融开关全部开源
3. 主动说明局限：无学习增益实验 / 单一模型后端 —— **主动说明比被审稿人挖出来好**
4. 建议 3 位审稿人（多智能体系统、教育技术、幻觉检测各一），并说明理由

---

## 7. 常见编译错误

| 报错 | 原因 | 处理 |
|---|---|---|
| `Undefined control sequence \sysname` | 宏定义在 main.tex，章节文件单独编译时缺失 | 整篇编译即可 |
| `Citation 'xxx' undefined` | bib 未编译 | Overleaf 点 Recompile 两次；或手动跑 bibtex |
| `Package{tikz} Error` | 缺少 tikzlibrary | 检查 `main.tex` 中 `\usetikzlibrary` 是否完整 |
| 表格超宽 | `table*` 环境用于跨双栏 | 宽表用 `\begin{table*}`，窄表用 `\begin{table}` |
