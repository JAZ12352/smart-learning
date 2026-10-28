<template>
  <div class="dashboard">
    <!-- ═══════ 头部 ═══════ -->
    <header class="sec-head">
      <div class="no">01</div>
      <h1>
        <span class="gl" data-t="OPERATIONS DECK">OPERATIONS DECK</span>
        <em>教学培训 · 实时运行态</em>
      </h1>
      <div class="sub">
        WEEK 42 &nbsp;/&nbsp; CYCLE <b>2026-Q4</b><br>
        UPLINK STABLE &nbsp;/&nbsp; <b>07 AGENTS ONLINE</b>
        <span class="ascii-strip">▓▒░ 0x01F ░▒▓ 01/07</span>
      </div>
    </header>

    <!-- ═══════ KPI ═══════ -->
    <div class="kpi">
      <div v-for="k in kpis" :key="k.en" class="kpi-cell">
        <div class="k">{{ k.cn }}<i>{{ k.en }}</i></div>
        <div class="v">{{ k.v }}<small v-if="k.u">{{ k.u }}</small></div>
        <div class="d" v-html="k.d"></div>
        <div class="spark">
          <svg viewBox="0 0 96 40" preserveAspectRatio="none">
            <polyline :points="sparkPoints(k.s)" fill="none" stroke="rgba(204,255,0,.55)" stroke-width="1" vector-effect="non-scaling-stroke" />
          </svg>
        </div>
      </div>
    </div>

    <!-- ═══════ 主区：吞吐曲线 + 核心仪表 ═══════ -->
    <div class="deck-main">
      <section class="panel">
        <div class="panel-hd">
          <span class="tag">01.1 / THROUGHPUT</span>
          <span class="meta">28 D · ROLLING</span>
        </div>
        <div class="panel-bd chart-bd">
          <div class="legend">
            <span class="a"><i></i>ACTIVE FLOW</span>
            <span class="b"><i></i>BASELINE</span>
            <span class="peak">PEAK <b>2,431</b> / DAY</span>
          </div>
          <svg class="area" viewBox="0 0 1000 240" preserveAspectRatio="none">
            <defs>
              <linearGradient id="fuiArea" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#CCFF00" stop-opacity="0.2" />
                <stop offset="100%" stop-color="#CCFF00" stop-opacity="0" />
              </linearGradient>
            </defs>
            <g stroke="rgba(174,182,171,.13)" stroke-width="1" vector-effect="non-scaling-stroke">
              <line v-for="i in 5" :key="'h' + i" x1="0" :y1="(i - 1) * 60" x2="1000" :y2="(i - 1) * 60" />
            </g>
            <g stroke="rgba(174,182,171,.06)" stroke-width="1" vector-effect="non-scaling-stroke">
              <line v-for="i in 11" :key="'v' + i" :x1="(i - 1) * 100" y1="0" :x2="(i - 1) * 100" y2="240" />
            </g>
            <path :d="areaPath" fill="url(#fuiArea)" />
            <path :d="basePath" fill="none" stroke="rgba(174,182,171,.4)" stroke-width="1" stroke-dasharray="2 3" vector-effect="non-scaling-stroke" />
            <path :d="flowPath" fill="none" stroke="#CCFF00" stroke-width="1.5" vector-effect="non-scaling-stroke" style="filter:drop-shadow(0 0 5px rgba(204,255,0,.55))" />
          </svg>
        </div>
      </section>

      <section class="panel">
        <div class="panel-hd">
          <span class="tag">01.2 / CORE GAUGES</span>
          <span class="meta">LIVE</span>
        </div>
        <div class="panel-bd g3">
          <div v-for="g in gauges" :key="g.label" class="gauge">
            <svg viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="38" fill="none" stroke="rgba(174,182,171,.14)" stroke-width="3"
                stroke-dasharray="178.6 238.8" transform="rotate(135 50 50)" />
              <circle cx="50" cy="50" r="38" fill="none" stroke="#CCFF00" stroke-width="3"
                :stroke-dasharray="g.len + ' 238.8'" transform="rotate(135 50 50)"
                style="filter:drop-shadow(0 0 5px rgba(204,255,0,.6))" />
              <g stroke="rgba(204,255,0,.18)" stroke-width="1">
                <line v-for="t in 30" :key="t" x1="50" y1="6" x2="50" y2="9"
                  :transform="'rotate(' + (135 + (t - 1) * 9) + ' 50 50)'" />
              </g>
              <line x1="50" y1="45" x2="50" y2="55" stroke="rgba(204,255,0,.4)" />
              <line x1="45" y1="50" x2="55" y2="50" stroke="rgba(204,255,0,.4)" />
              <text x="50" y="47" text-anchor="middle" font-size="19" fill="#E8EDE4"
                font-family="Bahnschrift, Arial Narrow, sans-serif" font-weight="700">{{ g.v }}</text>
              <text x="50" y="60" text-anchor="middle" font-size="5" letter-spacing="1.2"
                fill="#868E84" font-family="Consolas, monospace">{{ g.label }}</text>
            </svg>
            <div class="gk">{{ g.cn }}</div>
          </div>
        </div>
      </section>
    </div>

    <!-- ═══════ 功能导航 ═══════ -->
    <div class="sec-label">
      <span>02 / AGENT MODULES</span>
      <em>六大智能体 · 功能导航</em>
    </div>
    <div class="agents-grid">
      <div v-for="(a, i) in agents" :key="a.id" class="agent-card" @click="go(a.route)">
        <span class="ac-no">AGT-0{{ i + 1 }} / ONLINE</span>
        <h3 class="ac-name">{{ a.name }}</h3>
        <p class="ac-desc">{{ a.desc }}</p>
        <span class="ac-go">OPEN ↗</span>
        <span class="ac-strip"></span>
      </div>
    </div>

    <!-- ═══════ 负载 + 事件流 ═══════ -->
    <div class="deck-row">
      <section class="panel">
        <div class="panel-hd">
          <span class="tag">03.1 / AGENT LOAD</span>
          <span class="meta">AVG 72%</span>
        </div>
        <div class="panel-bd loadlist">
          <div v-for="a in loads" :key="a.en" class="bar-row">
            <span class="bk">{{ a.en }}<i>{{ a.cn }}</i></span>
            <span class="bv">{{ a.v }}%</span>
            <span class="bt"><i :style="{ width: a.v + '%' }"></i></span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-hd">
          <span class="tag">03.2 / EVENT STREAM</span>
          <span class="meta">TAIL -F</span>
        </div>
        <div class="panel-bd">
          <div class="term">
            <div v-for="(l, i) in logs" :key="i" class="term-line">
              <span class="t">{{ l.t }}</span>
              <span class="m" v-html="l.m"></span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data () {
    return {
      logTimer: null,
      agents: [
        { id: 'profile', name: '画像构建 Agent', desc: '对话抽取 · 8 维画像 · 动态更新', route: '/profile-agent/index' },
        { id: 'resource', name: '资源生成 Agent', desc: '文档 · 导图 · 题目 · 视频 · 代码', route: '/resource-agent/generate' },
        { id: 'path', name: '路径规划 Agent', desc: '知识图谱 · 顺序编排 · 自适应调整', route: '/learning-path-agent/index' },
        { id: 'qa', name: '辅导答疑 Agent', desc: '知识库 RAG · 多轮对话 · 精准回复', route: '/tutoring-agent/chat' },
        { id: 'teaching', name: 'PPT 课件生成', desc: 'AI 设计 · 多风格 · 一键生成', route: '/resource-agent/ppt-agent' },
        { id: 'eval', name: '学习评估 Agent', desc: '进度追踪 · 效果分析 · 改进建议', route: '/evaluation-agent/report' }
      ],
      kpis: [
        { cn: '在训学员', en: 'ACTIVE TRAINEES', v: '1,284', d: '<b>+38</b> 较上一周期', s: [42, 48, 45, 56, 52, 61, 58, 67, 72, 68, 76, 81] },
        { cn: '智能体会话', en: 'AGENT SESSIONS', v: '24,806', d: '<b>+12.4%</b> 环比', s: [30, 42, 38, 51, 60, 55, 68, 72, 66, 79, 84, 88] },
        { cn: '能力达标率', en: 'COMPETENCY PASS', v: '86.2', u: '%', d: '<b>+2.1</b> pts', s: [55, 58, 62, 60, 66, 70, 68, 74, 78, 80, 84, 86] },
        { cn: '平均编排延迟', en: 'ORCH LATENCY', v: '22', u: 'ms', d: '<b>-4ms</b> 优化', s: [48, 44, 46, 40, 38, 41, 35, 33, 36, 30, 28, 26] }
      ],
      gauges: [
        { v: 94, label: 'COVERAGE', cn: '知识点覆盖率', len: 178.6 * 0.94 },
        { v: 86, label: 'ADAPT', cn: '难度适配准确率', len: 178.6 * 0.86 },
        { v: 71, label: 'COMPLETE', cn: '培训完成率', len: 178.6 * 0.71 }
      ],
      loads: [
        { en: 'PROFILER', cn: '画像构建', v: 72 },
        { en: 'FABRICATOR', cn: '资源生成', v: 88 },
        { en: 'NAVIGATOR', cn: '路径规划', v: 64 },
        { en: 'ORACLE', cn: '辅导答疑', v: 93 },
        { en: 'DECKBUILDER', cn: '课件生成', v: 47 },
        { en: 'ASSESSOR', cn: '学习评估', v: 58 },
        { en: 'ORCHESTRATOR', cn: '协同编排', v: 81 }
      ],
      logs: [],
      series: {
        flow: [1180, 1320, 1250, 1480, 1610, 1520, 1740, 1890, 1760, 2010, 2180, 2060, 2240, 2110, 2330, 2200, 2380, 2260, 2431, 2280, 2160, 2310, 2180, 2260, 2100, 2240, 2310, 2190],
        base: [980, 1020, 990, 1080, 1120, 1090, 1180, 1210, 1170, 1260, 1300, 1270, 1340, 1320, 1390, 1360, 1420, 1400, 1450, 1420, 1380, 1430, 1400, 1440, 1390, 1420, 1450, 1410]
      }
    }
  },
  computed: {
    flowPath () {
      return this.smoothPath(this.series.flow)
    },
    basePath () {
      return this.smoothPath(this.series.base)
    },
    areaPath () {
      return this.flowPath + ' L1000 240 L0 240 Z'
    }
  },
  mounted () {
    this.pushLog(14)
    this.logTimer = setInterval(() => this.pushLog(1), 1600)
  },
  beforeDestroy () {
    if (this.logTimer) clearInterval(this.logTimer)
  },
  methods: {
    go (path) {
      if (path) this.$router.push(path)
    },
    smoothPath (vals) {
      const w = 1000
      const h = 240
      const max = Math.max.apply(null, vals) * 1.08
      const min = Math.min.apply(null, vals) * 0.6
      const pts = vals.map((v, i) => {
        return [i / (vals.length - 1) * w, h - (v - min) / (max - min) * h]
      })
      let d = 'M' + pts[0][0].toFixed(1) + ' ' + pts[0][1].toFixed(1)
      for (let i = 0; i < pts.length - 1; i++) {
        const a = pts[i]
        const b = pts[i + 1]
        const mx = ((a[0] + b[0]) / 2).toFixed(1)
        d += ' C' + mx + ' ' + a[1].toFixed(1) + ',' + mx + ' ' + b[1].toFixed(1) + ',' + b[0].toFixed(1) + ' ' + b[1].toFixed(1)
      }
      return d
    },
    sparkPoints (vals) {
      const max = Math.max.apply(null, vals)
      const min = Math.min.apply(null, vals)
      return vals.map((v, i) => {
        return (i / (vals.length - 1) * 96).toFixed(1) + ',' + (38 - (v - min) / (max - min || 1) * 34).toFixed(1)
      }).join(' ')
    },
    now () {
      const d = new Date()
      const p = n => String(n).padStart(2, '0')
      return p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
    },
    rnd (a, b) {
      return Math.floor(Math.random() * (b - a + 1)) + a
    },
    pushLog (n) {
      const pool = [
        ['ORCH', 'dispatch → <b>ORACLE</b> / task 0x' + this.rnd(10, 99) + 'F'],
        ['PROF', 'vector update · entity <b>T-0' + this.rnd(100, 999) + '</b>'],
        ['NAVI', 'route replanned · hop ' + this.rnd(2, 9)],
        ['FABR', 'resource synthesized · docx · 4.2kb'],
        ['ASSR', 'rubric scored · item C-0' + this.rnd(1, 9) + ' · <b>PASS</b>'],
        ['MESH', 'heartbeat ok · rtt ' + this.rnd(11, 40) + 'ms'],
        ['KB', 'retrieval hit ' + this.rnd(3, 9) + ' chunks'],
        ['SYS', 'retinal layer sync · frame ' + this.rnd(50, 60) + '/60']
      ]
      const now = this.now()
      for (let i = 0; i < n; i++) {
        const l = pool[Math.floor(Math.random() * pool.length)]
        this.logs.push({ t: now, m: '<b>' + l[0] + '</b> ' + l[1] })
      }
      if (this.logs.length > 40) this.logs.splice(0, this.logs.length - 40)
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  min-height: calc(100vh - 92px);
  display: flex;
  flex-direction: column;
  background: transparent;
  color: var(--paper);
  font-family: var(--f-sans);
}

/* ═══ 头部 ═══ */
.sec-head {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 18px 18px 13px 104px;
  border-bottom: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(var(--acid-rgb), 0.035), transparent);
  position: relative;
  overflow: hidden;
}

.sec-head .no {
  position: absolute;
  left: 16px;
  bottom: 11px;
  font-family: var(--f-cond);
  font-size: 50px;
  line-height: 0.8;
  color: transparent;
  -webkit-text-stroke: 1px rgba(var(--acid-rgb), 0.4);
  letter-spacing: -0.02em;
}

.sec-head h1 {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-family: var(--f-cond);
  font-size: clamp(24px, 2.9vw, 40px);
  line-height: 0.86;
  letter-spacing: -0.012em;
  text-transform: uppercase;
  color: var(--paper);
}

.sec-head h1 em {
  display: block;
  margin-top: 7px;
  font-family: var(--f-serif);
  font-style: italic;
  font-weight: 400;
  font-size: 0.46em;
  line-height: 1.2;
  letter-spacing: 0;
  text-transform: none;
  color: var(--acid);
}

.sec-head .sub {
  flex: none;
  text-align: right;
  font-size: 8.5px;
  letter-spacing: 0.2em;
  line-height: 1.9;
  text-transform: uppercase;
  color: var(--grey-3);
}

.sec-head .sub b {
  color: rgba(var(--acid-rgb), 0.55);
  font-weight: 400;
}

.ascii-strip {
  display: block;
  margin-top: 7px;
  font-family: var(--f-mono);
  font-size: 7.5px;
  letter-spacing: 0.22em;
  color: rgba(var(--acid-rgb), 0.3);
}

/* ═══ KPI ═══ */
.kpi {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-bottom: 1px solid var(--line);
}

.kpi-cell {
  position: relative;
  padding: 13px 16px 12px;
  border-right: 1px solid var(--line);
  background: var(--panel);
  overflow: hidden;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.kpi-cell:last-child {
  border-right: 0;
}

.kpi-cell .k {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 8px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--grey-3);
}

.kpi-cell .k i {
  font-style: normal;
  font-family: var(--f-mono);
  font-size: 7px;
  color: rgba(var(--acid-rgb), 0.3);
}

.kpi-cell .v {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 6px 0 3px;
  font-family: var(--f-cond);
  font-size: 36px;
  line-height: 0.9;
  letter-spacing: -0.02em;
  color: var(--paper);
}

.kpi-cell .v small {
  font-size: 11px;
  color: var(--acid);
  letter-spacing: 0.04em;
}

.kpi-cell .d {
  font-size: 8.5px;
  letter-spacing: 0.06em;
  color: var(--grey-2);
}

.kpi-cell .d b {
  color: var(--acid);
  font-weight: 400;
}

.kpi-cell .spark {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 96px;
  height: 40px;
  opacity: 0.8;
}

.kpi-cell .spark svg {
  width: 100%;
  height: 100%;
  display: block;
}

/* ═══ 面板通用 ═══ */
.panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--panel);
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  box-shadow: inset 0 1px 0 rgba(var(--acid-rgb), 0.05);
}

.panel:last-child {
  border-right: 0;
}

.panel-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 26px;
  padding: 7px 10px 6px;
  border-bottom: 1px solid var(--line-soft);
}

.panel-hd .tag {
  display: flex;
  align-items: center;
  gap: 7px;
  font-family: var(--f-mono);
  font-size: 8.5px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(var(--acid-rgb), 0.55);
}

.panel-hd .tag::before {
  content: "";
  width: 3px;
  height: 3px;
  background: var(--ink-3);
  box-shadow: 0 0 6px var(--acid);
}

.panel-hd .meta {
  font-family: var(--f-mono);
  font-size: 8px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--grey-3);
}

.panel-bd {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

/* ═══ 主区 ═══ */
.deck-main {
  display: grid;
  grid-template-columns: 1fr 320px;
  border-bottom: 1px solid var(--line);
}

.chart-bd {
  padding: 8px 8px 0;
  min-height: 258px;
  display: flex;
  flex-direction: column;
}

.legend {
  display: flex;
  gap: 14px;
  align-items: center;
  font-family: var(--f-mono);
  font-size: 8px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--grey-2);
  padding: 2px 6px 6px;
}

.legend i {
  display: inline-block;
  width: 14px;
  height: 1px;
  margin-right: 5px;
  vertical-align: 2px;
}

.legend .a i {
  background: var(--ink-3);
}

.legend .b i {
  background: var(--grey-3);
}

.legend .peak {
  margin-left: auto;
  color: var(--grey-3);
}

.legend .peak b {
  color: var(--acid);
  font-weight: 400;
}

.area {
  width: 100%;
  flex: 1;
  display: block;
  min-height: 0;
}

.g3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}

.gauge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1px solid var(--line-soft);
  padding: 6px;
}

.gauge:last-child {
  border-right: 0;
}

.gauge svg {
  width: 100%;
  max-width: 104px;
}

.gauge .gk {
  margin-top: 2px;
  font-size: 8px;
  letter-spacing: 0.12em;
  color: var(--grey-3);
  text-align: center;
}

/* ═══ 区块标题 ═══ */
.sec-label {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 11px 16px 9px;
  border-bottom: 1px solid var(--line-soft);
  background: linear-gradient(90deg, rgba(var(--acid-rgb), 0.04), transparent 45%);
}

.sec-label span {
  font-family: var(--f-mono);
  font-size: 8.5px;
  letter-spacing: 0.24em;
  color: rgba(var(--acid-rgb), 0.55);
}

.sec-label em {
  font-style: normal;
  font-family: var(--f-sans);
  font-size: 10px;
  letter-spacing: 0.18em;
  color: var(--grey-3);
}

/* ═══ 功能卡 ═══ */
.agents-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-bottom: 1px solid var(--line);
}

.agent-card {
  position: relative;
  padding: 12px 15px 13px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line-soft);
  background: var(--panel);
  cursor: pointer;
  transition: background 0.16s;
  overflow: hidden;
}

.agent-card:nth-child(3n) {
  border-right: 0;
}

.agent-card:hover {
  background: rgba(var(--acid-rgb), 0.055);
}

.agent-card .ac-no {
  font-family: var(--f-mono);
  font-size: 8px;
  letter-spacing: 0.2em;
  color: var(--grey-3);
}

.agent-card .ac-name {
  margin: 6px 0 4px;
  font-family: var(--f-cond);
  font-size: 19px;
  line-height: 1;
  letter-spacing: 0.02em;
  color: var(--paper);
  transition: color 0.16s;
}

.agent-card:hover .ac-name {
  color: var(--acid);
}

.agent-card .ac-desc {
  margin: 0;
  font-size: 10px;
  line-height: 1.6;
  color: var(--grey-2);
}

.agent-card .ac-go {
  display: inline-block;
  margin-top: 7px;
  font-family: var(--f-mono);
  font-size: 8px;
  letter-spacing: 0.2em;
  color: rgba(var(--acid-rgb), 0.45);
  transition: color 0.16s;
}

.agent-card:hover .ac-go {
  color: var(--acid);
}

.agent-card .ac-strip {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--ink-3);
  opacity: 0;
  transition: opacity 0.16s;
  box-shadow: 0 0 12px var(--acid);
}

.agent-card:hover .ac-strip {
  opacity: 1;
}

/* ═══ 负载 + 日志 ═══ */
.deck-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  flex: 1;
  min-height: 210px;
}

.deck-row .panel {
  border-bottom: 0;
}

.loadlist {
  padding: 8px 0;
}

.bar-row {
  display: grid;
  grid-template-columns: 1fr 44px;
  gap: 6px;
  padding: 4px 14px;
  align-items: center;
}

.bar-row .bk {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-family: var(--f-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--grey);
  white-space: nowrap;
  overflow: hidden;
}

.bar-row .bk i {
  font-style: normal;
  font-family: var(--f-sans);
  font-size: 8px;
  color: var(--grey-3);
}

.bar-row .bv {
  text-align: right;
  font-family: var(--f-mono);
  font-size: 9px;
  color: rgba(var(--acid-rgb), 0.8);
}

.bar-row .bt {
  grid-column: 1 / -1;
  height: 1px;
  background: var(--line-soft);
  position: relative;
}

.bar-row .bt i {
  position: absolute;
  left: 0;
  top: 0;
  height: 1px;
  background: var(--ink-3);
  box-shadow: 0 0 6px rgba(var(--acid-rgb), 0.6);
}

.term {
  height: 100%;
  overflow: hidden;
  padding: 8px 10px;
  font-family: var(--f-mono);
  font-size: 9.5px;
  line-height: 1.68;
}

.term-line {
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: 7px;
  white-space: nowrap;
  overflow: hidden;
}

.term-line .t {
  color: #2e342e;
}

.term-line .m {
  color: var(--grey-2);
  overflow: hidden;
  text-overflow: ellipsis;
}

.term-line .m b {
  color: rgba(var(--acid-rgb), 0.8);
  font-weight: 400;
}

/* ═══ 响应式 ═══ */
@media (max-width: 1280px) {
  .kpi {
    grid-template-columns: repeat(2, 1fr);
  }

  .kpi-cell:nth-child(2n) {
    border-right: 0;
  }

  .deck-main {
    grid-template-columns: 1fr;
  }

  .agents-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .agent-card:nth-child(3n) {
    border-right: 1px solid var(--line);
  }

  .agent-card:nth-child(2n) {
    border-right: 0;
  }

  .deck-row {
    grid-template-columns: 1fr;
  }
}
</style>
