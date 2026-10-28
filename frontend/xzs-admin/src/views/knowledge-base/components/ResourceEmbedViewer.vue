<template>
  <div class="resource-embed-viewer" v-if="visible" :class="{'is-fullscreen': fullscreen}">
    <div class="rev-overlay" @click="close"></div>
    <div class="rev-container">
      <!-- ═══ Header ═══ -->
      <div class="rev-header">
        <div class="rev-header-left">
          <span class="rev-type-icon">{{ typeIcon }}</span>
          <div class="rev-title-info">
            <h3>{{ title || '资源预览' }}</h3>
            <span class="rev-source" v-if="source">{{ source }}</span>
          </div>
        </div>
        <div class="rev-header-right">
          <button class="rev-btn rev-btn-icon" @click="openOriginal" title="在新窗口打开"><i class="el-icon-link"></i></button>
          <button class="rev-btn rev-btn-icon" @click="downloadFile" title="下载文件"><i class="el-icon-download"></i></button>
          <button class="rev-btn rev-btn-icon" @click="toggleFullscreen" :title="fullscreen ? '退出全屏' : '全屏'">
            <i :class="fullscreen ? 'el-icon-zoom-out' : 'el-icon-full-screen'"></i>
          </button>
          <button class="rev-btn rev-btn-icon" @click="close" title="关闭"><i class="el-icon-close"></i></button>
        </div>
      </div>

      <!-- ═══ Loading ═══ -->
      <div class="rev-body" v-if="loading">
        <div class="rev-loading">
          <div class="rev-spinner"></div>
          <p>正在加载资源...</p>
          <span class="rev-loading-tip" v-if="attemptCount > 1">尝试第 {{ attemptCount }} 种预览方式</span>
        </div>
      </div>

      <!-- ═══ Error ═══ -->
      <div class="rev-body" v-else-if="error">
        <div class="rev-error">
          <i class="el-icon-warning-outline"></i>
          <h3>预览加载失败</h3>
          <p>{{ errorMsg }}</p>
          <div class="rev-error-actions">
            <button class="rev-btn rev-btn-primary" @click="retry">重新加载</button>
            <button class="rev-btn rev-btn-outline" @click="openOriginal">在新窗口打开</button>
            <button class="rev-btn rev-btn-outline" @click="downloadFile">下载文件</button>
          </div>
        </div>
      </div>

      <!-- ═══ Content ═══ -->
      <div class="rev-body" v-else>
        <div v-if="hasEmbedUrl" class="rev-embed-wrap">
          <iframe
            ref="embedFrame"
            :src="embedUrl"
            class="rev-iframe"
            allowfullscreen
            allow="autoplay; fullscreen"
            @load="onIframeLoad"
            @error="onIframeError"
          />
        </div>
        <!-- Fallback: show info card -->
        <div v-else class="rev-fallback">
          <div class="rev-fallback-icon">{{ typeIcon }}</div>
          <h3>{{ title }}</h3>
          <p>{{ source }}</p>
          <div class="rev-fallback-meta">
            <span v-if="fileType">{{ fileType.toUpperCase() }}</span>
            <span v-if="fileSize">{{ fileSize }}</span>
          </div>
          <div class="rev-fallback-actions">
            <button class="rev-btn rev-btn-primary" @click="openOriginal">新窗口打开</button>
            <button class="rev-btn rev-btn-outline" @click="copyLink">复制链接</button>
            <button class="rev-btn rev-btn-outline" @click="downloadFile">下载文件</button>
            <button v-if="originalUrl" class="rev-btn rev-btn-outline" @click="tryOfficeViewer">Office 在线查看</button>
            <button v-if="originalUrl" class="rev-btn rev-btn-outline" @click="tryGoogleViewer">Google 查看</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ResourceEmbedViewer',
  data () {
    return {
      visible: false,
      loading: false,
      error: false,
      errorMsg: '',
      title: '',
      source: '',
      originalUrl: '',
      fileType: '',
      fileSize: '',
      embedUrl: '',
      fullscreen: false,
      attemptCount: 0,
      // Multi-level embed strategy
      strategies: [
        { name: 'office', url: null },
        { name: 'google', url: null },
        { name: 'direct', url: null }
      ],
      currentStrategy: 0
    }
  },
  computed: {
    typeIcon () {
      var t = this.fileType || ''
      if (t === 'pdf') return '📕'
      if (t === 'ppt' || t === 'pptx') return '📊'
      if (t === 'doc' || t === 'docx') return '📄'
      if (t === 'xls' || t === 'xlsx') return '📈'
      if (t && (t.startsWith('image/') || t === 'jpg' || t === 'png' || t === 'gif')) return '🖼️'
      return '📁'
    },
    hasEmbedUrl () {
      return !!this.embedUrl && !this.loading && !this.error
    }
  },
  methods: {
    /**
     * 打开资源预览
     * @param {Object} options
     * @param {string} options.url - 资源URL（必须）
     * @param {string} options.title - 资源标题
     * @param {string} options.source - 来源
     * @param {string} options.fileType - 文件类型 (pdf, pptx, docx, etc.)
     * @param {string} options.fileSize - 文件大小
     */
    open (options) {
      var opts = options || {}
      if (!opts.url) {
        console.warn('ResourceEmbedViewer: url is required')
        return
      }
      this.reset()
      this.visible = true
      this.loading = true
      this.title = opts.title || '资源预览'
      this.source = opts.source || ''
      this.originalUrl = opts.url
      this.fileType = opts.fileType || this.detectFileType(opts.url)
      this.fileSize = opts.fileSize || ''

      // Build multi-level strategies
      this.buildStrategies(opts.url, this.fileType)

      // Start attempting
      this.$nextTick(function () {
        this.tryNextStrategy()
      }.bind(this))
    },

    close () {
      this.visible = false
      this.reset()
    },

    reset () {
      this.loading = false
      this.error = false
      this.errorMsg = ''
      this.embedUrl = ''
      this.currentStrategy = 0
      this.attemptCount = 0
      this.strategies = [
        { name: 'office', url: null },
        { name: 'google', url: null },
        { name: 'direct', url: null }
      ]
    },

    buildStrategies (url, fileType) {
      var ft = (fileType || '').toLowerCase()
      var encoded = encodeURIComponent(url)

      // Strategy 1: Office Online Viewer (works for docx, pptx, xlsx, pdf)
      if (['pptx', 'ppt', 'docx', 'doc', 'xlsx', 'xls', 'pdf'].indexOf(ft) >= 0 || this.isLikelyDocument(url)) {
        this.strategies[0] = {
          name: 'office',
          url: 'https://view.officeapps.live.com/op/embed.aspx?src=' + encoded
        }
      }
      // Office viewer can also handle PDFs
      if (ft === 'pdf') {
        this.strategies[0] = {
          name: 'office',
          url: 'https://view.officeapps.live.com/op/embed.aspx?src=' + encoded
        }
      }

      // Strategy 2: Google Docs Viewer (works for pdf, ppt, doc)
      if (['pdf', 'ppt', 'pptx', 'doc', 'docx'].indexOf(ft) >= 0 || this.isLikelyDocument(url)) {
        this.strategies[1] = {
          name: 'google',
          url: 'https://docs.google.com/viewer?url=' + encoded + '&embedded=true'
        }
      }

      // Strategy 3: Direct embed
      // PDFs and images can be directly embedded
      if (ft === 'pdf') {
        this.strategies[2] = { name: 'direct', url: url + '#view=FitH' }
      } else if (ft && ['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp'].indexOf(ft) >= 0) {
        this.strategies[2] = { name: 'direct', url: url }
      } else if (url.match(/\.(pdf|jpg|jpeg|png|gif|svg|webp)(\?|#|$)/i)) {
        this.strategies[2] = { name: 'direct', url: url }
      } else if (ft === 'html' || ft === 'htm' || url.match(/\.(html?)(\?|#|$)/i)) {
        this.strategies[2] = { name: 'direct', url: url }
      }
      // If no strategy was built, use direct as last resort
      var hasAny = this.strategies.some(function (s) { return !!s.url })
      if (!hasAny) {
        this.strategies[2] = { name: 'direct', url: url }
      }
    },

    tryNextStrategy () {
      if (this.currentStrategy >= this.strategies.length) {
        // All strategies exhausted
        this.loading = false
        this.error = true
        this.errorMsg = '所有预览方式均无法加载此资源'
        return
      }

      var strategy = this.strategies[this.currentStrategy]
      if (!strategy || !strategy.url) {
        this.currentStrategy++
        this.tryNextStrategy()
        return
      }

      this.attemptCount = this.currentStrategy + 1
      this.embedUrl = strategy.url

      // Set a timeout for this strategy
      var self = this
      this._strategyTimer = setTimeout(function () {
        // If still loading after 15 seconds, try next
        if (self.loading && self.currentStrategy < self.strategies.length - 1) {
          self.currentStrategy++
          self.tryNextStrategy()
        } else if (self.loading) {
          self.loading = false
          self.error = true
          self.errorMsg = '资源加载超时，请尝试下载后查看'
        }
      }, 15000)
    },

    onIframeLoad () {
      clearTimeout(this._strategyTimer)
      this.loading = false
      this.error = false
    },

    onIframeError () {
      clearTimeout(this._strategyTimer)
      // Try next strategy
      if (this.currentStrategy < this.strategies.length - 1) {
        this.currentStrategy++
        this.tryNextStrategy()
      } else {
        this.loading = false
        this.error = true
        this.errorMsg = '资源加载失败，可能是网络问题或文件格式不支持'
      }
    },

    retry () {
      this.currentStrategy = 0
      this.attemptCount = 0
      this.error = false
      this.errorMsg = ''
      this.embedUrl = ''
      this.loading = true
      this.buildStrategies(this.originalUrl, this.fileType)
      this.tryNextStrategy()
    },

    toggleFullscreen () {
      this.fullscreen = !this.fullscreen
    },

    openOriginal () {
      if (this.originalUrl) {
        window.open(this.originalUrl, '_blank')
      }
    },

    downloadFile () {
      if (this.originalUrl) {
        var a = document.createElement('a')
        a.href = this.originalUrl
        a.download = this.title || ''
        a.target = '_blank'
        a.rel = 'noopener'
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
      }
    },

    copyLink () {
      if (!this.originalUrl) return
      var ta = document.createElement('textarea')
      ta.value = this.originalUrl
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      if (this.$message) {
        this.$message({ type: 'success', message: '链接已复制', duration: 1500 })
      }
    },

    tryOfficeViewer () {
      this.loading = true
      this.error = false
      this.embedUrl = 'https://view.officeapps.live.com/op/embed.aspx?src=' + encodeURIComponent(this.originalUrl)
    },

    tryGoogleViewer () {
      this.loading = true
      this.error = false
      this.embedUrl = 'https://docs.google.com/viewer?url=' + encodeURIComponent(this.originalUrl) + '&embedded=true'
    },

    detectFileType (url) {
      if (!url) return ''
      var u = url.toLowerCase()
      if (u.indexOf('.pdf') >= 0) return 'pdf'
      if (u.indexOf('.pptx') >= 0) return 'pptx'
      if (u.indexOf('.ppt') >= 0) return 'ppt'
      if (u.indexOf('.docx') >= 0) return 'docx'
      if (u.indexOf('.doc') >= 0) return 'doc'
      if (u.indexOf('.xlsx') >= 0) return 'xlsx'
      if (u.indexOf('.xls') >= 0) return 'xls'
      if (u.indexOf('.jpg') >= 0 || u.indexOf('.jpeg') >= 0) return 'jpg'
      if (u.indexOf('.png') >= 0) return 'png'
      if (u.indexOf('.gif') >= 0) return 'gif'
      if (u.indexOf('.svg') >= 0) return 'svg'
      return 'bin'
    },

    isLikelyDocument (url) {
      var docExts = /\.(pdf|ppt|pptx|doc|docx|xls|xlsx|odt|odp|ods)(\?|#|$)/i
      return docExts.test(url)
    }
  },
  beforeDestroy () {
    clearTimeout(this._strategyTimer)
  }
}
</script>

<style scoped>
/* ═══ Overlay ═══ */
.resource-embed-viewer {
  position: fixed; inset: 0; z-index: 3000;
  display: flex; align-items: center; justify-content: center;
  padding: 16px;
}
.rev-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.6);
  backdrop-filter: blur(6px);
}
.rev-container {
  position: relative; z-index: 1; width: 100%; max-width: 1100px;
  max-height: 90vh; display: flex; flex-direction: column;
  background: #fff; border-radius: 16px; overflow: hidden;
  box-shadow: 0 24px 80px rgba(0,0,0,0.2);
}
.resource-embed-viewer.is-fullscreen .rev-container {
  max-width: 100%; max-height: 100vh; border-radius: 0;
}
.resource-embed-viewer.is-fullscreen { padding: 0; }

/* ═══ Header ═══ */
.rev-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid rgba(0,0,0,0.06);
  background: var(--ink-3);
}
.rev-header-left { display: flex; align-items: center; gap: 12px; }
.rev-type-icon { font-size: 24px; }
.rev-title-info h3 { font-size: 15px; font-weight: 600; color: #1e293b; margin: 0; }
.rev-source { font-size: 12px; color: #94a3b8; }
.rev-header-right { display: flex; gap: 4px; }
.rev-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; font-size: 13px; font-weight: 500;
  border: none; border-radius: 8px; cursor: pointer;
  transition: all 0.2s; font-family: inherit;
}
.rev-btn-icon {
  width: 34px; height: 34px; padding: 0; justify-content: center;
  background: transparent; color: #64748b;
}
.rev-btn-icon:hover { background: rgba(0,0,0,0.06); color: #1e293b; }
.rev-btn-primary {
  background: linear-gradient(135deg, var(--acid), #7c6ff7);
  color: #fff; box-shadow: 0 2px 8px rgba(var(--acid-rgb), 0.25);
}
.rev-btn-primary:hover { box-shadow: 0 4px 16px rgba(var(--acid-rgb), 0.35); }
.rev-btn-outline {
  background: #fff; color: #1e293b; border: 1px solid rgba(0,0,0,0.1);
}
.rev-btn-outline:hover { border-color: var(--ink-5); color: var(--acid); }

/* ═══ Body ═══ */
.rev-body { flex: 1; min-height: 60vh; display: flex; flex-direction: column; }

/* ═══ Loading ═══ */
.rev-loading {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 12px;
}
.rev-spinner {
  width: 40px; height: 40px; border: 3px solid rgba(var(--acid-rgb), 0.12);
  border-top-color: var(--ink-5); border-radius: 50%;
  animation: rev-spin 0.8s linear infinite;
}
@keyframes rev-spin { to { transform: rotate(360deg); } }
.rev-loading p { font-size: 15px; color: #64748b; margin: 0; }
.rev-loading-tip { font-size: 12px; color: #94a3b8; }

/* ═══ Error ═══ */
.rev-error {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 8px;
  padding: 40px; text-align: center;
}
.rev-error i { font-size: 48px; color: #f59e0b; }
.rev-error h3 { font-size: 17px; color: #1e293b; margin: 0; }
.rev-error p { font-size: 14px; color: #94a3b8; margin: 0; }
.rev-error-actions { display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap; justify-content: center; }

/* ═══ Embed ═══ */
.rev-embed-wrap { flex: 1; position: relative; min-height: 60vh; }
.rev-iframe {
  position: absolute; inset: 0; width: 100%; height: 100%;
  border: none; background: var(--ink-3);
}

/* ═══ Fallback ═══ */
.rev-fallback {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 8px;
  padding: 48px 40px; text-align: center;
  background: linear-gradient(135deg, #f8fafc, #f0f2f5);
}
.rev-fallback-icon { font-size: 56px; margin-bottom: 8px; }
.rev-fallback h3 { font-size: 18px; color: #1e293b; margin: 0; }
.rev-fallback > p { font-size: 14px; color: #94a3b8; margin: 0; }
.rev-fallback-meta { display: flex; gap: 8px; margin: 8px 0; }
.rev-fallback-meta span {
  padding: 2px 10px; border-radius: 6px; font-size: 11px;
  font-weight: 500; background: rgba(var(--acid-rgb), 0.08); color: var(--acid);
}
.rev-fallback-actions { display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap; justify-content: center; }
</style>
