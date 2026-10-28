<template>
  <div class="dialogue-container">
    <!-- Left: Profile Sidebar -->
    <div class="profile-sidebar">
      <div class="sidebar-header">
        <h3 class="sidebar-title">画像构建进度</h3>
        <div class="progress-summary">
          <span class="progress-count">{{ filledCount }}</span>
          <span class="progress-total">/ {{ dimensions.length }}</span>
        </div>
      </div>

      <div class="progress-bar-wrap">
        <div class="progress-bar-bg">
          <div class="progress-bar-fill" :style="{ width: progressPercent + '%' }" />
        </div>
        <span class="progress-percent">{{ progressPercent }}%</span>
      </div>

      <div class="progress-list">
        <div
          v-for="dim in dimensions"
          :key="dim.key"
          class="progress-item"
          :class="{ filled: profile[dim.field] }"
        >
          <span class="progress-icon">{{ dim.icon }}</span>
          <span class="progress-label">{{ dim.label }}</span>
          <span class="progress-status">
            <span v-if="profile[dim.field]" class="status-yes">&#10003;</span>
            <span v-else class="status-no">○</span>
          </span>
        </div>
      </div>

      <el-button
        size="small"
        class="view-profile-btn"
        @click="viewProfile"
      >
        查看完整画像
      </el-button>
    </div>

    <!-- Right: Chat Panel -->
    <div class="chat-panel">
      <!-- Messages -->
      <div class="chat-messages" ref="messageList">
        <div
          v-for="(msg, i) in messages"
          :key="i"
          :class="['message', msg.role]"
        >
          <div class="message-avatar">
            <span v-if="msg.role === 'user'">👤</span>
            <span v-else>🤖</span>
          </div>
          <div class="message-bubble">
            <div class="message-content" v-html="formatContent(msg.content)" />
          </div>
        </div>

        <!-- Streaming message -->
        <div v-if="streaming" class="message assistant">
          <div class="message-avatar">
            <span>🤖</span>
          </div>
          <div class="message-bubble streaming-bubble">
            <div class="message-content">
              {{ streamContent }}<span class="cursor-blink">▊</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="chat-input-area">
        <div class="input-wrap">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            placeholder="描述你的学习情况、兴趣方向、遇到的困难..."
            class="chat-textarea"
            @keydown.enter.native="handleKeydown"
          />
        </div>
        <div class="input-actions">
          <span class="input-hint">Enter 发送，Shift+Enter 换行</span>
          <el-button
            type="primary"
            class="send-btn"
            :loading="streaming"
            @click="sendMessage"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import studentProfileApi from '@/api/studentProfile'

export default {
  name: 'StudentProfileDialogue',
  data () {
    return {
      userId: null,
      profile: {},
      messages: [],
      input: '',
      streaming: false,
      streamContent: '',
      hasInitialized: false,
      dimensions: [
        { key: 'knowledgeLevel', label: '知识水平', icon: '📚', field: 'knowledgeLevel' },
        { key: 'cognitiveStyle', label: '认知风格', icon: '🧠', field: 'cognitiveStyle' },
        { key: 'weakPoints', label: '薄弱环节', icon: '⚠️', field: 'weakPoints' },
        { key: 'learningGoals', label: '学习目标', icon: '🎯', field: 'learningGoals' },
        { key: 'learningPace', label: '学习节奏', icon: '⏱️', field: 'learningPace' },
        { key: 'interests', label: '兴趣方向', icon: '💡', field: 'interests' },
        { key: 'learningStyle', label: '学习风格', icon: '📖', field: 'learningStyle' },
        { key: 'motivationLevel', label: '动机水平', icon: '🔥', field: 'motivationLevel' }
      ]
    }
  },
  computed: {
    filledCount () {
      var self = this
      return this.dimensions.filter(function (d) {
        return self.profile[d.field]
      }).length
    },
    progressPercent () {
      return Math.round((this.filledCount / this.dimensions.length) * 100)
    }
  },
  created () {
    // 优级修正：URL 携带的目标学生 ID 必须高于当前登录账号 ID，
    // 否则从画像概览页跳转过来时会错误加载管理员自己的画像
    var queryId = parseInt(this.$route.query.userId)
    this.userId = (queryId > 0 && queryId) || this.$store.getters.userId || 1
  },
  mounted () {
    this.loadProfile()
  },
  // keep-alive 重新激活时刷新画像（解决画像概览页重新画像后进度不刷新的问题）
  activated () {
    this.loadProfile()
  },
  methods: {
    async loadProfile () {
      try {
        var res = await studentProfileApi.getProfile(this.userId)
        if (res && res.code === 1 && res.response) {
          var prevFilled = this.hasInitialized ? this.filledCount : -1
          this.profile = res.response
          var currFilled = this.filledCount
          // 首次进入 或 画像被重置（进度降低）时，重置对话并生成动态欢迎语
          if (!this.hasInitialized || currFilled < prevFilled) {
            this.messages = [{ role: 'assistant', content: this.buildWelcomeMessage() }]
            this.hasInitialized = true
          }
        }
      } catch (e) {
        console.error('加载画像失败:', e)
      }
    },

    // 根据未填维度动态生成欢迎语，只提问还没构建的维度
    buildWelcomeMessage () {
      var self = this
      var unfilled = this.dimensions.filter(function (d) {
        return !self.profile[d.field]
      })
      if (unfilled.length === 0) {
        return '画像已全部构建完成 ✅。如需调整某维度可直接告诉我，或前往画像概览页查看。'
      }
      var labels = unfilled.map(function (d) { return d.label })
      var firstDim = unfilled[0]
      var prompts = {
        knowledgeLevel: '你目前在学什么课程？基础如何？',
        cognitiveStyle: '学新知识喜欢先看框架还是直接做实例？',
        weakPoints: '哪些知识点觉得吃力？',
        learningGoals: '这学期想达到什么目标？',
        learningPace: '喜欢快节奏还是稳扎稳打？',
        interests: '对哪些技术方向感兴趣？',
        learningStyle: '偏好看视频、读书还是做项目？',
        motivationLevel: '给学习动力打 1-10 分？'
      }
      return '还需完善 ' + unfilled.length + ' 个维度：' + labels.join('、') + '\n' + prompts[firstDim.field]
    },

    handleKeydown (e) {
      if (e.shiftKey) return
      e.preventDefault()
      e.stopPropagation()
      this.sendMessage()
    },

    async sendMessage () {
      var text = this.input.trim()
      if (!text || this.streaming) return

      this.messages.push({ role: 'user', content: text })
      this.input = ''
      this.streaming = true
      this.streamContent = ''

      this.scrollToBottom()

      var conversationMessages = this.messages.map(function (m) {
        return { role: m.role, content: m.content }
      })

      try {
        await studentProfileApi.sendDialogue(this.userId, {
          message: text,
          messages: conversationMessages
        }, (chunk, full) => {
          this.streamContent = full
          this.scrollToBottom()
        })

        this.messages.push({ role: 'assistant', content: this.streamContent })
        this.streamContent = ''

        await this.tryParseProfileDelta(this.messages[this.messages.length - 1].content)
        await this.loadProfile()
      } catch (e) {
        console.error('发送失败:', e)
        this.messages.push({ role: 'assistant', content: '抱歉，发生了错误，请重试。' })
      } finally {
        this.streaming = false
        this.scrollToBottom()
      }
    },

    tryParseProfileDelta (content) {
      if (!content) return Promise.resolve()
      // 正则说明：
      //   值用 (.+?) 非贪婪匹配，允许中文/标点/空格，不跨行
      //   前瞻终止符包含换行、句号、分号、【】[]() 或文本结尾
      //   这样能同时匹配「【维度名：值】」「维度名：值。」「维度名：值\n」等格式
      var patterns = {
        knowledgeLevel: /(?:知识水平|知识基础)[：:]\s*(.+?)(?=[\n。；;】\]\)]|$)/,
        cognitiveStyle: /认知风格[：:]\s*(.+?)(?=[\n。；;】\]\)]|$)/,
        weakPoints: /(?:薄弱环节|薄弱点)[：:]\s*(.+?)(?=[\n。；;】\]\)]|$)/,
        learningGoals: /(?:学习目标|课程目标)[：:]\s*(.+?)(?=[\n。；;】\]\)]|$)/,
        learningPace: /学习节奏[：:]\s*(.+?)(?=[\n。；;】\]\)]|$)/,
        interests: /(?:兴趣方向|兴趣爱好)[：:]\s*(.+?)(?=[\n。；;】\]\)]|$)/,
        learningStyle: /学习风格[：:]\s*(.+?)(?=[\n。；;】\]\)]|$)/,
        motivationLevel: /动机水平[：:]\s*(\d+)/,
        profileSummary: /(?:画像摘要|画像总结)[：:]\s*(.+?)(?=[\n。；;】\]\)]|$)/
      }
      var self = this
      var updated = false
      Object.keys(patterns).forEach(function (key) {
        var match = content.match(patterns[key])
        if (match && match[1]) {
          var val = match[1].trim()
          // 动机水平转成数字
          if (key === 'motivationLevel') {
            val = parseInt(val, 10)
            if (isNaN(val) || val < 1 || val > 10) return
          }
          if (self.profile[key] !== val) {
            self.$set(self.profile, key, val)
            updated = true
          }
        }
      })
      // 保存到后端，返回 Promise 以便调用方 await（确保 loadProfile 能读到最新数据）
      if (updated) {
        return studentProfileApi.updateProfile(self.userId, self.profile).catch(function (e) {
          console.warn('保存画像失败:', e)
        })
      }
      return Promise.resolve()
    },

    scrollToBottom () {
      var self = this
      this.$nextTick(function () {
        if (self.$refs.messageList) {
          self.$refs.messageList.scrollTop = self.$refs.messageList.scrollHeight
        }
      })
    },

    formatContent (content) {
      if (!content) return ''
      return content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\n/g, '<br>')
    },

    viewProfile () {
      this.$router.push({ path: '/profile-agent/index', query: { userId: this.userId } })
    }
  }
}
</script>

<style scoped>
/* ============================================
   Container — 深空底色，与侧边栏深海蓝协调
   ============================================ */
.dialogue-container {
  display: flex;
  height: calc(100vh - 84px);
  background: var(--ink-2);
  color: #e2e8f0;
  overflow: hidden;
}

/* ============================================
   Left Sidebar — 用侧边栏的深海蓝 + 子菜单深色
   ============================================ */
.profile-sidebar {
  width: 300px;
  flex-shrink: 0;
  background: #141A14;
  border-right: 1px solid rgba(var(--acid-rgb), 0.08);
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 700;
  color: #f1f5f9;
  margin: 0;
}

.progress-summary {
  font-size: 14px;
}

.progress-count {
  font-size: 20px;
  font-weight: 800;
  color: var(--ice);
}

.progress-total {
  color: #64748b;
}

.progress-bar-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.progress-bar-bg {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: rgba(255,255,255,0.06);
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--acid), var(--ice));
  transition: width 0.5s ease;
  box-shadow: 0 0 8px rgba(var(--acid-rgb), 0.4);
}

.progress-percent {
  font-size: 12px;
  color: #94a3b8;
  min-width: 36px;
  text-align: right;
}

.progress-list {
  flex: 1;
  overflow-y: auto;
}

.progress-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 6px;
  transition: all 0.3s;
  background: rgba(255,255,255,0.02);
}

.progress-item:hover {
  background: rgba(var(--acid-rgb), 0.08);
}

.progress-item.filled {
  background: rgba(34,197,94,0.06);
}

.progress-icon {
  font-size: 18px;
  width: 28px;
  text-align: center;
}

.progress-label {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: var(--ice);
}

.progress-status {
  width: 22px;
  text-align: center;
}

.status-yes {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(34,197,94,0.15);
  color: #22c55e;
  font-size: 11px;
  font-weight: 700;
}

.status-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(255,255,255,0.04);
  color: var(--grey);
  font-size: 11px;
}

.view-profile-btn {
  margin-top: 16px;
  width: 100%;
  border-radius: 10px !important;
  background: linear-gradient(135deg, var(--acid), var(--ice)) !important;
  border: none !important;
  font-weight: 600 !important;
  box-shadow: 0 4px 16px rgba(var(--acid-rgb), 0.3) !important;
}

.view-profile-btn:hover {
  box-shadow: 0 6px 24px rgba(var(--acid-rgb), 0.5) !important;
}

/* ============================================
   Chat Panel — 稍亮区分于左侧，浅灰蓝底
   ============================================ */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: rgba(32, 40, 32, 0.06);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 80%;
}

.message.user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  background: rgba(32, 40, 32, 0.8);
  border: 1px solid rgba(var(--acid-rgb), 0.2);
}

.message.user .message-avatar {
  border-color: rgba(var(--ice-rgb), 0.3);
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 16px;
  line-height: 1.7;
  font-size: 14px;
  word-break: break-word;
}

.message.assistant .message-bubble {
  background: #141A14;
  border: 1px solid rgba(var(--acid-rgb), 0.08);
  color: #cbd5e1;
  border-top-left-radius: 4px;
}

.message.user .message-bubble {
  background: linear-gradient(135deg, rgba(var(--acid-rgb), 0.15), rgba(var(--acid-rgb), 0.12));
  border: 1px solid rgba(var(--acid-rgb), 0.15);
  color: #e2e8f0;
  border-top-right-radius: 4px;
}

.streaming-bubble {
  border-color: rgba(var(--acid-rgb), 0.15) !important;
}

.cursor-blink {
  color: var(--ice);
  animation: blink 0.8s step-end infinite;
  font-size: 14px;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ============================================
   Input Area
   ============================================ */
.chat-input-area {
  padding: 16px 28px 24px;
  border-top: 1px solid rgba(var(--acid-rgb), 0.06);
  background: rgba(32, 40, 32, 0.08);
}

.input-wrap {
  margin-bottom: 10px;
}

.input-wrap .chat-textarea >>> .el-textarea__inner {
  background: #141A14 !important;
  border: 1px solid rgba(var(--acid-rgb), 0.12) !important;
  border-radius: 12px !important;
  color: #e2e8f0 !important;
  font-size: 14px !important;
  padding: 12px 16px !important;
  resize: none !important;
}

.input-wrap .chat-textarea >>> .el-textarea__inner:focus {
  border-color: rgba(var(--acid-rgb), 0.3) !important;
  box-shadow: 0 0 0 2px rgba(var(--acid-rgb), 0.08) !important;
}

.input-wrap .chat-textarea >>> .el-textarea__inner::placeholder {
  color: var(--grey) !important;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.input-hint {
  font-size: 12px;
  color: var(--grey);
}

.send-btn {
  border-radius: 10px !important;
  padding: 10px 28px !important;
  background: linear-gradient(135deg, var(--acid), var(--ice)) !important;
  border: none !important;
  font-weight: 600 !important;
  box-shadow: 0 4px 16px rgba(var(--acid-rgb), 0.3) !important;
}

.send-btn:hover {
  box-shadow: 0 6px 24px rgba(var(--acid-rgb), 0.45) !important;
}

/* ============================================
   Responsive
   ============================================ */
@media (max-width: 768px) {
  .dialogue-container {
    flex-direction: column;
  }

  .profile-sidebar {
    width: 100%;
    max-height: 200px;
    border-right: none;
    border-bottom: 1px solid rgba(var(--acid-rgb), 0.08);
    padding: 16px;
  }

  .progress-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .progress-item {
    padding: 6px 10px;
    margin-bottom: 0;
  }

  .message {
    max-width: 90%;
  }

  .chat-messages {
    padding: 16px;
  }

  .chat-input-area {
    padding: 12px 16px 16px;
  }
}
</style>
