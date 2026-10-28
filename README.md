# 🎓 智慧乐学 — 领域知识个性化生成与多智能体协同决策系统

基于大模型与多智能体协同决策技术，面向**垂直领域技能培训**场景（人工智能、智能制造、工业互联网等），构建"学情画像构建 → 多智能体协同调度（诊断/生成/校验）→ 领域知识个性化生成 → 交互反馈 → 动态决策更新"全流程闭环系统，服务于应用型工程技术人才、高技能人才培养与企业内训/转岗培训。

<p align="center">
  <strong>Spring Boot + Vue 2 + FastAPI + LangGraph + ChromaDB + DeepSeek</strong>
</p>

---

## ✨ 核心功能

### 🧠 对话式学习画像自主构建（先验知识画像）
摒弃传统表单，通过自然语言对话自动抽取多维度特征（含先验画像字段）：
- **学历背景 / 岗位方向** — 在校生、在职工程师、转岗人员、企业内训学员等职业身份识别
- **理论测试结果** — 各科目掌握程度（0-100 分），作为个性化适配的量化输入
- **认知风格** — 视觉型/听觉型/动觉型/读写型
- **薄弱点** — 具体知识点缺陷（含严重程度）
- **学习目标** — 就业/考研/转岗上岗/技能认证等
- **学习节奏 / 兴趣偏好 / 学习风格 / 动机水平**

画像支持"随学随新"动态更新，每次对话自动抽取增量特征。

### 🤖 多智能体协同决策（14 Agent 全闭环）

| Agent | 职责 | 对应能力 |
|-------|------|---------|
| **Supervisor** | 任务分解、Agent 路由、执行编排 | 多智能体协同调度 |
| **Diagnosis Agent** | 学情诊断：锚定理论强项与技能盲区 | 学情诊断与角色分工 |
| **Profile Agent** | 对话式画像构建与动态更新 | 先验知识画像整合 |
| **MindMap Agent** | 知识结构先行，供全链路复用 | 协同生成机制 |
| **Doc Agent** | 个性化课程讲解文档 | 定制化学习资源 |
| **Reading Agent** | 学术前沿拓展阅读 | 定制化学习资源 |
| **Code Agent** | 可运行代码案例 | 实操技能训练 |
| **Guide Agent** | 岗位实操指南（分步骤+验收标准+行业规范） | 实操指南资源形态 |
| **Quiz Agent** | 分阶测试题（5 种题型、自动评分） | 分阶测试题资源形态 |
| **Video Agent** | 教学视频脚本 + AI 视频 | 多模态资源 |
| **Debate Agent** | 辩论与交叉验证：消除幻觉、知识溯源 | 多智能体交叉验证机制 |
| **Review Agent** | 四维度质量评估（准确性/难度适配/完整性/跨模态一致性） | 生成质量把控 |
| **Adapt Agent** | 动态迭代决策：降维解释 ⇄ 进阶挑战 | 基于反馈的动态迭代机制 |
| **Path Agent** | 知识点图谱 + 拓扑排序学习路径 | 学习路径规划图 |

**实现"分析-生成-校验-决策"协同闭环**，8 种资源形态全覆盖：文档 · 思维导图 · 实操指南 · 练习题 · 拓展阅读 · 视频 · 代码 · 配图

### 🗺️ 个性化学习路径规划
- 基于学生画像的知识点依赖图构建
- 拓扑排序 + 难度梯度排序
- 动态调整：完成节点后自动重新规划
- 每个路径节点包含：知识点、预估学时、难度、前置条件、推荐资源

### 💬 智能辅导
- 多模态答疑：文字解答 + 图解 + 短视频
- RAG 知识库检索增强（ChromaDB 向量语义匹配）
- 追问式对话，支持上下文记忆

### 📊 学习效果评估
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
├── scripts/                        # 运维与数据脚本
│   ├── import_kb.sh                # 知识库素材批量导入
│   ├── sync_kb_fallback.py         # 知识库回退数据同步
│   ├── deploy-on-server.sh         # 服务器端一键部署
│   └── ecs_bootstrap.sh            # 云主机环境引导（Docker 等）
│
├── docker-compose.yml              # 三服务编排（mysql / backend / admin）
├── .env.example                    # 环境变量模板（复制为 .env 后填写密钥）
├── backend/xzs/src/main/resources/db/   # 数据库脚本（schema / 种子数据）
│
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

### 方式一：Docker 一键部署（推荐）

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
