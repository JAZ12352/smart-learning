<template>
  <div class="exp-report">
    <!-- ── Hero ── -->
    <div class="exp-hero">
      <div class="hero-glow hero-glow-1"></div>
      <div class="hero-glow hero-glow-2"></div>
      <div class="hero-grid"></div>
      <div class="exp-hero-inner">
        <div class="exp-hero-badge">
          <span class="badge-dot"></span> 可复现实验报告 · Experiment Report
        </div>
        <h1 class="exp-hero-title">{{ report.title || '可复现实验报告' }}</h1>
        <p class="exp-hero-sub">
          多维度量化评测体系，覆盖知识准确性、难度适配性与内容完整性，
          为系统迭代提供数据支撑。
        </p>
        <div class="exp-hero-meta" v-if="report.generatedAt">
          <i class="el-icon-time"></i> 报告生成时间：{{ report.generatedAt }}
          <span class="meta-sep">·</span>
          <i class="el-icon-cpu"></i> 数据来源：系统运行态实时汇聚
        </div>
        <div class="exp-hero-actions">
          <el-button class="run-btn" :loading="running" @click="runExperiment">
            <i v-if="!running" class="el-icon-video-play"></i>
            {{ running ? '评测运行中…' : (metrics.length ? '重新运行可复现评测' : '运行可复现评测') }}
          </el-button>
          <span class="run-hint" v-if="running">正在逐条核验样本事实性并判定难度档位，20 个样本约需 8~10 分钟…</span>
        </div>
      </div>
    </div>

    <div class="exp-body" v-if="report.knowledgeBase || report.profiles">
      <!-- ── 评测运行中 ── -->
      <div class="conclusion running" v-if="running">
        <div class="conclusion-icon"><i class="el-icon-loading"></i></div>
        <div class="conclusion-text">
          <div class="conclusion-title">评测运行中</div>
          <div class="conclusion-sub">正在对抽样资源逐条做事实性核验与难度档位判定，20 个样本约需 8~10 分钟，请勿关闭页面。</div>
        </div>
      </div>

      <!-- ── 运行评测 CTA（未评测时） ── -->
      <div class="conclusion run-pending" v-else-if="!metrics.length">
        <div class="conclusion-icon"><i class="el-icon-video-play"></i></div>
        <div class="conclusion-text">
          <div class="conclusion-title">核心评测指标尚未运行</div>
          <div class="conclusion-sub">点击「运行可复现评测」，系统将对大模型生成资源做事实性核验与难度匹配，输出实测值。</div>
        </div>
        <el-button class="run-inline-btn" @click="runExperiment">运行可复现评测</el-button>
      </div>

      <!-- ── 评测结论（已评测时） ── -->
      <div class="conclusion" :class="{ pass: allPass }" v-else>
        <div class="conclusion-icon"><i :class="allPass ? 'el-icon-circle-check' : 'el-icon-warning-outline'"></i></div>
        <div class="conclusion-text">
          <div class="conclusion-title">{{ allPass ? '评测结论：全部核心指标达标' : '评测结论：部分指标未达标' }}</div>
          <div class="conclusion-sub">
            核心指标 {{ passCount }}/{{ metrics.length }} 项达到验收阈值。
            <template v-if="runResult">
              （样本 {{ runResult.sampleCount }} 个<template v-if="runResult.matchedTotal && runResult.matchedTotal !== runResult.sampleCount"> · 难度匹配有效样本 {{ runResult.matchedTotal }} 个</template><template v-if="runResult.judgeFailedCount"> · 判定失败 {{ runResult.judgeFailedCount }} 条</template> · 学习者档位「{{ runResult.learnerTier }}」 · 耗时 {{ runResult.durationMs }}ms）
            </template>
          </div>
        </div>
        <div class="conclusion-tags">
          <span v-for="m in metrics" :key="m.key" class="ct-tag" :class="{ ok: m.pass }">
            {{ m.name }} {{ m.value }}{{ m.unit }}
          </span>
        </div>
      </div>

      <!-- ── 可靠性告警（判定大面积失败 / 画像缺失等） ── -->
      <div class="exp-alert" v-if="warning">
        <i class="el-icon-warning-outline"></i>
        <span>{{ warning }}</span>
      </div>

      <!-- ── 核心评测指标 ── -->
      <div class="exp-section" v-if="metrics.length">
        <div class="section-head">
          <div>
            <h2 class="section-title"><i class="el-icon-odometer"></i> 核心评测指标</h2>
            <p class="section-sub">目标值为验收阈值，实测值由大模型评测引擎实时汇聚</p>
          </div>
        </div>

        <div class="metric-grid">
          <div v-for="m in metrics" :key="m.key" class="metric-card" :class="{ pass: m.pass }">
            <div class="metric-ring" :style="ringStyle(m)">
              <div class="metric-ring-inner">
                <span class="metric-ring-value">{{ m.value }}<small>{{ m.unit }}</small></span>
              </div>
            </div>
            <div class="metric-info">
              <div class="metric-name">{{ m.name }}</div>
              <div class="metric-target">目标 {{ targetLabel(m) }}</div>
              <div class="metric-method">{{ m.method }}</div>
            </div>
            <div class="metric-pass" :class="{ ok: m.pass }">
              <i :class="m.pass ? 'el-icon-circle-check' : 'el-icon-circle-close'"></i>
              {{ m.pass ? '达标' : '未达标' }}
            </div>
          </div>
        </div>
      </div>

      <!-- ── 验收指标对照表 ── -->
      <div class="exp-section" v-if="metrics.length">
        <div class="section-head">
          <div>
            <h2 class="section-title"><i class="el-icon-document-checked"></i> 验收指标对照</h2>
            <p class="section-sub">量化验收阈值 vs 系统实测值</p>
          </div>
        </div>
        <div class="criteria-table">
          <div class="ct-row ct-head">
            <div class="ct-cell ct-name">评测指标</div>
            <div class="ct-cell">目标值</div>
            <div class="ct-cell">实测值</div>
            <div class="ct-cell">结论</div>
          </div>
          <div v-for="m in metrics" :key="'r-' + m.key" class="ct-row">
            <div class="ct-cell ct-name">{{ m.name }}</div>
            <div class="ct-cell">{{ targetLabel(m) }}</div>
            <div class="ct-cell ct-value" :class="{ ok: m.pass }">{{ m.value }}{{ m.unit }}</div>
            <div class="ct-cell">
              <span class="ct-badge" :class="{ ok: m.pass }">
                <i :class="m.pass ? 'el-icon-circle-check' : 'el-icon-circle-close'"></i>
                {{ m.pass ? '达标' : '未达标' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- ── 评测明细（LLM-as-Judge 逐条核验结果） ── -->
      <div class="exp-section" v-if="runResult && runResult.details && runResult.details.length">
        <div class="section-head">
          <div>
            <h2 class="section-title"><i class="el-icon-view"></i> 评测明细</h2>
            <p class="section-sub">LLM-as-Judge 对每条抽样资源的逐条核验结果（结合 RAG 检索的权威知识库）</p>
          </div>
          <span class="path-badge">样本 {{ runResult.sampleCount }} 条<template v-if="tierSummary"> · 各画像档位 {{ tierSummary }}</template></span>
        </div>
        <div class="detail-list">
          <div v-for="(d, di) in runResult.details" :key="di" class="detail-item">
            <div class="detail-head">
              <span class="detail-idx">{{ String(di + 1).padStart(2, '0') }}</span>
              <span class="detail-title">{{ d.title }}</span>
              <span class="detail-difficulty">难度：{{ d.difficulty }}<template v-if="d.expectedTier"> · 目标 {{ d.expectedTier }}</template></span>
              <span class="detail-verdict" :class="{ ok: d.accurate }">
                <i :class="d.accurate ? 'el-icon-circle-check' : 'el-icon-circle-close'"></i>
                {{ d.accurate ? '事实准确' : '发现错误' }}
              </span>
            </div>
            <div class="detail-reason" v-if="d.reason">{{ d.reason }}</div>
          </div>
        </div>
      </div>

      <!-- ── 判定可靠性自检（负样本探针） ── -->
      <div class="exp-section" v-if="runResult && runResult.negativeControls">
        <div class="section-head">
          <div>
            <h2 class="section-title"><i class="el-icon-aim"></i> 判定可靠性自检</h2>
            <p class="section-sub">向同一条判定链路混入已知含公认事实错误的内容（负样本探针），检验 Judge 能否识破；该环节不计入知识准确率</p>
          </div>
          <span class="path-badge" :class="{ warn: !probeHealthy }">
            检出率 {{ runResult.negativeControls.recall }}% · 下限 {{ runResult.negativeControls.floor }}%
          </span>
        </div>
        <div class="detail-list">
          <div v-for="(p, pi) in runResult.negativeControls.items" :key="'p-' + pi" class="detail-item">
            <div class="detail-head">
              <span class="detail-idx">{{ String(pi + 1).padStart(2, '0') }}</span>
              <span class="detail-title">{{ p.title }}</span>
              <span class="detail-verdict" :class="{ ok: p.caught }">
                <i :class="p.caught ? 'el-icon-circle-check' : 'el-icon-circle-close'"></i>
                {{ p.caught ? '已识破' : '漏检' }}
              </span>
            </div>
            <div class="detail-reason" v-if="p.reason">{{ p.reason }}</div>
          </div>
        </div>
      </div>

      <!-- ── 垂直领域知识库 ── -->
      <div class="exp-section" v-if="report.knowledgeBase">
        <div class="section-head">
          <div>
            <h2 class="section-title"><i class="el-icon-collection"></i> 垂直领域知识库</h2>
            <p class="section-sub">RAG 检索增强生成的专业知识底座</p>
          </div>
        </div>
        <div class="kb-card">
          <div class="kb-head">
            <div class="kb-icon"><i class="el-icon-cpu"></i></div>
            <div class="kb-title-wrap">
              <div class="kb-title">{{ report.knowledgeBase.name }}</div>
              <div class="kb-desc">{{ report.knowledgeBase.description }}</div>
            </div>
            <div class="kb-count">
              <span class="kb-count-num">{{ report.knowledgeBase.docCount }}</span>
              <span class="kb-count-label">文本块</span>
            </div>
          </div>
          <div class="kb-meta">
            <span class="kb-meta-item"><b>向量库</b>{{ report.knowledgeBase.vectorDb }}</span>
            <span class="kb-meta-item"><b>集合</b>{{ report.knowledgeBase.collection }}</span>
            <span class="kb-meta-item"><b>领域</b>{{ report.knowledgeBase.domain }}</span>
          </div>
          <div class="kb-domains">
            <span class="kb-domains-label">覆盖子领域：</span>
            <el-tag v-for="d in report.knowledgeBase.subDomains" :key="d" size="small" type="info" class="kb-domain-tag">{{ d }}</el-tag>
          </div>
        </div>
      </div>

      <!-- ── 学习者画像 ── -->
      <div class="exp-section" v-if="report.profiles && report.profiles.length">
        <div class="section-head">
          <div>
            <h2 class="section-title"><i class="el-icon-user"></i> 差异化学习者画像</h2>
            <p class="section-sub">{{ report.profiles.length }} 组不同学习者的画像演示数据</p>
          </div>
        </div>
        <div class="profile-grid">
          <div v-for="(p, pi) in report.profiles" :key="p.userId" class="profile-card">
            <div class="profile-head">
              <div class="profile-avatar" :class="'pa-' + pi">{{ p.label }}</div>
              <div class="profile-meta">
                <div class="profile-label">{{ p.label }}</div>
                <div class="profile-uid">user_id: {{ p.userId }}</div>
              </div>
              <div class="profile-motivation" v-if="p.motivationLevel">
                <span class="motiv-num">{{ p.motivationLevel }}</span>
                <span class="motiv-label">动机</span>
              </div>
            </div>
            <div class="profile-fields">
              <div class="pf-row"><span class="pf-key">知识基础</span><span class="pf-val">{{ p.knowledgeLevel || '—' }}</span></div>
              <div class="pf-row"><span class="pf-key">认知风格</span><span class="pf-val">{{ p.cognitiveStyle || '—' }}</span></div>
              <div class="pf-row"><span class="pf-key">薄弱点</span><span class="pf-val">{{ p.weakPoints || '—' }}</span></div>
              <div class="pf-row"><span class="pf-key">学习目标</span><span class="pf-val">{{ p.learningGoals || '—' }}</span></div>
              <div class="pf-row"><span class="pf-key">学习节奏</span><span class="pf-val">{{ p.learningPace || '—' }}</span></div>
              <div class="pf-row"><span class="pf-key">学习风格</span><span class="pf-val">{{ p.learningStyle || '—' }}</span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── 学习路径 + 评测方法 ── -->
      <div class="exp-section" v-if="report.learningPaths || report.methodology">
        <div class="section-head">
          <div>
            <h2 class="section-title"><i class="el-icon-document"></i> 评测方法说明</h2>
            <p class="section-sub">可复现的评测口径</p>
          </div>
          <span class="path-badge" v-if="report.learningPaths">个性化学习路径 {{ report.learningPaths.total }} 条</span>
        </div>
        <div class="method-card">
          <pre class="method-text">{{ report.methodology }}</pre>
        </div>
      </div>
    </div>

    <div class="exp-loading" v-else>
      <i class="el-icon-loading"></i> 正在加载实验报告…
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'ExperimentReport',
  data () {
    return {
      report: {},
      running: false,
      runResult: null
    }
  },
  computed: {
    metrics () {
      return (this.runResult && this.runResult.metrics) || this.report.metrics || []
    },
    allPass () {
      if (!this.metrics.length) return false
      return this.metrics.every(function (m) { return m.pass })
    },
    passCount () {
      return this.metrics.filter(function (m) { return m.pass }).length
    },
    /** 后端下发的可靠性告警（判定大面积失败 / 画像缺失），必须显式告知用户 */
    warning () {
      return (this.runResult && this.runResult.warning) || ''
    },
    /** 各学习者画像的档位分布，如「user 2：基础 · user 3：进阶 · user 4：高级」 */
    tierSummary () {
      var t = this.runResult && this.runResult.tierByUser
      if (!t) return ''
      var parts = []
      Object.keys(t).forEach(function (k) { parts.push('user ' + k + '：' + t[k]) })
      return parts.join(' · ')
    },
    /** 负样本探针检出率是否达到下限：未达标说明 Judge 偏松，准确率不宜采信 */
    probeHealthy () {
      var nc = this.runResult && this.runResult.negativeControls
      if (!nc) return true
      return nc.recall >= nc.floor
    }
  },
  mounted () { this.load() },
  methods: {
    async load () {
      try {
        var res = await request({ url: '/api/admin/evaluation/experiment-report', method: 'get' })
        if (res && res.code === 1) this.report = res.response || {}
      } catch (e) {
        this.$message && this.$message.error('实验报告加载失败')
      }
    },
    async runExperiment () {
      if (this.running) return
      this.running = true
      this.runResult = null
      try {
        // LLM-as-Judge 逐条实测约需 20s/样本，20 样本约 8-10 分钟，超时设 30 分钟
        var res = await request({ url: '/api/admin/evaluation/run-experiment', method: 'post', timeout: 1800000 })
        if (res && res.code === 1) {
          this.runResult = res.response || {}
          this.$message && this.$message.success('评测完成：' + (this.runResult.sampleCount || 0) + ' 个样本，耗时 ' + (this.runResult.durationMs || 0) + 'ms')
        } else {
          this.$message && this.$message.error('评测失败：' + (res && res.message))
        }
      } catch (e) {
        var isTimeout = e && (e.code === 'ECONNABORTED' || /timeout/i.test(e.message || ''))
        this.$message && this.$message.error(isTimeout ? '评测耗时超过预期（20 样本实测约需 8-10 分钟），请确认网络与模型配置后重试' : '评测失败，请稍后重试')
      } finally {
        this.running = false
      }
    },
    ringStyle (m) {
      // 环表示「目标达成度」，需区分正向与逆向指标：
      // 逆向指标（如幻觉率）越低越好，若沿用 value/target，满分 0% 会画成空环，看起来像没完成
      var pct
      if (m.direction === 'lower_better') {
        pct = m.value <= 0 ? 100 : Math.min((m.target / m.value) * 100, 100)
      } else {
        pct = Math.min((m.value / (m.target || 100)) * 100, 100)
      }
      var color = m.pass ? '#10b981' : '#ef4444'
      return { background: 'conic-gradient(' + color + ' ' + pct + '%, rgba(232,238,247,0.6) 0)' }
    },
    targetLabel (m) {
      // 由后端 direction 字段决定比较符号，避免硬编码 key 导致新增指标时口径写反
      var sym = m.direction === 'lower_better' ? '≤ ' : '≥ '
      return sym + m.target + m.unit
    }
  }
}
</script>

<style scoped>
.exp-report { min-height: 100vh; background: var(--ink-3); color: var(--grey); }

/* ── Hero ── */
.exp-hero {
  background: linear-gradient(165deg, var(--ink) 0%, #16284a 35%, #1a3055 60%, #15263f 100%);
  padding: 64px 32px 88px; position: relative; overflow: hidden;
}
.hero-glow {
  position: absolute; border-radius: 50%; pointer-events: none;
}
.hero-glow-1 {
  top: -30%; right: -8%; width: 500px; height: 500px;
  background: radial-gradient(circle, rgba(var(--acid-rgb), 0.22), transparent 65%);
}
.hero-glow-2 {
  bottom: -40%; left: -10%; width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(var(--acid-rgb), 0.15), transparent 65%);
}
.hero-grid {
  position: absolute; inset: 0; opacity: 0.04;
  background-image:
    linear-gradient(rgba(255,255,255,0.5) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.5) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 80%);
}
.exp-hero-inner { max-width: 1080px; margin: 0 auto; position: relative; }
.exp-hero-badge {
  display: inline-flex; align-items: center; gap: 9px; padding: 7px 18px; border-radius: 100px;
  background: rgba(var(--acid-rgb), 0.14); border: 1px solid rgba(var(--ice-rgb), 0.28); margin-bottom: 22px;
  color: var(--ice); font-size: 13px; font-weight: 600; letter-spacing: 1px;
  backdrop-filter: blur(8px);
}
.badge-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #22c55e;
  box-shadow: 0 0 8px rgba(34,197,94,0.6);
}
.exp-hero-title {
  font-size: 34px; font-weight: 800; color: #fff; margin: 0 0 16px; letter-spacing: 0.5px;
  background: linear-gradient(135deg, #fff 0%, #cbd5e1 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.exp-hero-sub {
  font-size: 14.5px; color: rgba(148,163,184,0.9); line-height: 1.9; max-width: 720px; margin: 0 0 22px;
}
.exp-hero-meta { font-size: 13px; color: rgba(148,163,184,0.6); }
.exp-hero-meta i { margin-right: 4px; }
.meta-sep { margin: 0 12px; color: rgba(148,163,184,0.3); }

/* ── Body ── */
.exp-body { max-width: 1080px; margin: -48px auto 0; padding: 0 24px 70px; position: relative; }
.exp-section { margin-bottom: 36px; }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.section-title { font-size: 18px; font-weight: 700; color: var(--grey); margin: 0; }
.section-title i { color: var(--acid); margin-right: 8px; }
.section-sub { font-size: 13px; color: #94a3b8; margin: 5px 0 0; }

/* ── Conclusion ── */
.conclusion {
  display: flex; align-items: center; gap: 18px; flex-wrap: wrap;
  background: #fff; border: 1px solid #e9edf4; border-radius: 18px; padding: 22px 26px; margin-bottom: 32px;
  border-left: 4px solid #10b981;
  box-shadow: 0 2px 12px rgba(21,34,56,0.04);
}
.conclusion-icon {
  width: 48px; height: 48px; border-radius: 14px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--ink-3); color: #059669; font-size: 24px;
}
.conclusion-text { flex: 1; min-width: 0; }
.conclusion-title { font-size: 16px; font-weight: 700; color: var(--grey); }
.conclusion-sub { font-size: 13px; color: #64748b; margin-top: 4px; }
.conclusion-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.ct-tag { padding: 4px 12px; border-radius: 100px; font-size: 12px; font-weight: 600; background: var(--ink-3); color: #ef4444; }
.ct-tag.ok { background: var(--ink-3); color: #059669; }

/* ── Metrics ── */
.metric-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 18px; }
.metric-card {
  display: flex; align-items: center; gap: 20px;
  background: #fff; border: 1px solid #e9edf4; border-radius: 18px; padding: 24px;
  position: relative; transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(21,34,56,0.04);
}
.metric-card:hover {
  box-shadow: 0 20px 40px rgba(21,34,56,0.1);
  transform: translateY(-2px);
}
.metric-card.pass { border-left: 4px solid #10b981; }
.metric-ring {
  width: 100px; height: 100px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.4s ease;
}
.metric-card:hover .metric-ring { transform: scale(1.05); }
.metric-ring-inner {
  width: 76px; height: 76px; border-radius: 50%; background: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: inset 0 2px 6px rgba(21,34,56,0.08);
}
.metric-ring-value { font-size: 24px; font-weight: 800; color: var(--grey); }
.metric-ring-value small { font-size: 11px; color: #94a3b8; font-weight: 600; margin-left: 1px; }
.metric-info { flex: 1; min-width: 0; }
.metric-name { font-size: 15px; font-weight: 700; color: var(--grey); margin-bottom: 4px; }
.metric-target { font-size: 12px; color: #94a3b8; margin-bottom: 8px; }
.metric-method { font-size: 12px; color: #64748b; line-height: 1.6; }
.metric-pass { position: absolute; top: 16px; right: 16px; font-size: 12px; font-weight: 700; padding: 3px 10px; border-radius: 100px; background: var(--ink-3); color: #ef4444; }
.metric-pass.ok { background: var(--ink-3); color: #059669; }

/* ── Criteria table ── */
.criteria-table {
  background: #fff; border: 1px solid #e9edf4; border-radius: 18px; overflow: hidden;
  box-shadow: 0 2px 12px rgba(21,34,56,0.04);
}
.ct-row { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; padding: 15px 24px; border-bottom: 1px solid #f1f5f9; align-items: center; transition: background 0.2s; }
.ct-row:not(.ct-head):hover { background: var(--ink-3); }
.ct-row:last-child { border-bottom: none; }
.ct-head { background: var(--ink-3); font-size: 13px; font-weight: 700; color: #64748b; }
.ct-cell { font-size: 13.5px; color: var(--grey); }
.ct-name { font-weight: 600; color: var(--grey); }
.ct-value { font-weight: 700; }
.ct-value.ok { color: #059669; }
.ct-badge { display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px; border-radius: 100px; font-size: 12px; font-weight: 700; background: var(--ink-3); color: #ef4444; }
.ct-badge.ok { background: var(--ink-3); color: #059669; }

/* ── Knowledge base ── */
.kb-card {
  background: #fff; border: 1px solid #e9edf4; border-radius: 18px; padding: 26px;
  box-shadow: 0 2px 12px rgba(21,34,56,0.04);
}
.kb-head { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; }
.kb-icon {
  width: 54px; height: 54px; border-radius: 16px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--acid), var(--ice)); color: #fff; font-size: 26px;
  box-shadow: 0 8px 20px rgba(var(--acid-rgb), 0.3);
}
.kb-title-wrap { flex: 1; min-width: 0; }
.kb-title { font-size: 16px; font-weight: 700; color: var(--grey); margin-bottom: 4px; }
.kb-desc { font-size: 12.5px; color: #64748b; line-height: 1.6; }
.kb-count { text-align: center; flex-shrink: 0; }
.kb-count-num { display: block; font-size: 32px; font-weight: 800; color: var(--acid); line-height: 1; }
.kb-count-label { font-size: 12px; color: #94a3b8; }
.kb-meta { display: flex; gap: 24px; flex-wrap: wrap; padding: 14px 0; border-top: 1px solid #f1f5f9; border-bottom: 1px solid #f1f5f9; margin-bottom: 14px; }
.kb-meta-item { font-size: 12.5px; color: #64748b; }
.kb-meta-item b { color: var(--grey); margin-right: 6px; }
.kb-domains { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.kb-domains-label { font-size: 12.5px; color: #64748b; font-weight: 600; }
.kb-domain-tag { margin: 0; }

/* ── Profiles ── */
.profile-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.profile-card {
  background: #fff; border: 1px solid #e9edf4; border-radius: 18px; padding: 22px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(21,34,56,0.04);
}
.profile-card:hover {
  box-shadow: 0 16px 36px rgba(21,34,56,0.1);
  transform: translateY(-2px);
}
.profile-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.profile-avatar {
  width: 46px; height: 46px; border-radius: 14px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 13px; font-weight: 700;
  background: linear-gradient(135deg, var(--acid), var(--ice));
  box-shadow: 0 6px 16px rgba(var(--acid-rgb), 0.25);
}
.profile-avatar.pa-1 { background: linear-gradient(135deg, var(--ice), var(--acid)); box-shadow: 0 6px 16px rgba(var(--ice-rgb), 0.25); }
.profile-avatar.pa-2 { background: linear-gradient(135deg, var(--acid), var(--ice)); box-shadow: 0 6px 16px rgba(var(--acid-rgb), 0.25); }
.profile-meta { flex: 1; min-width: 0; }
.profile-label { font-size: 15px; font-weight: 700; color: var(--grey); }
.profile-uid { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.profile-motivation { text-align: center; }
.motiv-num { display: block; font-size: 20px; font-weight: 800; color: var(--acid); line-height: 1; }
.motiv-label { font-size: 10px; color: #94a3b8; }
.profile-fields { display: flex; flex-direction: column; gap: 8px; }
.pf-row { display: flex; align-items: baseline; gap: 10px; }
.pf-key { font-size: 12px; color: #94a3b8; flex-shrink: 0; width: 56px; }
.pf-val { font-size: 12.5px; color: var(--grey); flex: 1; line-height: 1.5; }

/* ── Methodology ── */
.path-badge { padding: 5px 14px; border-radius: 100px; font-size: 12px; font-weight: 600; background: var(--ink-3); color: var(--acid); }
.path-badge.warn { background: var(--ink-3); color: #b45309; }
.method-card {
  background: #fff; border: 1px solid #e9edf4; border-radius: 18px; padding: 24px;
  box-shadow: 0 2px 12px rgba(21,34,56,0.04);
}
.method-text { font-size: 13px; color: var(--grey); line-height: 1.9; white-space: pre-wrap; word-break: break-word; margin: 0; font-family: inherit; }

.exp-loading { text-align: center; padding: 120px 40px; color: #94a3b8; }
.exp-loading i { font-size: 28px; margin-bottom: 12px; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Hero actions ── */
.exp-hero-actions { display: flex; align-items: center; gap: 14px; margin-top: 22px; flex-wrap: wrap; }
.run-btn {
  height: 44px; padding: 0 24px; border-radius: 12px; font-weight: 600; font-size: 14px;
  background: linear-gradient(135deg, var(--acid), var(--ice)); border: none; color: #fff;
  box-shadow: 0 10px 28px rgba(var(--acid-rgb), 0.4);
  transition: all 0.3s ease;
}
.run-btn:hover {
  opacity: 0.92;
  box-shadow: 0 14px 36px rgba(var(--acid-rgb), 0.5);
  transform: translateY(-1px);
}
.run-hint { font-size: 12px; color: rgba(148,163,184,0.7); }

/* ── Run pending CTA ── */
.run-pending { border-left-color: var(--ink-5); }
.run-pending .conclusion-icon { background: var(--ink-3); color: var(--acid); }

/* ── Running state ── */
.conclusion.running { border-left-color: #f59e0b; }
.conclusion.running .conclusion-icon { background: var(--ink-3); color: #b45309; }
.conclusion.running .conclusion-icon i { animation: spin 1s linear infinite; }

/* ── 可靠性告警 ── */
.exp-alert {
  display: flex; align-items: center; gap: 10px;
  background: var(--ink-3); border: 1px solid #fde68a; border-left: 4px solid #f59e0b;
  border-radius: 14px; padding: 14px 20px; margin-bottom: 24px;
  font-size: 13px; color: #92400e; line-height: 1.6;
}
.exp-alert i { font-size: 16px; color: #b45309; flex-shrink: 0; }
.run-inline-btn {
  height: 40px; padding: 0 22px; border-radius: 11px; font-weight: 600;
  background: linear-gradient(135deg, var(--acid), var(--ice)); border: none; color: #fff;
  flex-shrink: 0;
  transition: all 0.3s ease;
}
.run-inline-btn:hover { opacity: 0.92; transform: translateY(-1px); }

/* ── 评测明细 ── */
.detail-list { display: flex; flex-direction: column; gap: 10px; }
.detail-item {
  background: #fff; border: 1px solid #e9edf4; border-radius: 14px; padding: 15px 20px;
  transition: background 0.2s;
}
.detail-item:hover { background: var(--ink-3); }
.detail-head { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.detail-idx { font-size: 12px; font-weight: 800; color: var(--acid); }
.detail-title { font-size: 14px; font-weight: 600; color: var(--grey); }
.detail-difficulty { font-size: 12px; color: #64748b; padding: 2px 10px; border-radius: 100px; background: var(--ink-3); }
.detail-verdict { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; font-weight: 700; padding: 2px 10px; border-radius: 100px; margin-left: auto; }
.detail-verdict.ok { background: var(--ink-3); color: #059669; }
.detail-verdict:not(.ok) { background: var(--ink-3); color: #ef4444; }
.detail-reason { font-size: 12.5px; color: #64748b; line-height: 1.6; margin-top: 8px; padding-top: 8px; border-top: 1px dashed #eef2f7; }

@media (max-width: 900px) {
  .metric-grid { grid-template-columns: 1fr; }
  .profile-grid { grid-template-columns: 1fr; }
  .ct-row { grid-template-columns: 1fr 1fr; }
  .ct-head { display: none; }
}
</style>
