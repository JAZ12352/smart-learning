<template>
  <div class="bilibili-player-wrapper">
    <!-- Loading state -->
    <div v-if="loading" class="bl-loading">
      <div class="bl-loading-spinner"></div>
      <p class="bl-loading-text">正在加载B站视频...</p>
      <p class="bl-loading-sub">{{ videoUrl }}</p>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="bl-error">
      <div class="bl-error-icon"><i class="el-icon-warning"></i></div>
      <p class="bl-error-text">{{ error }}</p>
      <div class="bl-error-actions">
        <el-button size="small" type="primary" @click="retry"><i class="el-icon-refresh"></i> 重试</el-button>
      </div>
    </div>

    <!-- B站 iframe 嵌入播放器（最稳定可靠的方式） -->
    <div v-show="!loading && !error" class="bl-iframe-container">
      <iframe
        :src="embedUrl"
        class="bl-iframe"
        allowfullscreen="true"
        allow="autoplay; encrypted-media; fullscreen"
        scrolling="no"
        frameborder="0"
        sandbox="allow-scripts allow-same-origin allow-popups allow-presentation"
        @load="onIframeLoad"
        @error="onIframeError"
      />
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'BilibiliPlayer',

  props: {
    videoUrl: { type: String, required: true },
    autoplay: { type: Boolean, default: false }
  },

  data () {
    return {
      loading: true,
      error: null,
      videoInfo: null, // 后端返回的视频元数据
      embedUrl: '' // B站 iframe 嵌入地址
    }
  },

  computed: {
    apiBaseUrl () {
      return process.env.VUE_APP_URL || ''
    }
  },

  watch: {
    videoUrl () {
      this.reload()
    }
  },

  mounted () {
    this.init()
  },

  beforeDestroy () {
    // iframe 无需特殊清理
  },

  methods: {
    async init () {
      this.loading = true
      this.error = null

      // 先构建 iframe 嵌入地址（立即可用）
      this.embedUrl = this.buildEmbedUrl(this.videoUrl)

      // 尝试调用后端API获取视频元数据（增强体验）
      try {
        const res = await axios.post(this.apiBaseUrl + '/api/admin/bilibili/resolve', {
          url: this.videoUrl,
          quality: 80
        })

        if (res.data && res.data.code === 1 && res.data.response) {
          this.videoInfo = res.data.response
          this.$emit('ready', this.videoInfo)
        }
      } catch (e) {
        // 后端API失败不影响播放（iframe embed 仍然可用）
        console.warn('B站元数据获取失败，使用iframe嵌入播放:', e.message)
      }

      this.loading = false
    },

    onIframeLoad () {
      // iframe 加载完成
    },

    onIframeError () {
      this.error = 'B站视频加载失败，请检查网络连接或视频是否可用'
      this.$emit('error', { message: this.error })
    },

    reload () {
      this.loading = true
      this.error = null
      this.embedUrl = this.buildEmbedUrl(this.videoUrl)
      this.$nextTick(() => { this.loading = false })
    },

    retry () {
      this.reload()
    },

    /**
     * 构建B站 iframe 嵌入地址
     * 支持 AV号、BV号、b23.tv 短链接
     */
    buildEmbedUrl (url) {
      if (!url) return ''

      // AV号: bilibili.com/video/av12345
      var avMatch = url.match(/bilibili\.com\/video\/av(\d+)/i)
      if (avMatch) {
        var aid = avMatch[1]
        var pm = url.match(/[?&]p=(\d+)/i)
        return '//player.bilibili.com/player.html?aid=' + aid +
          '&page=' + (pm ? pm[1] : '1') +
          '&high_quality=1&as_wide=1'
      }

      // BV号: bilibili.com/video/BVxxxxxx
      var bvMatch = url.match(/bilibili\.com\/video\/(BV[a-zA-Z0-9]+)/i)
      if (bvMatch) {
        var bvid = bvMatch[1]
        var p2 = url.match(/[?&]p=(\d+)/i)
        return '//player.bilibili.com/player.html?bvid=' + bvid +
          '&page=' + (p2 ? p2[1] : '1') +
          '&high_quality=1&as_wide=1'
      }

      // b23.tv 短链接或其他格式直接尝试
      return url
    }
  }
}
</script>

<style scoped>
.bilibili-player-wrapper {
  width: 100%;
  background: #000;
  border-radius: 10px;
  overflow: hidden;
  min-height: 400px;
  position: relative;
}

/* Loading */
.bl-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: var(--ink-2);
  gap: 10px;
}
.bl-loading-spinner {
  width: 40px; height: 40px;
  border: 3px solid rgba(var(--acid-rgb), 0.15);
  border-top-color: var(--ink-5);
  border-radius: 50%;
  animation: bl-spin 0.8s linear infinite;
}
@keyframes bl-spin { to { transform: rotate(360deg); } }
.bl-loading-text { font-size: 14px; color: #e2e8f0; margin: 0; }
.bl-loading-sub { font-size: 11px; color: #64748b; margin: 0; max-width: 90%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Error */
.bl-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: var(--ink-2);
  gap: 10px;
}
.bl-error-icon { font-size: 40px; color: #ef4444; }
.bl-error-text { font-size: 14px; color: #94a3b8; margin: 0; }
.bl-error-actions { margin-top: 8px; }

/* Iframe container */
.bl-iframe-container {
  position: relative;
  padding-top: 56.25%; /* 16:9 */
  background: #000;
  border-radius: 10px;
  overflow: hidden;
}
.bl-iframe {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  border: 0;
}
</style>
