<template>
  <div class="agent-layout">
    <!-- 左侧垂直 Agent 导航 -->
    <agent-sidebar
      :active="'ppt'"
      @switch="switchAgent"
      @collab="goCollab"
      @report="goReport"
    />

    <!-- 右侧 PPT 工作区 -->
    <main class="agent-workspace">
      <div class="ws-header">
        <div class="ws-header-left">
          <span class="ws-agent-badge"><i class="el-icon-data-board"></i> PPT Agent</span>
          <span class="ws-agent-desc">12 页清新校园风课堂课件 · PPTX 一键下载</span>
        </div>
        <span class="ws-closed-loop"><i class="el-icon-s-data"></i> 诊断 → 生成 → 校对 → 决策</span>
      </div>

      <!-- Header -->
      <div class="ppt-header">
      <div class="ppt-header-content">
        <div class="ppt-header-icon">📖</div>
        <div class="ppt-header-text">
          <h1>PPT 课件生成</h1>
          <p>AI 智能生成器 — 输入主题，获得结构完整、图文并茂的校园风教学课件</p>
        </div>
      </div>
    </div>

    <div class="ppt-body">
      <!-- 左侧配置面板 -->
      <div class="ppt-config-panel">
        <div class="config-card">
          <h3 class="config-title">📝 课件主题</h3>
          <el-input v-model="topic" placeholder="输入课件主题，如：卷积神经网络-CNN原理与工业应用" size="medium" @keyup.enter.native="generatePPT" />
        </div>

        <div class="config-card">
          <h3 class="config-title">📄 幻灯片数量</h3>
          <el-slider v-model="slideCount" :min="6" :max="20" show-input :step="1" />
        </div>

        <el-button type="primary" size="large" :loading="generating" :disabled="!topic.trim()"
          @click="generatePPT" class="generate-btn" icon="el-icon-magic-stick">
          {{ generating ? 'AI 正在生成课件...' : '开始生成 PPT 课件' }}
        </el-button>

        <!-- 快速话题 -->
        <div class="quick-topics">
          <div class="quick-label">💡 快速试试：</div>
          <el-tag v-for="t in quickTopics" :key="t" size="small" type="info"
            style="cursor:pointer;margin:3px" @click="topic=t;generatePPT()">{{ t }}</el-tag>
        </div>

        <!-- 生成历史 -->
        <div class="ppt-history" v-if="history.length > 0">
          <div class="history-header">📋 生成历史</div>
          <div v-for="(h, hi) in history" :key="hi" class="history-item" @click="loadHistory(h)">
            <span class="history-topic">{{ h.topic }}</span>
            <span class="history-meta">{{ h.slides }}页 · {{ h.time }}</span>
            <el-button size="mini" type="text" icon="el-icon-delete" @click.stop="deleteHistory(hi)" />
          </div>
        </div>
      </div>

      <!-- 右侧预览区 -->
      <div class="ppt-preview-panel">
        <!-- 空状态 -->
        <div v-if="pptSlides.length === 0 && !generating" class="ppt-empty">
          <div class="empty-icon">🎓</div>
          <h2>课堂演示文稿预览</h2>
          <p>输入课件主题，AI 将为你创建 12 页校园风教学课件</p>
          <div class="empty-features">
            <div class="feat-item">✨ 清新校园风格</div>
            <div class="feat-item">📐 12页标准框架</div>
            <div class="feat-item">🎯 通俗易懂语言</div>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="generating" class="ppt-loading">
          <div class="loading-spinner"><i class="el-icon-loading"></i></div>
          <h3>正在为「{{ topic }}」生成课件</h3>
          <p style="color:#94a3b8;font-size:13px">{{ loadingHint }}</p>
          <el-progress :percentage="loadingProgress" :stroke-width="6" color="#4A90A4" style="max-width:360px;margin:16px auto" />
        </div>

        <!-- 幻灯片预览 -->
        <div v-if="pptSlides.length > 0 && !generating" class="ppt-slideshow">
          <div class="slideshow-toolbar">
            <span class="toolbar-title">📄 {{ topic }}</span>
            <span class="toolbar-page">{{ currentSlide + 1 }} / {{ pptSlides.length }}</span>
            <div class="toolbar-actions">
              <el-button size="mini" icon="el-icon-refresh" @click="generatePPT">重新生成</el-button>
              <el-button size="mini" v-if="!fullscreenMode" icon="el-icon-full-screen" type="warning" @click="enterFullscreen">演示模式</el-button>
              <el-button size="mini" icon="el-icon-download" @click="downloadPPTX" type="primary" :loading="downloadingPptx">
                {{ downloadingPptx ? '下载中...' : '下载 PPTX' }}
              </el-button>
            </div>
          </div>

          <!-- 幻灯片卡片（按PPTX页面类型渲染，保证预览与下载一致） -->
          <div class="slide-display">
            <transition :name="slideDirection" mode="out-in">
              <div :key="currentSlide" class="slide-card" :style="{ background: getSlideBg(currentSlide) }">
                <div v-if="slideViewType === 'cover'" class="sv-cover">
                  <div class="sv-cover-badge">课堂演示文稿</div>
                  <h1 class="sv-cover-title">{{ topic }}</h1>
                  <p class="sv-cover-sub">{{ pptSlides.length }} 页 · 校园清新风格 · PPTX 已生成</p>
                  <div class="sv-cover-meta"><span>📅 {{ today }}</span><span>🎓 适配课堂教学</span></div>
                </div>

                <div v-else-if="slideViewType === 'goal'" class="sv-goal">
                  <div class="sv-topbar"></div>
                  <h2 class="sv-section-title">学完这节课，你将能……</h2>
                  <div class="sv-goal-cards">
                    <div v-for="(pt, pi) in curSlide.points.slice(0, 3)" :key="pi" class="sv-goal-card" :class="'sv-goal-card--' + pi">
                      <span class="sv-goal-num">0{{ pi + 1 }}</span>
                      <p class="sv-goal-text">{{ pt }}</p>
                    </div>
                  </div>
                  <span class="sv-footer-tag">学习目标</span>
                </div>

                <div v-else-if="slideViewType === 'scenario'" class="sv-scenario">
                  <div class="sv-topbar"></div>
                  <h2 class="sv-section-title">情境导入</h2>
                  <div class="sv-scenario-box">
                    <div class="sv-scenario-bar"></div>
                    <p class="sv-scenario-story">{{ curSlide.points[0] || '请思考以下场景……' }}</p>
                    <p class="sv-scenario-story" v-if="curSlide.points[1]">{{ curSlide.points[1] }}</p>
                  </div>
                  <div class="sv-think-tag">想一想</div>
                  <p class="sv-question" v-if="curSlide.points[2]">❓ {{ curSlide.points[2] }}</p>
                  <p class="sv-question" v-if="curSlide.points[3]">❓ {{ curSlide.points[3] || '这和已学知识有什么联系？' }}</p>
                  <span class="sv-footer-tag">情境导入</span>
                </div>

                <div v-else-if="slideViewType === 'review'" class="sv-review">
                  <div class="sv-topbar"></div>
                  <h2 class="sv-section-title">知识回顾</h2>
                  <div v-for="(pt, pi) in curSlide.points.slice(0, 3)" :key="pi" class="sv-review-card">
                    <div class="sv-review-bar"></div>
                    <span class="sv-review-num">{{ pi + 1 }}</span>
                    <p class="sv-review-text">{{ pt }}</p>
                  </div>
                  <span class="sv-footer-tag">知识回顾</span>
                </div>

                <div v-else-if="slideViewType === 'keypoint'" class="sv-keypoint">
                  <div class="sv-topbar sv-topbar--orange"></div>
                  <h2 class="sv-section-title sv-section-title--orange">{{ curSlide.title || '重难点突破' }}</h2>
                  <div class="sv-kp-main">
                    <div class="sv-kp-steps">
                      <div v-for="(pt, pi) in curSlide.points.slice(0, 5)" :key="pi" class="sv-kp-step">
                        <div class="sv-kp-dot">{{ pi + 1 }}</div>
                        <p class="sv-kp-text">{{ pt }}</p>
                        <div v-if="pi < Math.min(curSlide.points.length, 5) - 1" class="sv-kp-line"></div>
                      </div>
                    </div>
                    <div class="sv-kp-warn">
                      <div class="sv-kp-warn-bar"></div>
                      <h4>⚠ 常见误区</h4>
                      <p>{{ curSlide.points[5] || '注意区分相似概念，避免混淆。' }}</p>
                    </div>
                  </div>
                  <span class="sv-footer-tag sv-footer-tag--orange">重难点突破</span>
                </div>

                <div v-else-if="slideViewType === 'example'" class="sv-example">
                  <div class="sv-topbar"></div>
                  <h2 class="sv-section-title">{{ curSlide.title || '例题精讲' }}</h2>
                  <div class="sv-exam-question">
                    <span class="sv-exam-label">📝 题干</span>
                    <p class="sv-exam-qtext">{{ curSlide.points[0] || '题目内容' }}</p>
                  </div>
                  <div class="sv-exam-cols">
                    <div class="sv-exam-col sv-exam-col--hint">
                      <div class="sv-exam-col-bar sv-exam-col-bar--purple"></div>
                      <span class="sv-exam-label">💡 思路点拨</span>
                      <p>{{ curSlide.points[1] || '分析关键信息，找到解题突破口。' }}</p>
                    </div>
                    <div class="sv-exam-col sv-exam-col--solve">
                      <div class="sv-exam-col-bar sv-exam-col-bar--orange"></div>
                      <span class="sv-exam-label">✏ 分步解答</span>
                      <p>{{ curSlide.points[2] || '逐步推导，写出完整解答过程。' }}</p>
                    </div>
                  </div>
                  <span class="sv-footer-tag">例题精讲</span>
                </div>

                <div v-else-if="slideViewType === 'exercise'" class="sv-exercise">
                  <div class="sv-topbar"></div>
                  <h2 class="sv-section-title">{{ curSlide.title || '思考与练习' }}</h2>
                  <div class="sv-exer-grid">
                    <div v-for="(pt, pi) in curSlide.points.slice(0, 4)" :key="pi" class="sv-exer-card" :class="pi < 2 ? 'sv-exer-card--think' : 'sv-exer-card--do'">
                      <div class="sv-exer-head">{{ (pi &lt; 2 ? '💬 思考题 ' : '✍ 练习题 ') + (pi + 1) }}</div>
                      <p class="sv-exer-text">{{ pt }}</p>
                    </div>
                  </div>
                  <span class="sv-footer-tag">思考与练习</span>
                </div>

                <div v-else-if="slideViewType === 'summary'" class="sv-summary">
                  <div class="sv-topbar"></div>
                  <h2 class="sv-section-title">课堂小结</h2>
                  <p class="sv-summary-sub">一起来回顾今天学到的知识吧 ✨</p>
                  <div class="sv-summary-cards">
                    <div v-for="(pt, pi) in curSlide.points.slice(0, 6)" :key="pi" class="sv-summary-card" :class="'sv-summary-card--' + (pi % 3)">
                      <div class="sv-summary-card-bar"></div>
                      <span class="sv-summary-card-num">0{{ pi + 1 }}</span>
                      <p class="sv-summary-card-text">{{ pt }}</p>
                    </div>
                  </div>
                  <span class="sv-footer-tag">课堂小结</span>
                </div>

                <div v-else-if="slideViewType === 'toc'" class="sv-toc">
                  <div class="sv-toc-bar"></div>
                  <h2 class="sv-section-title">目&emsp;录</h2>
                  <div v-for="(pt, pi) in curSlide.points.slice(0, 8)" :key="pi" class="sv-toc-item">
                    <span class="sv-toc-num">{{ String(pi + 1).padStart(2, '0') }}</span>
                    <span class="sv-toc-text">{{ pt }}</span>
                  </div>
                </div>

                <div v-else-if="slideViewType === 'end'" class="sv-end">
                  <h1 class="sv-end-title">{{ curSlide.points[0] || '今天的课程就到这里啦～' }}</h1>
                  <div class="sv-end-divider"></div>
                  <p class="sv-end-next">{{ curSlide.points[1] || '下节课见！' }}</p>
                  <p class="sv-end-meta">感谢聆听 · 期待下次再见</p>
                </div>

                <!-- 新知讲解（默认类型） -->
                <div v-else class="sv-knowledge">
                  <div class="sv-topbar"></div>
                  <h2 class="sv-section-title">{{ curSlide.title || '新知讲解' }}</h2>
                  <div class="sv-kn-list">
                    <div v-for="(pt, pi) in curSlide.points" :key="pi" class="sv-kn-item">
                      <span class="sv-kn-dot" :class="'sv-kn-dot--' + (pi % 3)">{{ pi + 1 }}</span>
                      <span class="sv-kn-text">{{ pt }}</span>
                    </div>
                  </div>
                  <span class="sv-footer-tag">{{ curSlide.type || '新知讲解' }}</span>
                </div>
              </div>
            </transition>
          </div>

          <!-- 翻页控件 -->
          <div class="slide-controls">
            <el-button circle icon="el-icon-arrow-left" :disabled="currentSlide === 0" @click="prevSlide" />
            <div class="slide-thumbnails">
              <div v-for="(s, si) in pptSlides" :key="si"
                :class="['slide-thumb', { active: si === currentSlide }]"
                @click="currentSlide = si"
                :style="{ background: getThumbBg(si) }">
                <span class="thumb-num" :style="{ color: getThumbColor(si) }">{{ si + 1 }}</span>
              </div>
            </div>
            <el-button circle icon="el-icon-arrow-right" :disabled="currentSlide === pptSlides.length - 1" @click="nextSlide" />
          </div>

          <!-- 自动播放 -->
          <div class="auto-play-bar">
            <el-button size="mini" :type="autoPlay ? 'primary' : 'default'" icon="el-icon-video-play" @click="toggleAutoPlay" circle />
            <span style="font-size:12px;color:#94a3b8;margin-left:8px">{{ autoPlay ? '自动播放中 (' + autoPlayInterval + 's)' : '自动翻页' }}</span>
            <span v-if="fileSize" style="font-size:12px;color:#94a3b8;margin-left:auto">{{ formatFileSize(fileSize) }}</span>
          </div>

          <!-- 全屏演示专用翻页条（仅演示模式可见） -->
          <div v-if="fullscreenMode" class="fs-nav-bar">
            <el-button circle icon="el-icon-arrow-left" :disabled="currentSlide === 0" @click="prevSlide" />
            <span class="fs-nav-page">{{ currentSlide + 1 }} / {{ pptSlides.length }}</span>
            <el-button circle icon="el-icon-arrow-right" :disabled="currentSlide === pptSlides.length - 1" @click="nextSlide" />
            <span class="fs-nav-hint">← → 翻页 · ESC 退出</span>
            <el-button size="mini" type="danger" icon="el-icon-close" @click="exitFullscreen">退出演示</el-button>
          </div>
        </div>
      </div>
    </div>
    </main>
  </div>
</template>

<script>
import AgentSidebar from './AgentSidebar'

export default {
  name: 'PptAgent',
  components: { AgentSidebar },
  data () {
    return {
      topic: '',
      slideCount: 12,
      generating: false,
      downloadingPptx: false,
      currentSlide: 0,
      slideDirection: 'slide-left',
      autoPlay: false,
      autoPlayInterval: 4,
      autoPlayTimer: null,
      loadingProgress: 0,
      loadingHint: '连接 AI 引擎…',
      pptSlides: [],
      downloadUrl: '',
      fileName: '',
      fileSize: 0,
      fullscreenMode: false,
      history: [],
      quickTopics: [
        '卷积神经网络CNN-原理与工业应用', 'RAG检索增强生成-系统设计',
        '工业视觉质检-YOLO缺陷检测实训', '模型部署-vLLM推理加速',
        'LoRA微调实战-显存优化', '预测性维护-轴承故障诊断'
      ],
      agents: [
        { key: 'document', icon: 'el-icon-document', agent: 'Doc Agent', route: '/resource-agent/generate?type=document' },
        { key: 'mind_map', icon: 'el-icon-share', agent: 'MindMap Agent', route: '/resource-agent/generate?type=mind_map' },
        { key: 'exercise', icon: 'el-icon-edit-outline', agent: 'Quiz Agent', route: '/resource-agent/generate?type=exercise' },
        { key: 'extended_reading', icon: 'el-icon-reading', agent: 'Reading Agent', route: '/resource-agent/generate?type=extended_reading' },
        { key: 'video', icon: 'el-icon-video-camera', agent: 'Video Agent', route: '/resource-agent/generate?type=video' },
        { key: 'code_example', icon: 'el-icon-cpu', agent: 'Code Agent', route: '/resource-agent/generate?type=code_example' },
        { key: 'ppt', icon: 'el-icon-data-board', agent: 'PPT Agent', route: '/resource-agent/ppt-agent' }
      ]
    }
  },
  computed: {
    today () {
      var d = new Date()
      return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate()
    },
    curSlide () {
      return this.pptSlides[this.currentSlide] || {}
    },
    slideViewType () {
      return this.getSlideViewType(this.currentSlide)
    }
  },
  mounted () {
    try {
      var saved = localStorage.getItem('ppt_history')
      if (saved) this.history = JSON.parse(saved)
    } catch (e) { /* ignore */ }
    this._onFsChange = this.handleFsChange.bind(this)
    this._onFsKeydown = this.handleFsKeydown.bind(this)
    document.addEventListener('fullscreenchange', this._onFsChange)
    document.addEventListener('webkitfullscreenchange', this._onFsChange)
    document.addEventListener('keydown', this._onFsKeydown)
  },
  beforeDestroy () {
    this.stopAutoPlay()
    document.removeEventListener('fullscreenchange', this._onFsChange)
    document.removeEventListener('webkitfullscreenchange', this._onFsChange)
    document.removeEventListener('keydown', this._onFsKeydown)
  },
  methods: {
    goCollab: function () { this.$router.push('/resource-agent/collaboration') },
    goReport: function () { this.$router.push('/evaluation-agent/experiment') },
    switchAgent: function (t) {
      if (t.key === 'ppt') return
      this.$router.push('/resource-agent/generate?type=' + t.key)
    },

    /* ═══ 核心：调用后端 AI 生成 PPT ═══ */
    generatePPT: async function () {
      if (!this.topic.trim() || this.generating) return
      this.generating = true
      this.loadingProgress = 0
      this.currentSlide = 0
      this.pptSlides = []
      var self = this

      try {
        self.loadingProgress = 10
        self.loadingHint = '连接 AI 引擎，生成课件脚本…'

        // 调用后端 PPTX 生成接口（后端返回结构化幻灯片 + 下载URL）
        var resp = await fetch('/api/admin/resource/generate-pptx', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': localStorage.getItem('token') || ''
          },
          body: JSON.stringify({
            topic: self.topic.trim()
          })
        })

        if (!resp.ok) {
          throw new Error('服务器响应异常: ' + resp.status)
        }

        var result = await resp.json()

        if (result.code === 1 && result.response) {
          self.loadingProgress = 85
          self.loadingHint = '解析课件内容…'

          var data = result.response
          // 使用后端返回的结构化幻灯片
          self.pptSlides = data.slides || []
          self.downloadUrl = data.downloadUrl || ''
          self.fileName = data.fileName || ''
          self.fileSize = data.fileSize || 0
          self.topic = data.topic || self.topic

          self.loadingProgress = 100
          self.loadingHint = '完成！'

          if (self.pptSlides.length === 0) {
            self.$message.warning('AI 返回内容无法解析，请重试')
          } else {
            self.$message.success('课件生成成功，共 ' + self.pptSlides.length + ' 页（PPTX 文件已生成）')
            self.addHistory(self.pptSlides.length)
          }
        } else {
          throw new Error(result.message || '生成失败')
        }
      } catch (e) {
        console.error('PPT generation failed:', e)
        self.$message.error('生成失败：' + (e.message || '网络错误'))
      } finally {
        self.generating = false
        setTimeout(() => { self.loadingProgress = 0 }, 500)
      }
    },

    /* ═══ 下载后端生成的 PPTX 文件 ═══ */
    downloadPPTX: function () {
      if (!this.downloadUrl) {
        this.$message.warning('尚未生成 PPT 文件')
        return
      }
      this.downloadingPptx = true

      // 后端已设置 Content-Disposition: attachment，浏览器收到后自动下载
      // 多重降级策略确保所有浏览器都能下载
      var url = this.downloadUrl

      // 方式1: window.open（最兼容，浏览器不会拦截用户点击触发的弹窗）
      var w = window.open(url, '_blank')

      // 方式2: 如果弹窗被拦截，用 <a> 标签（定位到可视区域外，避免 display:none 被忽略）
      if (!w) {
        var a = document.createElement('a')
        a.href = url
        a.download = this.fileName || ''
        a.style.cssText = 'position:fixed;left:-10000px;top:-10000px'
        document.body.appendChild(a)
        a.click()
        setTimeout(function () { document.body.removeChild(a) }, 100)
      }

      var self = this
      setTimeout(function () {
        self.downloadingPptx = false
      }, 2000)
    },

    /* ═══ 幻灯片导航 ═══ */
    prevSlide: function () {
      if (this.currentSlide > 0) {
        this.slideDirection = 'slide-right'
        this.currentSlide--
      }
    },
    nextSlide: function () {
      if (this.currentSlide < this.pptSlides.length - 1) {
        this.slideDirection = 'slide-left'
        this.currentSlide++
      }
    },
    toggleAutoPlay: function () {
      if (this.autoPlay) {
        this.stopAutoPlay()
      } else {
        this.startAutoPlay()
      }
    },
    startAutoPlay: function () {
      this.autoPlay = true
      var self = this
      this.autoPlayTimer = setInterval(function () {
        if (self.currentSlide < self.pptSlides.length - 1) {
          self.nextSlide()
        } else {
          self.stopAutoPlay()
        }
      }, this.autoPlayInterval * 1000)
    },
    stopAutoPlay: function () {
      this.autoPlay = false
      if (this.autoPlayTimer) {
        clearInterval(this.autoPlayTimer)
        this.autoPlayTimer = null
      }
    },

    /* ═══ 幻灯片背景色（与 PPTX 一致） ═══ */
    getSlideBg: function (idx) {
      var vt = this.getSlideViewType(idx)
      if (vt === 'cover' || vt === 'end') return 'linear-gradient(135deg, #4A90A4, #387a8c)'
      if (vt === 'scenario' || vt === 'exercise') return '#F2F7F2'
      if (vt === 'example') return '#F0F4F8'
      return '#FFFAF5'
    },

    /* ═══ 页面类型判定（与 PPTX 10种布局完全对应） ═══ */
    getSlideViewType: function (idx) {
      if (idx === 0) return 'cover'
      var slide = this.pptSlides[idx]
      if (!slide) return 'knowledge'
      var type = (slide.type || '').trim()
      if (type.indexOf('封面') !== -1) return 'cover'
      if (type.indexOf('学习目标') !== -1) return 'goal'
      if (type.indexOf('情境导入') !== -1) return 'scenario'
      if (type.indexOf('知识回顾') !== -1) return 'review'
      if (type.indexOf('重难点') !== -1) return 'keypoint'
      if (type.indexOf('例题') !== -1 || type.indexOf('案例') !== -1) return 'example'
      if (type.indexOf('练习') !== -1 || type.indexOf('思考题') !== -1) return 'exercise'
      if (type.indexOf('小结') !== -1) return 'summary'
      if (type.indexOf('目录') !== -1) return 'toc'
      if (idx === this.pptSlides.length - 1 || type.indexOf('结束') !== -1) return 'end'
      return 'knowledge'
    },

    /* ═══ 缩略图颜色 ═══ */
    getThumbBg: function (si) {
      var vt = this.getSlideViewType(si)
      if (vt === 'cover' || vt === 'end') return '#4A90A4'
      if (vt === 'keypoint') return '#E8956A'
      return '#f0f4f8'
    },
    getThumbColor: function (si) {
      var vt = this.getSlideViewType(si)
      if (vt === 'cover' || vt === 'end' || vt === 'keypoint') return '#fff'
      return '#4A90A4'
    },

    /* ═══ 演示模式全屏 ═══ */
    enterFullscreen: function () {
      var el = this.$el.querySelector('.ppt-slideshow')
      if (!el) return
      var req = el.requestFullscreen || el.webkitRequestFullscreen || el.msRequestFullscreen
      if (req) {
        req.call(el).catch(function (e) { /* 浏览器拒绝全屏则忽略 */ })
      }
      // 状态由 fullscreenchange 回调同步，避免不同步
    },
    exitFullscreen: function () {
      var ex = document.exitFullscreen || document.webkitExitFullscreen || document.msExitFullscreen
      if (ex) {
        ex.call(document).catch(function () {})
      }
    },
    handleFsChange: function () {
      var fsEl = document.fullscreenElement || document.webkitFullscreenElement
      this.fullscreenMode = !!fsEl
      if (!fsEl) {
        this.stopAutoPlay()
      }
    },
    handleFsKeydown: function (e) {
      if (!this.fullscreenMode) return
      var key = e.key
      if (key === 'ArrowRight' || key === 'PageDown' || key === ' ') {
        e.preventDefault()
        this.nextSlide()
      } else if (key === 'ArrowLeft' || key === 'PageUp') {
        e.preventDefault()
        this.prevSlide()
      } else if (key === 'Escape') {
        e.preventDefault()
        this.exitFullscreen()
      }
    },
    formatFileSize: function (bytes) {
      if (!bytes) return ''
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / 1048576).toFixed(1) + ' MB'
    },
    addHistory: function (count) {
      var h = {
        topic: this.topic,
        slides: count,
        time: new Date().toLocaleString(),
        downloadUrl: this.downloadUrl,
        fileName: this.fileName,
        data: JSON.parse(JSON.stringify(this.pptSlides))
      }
      this.history.unshift(h)
      if (this.history.length > 10) this.history.pop()
      try { localStorage.setItem('ppt_history', JSON.stringify(this.history)) } catch (e) { /* ignore */ }
    },
    loadHistory: function (h) {
      this.topic = h.topic
      this.pptSlides = JSON.parse(JSON.stringify(h.data))
      this.downloadUrl = h.downloadUrl || ''
      this.fileName = h.fileName || ''
      this.currentSlide = 0
    },
    deleteHistory: function (idx) {
      this.history.splice(idx, 1)
      try { localStorage.setItem('ppt_history', JSON.stringify(this.history)) } catch (e) { /* ignore */ }
    }
  }
}
</script>

<style scoped>
.ppt-agent-page { min-height: 100vh; background: var(--ink-3); padding-bottom: 40px; }

/* ── 两栏布局：左侧 Agent 导航 + 右侧工作区 ── */
.agent-layout { display: flex; height: calc(100vh - 92px); overflow: hidden; background: var(--ink-3); }
.agent-workspace { flex: 1; min-width: 0; padding: 24px 28px 60px; overflow-y: auto; }
.agent-workspace::-webkit-scrollbar { width: 6px; }
.agent-workspace::-webkit-scrollbar-thumb { background: var(--ink-3); border-radius: 3px; }
.ws-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.ws-header-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.ws-agent-badge {
  display: inline-flex; align-items: center; gap: 7px; padding: 6px 14px; border-radius: 100px;
  background: linear-gradient(135deg, var(--acid), var(--ice)); color: #fff; font-size: 13px; font-weight: 700;
  letter-spacing: 0.5px; box-shadow: 0 6px 16px rgba(var(--acid-rgb), 0.3); flex-shrink: 0;
}
.ws-agent-desc { font-size: 13px; color: #64748b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ws-closed-loop { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 100px; background: var(--ink-3); color: var(--acid); font-size: 12px; font-weight: 600; flex-shrink: 0; }
.ws-closed-loop i { font-size: 14px; }

.ppt-header { border-radius: 14px; overflow: hidden; margin-bottom: 20px; }

/* ── 多智能体导航 ── */
.ppt-agent-nav {
  max-width: 1200px; margin: 0 auto; padding: 14px 20px 0;
  display: flex; align-items: center; gap: 18px; flex-wrap: wrap;
}
.pan-back { color: var(--acid); font-weight: 600; padding: 0; }
.pan-back i { margin-right: 2px; }
.pan-tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.pan-tab {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 13px; border-radius: 10px;
  background: #fff; border: 1px solid #e8ecf1;
  color: #64748b; font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease;
}
.pan-tab i { font-size: 15px; }
.pan-tab:hover { color: var(--grey); border-color: var(--ink-5); }
.pan-tab.active {
  color: #fff; background: linear-gradient(135deg, #4A90A4, #E8956A);
  border-color: transparent; box-shadow: 0 4px 14px rgba(74, 144, 164, 0.3);
}

.ppt-header { background: linear-gradient(135deg, #141A14, #334155); padding: 28px 32px; color: #fff; }
.ppt-header-content { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; gap: 16px; }
.ppt-header-icon { width: 52px; height: 52px; border-radius: 14px; background: linear-gradient(135deg, #4A90A4, #E8956A); display: flex; align-items: center; justify-content: center; font-size: 26px; flex-shrink: 0; }
.ppt-header-text h1 { margin: 0 0 4px; font-size: 22px; font-weight: 700; }
.ppt-header-text p { margin: 0; font-size: 13px; opacity: 0.65; }

.ppt-body { max-width: 1200px; margin: 24px auto 0; padding: 0 20px; display: flex; gap: 24px; align-items: flex-start; }
.ppt-config-panel { width: 300px; flex-shrink: 0; display: flex; flex-direction: column; gap: 14px; }
.config-card { background: #fff; border-radius: 12px; padding: 16px; border: 1px solid #e8ecf1; }
.config-title { font-size: 14px; font-weight: 600; color: #333; margin: 0 0 10px; }
.generate-btn { height: 46px; border-radius: 12px; background: linear-gradient(135deg, #4A90A4, #E8956A); border: none; font-weight: 600; font-size: 15px; letter-spacing: 2px; }
.generate-btn:hover { opacity: 0.92; }

.quick-topics { padding: 0 4px; }
.quick-label { font-size: 12px; color: #94a3b8; margin-bottom: 6px; }

.ppt-history { margin-top: 8px; }
.history-header { font-size: 13px; font-weight: 600; color: #666; margin-bottom: 8px; }
.history-item { display: flex; align-items: center; padding: 8px 12px; background: #fff; border-radius: 8px; margin-bottom: 4px; cursor: pointer; border: 1px solid #e8ecf1; transition: all 0.2s; }
.history-item:hover { border-color: #4A90A4; box-shadow: 0 2px 8px rgba(74,144,164,0.1); }
.history-topic { flex: 1; font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-meta { font-size: 11px; color: #94a3b8; margin: 0 8px; white-space: nowrap; }

/* 预览面板 */
.ppt-preview-panel { flex: 1; min-height: 500px; }
.ppt-empty { text-align: center; padding: 80px 40px; background: #fff; border-radius: 16px; border: 1px solid #e8ecf1; }
.empty-icon { font-size: 56px; margin-bottom: 16px; }
.ppt-empty h2 { font-size: 20px; color: #333; margin: 0 0 8px; }
.ppt-empty p { color: #94a3b8; font-size: 14px; margin: 0 0 24px; }
.empty-features { display: flex; justify-content: center; gap: 20px; }
.feat-item { padding: 8px 16px; background: var(--ink-3); border-radius: 8px; font-size: 13px; color: #4A90A4; }

.ppt-loading { text-align: center; padding: 100px 40px; background: #fff; border-radius: 16px; border: 1px solid #e8ecf1; }
.loading-spinner { font-size: 40px; color: #4A90A4; margin-bottom: 16px; animation: spin 2s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.ppt-loading h3 { font-size: 18px; color: #333; margin: 0 0 8px; }

/* 幻灯片卡片 */
.ppt-slideshow { }
.slideshow-toolbar { display: flex; align-items: center; padding: 12px 20px; background: #fff; border-radius: 12px 12px 0 0; border: 1px solid #e8ecf1; border-bottom: none; }
.toolbar-title { font-size: 15px; font-weight: 600; color: #333; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.toolbar-page { font-size: 12px; color: #94a3b8; margin: 0 16px; }
.toolbar-actions { display: flex; gap: 8px; }

.slide-display { }
.slide-card { min-height: 420px; border-radius: 0; padding: 0; display: flex; align-items: stretch; justify-content: stretch; position: relative; border: 1px solid #e8ecf1; border-top: none; overflow: hidden; }
.slide-card > div { width: 100%; }

/* ═══ 通用小组件 ═══ */
.sv-topbar { position: absolute; top: 0; left: 0; width: 100%; height: 4px; background: #4A90A4; }
.sv-topbar--orange { background: #E8956A; }
.sv-section-title { font-size: 22px; font-weight: 700; color: #4A90A4; margin: 0; padding: 32px 50px 0; }
.sv-section-title--orange { color: #E8956A; }
.sv-footer-tag { position: absolute; bottom: 14px; left: 50px; font-size: 11px; color: #4A90A4; opacity: 0.7; }
.sv-footer-tag--orange { color: #E8956A; }

/* ═══ 封面 ═══ */
.sv-cover { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 420px; padding: 60px 50px; text-align: center; }
.sv-cover-badge { display: inline-block; padding: 4px 18px; background: rgba(255,255,255,0.18); border-radius: 20px; color: #fff; font-size: 13px; margin-bottom: 22px; }
.sv-cover-title { font-size: 36px; color: #fff; font-weight: 700; margin: 0 0 14px; line-height: 1.3; }
.sv-cover-sub { font-size: 15px; color: rgba(255,255,255,0.7); margin: 0 0 26px; }
.sv-cover-meta { display: flex; gap: 28px; }
.sv-cover-meta span { font-size: 13px; color: rgba(255,255,255,0.55); }

/* ═══ 学习目标 ═══ */
.sv-goal { min-height: 420px; position: relative; padding-bottom: 40px; }
.sv-goal-cards { display: flex; gap: 24px; padding: 28px 50px; }
.sv-goal-card { flex: 1; background: #fff; border-radius: 14px; padding: 24px 20px; position: relative; overflow: hidden; }
.sv-goal-card::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 4px; }
.sv-goal-card--0::before { background: #E8956A; }
.sv-goal-card--1::before { background: #4A90A4; }
.sv-goal-card--2::before { background: #9B8EC4; }
.sv-goal-num { font-size: 22px; font-weight: 700; color: #4A90A4; display: block; margin-bottom: 10px; }
.sv-goal-card--0 .sv-goal-num { color: #E8956A; }
.sv-goal-card--2 .sv-goal-num { color: #9B8EC4; }
.sv-goal-text { font-size: 14px; color: #3a3a3a; line-height: 1.7; margin: 0; }

/* ═══ 情境导入 ═══ */
.sv-scenario { min-height: 420px; position: relative; padding-bottom: 40px; }
.sv-scenario-box { margin: 22px 50px 0; background: #fff; border-radius: 12px; padding: 22px 28px; position: relative; }
.sv-scenario-bar { position: absolute; left: 0; top: 0; width: 5px; height: 100%; background: #E8956A; border-radius: 12px 0 0 12px; }
.sv-scenario-story { font-size: 15px; color: #3a3a3a; line-height: 1.8; margin: 0 0 6px; }
.sv-think-tag { display: inline-block; margin: 16px 50px 8px; padding: 4px 14px; background: #E8956A; color: #fff; border-radius: 14px; font-size: 13px; font-weight: 600; }
.sv-question { margin: 6px 50px; font-size: 14px; color: #3a3a3a; line-height: 1.6; }

/* ═══ 知识回顾 ═══ */
.sv-review { min-height: 420px; position: relative; padding-bottom: 40px; }
.sv-review-card { margin: 14px 50px 0; background: #fff; border-radius: 10px; padding: 18px 24px; position: relative; display: flex; align-items: center; gap: 16px; }
.sv-review-bar { position: absolute; left: 0; top: 0; width: 5px; height: 100%; background: #4A90A4; border-radius: 10px 0 0 10px; }
.sv-review-num { font-size: 20px; font-weight: 700; color: #4A90A4; min-width: 32px; text-align: center; }
.sv-review-text { font-size: 14px; color: #3a3a3a; line-height: 1.7; margin: 0; flex: 1; }

/* ═══ 重难点突破 ═══ */
.sv-keypoint { min-height: 420px; position: relative; padding-bottom: 40px; }
.sv-kp-main { display: flex; gap: 24px; padding: 20px 50px; }
.sv-kp-steps { flex: 1; position: relative; }
.sv-kp-step { position: relative; padding: 6px 0 10px 36px; }
.sv-kp-dot { position: absolute; left: 0; top: 8px; width: 22px; height: 22px; border-radius: 50%; background: #E8956A; color: #fff; font-size: 11px; font-weight: 700; display: flex; align-items: center; justify-content: center; z-index: 1; }
.sv-kp-text { font-size: 14px; color: #3a3a3a; line-height: 1.7; margin: 0; }
.sv-kp-line { position: absolute; left: 10px; top: 30px; width: 2px; height: calc(100% - 18px); background: #4A90A4; }
.sv-kp-warn { width: 220px; background: var(--ink-3); border-radius: 10px; padding: 18px 16px; position: relative; flex-shrink: 0; align-self: flex-start; }
.sv-kp-warn-bar { position: absolute; left: 0; top: 0; width: 4px; height: 100%; background: #E8956A; border-radius: 10px 0 0 10px; }
.sv-kp-warn h4 { margin: 0 0 8px; font-size: 14px; color: #E8956A; }
.sv-kp-warn p { margin: 0; font-size: 12px; color: #3a3a3a; line-height: 1.6; }

/* ═══ 例题精讲 ═══ */
.sv-example { min-height: 420px; position: relative; padding-bottom: 40px; }
.sv-exam-question { margin: 18px 50px 0; background: #fff; border-radius: 10px; padding: 18px 24px; border-left: 5px solid #4A90A4; }
.sv-exam-label { font-size: 13px; font-weight: 600; color: #4A90A4; display: block; margin-bottom: 6px; }
.sv-exam-qtext { font-size: 14px; color: #3a3a3a; line-height: 1.7; margin: 0; }
.sv-exam-cols { display: flex; gap: 20px; padding: 16px 50px 0; }
.sv-exam-col { flex: 1; background: #fff; border-radius: 10px; padding: 18px 20px; position: relative; }
.sv-exam-col-bar { position: absolute; left: 0; top: 0; width: 4px; height: 100%; border-radius: 10px 0 0 10px; }
.sv-exam-col-bar--purple { background: #9B8EC4; }
.sv-exam-col-bar--orange { background: #E8956A; }
.sv-exam-col .sv-exam-label { color: #9B8EC4; }
.sv-exam-col--solve .sv-exam-label { color: #E8956A; }
.sv-exam-col p { font-size: 13px; color: #3a3a3a; line-height: 1.7; margin: 6px 0 0; }

/* ═══ 课堂练习 / 思考题 ═══ */
.sv-exercise { min-height: 420px; position: relative; padding-bottom: 40px; }
.sv-exer-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; padding: 22px 50px; }
.sv-exer-card { background: #fff; border-radius: 10px; overflow: hidden; }
.sv-exer-head { padding: 8px 16px; font-size: 13px; font-weight: 600; color: #fff; }
.sv-exer-card--think .sv-exer-head { background: #9B8EC4; }
.sv-exer-card--do .sv-exer-head { background: #E8956A; }
.sv-exer-text { padding: 14px 16px; font-size: 13px; color: #3a3a3a; line-height: 1.6; margin: 0; }

/* ═══ 课堂小结 ═══ */
.sv-summary { min-height: 420px; position: relative; padding-bottom: 40px; }
.sv-summary-sub { font-size: 13px; color: #E8956A; margin: 4px 50px 18px; }
.sv-summary-cards { display: flex; gap: 10px; padding: 0 50px; }
.sv-summary-card { flex: 1; background: #fff; border-radius: 10px; padding: 16px 12px; position: relative; overflow: hidden; text-align: center; }
.sv-summary-card-bar { position: absolute; top: 0; left: 0; width: 100%; height: 4px; }
.sv-summary-card--0 .sv-summary-card-bar { background: #E8956A; }
.sv-summary-card--1 .sv-summary-card-bar { background: #4A90A4; }
.sv-summary-card--2 .sv-summary-card-bar { background: #9B8EC4; }
.sv-summary-card-num { font-size: 18px; font-weight: 700; color: #4A90A4; display: block; margin-bottom: 8px; }
.sv-summary-card--0 .sv-summary-card-num { color: #E8956A; }
.sv-summary-card--2 .sv-summary-card-num { color: #9B8EC4; }
.sv-summary-card-text { font-size: 12px; color: #3a3a3a; line-height: 1.5; margin: 0; }

/* ═══ 目录 ═══ */
.sv-toc { min-height: 420px; position: relative; padding: 32px 50px; }
.sv-toc-bar { position: absolute; left: 0; top: 0; width: 6px; height: 100%; background: #4A90A4; }
.sv-toc-item { display: flex; align-items: center; gap: 16px; padding: 14px 0; border-bottom: 1px solid #E8E4DF; }
.sv-toc-num { font-size: 20px; font-weight: 700; color: #4A90A4; min-width: 36px; }
.sv-toc-text { font-size: 15px; color: #3a3a3a; }

/* ═══ 结束页 ═══ */
.sv-end { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 420px; padding: 60px 50px; text-align: center; }
.sv-end-title { font-size: 30px; color: #fff; font-weight: 700; margin: 0 0 16px; }
.sv-end-divider { width: 60px; height: 3px; background: rgba(255,255,255,0.5); margin-bottom: 14px; }
.sv-end-next { font-size: 18px; color: #E8956A; margin: 0 0 20px; }
.sv-end-meta { font-size: 12px; color: rgba(255,255,255,0.5); margin: 0; }

/* ═══ 新知讲解（默认类型） ═══ */
.sv-knowledge { min-height: 420px; position: relative; padding-bottom: 40px; }
.sv-kn-list { padding: 24px 50px; }
.sv-kn-item { display: flex; align-items: flex-start; gap: 14px; padding: 12px 0; border-bottom: 1px solid #E8E4DF; }
.sv-kn-item:last-child { border-bottom: none; }
.sv-kn-dot { width: 22px; height: 22px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 10px; font-weight: 700; color: #fff; flex-shrink: 0; margin-top: 2px; }
.sv-kn-dot--0 { background: #E8956A; }
.sv-kn-dot--1 { background: #4A90A4; }
.sv-kn-dot--2 { background: #9B8EC4; }
.sv-kn-text { font-size: 14px; color: #3a3a3a; line-height: 1.7; flex: 1; }

/* 缩略图 */
.slide-controls { display: flex; align-items: center; padding: 12px 20px; background: #fff; border-radius: 0 0 12px 12px; border: 1px solid #e8ecf1; border-top: none; gap: 12px; }
.slide-thumbnails { display: flex; gap: 6px; flex: 1; overflow-x: auto; padding: 4px 0; }
.slide-thumb { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; cursor: pointer; border: 2px solid transparent; flex-shrink: 0; transition: all 0.2s; }
.slide-thumb.active { border-color: #4A90A4; box-shadow: 0 2px 8px rgba(74,144,164,0.25); transform: scale(1.1); }
.slide-thumb:hover { border-color: #94a3b8; }
.thumb-num { font-size: 12px; font-weight: 700; }

.auto-play-bar { display: flex; align-items: center; padding: 10px 20px; background: #fff; border-radius: 0 0 12px 12px; border: 1px solid #e8ecf1; border-top: none; }

/* 过渡动画 */
.slide-left-enter-active, .slide-left-leave-active,
.slide-right-enter-active, .slide-right-leave-active { transition: all 0.35s ease; }
.slide-left-enter { transform: translateX(60px); opacity: 0; }
.slide-left-leave-to { transform: translateX(-60px); opacity: 0; }
.slide-right-enter { transform: translateX(-60px); opacity: 0; }
.slide-right-leave-to { transform: translateX(60px); opacity: 0; }

/* ═══ 演示模式全屏 ═══ */
.ppt-slideshow:fullscreen, .ppt-slideshow:-webkit-full-screen {
  width: 100vw; height: 100vh; max-width: none; background: #000;
  display: flex; flex-direction: column; justify-content: center; align-items: center;
  overflow: hidden; border-radius: 0;
}
.ppt-slideshow:fullscreen .slideshow-toolbar,
.ppt-slideshow:-webkit-full-screen .slideshow-toolbar,
.ppt-slideshow:fullscreen .slide-controls,
.ppt-slideshow:-webkit-full-screen .slide-controls,
.ppt-slideshow:fullscreen .auto-play-bar,
.ppt-slideshow:-webkit-full-screen .auto-play-bar { display: none; }
.ppt-slideshow:fullscreen .slide-display,
.ppt-slideshow:-webkit-full-screen .slide-display { width: 100%; flex: 0 0 auto; display: flex; justify-content: center; }
.ppt-slideshow:fullscreen .slide-card,
.ppt-slideshow:-webkit-full-screen .slide-card {
  min-height: 0; width: min(96vw, calc(96vh * 16 / 9)); height: 72vh;
  border: none; border-radius: 10px; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,0.5);
}
.fs-nav-bar {
  position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 14px; z-index: 10;
  padding: 10px 22px; background: rgba(10, 13, 10, 0.82); backdrop-filter: blur(8px);
  border-radius: 40px; border: 1px solid rgba(255,255,255,0.08);
}
.fs-nav-page { color: #f8fafc; font-size: 15px; font-weight: 600; min-width: 64px; text-align: center; }
.fs-nav-hint { color: #94a3b8; font-size: 12px; }
</style>
