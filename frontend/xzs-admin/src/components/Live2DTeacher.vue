<template>
  <div class="live2d-teacher-container" :class="{ 'minimized': minimized, 'hidden': !visible }">
    <!-- 收起/展开按钮 -->
    <div class="live2d-toggle" @click="minimized = !minimized">
      <i :class="minimized ? 'el-icon-arrow-left' : 'el-icon-arrow-right'"></i>
    </div>

    <!-- 画布容器 -->
    <div v-show="!minimized" class="live2d-canvas-wrap">
      <canvas ref="live2dCanvas" class="live2d-canvas"></canvas>
      <!-- 加载状态 -->
      <div v-if="loading" class="live2d-loading">
        <div class="live2d-loading-spinner"></div>
        <span>AI 教师加载中...</span>
      </div>
    </div>

    <!-- 对话气泡 -->
    <transition name="bubble-fade">
      <div v-if="showBubble && !minimized" class="live2d-bubble">
        <div class="live2d-bubble-inner">{{ bubbleText }}</div>
        <div class="live2d-bubble-tail"></div>
      </div>
    </transition>

    <!-- 底部控制栏 -->
    <div v-show="!minimized" class="live2d-controls">
      <span class="live2d-badge">AI 助教</span>
      <div class="live2d-ctrl-btns">
        <el-tooltip content="切换模型" placement="top">
          <i class="el-icon-refresh" @click="switchModel"></i>
        </el-tooltip>
        <el-tooltip content="关闭" placement="top">
          <i class="el-icon-close" @click="visible = false"></i>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script>
/* eslint-disable */
export default {
  name: 'Live2DTeacher',
  props: {
    autoShow: { type: Boolean, default: false },
    welcomeMessages: { type: Array, default: () => [] }
  },
  data () {
    return {
      visible: true,
      minimized: false,
      loading: true,
      showBubble: false,
      bubbleText: '你好！我是你的AI学习助手，有什么可以帮你的？',
      bubbleTimer: null,
      modelIdx: 0,
      // 多个免费 Live2D 模型 CDN
      models: [
        'https://cdn.jsdelivr.net/npm/oh-my-live2d@latest/models/haru/haru_greeter_t03.model3.json',
        'https://cdn.jsdelivr.net/npm/oh-my-live2d@latest/models/haru/haru_greeter_t04.model3.json'
      ]
    }
  },
  watch: {
    autoShow (v) { if (v) { this.visible = true; this.minimized = false } }
  },
  mounted () {
    this.initLive2D()
    this.startBubble()
    // 暴露方法到全局
    window.$live2dTeacher = this
  },
  beforeDestroy () {
    this.clearBubbleTimer()
    if (this.live2dModel) {
      try { this.live2dModel.destroy() } catch (e) {}
      this.live2dModel = null
    }
    window.$live2dTeacher = null
  },
  methods: {
    async initLive2D () {
      try {
        // 动态加载 oh-my-live2d
        const OML2D = await import('oh-my-live2d')
        if (OML2D && OML2D.loadOhMyLive2D) {
          await OML2D.loadOhMyLive2D({
            path: this.models[0],
            target: this.$refs.live2dCanvas,
            dockedPosition: 'right',
            size: { width: 260, height: 360 },
            stageStyle: { backgroundColor: 'transparent' }
          })
          this.loading = false
        }
      } catch (e) {
        console.warn('Live2D 初始化失败，使用备选方案:', e.message)
        // 备选：绘制一个简单的 SVG 数字人形象
        this.drawFallbackAvatar()
      }
    },

    drawFallbackAvatar () {
      // 当 Live2D 不可用时，绘制一个美观的 SVG 头像作为备选
      var canvas = this.$refs.live2dCanvas
      if (!canvas) return
      canvas.width = 260
      canvas.height = 320
      var ctx = canvas.getContext('2d')

      // 身体
      var grad = ctx.createLinearGradient(130, 100, 130, 320)
      grad.addColorStop(0, '#CCFF00')
      grad.addColorStop(0.5, '#CCFF00')
      grad.addColorStop(1, '#A8D400')

      // 头部
      ctx.beginPath()
      ctx.arc(130, 110, 55, 0, Math.PI * 2)
      ctx.fillStyle = '#fbbf24'
      ctx.fill()
      ctx.strokeStyle = '#f59e0b'
      ctx.lineWidth = 2
      ctx.stroke()

      // 眼睛
      ctx.beginPath()
      ctx.arc(110, 100, 5, 0, Math.PI, true)
      ctx.strokeStyle = '#141A14'
      ctx.lineWidth = 2
      ctx.stroke()
      ctx.beginPath()
      ctx.arc(150, 100, 5, 0, Math.PI, true)
      ctx.stroke()

      // 嘴巴
      ctx.beginPath()
      ctx.arc(130, 125, 15, 0.1 * Math.PI, 0.9 * Math.PI)
      ctx.strokeStyle = '#141A14'
      ctx.lineWidth = 2
      ctx.stroke()

      // 身体
      ctx.beginPath()
      ctx.moveTo(80, 165)
      ctx.lineTo(180, 165)
      ctx.lineTo(200, 320)
      ctx.lineTo(60, 320)
      ctx.closePath()
      ctx.fillStyle = grad
      ctx.fill()

      // 领带/装饰
      ctx.beginPath()
      ctx.moveTo(130, 165)
      ctx.lineTo(145, 240)
      ctx.lineTo(130, 280)
      ctx.lineTo(115, 240)
      ctx.closePath()
      ctx.fillStyle = 'rgba(255,255,255,0.15)'
      ctx.fill()

      // 书本
      ctx.fillStyle = '#fff'
      ctx.fillRect(85, 200, 20, 28)
      ctx.strokeStyle = '#CCFF00'
      ctx.lineWidth = 1
      ctx.strokeRect(85, 200, 20, 28)

      this.loading = false
    },

    switchModel () {
      this.modelIdx = (this.modelIdx + 1) % this.models.length
      this.loading = true
      if (this.live2dModel) {
        try { this.live2dModel.destroy() } catch (e) {}
      }
      this.initLive2D()
    },

    say (text) {
      this.bubbleText = text
      this.showBubble = true
      this.clearBubbleTimer()
      this.bubbleTimer = setTimeout(() => { this.showBubble = false }, 5000)
    },

    startBubble () {
      var msgs = this.welcomeMessages.length ? this.welcomeMessages : [
        '你好！我是你的 AI 学习助手 👋',
        '有任何课程问题都可以问我哦~',
        '你可以让我讲解知识点、生成习题或推荐学习路径',
        '多智能体教学系统为你提供个性化学习体验！'
      ]
      var idx = 0
      var self = this
      this.bubbleTimer = setInterval(function () {
        self.say(msgs[idx % msgs.length])
        idx++
      }, 15000)
    },

    clearBubbleTimer () {
      if (this.bubbleTimer) { clearInterval(this.bubbleTimer); this.bubbleTimer = null }
    }
  }
}
</script>

<style scoped>
.live2d-teacher-container {
  position: fixed;
  right: 24px;
  bottom: 20px;
  z-index: 999;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.live2d-teacher-container.hidden {
  display: none;
}

.live2d-teacher-container.minimized {
  right: -200px;
}

/* ── 切换按钮 ── */
.live2d-toggle {
  position: absolute;
  left: -36px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 64px;
  background: rgba(10, 13, 10, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(var(--acid-rgb), 0.2);
  border-right: none;
  border-radius: 16px 0 0 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s;
  z-index: 2;
}

.live2d-toggle:hover {
  color: #fff;
  background: rgba(var(--acid-rgb), 0.5);
}

/* ── 画布 ── */
.live2d-canvas-wrap {
  position: relative;
  width: 260px;
  height: 340px;
  border-radius: 20px;
  overflow: hidden;
  background: radial-gradient(ellipse at 50% 100%, rgba(var(--acid-rgb), 0.05) 0%, transparent 60%),
              linear-gradient(180deg, transparent 0%, rgba(10, 13, 10, 0.02) 100%);
}

.live2d-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

/* ── 加载动画 ── */
.live2d-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(10, 13, 10, 0.6);
  backdrop-filter: blur(4px);
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  gap: 12px;
}

.live2d-loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(var(--acid-rgb), 0.2);
  border-top-color: var(--ink-5);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── 对话气泡 ── */
.live2d-bubble {
  position: absolute;
  right: 270px;
  top: 40px;
  max-width: 240px;
  z-index: 3;
}

.live2d-bubble-inner {
  background: rgba(10, 13, 10, 0.9);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(var(--acid-rgb), 0.25);
  border-radius: 16px;
  padding: 12px 16px;
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  line-height: 1.6;
  box-shadow: 0 8px 32px rgba(var(--acid-rgb), 0.15);
}

.live2d-bubble-tail {
  position: absolute;
  right: -8px;
  top: 20px;
  width: 16px;
  height: 16px;
  background: rgba(10, 13, 10, 0.9);
  border: 1px solid rgba(var(--acid-rgb), 0.25);
  border-left: none;
  border-top: none;
  transform: rotate(-45deg);
}

.bubble-fade-enter-active,
.bubble-fade-leave-active {
  transition: opacity 0.4s, transform 0.4s;
}

.bubble-fade-enter,
.bubble-fade-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* ── 控制栏 ── */
.live2d-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: rgba(10, 13, 10, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(var(--acid-rgb), 0.12);
  border-top: none;
  border-radius: 0 0 16px 16px;
}

.live2d-badge {
  font-size: 11px;
  font-weight: 600;
  color: rgba(var(--acid-rgb), 0.9);
  letter-spacing: 0.5px;
  background: rgba(var(--acid-rgb), 0.1);
  padding: 2px 10px;
  border-radius: 6px;
}

.live2d-ctrl-btns {
  display: flex;
  gap: 8px;
}

.live2d-ctrl-btns i {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
}

.live2d-ctrl-btns i:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}
</style>
