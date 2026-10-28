# 🎓 智慧乐学 — 领域知识个性化生成与多智能体协同决策系统

> "挑战杯"揭榜挂帅擂台赛 · 发榜题目 **XH-202630**（发榜单位：上海云之脑智能科技有限公司）

基于大模型与多智能体协同决策技术，面向**垂直领域技能培训**场景（人工智能、智能制造、工业互联网等），构建"学情画像构建 → 多智能体协同调度（诊断/生成/校验）→ 领域知识个性化生成 → 交互反馈 → 动态决策更新"全流程闭环系统，服务于应用型工程技术人才、高技能人才培养与企业内训/转岗培训。

<p align="center">
  <strong>Spring Boot + Vue 2 + FastAPI + LangGraph + ChromaDB + DeepSeek</strong>
</p>

---

## ✨ 核心功能

### 🧠 对话式学习画像自主构建（先验知识画像）
摒弃传统表单，通过自然语言对话自动抽取多维度特征（含赛题指定的先验知识字段）：
- **学历背景 / 岗位方向** — 在校生、在职工程师、转岗人员、企业内训学员等职业身份识别
- **理论测试结果** — 各科目掌握程度（0-100 分），作为个性化适配的量化输入
- **认知风格** — 视觉型/听觉型/动觉型/读写型
- **薄弱点** — 具体知识点缺陷（含严重程度）
- **学习目标** — 就业/考研/转岗上岗/技能认证等
- **学习节奏 / 兴趣偏好 / 学习风格 / 动机水平**

画像支持"随学随新"动态更新，每次对话自动抽取增量特征。

### 🤖 多智能体协同决策（14 Agent 全闭环）

| Agent | 职责 | 赛题对标 |
|-------|------|---------|
| **Supervisor** | 任务分解、Agent 路由、执行编排 | 多智能体协同调度 |
| **Diagnosis Agent** | 学情诊断：锚定理论强项与技能盲区 | "学情诊断 Agent"明确分工 |
| **Profile Agent** | 对话式画像构建与动态更新 | 先验知识画像整合 |
| **MindMap Agent** | 知识结构先行，供全链路复用 | 协同生成机制 |
| **Doc Agent** | 个性化课程讲解文档 | 定制化学习资源 |
| **Reading Agent** | 学术前沿拓展阅读 | 定制化学习资源 |
| **Code Agent** | 可运行代码案例 | 实操技能训练 |
| **Guide Agent** | 岗位实操指南（分步骤+验收标准+行业规范） | 实操指南资源形态 |
| **Quiz Agent** | 分阶测试题（5 种题型、自动评分） | 分阶测试题资源形态 |
| **Video Agent** | 教学视频脚本 + AI 视频 | 多模态资源 |
| **Debate Agent** | 辩论与交叉验证：消除幻觉、知识溯源 | 创新性要求①核心创新点 |
| **Review Agent** | 四维度质量评估（准确性/难度适配/完整性/跨模态一致性） | 生成质量把控 |
| **Adapt Agent** | 动态迭代决策：降维解释 ⇄ 进阶挑战 | 基于反馈的动态迭代机制 |
| **Path Agent** | 知识点图谱 + 拓扑排序学习路径 | 学习路径规划图 |

**实现"分析-生成-校验-决策"协同闭环**，8 种资源形态全覆盖：文档 · 思维导图 · 实操指南 · 练习题 · 拓展阅读 · 视频 · 代码 · 配图

### 🗺️ 个性化学习路径规划
- 基于学生画像的知识点依赖图构建
- 拓扑排序 + 难度梯度排序
- 动态调整：完成节点后自动重新规划
- 每个路径节点包含：知识点、预估学时、难度、前置条件、推荐资源

### 💬 智能辅导（可选加分项）
- 多模态答疑：文字解答 + 图解 + 短视频
- RAG 知识库检索增强（ChromaDB 向量语义匹配）
- 追问式对话，支持上下文记忆

### 📊 学习效果评估（可选加分项）
- 多维度精准评估：知识掌握度、进步趋势、薄弱点变化
- 实时学习行为追踪与画像联动更新
- 基于评估结果动态调整学习路径和推送策略

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                        接入层                                │
│   Vue 2.7 Admin (8006)  │  Nginx + 静态资源 (8010)         │
├─────────────────────────────────────────────────────────────┤
│                        编排层                                │
│   LangGraph Supervisor → 9 Agent 协同工作流                  │
│   Python FastAPI (8100) ← PythonAgentClient → Java Backend  │
├─────────────────────────────────────────────────────────────┤
│                        能力层                                │
│   DeepSeek V4 Pro  │  Agnes Video V2.0  │  Agnes Image 2.1 │
│   ChromaDB (RAG)   │  流式输出 (SSE)     │  七牛云存储      │
├─────────────────────────────────────────────────────────────┤
│                        数据层                                │
│   MySQL 8.0 / H2   │   Redis 缓存    │  本地文件存储        │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈一览

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot + Undertow | 2.1.6 |
| **安全认证** | Spring Security + Sa-Token (JWT) | 1.28.0 |
| **ORM** | MyBatis + PageHelper | — |
| **AI 编排** | LangGraph (Python) | — |
| **向量数据库** | ChromaDB | — |
| **大语言模型** | DeepSeek V4 Pro (OpenAI 兼容) | — |
| **视频生成** | Agnes Video V2.0 | — |
| **图片生成** | Agnes Image 2.1 Flash | — |
| **前端** | Vue 2.7 + Element UI + ECharts 5 | — |
| **数据库** | MySQL 8.0 (生产) / H2 (开发) | — |
| **容器化** | Docker + Docker Compose | — |

---

## 📁 项目结构

```
├── backend/xzs/                     # Java 后端（Spring Boot）
│   ├── src/main/java/com/mindskip/xzs/
│   │   ├── controller/admin/        # 管理端 API 控制器
│   │   ├── service/                 # 业务逻辑层
│   │   │   ├── DeepSeekService.java       # AI 模型调用
│   │   │   ├── AgnesVideoService.java     # AI 视频/图片生成
│   │   │   ├── PythonAgentClient.java     # Java ↔ Python 桥接
│   │   │   ├── MultiAgentService.java     # 多智能体编排
│   │   │   ├── StudentProfileService.java # 学生画像
│   │   │   ├── LearningPathService.java   # 学习路径
│   │   │   ├── KnowledgeBaseService.java  # RAG 知识库
│   │   │   └── ModelConfigService.java    # 模型配置管理
│   │   ├── domain/                  # 实体 & 枚举
│   │   └── repository/             # MyBatis Mapper
│   └── src/main/resources/
│       ├── application.yml          # 主配置（含 RSA 密钥）
│       ├── application-h2.yml       # H2 开发环境
│       └── db/schema-h2.sql         # H2 初始化脚本
│
├── multi-agent/                     # Python 多智能体服务
│   ├── orchestrator/workflow.py     # LangGraph 工作流编排
│   ├── models/schemas.py           # 数据模型定义
│   ├── infrastructure/providers.py  # LLM 后端适配
│   ├── tools/registry.py           # Agent 工具注册
│   ├── config.yaml                 # Agent 路由配置
│   └── main.py                     # FastAPI 入口
│
├── frontend/xzs-admin/              # Vue 管理后台
│   └── src/views/                  # 页面组件
│       ├── dashboard/              # 数据看板
│       ├── settings/               # AI 模型配置
│       ├── profile/                # 学生画像
│       ├── resource/               # 资源生成
│       ├── learning-path/          # 学习路径
│       ├── exam/                   # 考试管理
│       └── knowledge-base/         # 知识库管理
│
├── docker-compose.yml              # 三服务编排（mysql / backend / admin）
├── .env.example                    # 环境变量模板（复制为 .env 后填写密钥）
├── backend/xzs/src/main/resources/db/   # 数据库脚本（schema / 种子数据）
│
├── docs/                           # 项目文档
└── README.md                       # 本文件
```

---

## 🚀 快速启动

### 第 0 步：配置密钥（必读）

仓库**不含任何凭据**，所有密钥通过环境变量注入。先复制模板并填写：

```bash
cp .env.example .env
# 编辑 .env，至少填入 DEEPSEEK_API_KEY；七牛云 QN_ACCESS_KEY / QN_SECRET_KEY 用于文件上传，可留空
```

| 变量 | 用途 | 不填的后果 |
|---|---|---|
| `DEEPSEEK_API_KEY` | 大模型调用 | AI 对话、资源生成、画像抽取全部不可用 |
| `QN_ACCESS_KEY` / `QN_SECRET_KEY` | 七牛云对象存储 | 课件与图片上传接口不可用 |
| `MYSQL_ROOT_PASSWORD` | 数据库口令 | 使用默认值 123456 |

> `.env` 已被 `.gitignore` 忽略，不会误提交；后台「模型配置」页写入的 `model-config.json` 同样不入库。

### 方式一：Docker 一键部署（推荐评委使用）

```bash
cp .env.example .env   # 填入 DEEPSEEK_API_KEY
docker compose up -d
```

访问 http://localhost:8006 （管理端），使用 `admin` / `123456` 登录。

> 前置要求：Docker Desktop 4.x+，确保 3306 / 8001 / 8006 端口未被占用。
> compose 会自动读取根目录 `.env` 并把密钥透传给后端容器。

### 方式二：本地开发启动

**后端（H2 内嵌数据库，零依赖）：**

```bash
cd backend/xzs
mvn clean package -DskipTests
java -jar target/xzs-3.9.0.jar --spring.profiles.active=h2
```

**前端：**

```bash
cd frontend/xzs-admin
npm install
npm run serve
```

访问 http://localhost:8006 （前端开发服务器自动代理 API 到后端 8001）。

### 方式三：完整本地环境（MySQL）

```bash
# 1. 启动 MySQL 8.0，创建数据库 xzs
# 2. 导入建表与种子数据（db 目录下按需导入）
mysql -uroot -p xzs < backend/xzs/src/main/resources/db/ai_schema.sql
# 3. 启动后端（密钥由 .env 注入，见第 0 步）
cd backend/xzs
mvn clean package -DskipTests
java -jar target/xzs-3.9.0.jar --spring.profiles.active=dev
# 4. 启动前端
cd frontend/xzs-admin && npm run serve
```

---

## 📖 配套文档

| 文档 | 说明 |
|------|------|
| 需求功能说明书 | 系统需求分析与功能规划 |
| 概要设计说明书 | 系统架构与模块设计 |
| 详细设计说明书 | 数据库设计、接口定义、核心算法 |
| 部署说明书 | Docker 部署与环境配置 |
| 测试报告 | 功能测试、性能测试结果 |
| 产品设计方案 | UI 设计、交互流程 |

---

## 🎯 赛题对标（XH-202630）

| 赛题要求 | 本项目实现 |
|----------|-----------|
| 垂直领域技能培训场景 + 差异化适配 | ✅ AI 垂直领域知识库（理论/工程实操/大模型应用/行业落地 4 大切片）；12 类差异化学习者画像（在校生/转岗/在职/企业内训） |
| ≥3 个明确分工的智能体，"分析-生成-校验-决策"闭环 | ✅ 14 Agent：Supervisor 编排 + Diagnosis 学情诊断 + 7 资源 Agent + Debate 校验 + Review 审查 + Adapt 决策 |
| 整合先验知识画像与领域知识库 | ✅ 画像含学历背景、岗位方向、理论测试结果；ChromaDB RAG 检索增强生成 |
| ≥3 种形态个性化学习资源 | ✅ 8 种：文档/思维导图/实操指南/分阶测试题/拓展阅读/视频/代码/配图 |
| 可视化学情匹配度报告 + 学习路径规划图 | ✅ 学情诊断报告（盲区定位/难度定标）、ECharts 协作拓扑图、学习路径规划 |
| 基于交互反馈的动态迭代机制 | ✅ 答题正确率 → Adapt Agent 决策"降维解释"或"进阶挑战任务" |
| 辩论与交叉验证消除幻觉 + 高保真知识溯源 | ✅ Debate & Cross-Validation Agent：幻觉四分类识别、辩论裁决、知识库引用溯源 |
| 动态追问与启发式交互导学 | ✅ 多轮追问式对话、SSE 流式输出、上下文记忆 |
| 数据合规与伦理规范 | ✅ 画像数据脱敏存储、密钥环境变量管理、日志不含敏感信息 |

### 📊 量化测试（详见 `docs/量化评测报告.md`）

- **61 条**端到端测试用例 × **14 个**垂直主题 × **12 类**差异化画像（超过评审细则 50 组基线）
- 难度适配判定规则公开可复核；`tests/evaluate_quality.py` 一键复现三项核心指标
- 赛题目标：幻觉率 <5%、画像-难度适配准确率 ≥85%、知识点覆盖率 ≥90%

**实测状态（如实记录，未达标即写未达标）：**

| 指标 | 目标 | 首轮实测（n=3 探针） | 判定 |
|---|---|---|---|
| 画像-资源难度适配准确率 | ≥85% | **33.3%** | ❌ 未达标 |
| 核心知识点覆盖率 | ≥90% | **72.2%** | ❌ 未达标 |
| 幻觉率 | <5% | 0.00% | — 样本过小，不足以判定 |
| 用例-规则一致性（数据集自检） | 100% | 100.0%（n=61） | ✅ |
| 知识库证据支撑率 | 参考 | 98.3%（57/58） | — |

> 首轮实测暴露了**难度定标系统性偏低一档**的缺陷（适配错误全部为"降一级"）。
> 根因与修复方案见 [`docs/难度定标缺陷分析与修复.md`](docs/难度定标缺陷分析与修复.md)：
> ① Supervisor 提示词缺少聚合规则与阈值；② 评测请求未传 `modifiers` 修正项；
> ③ 缺少"主题→关联科目"映射。三项均已修复，**全量 61 条复测后回填真实指标**。

全量并发复测命令（单条约 8–9 分钟，并发 4 约 2–2.5 小时，支持断点续跑）：

```bash
python tests/run_live_parallel.py --all --concurrency 4
```

---


