<template>
  <div class="agent-home">
    <!-- 动态背景 -->
    <div class="bg-grid" />
    <div class="bg-glow glow-1" />
    <div class="bg-glow glow-2" />

    <!-- 顶部状态栏 -->
    <header class="top-bar">
      <div class="status-badge">
        <span class="pulse-dot" />
        <span>AI Engine Active</span>
      </div>
      <div class="tech-tags">
        <span class="tech-tag">DeepSeek V4</span>
        <span class="tech-tag">LangGraph</span>
        <span class="tech-tag">RAG</span>
      </div>
    </header>

    <!-- 标题区域 -->
    <section class="hero">
      <h1 class="hero-title">
        <span class="gradient-text">智能教师</span>
        <span class="sub-text">核心能力矩阵</span>
      </h1>
      <p class="hero-desc">五大 AI 核心引擎，覆盖教学全流程</p>
    </section>

    <!-- 核心功能卡片 - 不对称布局 -->
    <section class="features-grid">
      <!-- 大卡片: AI 对话 -->
      <div
        class="feature-card card-xl"
        style="--accent: #6366f1; --accent-rgb: 99,102,241"
        @click="go('/agent/ai-chat/chat')"
      >
        <div class="card-bg-pattern" />
        <div class="card-content">
          <div class="card-badge">Core Engine</div>
          <div class="card-icon-lg">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M8 10h.01M12 10h.01M16 10h.01"/></svg>
          </div>
          <h2 class="card-title">AI 智能对话</h2>
          <p class="card-desc">基于 DeepSeek 大模型的多轮对话引擎，支持上下文记忆、意图识别与知识引导式问答</p>
          <div class="card-tags">
            <span>多轮对话</span>
            <span>上下文记忆</span>
            <span>流式响应</span>
            <span>知识引导</span>
          </div>
          <div class="card-enter">
            <span>开始对话</span>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
          </div>
        </div>
      </div>

      <!-- 右上: 知识库 RAG -->
      <div
        class="feature-card card-md"
        style="--accent: #10b981; --accent-rgb: 16,185,129"
        @click="go('/agent/knowledge-base/list')"
      >
        <div class="card-bg-pattern" />
        <div class="card-content">
          <div class="card-icon-md">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><path d="M8 7h8M8 11h6"/></svg>
          </div>
          <h3 class="card-title">知识库 RAG</h3>
          <p class="card-desc">本地文档管理 + 向量检索增强生成，精准引用知识源</p>
          <div class="card-tags">
            <span>文档管理</span>
            <span>向量检索</span>
            <span>引用溯源</span>
          </div>
        </div>
      </div>

      <!-- 右下: 教学意图 -->
      <div
        class="feature-card card-md"
        style="--accent: #f59e0b; --accent-rgb: 245,158,11"
        @click="go('/agent/teaching-intent/analyze')"
      >
        <div class="card-bg-pattern" />
        <div class="card-content">
          <div class="card-icon-md">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/><path d="M8 12h8"/></svg>
          </div>
          <h3 class="card-title">教学意图理解</h3>
          <p class="card-desc">深度解析教学目标与学生需求，智能匹配最优教学策略</p>
          <div class="card-tags">
            <span>意图识别</span>
            <span>目标匹配</span>
            <span>策略推荐</span>
          </div>
        </div>
      </div>

      <!-- 左下: 多模态教案 -->
      <div
        class="feature-card card-lg"
        style="--accent: #8b5cf6; --accent-rgb: 139,92,246"
        @click="go('/agent/beike/list')"
      >
        <div class="card-bg-pattern" />
        <div class="card-content">
          <div class="card-icon-md">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
          </div>
          <h3 class="card-title">多模态教案</h3>
          <p class="card-desc">AI 一键生成完整教案，包含教学目标、知识点版块、互动动画与课堂游戏</p>
          <div class="card-tags">
            <span>教案生成</span>
            <span>HTML5 动画</span>
            <span>互动游戏</span>
            <span>多格式导出</span>
          </div>
          <div class="card-enter">
            <span>创建教案</span>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
          </div>
        </div>
      </div>

      <!-- 底部: 学生画像 -->
      <div
        class="feature-card card-xl-bottom"
        style="--accent: #ec4899; --accent-rgb: 236,72,153"
        @click="go('/profile-agent/index')"
      >
        <div class="card-bg-pattern" />
        <div class="card-content">
          <div class="card-badge">8 Dimensions</div>
          <div class="card-icon-lg">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
          <h2 class="card-title">学生画像</h2>
          <p class="card-desc">8 维度动态画像构建 — 知识基础、认知风格、薄弱点、学习目标、学习节奏、兴趣方向、学习风格、动机水平，支持 AI 对话式画像采集与智能解读</p>
          <div class="card-tags">
            <span>8 维画像</span>
            <span>对话式构建</span>
            <span>AI 解读</span>
            <span>版本追踪</span>
          </div>
          <div class="card-enter">
            <span>查看画像</span>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部技术栈 -->
    <footer class="page-footer">
      <span>智慧乐学 · Multi-Agent Architecture · Powered by DeepSeek</span>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'AgentIndex',
  methods: {
    go (path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
.agent-home {
  position: relative;
  min-height: 100vh;
  background: var(--ink-3);
  color: #1e293b;
  padding: 24px 32px 40px;
  overflow: hidden;
}

/* ── 背景装饰 ── */
.bg-grid {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(var(--acid-rgb), 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(var(--acid-rgb), 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  z-index: 0;
}

.bg-glow {
  position: fixed;
  border-radius: 50%;
  filter: blur(120px);
  pointer-events: none;
  z-index: 0;
}

.glow-1 {
  width: 500px;
  height: 500px;
  background: rgba(var(--acid-rgb), 0.04);
  top: -100px;
  right: -100px;
}

.glow-2 {
  width: 400px;
  height: 400px;
  background: rgba(var(--acid-rgb), 0.03);
  bottom: -80px;
  left: -80px;
}

/* ── 顶部状态栏 ── */
.top-bar {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(34,197,94,0.08);
  border: 1px solid rgba(34,197,94,0.2);
  border-radius: 100px;
  font-size: 12px;
  color: #4ade80;
  font-weight: 500;
}

.pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 8px rgba(34,197,94,0.6);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.tech-tags {
  display: flex;
  gap: 8px;
}

.tech-tag {
  padding: 4px 12px;
  font-size: 11px;
  color: var(--grey);
  background: rgba(255,255,255,0.03);
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-family: 'Courier New', monospace;
}

/* ── 标题区域 ── */
.hero {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-bottom: 36px;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.3;
}

.gradient-text {
  background: linear-gradient(135deg, var(--ice), var(--acid), #e879f9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sub-text {
  display: block;
  color: var(--grey);
  font-size: 18px;
  font-weight: 400;
  margin-top: 4px;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  color: var(--grey);
}

/* ── 功能卡片网格 - 不对称布局 ── */
.features-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  grid-template-rows: auto auto auto;
  gap: 18px;
  max-width: 1100px;
  margin: 0 auto;
}

/* AI对话 - 左上大卡片, 占2行 */
.card-xl {
  grid-row: 1 / 3;
}

/* 学生画像 - 右下大卡片, 占2列 */
.card-xl-bottom {
  grid-column: 1 / 3;
}

/* ── 卡片通用 ── */
.feature-card {
  position: relative;
  background: var(--ink-3);
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--accent), transparent);
  opacity: 0;
  transition: opacity 0.4s;
}

.feature-card:hover {
  border-color: rgba(var(--accent-rgb), 0.4);
  transform: translateY(-4px);
  box-shadow:
    0 20px 60px rgba(0,0,0,0.06),
    0 0 40px rgba(var(--accent-rgb), 0.08);
}

.feature-card:hover::before {
  opacity: 1;
}

.card-bg-pattern {
  position: absolute;
  top: -40%;
  right: -20%;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(var(--accent-rgb), 0.06) 0%, transparent 70%);
  pointer-events: none;
}

.card-content {
  position: relative;
  padding: 28px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-badge {
  align-self: flex-start;
  padding: 3px 12px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--accent);
  background: rgba(var(--accent-rgb), 0.1);
  border: 1px solid rgba(var(--accent-rgb), 0.2);
  border-radius: 100px;
  margin-bottom: 16px;
}

.card-icon-lg {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--accent-rgb), 0.1);
  border: 1px solid rgba(var(--accent-rgb), 0.15);
  border-radius: 16px;
  margin-bottom: 18px;
  color: var(--accent);
}

.card-icon-lg svg {
  width: 26px;
  height: 26px;
}

.card-icon-md {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--accent-rgb), 0.1);
  border: 1px solid rgba(var(--accent-rgb), 0.15);
  border-radius: 12px;
  margin-bottom: 14px;
  color: var(--accent);
}

.card-icon-md svg {
  width: 22px;
  height: 22px;
}

.card-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.card-md .card-title {
  font-size: 17px;
}

.card-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--grey);
  line-height: 1.7;
  flex: 1;
}

.card-xl .card-desc {
  font-size: 14px;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.card-tags span {
  padding: 3px 10px;
  font-size: 11px;
  color: rgba(var(--accent-rgb), 0.9);
  background: rgba(var(--accent-rgb), 0.08);
  border: 1px solid rgba(var(--accent-rgb), 0.12);
  border-radius: 100px;
}

.card-enter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: rgba(var(--accent-rgb), 0.15);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  border-radius: 10px;
  align-self: flex-start;
  transition: all 0.3s;
}

.feature-card:hover .card-enter {
  background: rgba(var(--accent-rgb), 0.25);
  border-color: var(--accent);
}

.card-enter svg {
  transition: transform 0.3s;
}

.feature-card:hover .card-enter svg {
  transform: translateX(3px);
}

/* ── 底部 ── */
.page-footer {
  position: relative;
  z-index: 1;
  text-align: center;
  padding-top: 40px;
  font-size: 12px;
  color: var(--grey);
}

/* ── 响应式 ── */
@media (max-width: 900px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
  .card-xl {
    grid-row: auto;
  }
  .card-xl-bottom {
    grid-column: auto;
  }
  .agent-home {
    padding: 16px;
  }
}
</style>
