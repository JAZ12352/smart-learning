<template>
  <div class="digital-teacher" :class="{ speaking: isSpeaking, listening: isListening }">
    <!-- 虚拟教师 SVG 形象（Q版动漫风格） -->
    <div class="teacher-avatar" @click="toggleSpeech">
      <svg viewBox="0 0 200 240" xmlns="http://www.w3.org/2000/svg" class="teacher-svg">
        <defs>
          <linearGradient id="hairGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#a78bfa"/>
            <stop offset="100%" style="stop-color:#7c3aed"/>
          </linearGradient>
          <linearGradient id="dressGrad" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" style="stop-color:#f0abfc"/>
            <stop offset="100%" style="stop-color:#e879f9"/>
          </linearGradient>
        </defs>
        <ellipse cx="100" cy="25" rx="18" ry="6" fill="#fef08a" opacity="0.4" class="teacher-halo"/>
        <rect x="65" y="150" width="70" height="50" rx="14" fill="url(#dressGrad)"/>
        <rect x="72" y="165" width="56" height="35" rx="10" fill="#fce7f3" opacity="0.6"/>
        <polygon points="92,150 100,160 108,150" fill="#f472b6"/>
        <circle cx="100" cy="155" r="4" fill="#ec4899"/>
        <circle cx="100" cy="80" r="48" fill="#ffe4d6"/>
        <ellipse cx="100" cy="48" rx="48" ry="28" fill="url(#hairGrad)"/>
        <path d="M52,60 Q48,35 60,28 Q72,22 80,35" fill="url(#hairGrad)"/>
        <path d="M148,60 Q152,35 140,28 Q128,22 120,35" fill="url(#hairGrad)"/>
        <ellipse cx="52" cy="65" rx="16" ry="24" fill="url(#hairGrad)" transform="rotate(-15,52,65)"/>
        <ellipse cx="148" cy="65" rx="16" ry="24" fill="url(#hairGrad)" transform="rotate(15,148,65)"/>
        <path d="M70,48 Q80,40 90,48 Q95,38 100,48 Q105,38 110,48 Q120,40 130,48" fill="url(#hairGrad)"/>
        <g class="teacher-eyes">
          <ellipse cx="82" cy="78" rx="11" ry="12" fill="#fff"/>
          <ellipse cx="118" cy="78" rx="11" ry="12" fill="#fff"/>
          <ellipse cx="84" cy="78" rx="7" ry="8" fill="#6d28d9"/>
          <ellipse cx="120" cy="78" rx="7" ry="8" fill="#6d28d9"/>
          <ellipse cx="85" cy="76" rx="4" ry="4" fill="#fff"/>
          <ellipse cx="121" cy="76" rx="4" ry="4" fill="#fff"/>
          <circle cx="82" cy="74" r="2" fill="#fff" opacity="0.8"/>
          <circle cx="118" cy="74" r="2" fill="#fff" opacity="0.8"/>
        </g>
        <ellipse cx="68" cy="90" rx="8" ry="4" fill="#fca5a5" opacity="0.4"/>
        <ellipse cx="132" cy="90" rx="8" ry="4" fill="#fca5a5" opacity="0.4"/>
        <g class="teacher-mouth">
          <path d="M93,96 Q100,101 107,96" fill="none" stroke="#e11d48" stroke-width="2" stroke-linecap="round">
            <animate attributeName="d" values="M93,96 Q100,101 107,96;M93,98 Q100,105 107,98;M93,96 Q100,101 107,96" dur="0.6s" repeatCount="indefinite" begin="indefinite" id="mouthAnim"/>
          </path>
        </g>
        <g class="teacher-arms">
          <ellipse cx="55" cy="158" rx="12" ry="8" fill="#ffe4d6" class="arm-left"/>
          <ellipse cx="145" cy="158" rx="12" ry="8" fill="#ffe4d6" class="arm-right"/>
        </g>
        <line x1="152" y1="155" x2="185" y2="125" stroke="#a78bfa" stroke-width="2.5" stroke-linecap="round" class="pointer" v-if="showPointer"/>
        <circle cx="185" cy="125" r="4" fill="#fef08a" class="pointer"/>
      </svg>
      <div class="teacher-status">
        <span class="status-dot" :class="{ active: isSpeaking }"></span>
        <span class="status-text">{{ isSpeaking ? '讲解中...' : isListening ? '聆听中...' : '待命' }}</span>
      </div>
    </div>

    <!-- 语音气泡 -->
    <transition name="bubble">
      <div v-if="currentText" class="speech-bubble">
        <div class="bubble-content" v-html="currentText"></div>
      </div>
    </transition>

    <!-- 控制栏 -->
    <div class="teacher-controls" v-if="showControls">
      <el-button size="mini" circle :type="isSpeaking ? 'warning' : 'primary'" @click="toggleSpeech" :title="isSpeaking ? '暂停' : '讲解'">
        <i :class="isSpeaking ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
      </el-button>
      <el-button size="mini" circle :type="isListening ? 'success' : 'default'" @click="toggleListening" title="语音交互">
        <i class="el-icon-microphone"></i>
      </el-button>
      <el-button size="mini" circle @click="$emit('close')" title="关闭">
        <i class="el-icon-close"></i>
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DigitalTeacher',
  props: {
    text: { type: String, default: '' },
    autoSpeak: { type: Boolean, default: false },
    showControls: { type: Boolean, default: true },
    showPointer: { type: Boolean, default: false }
  },
  data () {
    return { isSpeaking: false, isListening: false, currentText: '' }
  },
  watch: {
    text (val) {
      this.currentText = val
      if (val && this.autoSpeak) this.startSpeaking()
    }
  },
  methods: {
    toggleSpeech () {
      if (this.isSpeaking) this.stopSpeaking()
      else this.startSpeaking()
    },
    startSpeaking () {
      this.isSpeaking = true
      this.isListening = false
      // 触发嘴巴动画
      const mouth = document.getElementById('mouthAnim')
      if (mouth) mouth.beginElement()
      this.$emit('speak-start')
    },
    stopSpeaking () {
      this.isSpeaking = false
      this.$emit('speak-stop')
    },
    toggleListening () {
      this.isListening = !this.isListening
      if (this.isListening) this.isSpeaking = false
      this.$emit(this.isListening ? 'listen-start' : 'listen-stop')
    },
    speak (text) {
      this.currentText = text
      this.startSpeaking()
    }
  }
}
</script>

<style scoped>
.digital-teacher {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.teacher-avatar {
  width: 140px;
  height: 170px;
  cursor: pointer;
  transition: transform 0.3s;
  position: relative;
}
.teacher-avatar:hover { transform: scale(1.05); }
.teacher-svg { width: 100%; height: 100%; }

.teacher-arms .arm-left {
  animation: waveLeft 3s ease-in-out infinite;
  transform-origin: 52px 154px;
}
.teacher-arms .arm-right {
  animation: waveRight 3s ease-in-out infinite;
  transform-origin: 148px 154px;
}
.speaking .arm-left { animation-duration: 1.5s; }
.speaking .arm-right { animation-duration: 1.5s; }

@keyframes waveLeft {
  0%, 100% { transform: rotate(0deg); }
  50% { transform: rotate(-10deg); }
}
@keyframes waveRight {
  0%, 100% { transform: rotate(0deg); }
  50% { transform: rotate(10deg); }
}

.teacher-eyes { animation: blink 4s ease-in-out infinite; }
.speaking .teacher-eyes { animation-duration: 2s; }
@keyframes blink {
  0%, 95%, 100% { transform: scaleY(1); }
  97% { transform: scaleY(0.1); }
}

.pointer {
  animation: point 2s ease-in-out infinite;
  transform-origin: 148px 154px;
}
@keyframes point {
  0%, 100% { transform: rotate(0deg); }
  50% { transform: rotate(-15deg); }
}

.teacher-status {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255,255,255,0.95);
  padding: 2px 10px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  font-size: 11px;
  color: #64748b;
  white-space: nowrap;
}
.status-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #cbd5e1;
  transition: all 0.3s;
}
.status-dot.active {
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34,197,94,0.5);
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.speech-bubble {
  position: absolute;
  top: -10px;
  right: -220px;
  width: 200px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  font-size: 13px;
  line-height: 1.6;
  color: #1e293b;
  z-index: 10;
}
.speech-bubble::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 20px;
  border: 8px solid transparent;
  border-right-color: #fff;
}
.bubble-content { max-height: 120px; overflow-y: auto; }
.bubble-content >>> p { margin: 4px 0; }
.bubble-content >>> strong { color: #4a90d9; }

.bubble-enter-active { animation: bubbleIn 0.3s ease; }
.bubble-leave-active { animation: bubbleIn 0.3s ease reverse; }
@keyframes bubbleIn {
  from { opacity: 0; transform: translateY(10px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.teacher-controls {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}
</style>
