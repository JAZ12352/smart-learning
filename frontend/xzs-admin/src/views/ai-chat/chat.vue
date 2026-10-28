<template>
  <div class="ai-tutor-page">
    <!-- 背景光斑 -->
    <div class="bg-orbs">
      <span class="orb orb-1"></span>
      <span class="orb orb-2"></span>
      <span class="orb orb-3"></span>
    </div>

    <div class="tutor-layout">
      <!-- 历史对话侧栏 -->
      <aside class="history-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">对话历史</span>
          <span class="sidebar-count">{{ chatSessions.length }}</span>
        </div>
        <button class="new-chat-btn" @click="startNewChat">
          <i class="el-icon-plus"></i>
          <span>新建对话</span>
        </button>
        <div class="history-list">
          <div v-if="chatSessions.length === 0" class="empty-tip">
            <i class="el-icon-chat-line-square"></i>
            <p>暂无历史对话</p>
            <span>开始你的第一次提问吧</span>
          </div>
          <div
            v-for="session in chatSessions"
            :key="session.id"
            :class="['history-item', { active: session.id === activeChatId }]"
            @click="switchChat(session)"
          >
            <div class="history-item-main">
              <i class="el-icon-chat-dot-square history-item-icon"></i>
              <div class="history-item-text">
                <div class="history-item-title">{{ session.title || '新对话' }}</div>
                <div class="history-item-time">{{ formatHistoryTime(session.updateTime || session.createTime) }}</div>
              </div>
            </div>
            <el-tooltip content="删除对话" placement="top">
              <i class="el-icon-delete history-item-del" @click.stop="removeChat(session)"></i>
            </el-tooltip>
          </div>
        </div>
      </aside>

      <!-- 主对话卡片 -->
      <div class="tutor-card">
        <!-- 顶部 -->
        <div class="tutor-header">
          <div class="header-left">
            <div class="ai-avatar">
              <span class="ai-avatar-core">AI</span>
            </div>
            <div class="header-title">
              <h2>AI 智能辅导</h2>
              <span class="status-line">
                <i class="status-dot" :class="{ thinking: loading }"></i>
                {{ loading ? '思考中…' : '在线 · 随时为你解答' }}
              </span>
            </div>
          </div>
          <div class="header-actions">
            <el-tooltip :content="voiceEnabled ? '关闭 AI 配音' : '开启 AI 配音'" placement="bottom">
              <el-button
                circle
                :type="voiceEnabled ? 'warning' : 'default'"
                icon="el-icon-bell"
                @click="toggleVoice"
                class="icon-btn"
              ></el-button>
            </el-tooltip>
          </div>
        </div>

        <!-- 消息区 -->
        <div class="chat-box" ref="chatBox">
          <transition-group name="msg" tag="div">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              :class="['message', msg.role]"
            >
              <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
              <div class="bubble-wrap">
                <div class="content" v-html="formatContent(msg.content)"></div>
                <div class="bubble-actions" v-if="msg.role === 'assistant' && msg.content">
                  <el-tooltip content="朗读" placement="top">
                    <i class="el-icon-headset" @click="replayMessage(msg)"></i>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </transition-group>

          <!-- 思考态指示器 -->
          <div class="typing-indicator" v-if="loading && !lastAiStreaming">
            <span></span><span></span><span></span>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="input-box">
          <div v-if="uploadedFiles.length > 0" class="uploaded-files">
            <el-tag
              v-for="(file, index) in uploadedFiles"
              :key="index"
              closable
              size="small"
              @close="removeFile(index)"
            >
              {{ file.name }}
            </el-tag>
          </div>

          <div class="input-row">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 2, maxRows: 5 }"
              placeholder="输入你的问题，AI 将为你详细解答…"
              @keydown.enter.native="handleKeydown"
              class="chat-input"
            />

            <el-upload
              class="upload-btn"
              action="#"
              :auto-upload="false"
              :on-change="handleFileChange"
              accept=".pdf,.doc,.docx,.ppt,.pptx,.jpg,.jpeg,.png,.gif,.mp4,.avi,.mov"
              :multiple="true"
              :show-file-list="false"
            >
              <el-tooltip content="上传参考资料" placement="top">
                <el-button circle icon="el-icon-upload2" class="icon-btn"></el-button>
              </el-tooltip>
            </el-upload>

            <el-tooltip content="语音输入" placement="top">
              <el-button
                circle
                :type="isRecording ? 'danger' : 'default'"
                icon="el-icon-microphone"
                :loading="isRecording"
                @click="toggleVoiceInput"
                class="icon-btn"
              ></el-button>
            </el-tooltip>

            <el-button
              type="primary"
              @click="handleSend"
              :loading="loading"
              class="send-btn"
            >
              <i class="el-icon-position"></i>
              <span>发送</span>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import aiChatApi from '@/api/ai-chat'

const DEFAULT_MESSAGES = [
  { role: 'assistant', content: '你好！我是 AI 智能辅导助手，有任何学习问题都可以问我，我会用语音为你讲解哦～' }
]

export default {
  name: 'DeepSeekChat',
  data () {
    return {
      chatId: null,
      activeChatId: null,
      chatSessions: [],
      messages: [...DEFAULT_MESSAGES],
      inputMessage: '',
      loading: false,
      isRecording: false,
      recognition: null,
      uploadedFiles: [],
      voiceEnabled: true,
      lastAiStreaming: false
    }
  },
  mounted () {
    this.initSpeechRecognition()
    this.loadChatList()
    this.loadChatSession()
    try {
      var saved = localStorage.getItem('ai_voice_enabled')
      if (saved !== null) this.voiceEnabled = saved === '1'
    } catch (e) { /* ignore */ }
  },
  beforeDestroy () {
    this.stopSpeak()
  },
  methods: {
    async loadChatList () {
      try {
        const res = await aiChatApi.list()
        if (res && res.data && res.data.list) {
          this.chatSessions = res.data.list
        }
      } catch (error) {
        console.error('加载对话列表失败:', error)
      }
    },

    async loadChatSession () {
      const chatId = this.$route.query.chatId
      if (!chatId) {
        this.activeChatId = null
        return
      }

      try {
        const res = await aiChatApi.get(chatId)
        if (res && res.data) {
          this.chatId = res.data.id
          this.activeChatId = res.data.id
          this.messages = Array.isArray(res.data.messages) && res.data.messages.length > 0
            ? res.data.messages
            : [...DEFAULT_MESSAGES]
          this.scrollToBottom()
        }
      } catch (error) {
        console.error('加载对话失败:', error)
      }
    },

    async persistChatSession () {
      const chatId = this.chatId || ('chat_' + Date.now())
      const effectiveMessages = this.messages.filter(msg => msg.content)
      const title = this.$route.query.title || this.createChatTitle(effectiveMessages)

      await aiChatApi.saveSession({
        id: chatId,
        title,
        messages: effectiveMessages
      })

      if (!this.chatId) {
        this.chatId = chatId
        this.activeChatId = chatId
        this.$router.replace({
          path: '/tutoring-agent/chat',
          query: {
            chatId,
            title
          }
        })
      }
      // 刷新历史列表（标题/时间可能更新）
      this.loadChatList()
    },

    createChatTitle (messages) {
      const firstUserMessage = messages.find(msg => msg.role === 'user' && msg.content)
      if (!firstUserMessage) return '新对话'
      const title = firstUserMessage.content.trim()
      return title.length > 20 ? `${title.slice(0, 20)}...` : title
    },

    startNewChat () {
      // 停止当前朗读与加载
      this.stopSpeak()
      this.chatId = null
      this.activeChatId = null
      this.messages = [...DEFAULT_MESSAGES]
      this.inputMessage = ''
      this.uploadedFiles = []
      this.$router.replace({ path: '/tutoring-agent/chat' })
    },

    async switchChat (session) {
      if (session.id === this.activeChatId) return
      this.stopSpeak()
      this.$router.replace({
        path: '/tutoring-agent/chat',
        query: { chatId: session.id, title: session.title }
      })
      // 路由 replace 不会触发组件重载，手动加载
      this.chatId = null
      await this.loadChatSession()
    },

    async removeChat (session) {
      try {
        await this.$confirm('确定删除该对话？删除后无法恢复。', '删除对话', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        })
      } catch (e) {
        return // 取消
      }

      try {
        await aiChatApi.delete(session.id)
        this.$message.success('对话已删除')

        // 如果删的是当前对话，开启新对话
        if (session.id === this.activeChatId) {
          this.startNewChat()
        }
        this.loadChatList()
      } catch (error) {
        console.error('删除对话失败:', error)
        this.$message.error('删除失败')
      }
    },

    formatHistoryTime (timeStr) {
      if (!timeStr) return ''
      // 简化显示：仅显示 月-日 时:分
      const parts = timeStr.split(' ')
      if (parts.length < 2) return timeStr
      const datePart = parts[0] || ''
      const timePart = (parts[1] || '').slice(0, 5)
      const dateSegments = datePart.split('-')
      const shortDate = dateSegments.length === 3
        ? `${dateSegments[1]}-${dateSegments[2]}`
        : datePart
      return `${shortDate} ${timePart}`
    },

    handleKeydown (e) {
      if (e.shiftKey) return
      e.preventDefault()
      e.stopPropagation()
      this.handleSend()
    },

    async handleSend () {
      if (!this.inputMessage.trim() && this.uploadedFiles.length === 0) return

      let messageContent = this.inputMessage.trim()
      if (this.uploadedFiles.length > 0) {
        const fileInfo = this.uploadedFiles.map(file => file.name).join('、')
        messageContent += `${messageContent ? '\n\n' : ''}参考资料：${fileInfo}`
      }

      this.messages.push({ role: 'user', content: messageContent })
      this.inputMessage = ''
      this.loading = true
      this.lastAiStreaming = true
      this.stopSpeak() // 发送新消息时停止上一条朗读

      this.messages.push({ role: 'assistant', content: '' })
      const aiMessageIndex = this.messages.length - 1
      const conversationMessages = this.messages
        .filter((msg, index) => index !== aiMessageIndex && msg.content)
        .map(msg => ({
          role: msg.role,
          content: msg.content
        }))

      this.scrollToBottom()

      try {
        var self = this
        await aiChatApi.sendStream({
          content: messageContent,
          messages: conversationMessages
        }, function (chunk, completeContent) {
          self.messages[aiMessageIndex].content = completeContent
          self.scrollToBottom()
        })

        this.uploadedFiles = []
        await this.persistChatSession()

        // AI 回复完成后配音
        var finalContent = this.messages[aiMessageIndex].content
        if (finalContent && this.voiceEnabled) {
          this.speak(finalContent)
        }
      } catch (error) {
        console.error('发送失败:', error)
        this.messages[aiMessageIndex].content = error.message || '发送失败，请重试'
      } finally {
        this.loading = false
        this.lastAiStreaming = false
        this.scrollToBottom()
      }
    },

    scrollToBottom () {
      this.$nextTick(() => {
        if (this.$refs.chatBox) {
          this.$refs.chatBox.scrollTo({
            top: this.$refs.chatBox.scrollHeight,
            behavior: 'smooth'
          })
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

    // ═══ AI 配音（Web Speech API）═══
    toggleVoice () {
      this.voiceEnabled = !this.voiceEnabled
      try { localStorage.setItem('ai_voice_enabled', this.voiceEnabled ? '1' : '0') } catch (e) { /* ignore */ }
      if (!this.voiceEnabled) {
        this.stopSpeak()
        this.$message.info('已关闭 AI 配音')
      } else {
        this.$message.success('已开启 AI 配音')
      }
    },

    speak (text) {
      if (!window.speechSynthesis) return
      this.stopSpeak()
      var clean = this.cleanTextForTTS(text)
      if (!clean) return
      var utter = new SpeechSynthesisUtterance(clean)
      utter.lang = 'zh-CN'
      utter.rate = 1.0
      utter.pitch = 1.0
      // 优先选择中文语音
      var voices = window.speechSynthesis.getVoices()
      var zhVoice = voices.find(function (v) { return v.lang && v.lang.toLowerCase().indexOf('zh') === 0 })
      if (zhVoice) utter.voice = zhVoice
      this.currentUtterance = utter
      var self = this
      utter.onend = function () { self.currentUtterance = null }
      utter.onerror = function () { self.currentUtterance = null }
      window.speechSynthesis.speak(utter)
    },

    stopSpeak () {
      if (window.speechSynthesis) {
        window.speechSynthesis.cancel()
      }
      this.currentUtterance = null
    },

    replayMessage (msg) {
      if (msg.content) this.speak(msg.content)
    },

    cleanTextForTTS (text) {
      if (!text) return ''
      return text
        .replace(/```[\s\S]*?```/g, '代码块')
        .replace(/`([^`]+)`/g, '$1')
        .replace(/[#*_>`~]/g, '')
        .replace(/\[(.*?)\]\(.*?\)/g, '$1')
        .replace(/\n{2,}/g, '。')
        .replace(/\n/g, '，')
        .trim()
    },

    initSpeechRecognition () {
      if ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window) {
        this.recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)()
        this.recognition.lang = 'zh-CN'
        this.recognition.continuous = false
        this.recognition.interimResults = true

        this.recognition.onresult = (event) => {
          let transcript = ''
          for (let i = event.resultIndex; i < event.results.length; i++) {
            transcript += event.results[i][0].transcript
          }
          this.inputMessage = transcript
        }

        this.recognition.onend = () => {
          this.isRecording = false
        }

        this.recognition.onerror = (event) => {
          console.error('语音识别错误:', event.error)
          this.isRecording = false
          this.$message.error('语音识别失败，请重试')
        }
      }
    },

    toggleVoiceInput () {
      if (!this.recognition) {
        this.$message.warning('当前浏览器不支持语音识别')
        return
      }

      if (this.isRecording) {
        this.recognition.stop()
        this.isRecording = false
      } else {
        try {
          this.recognition.start()
          this.isRecording = true
          this.$message.success('开始语音输入，请开始讲话')
        } catch (error) {
          console.error('语音识别启动失败:', error)
          this.$message.error('语音识别启动失败，请重试')
        }
      }
    },

    handleFileChange (file) {
      this.uploadedFiles.push(file.raw)
      this.$message.success('文件已加入参考资料')
    },

    removeFile (index) {
      this.uploadedFiles.splice(index, 1)
      this.$message.success('文件已移除')
    }
  }
}
</script>

<style scoped>
.ai-tutor-page {
  position: relative;
  min-height: calc(100vh - 100px);
  background: linear-gradient(160deg, var(--ink-2) 0%, #141A14 55%, var(--ink-2) 100%);
  padding: 24px;
  overflow: hidden;
}

/* ── 背景光斑 ── */
.bg-orbs { position: absolute; inset: 0; pointer-events: none; }
.orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.35; }
.orb-1 { width: 340px; height: 340px; background: var(--ink-3); top: -80px; left: -60px; animation: float1 14s ease-in-out infinite; }
.orb-2 { width: 280px; height: 280px; background: var(--ink-3); bottom: -60px; right: -40px; animation: float2 18s ease-in-out infinite; }
.orb-3 { width: 220px; height: 220px; background: var(--ink-3); top: 40%; right: 30%; animation: float1 22s ease-in-out infinite reverse; }
@keyframes float1 { 0%, 100% { transform: translate(0, 0); } 50% { transform: translate(40px, 30px); } }
@keyframes float2 { 0%, 100% { transform: translate(0, 0); } 50% { transform: translate(-30px, -40px); } }

/* ── 主布局：侧栏 + 对话卡片 ── */
.tutor-layout {
  position: relative;
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  gap: 16px;
  height: calc(100vh - 148px);
}

/* ── 历史对话侧栏 ── */
.history-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: rgba(10, 13, 10, 0.55);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(var(--acid-rgb), 0.18);
  border-radius: 22px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid rgba(var(--acid-rgb), 0.12);
}
.sidebar-title {
  color: #f8fafc;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.sidebar-count {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  background: rgba(var(--acid-rgb), 0.2);
  color: var(--ice);
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.new-chat-btn {
  margin: 14px 16px 10px;
  height: 40px;
  border: 1px solid rgba(var(--acid-rgb), 0.35);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(var(--acid-rgb), 0.25), rgba(var(--acid-rgb), 0.25));
  color: var(--ice);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.22s;
}
.new-chat-btn:hover {
  background: linear-gradient(135deg, var(--acid), var(--ice));
  border-color: transparent;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--acid-rgb), 0.35);
}
.new-chat-btn i { font-size: 14px; }

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 10px 12px;
}
.history-list::-webkit-scrollbar { width: 5px; }
.history-list::-webkit-scrollbar-thumb { background: rgba(var(--acid-rgb), 0.3); border-radius: 3px; }

.empty-tip {
  text-align: center;
  color: #64748b;
  padding: 40px 16px;
}
.empty-tip i { font-size: 38px; color: rgba(var(--acid-rgb), 0.4); }
.empty-tip p { margin: 12px 0 4px; color: #94a3b8; font-size: 13px; font-weight: 500; }
.empty-tip span { font-size: 12px; color: #64748b; }

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.18s;
}
.history-item:hover {
  background: rgba(var(--acid-rgb), 0.12);
  border-color: rgba(var(--acid-rgb), 0.2);
}
.history-item.active {
  background: linear-gradient(135deg, rgba(var(--acid-rgb), 0.22), rgba(var(--acid-rgb), 0.22));
  border-color: rgba(var(--acid-rgb), 0.45);
  box-shadow: 0 4px 12px rgba(var(--acid-rgb), 0.2);
}
.history-item-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}
.history-item-icon {
  color: var(--ice);
  font-size: 14px;
  flex-shrink: 0;
}
.history-item.active .history-item-icon { color: var(--ice); }
.history-item-text { min-width: 0; flex: 1; }
.history-item-title {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.history-item.active .history-item-title { color: #fff; }
.history-item-time {
  color: #64748b;
  font-size: 11px;
  margin-top: 2px;
}
.history-item-del {
  color: #64748b;
  font-size: 13px;
  padding: 4px;
  border-radius: 6px;
  opacity: 0;
  transition: all 0.18s;
  flex-shrink: 0;
}
.history-item:hover .history-item-del { opacity: 1; }
.history-item-del:hover { color: #f87171; background: rgba(248, 113, 113, 0.12); }

/* ── 主卡片 ── */
.tutor-card {
  position: relative;
  flex: 1;
  background: rgba(10, 13, 10, 0.55);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(var(--acid-rgb), 0.18);
  border-radius: 22px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ── 顶部 ── */
.tutor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(var(--acid-rgb), 0.12);
}
.header-left { display: flex; align-items: center; gap: 14px; }
.ai-avatar {
  width: 48px; height: 48px; border-radius: 50%;
  background: linear-gradient(135deg, var(--acid), var(--ice));
  display: flex; align-items: center; justify-content: center;
  position: relative;
}
.ai-avatar::before {
  content: ''; position: absolute; inset: -4px; border-radius: 50%;
  background: linear-gradient(135deg, var(--acid), var(--ice));
  opacity: 0.4; z-index: -1; animation: pulse 2.4s ease-in-out infinite;
}
@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 0.4; } 50% { transform: scale(1.18); opacity: 0; } }
.ai-avatar-core { color: #fff; font-weight: 700; font-size: 16px; }
.header-title h2 { margin: 0; color: #f8fafc; font-size: 18px; font-weight: 600; }
.status-line { display: flex; align-items: center; gap: 6px; color: #94a3b8; font-size: 12px; margin-top: 3px; }
.status-dot {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%;
  background: #22c55e; box-shadow: 0 0 8px #22c55e;
}
.status-dot.thinking { background: #f59e0b; box-shadow: 0 0 8px #f59e0b; animation: blink 0.9s infinite; }
@keyframes blink { 50% { opacity: 0.3; } }

/* ── 消息区 ── */
.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.chat-box::-webkit-scrollbar { width: 6px; }
.chat-box::-webkit-scrollbar-thumb { background: rgba(var(--acid-rgb), 0.3); border-radius: 3px; }

.message { display: flex; margin-bottom: 22px; gap: 12px; }
.message.user { flex-direction: row-reverse; }

.avatar {
  width: 38px; height: 38px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600; color: #fff;
}
.message .avatar { background: linear-gradient(135deg, var(--acid), var(--ice)); }
.message.user .avatar { background: linear-gradient(135deg, var(--acid), var(--ice)); }

.bubble-wrap { max-width: 72%; display: flex; flex-direction: column; }
.message.user .bubble-wrap { align-items: flex-end; }

.content {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}
.message .content {
  background: rgba(30, 41, 59, 0.85);
  border: 1px solid rgba(var(--acid-rgb), 0.18);
  color: #e2e8f0;
  border-top-left-radius: 4px;
}
.message.user .content {
  background: linear-gradient(135deg, var(--acid), var(--ice));
  color: #fff;
  border: none;
  border-top-right-radius: 4px;
}

.bubble-actions { margin-top: 5px; padding: 0 4px; }
.bubble-actions i {
  color: #64748b; font-size: 14px; cursor: pointer; transition: color 0.2s;
}
.bubble-actions i:hover { color: var(--ice); }

/* 消息入场动画 */
.msg-enter-active { transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1); }
.msg-enter { opacity: 0; transform: translateY(14px); }

/* 思考态指示器 */
.typing-indicator { display: flex; gap: 5px; padding: 12px 18px; width: fit-content; }
.typing-indicator span {
  width: 8px; height: 8px; border-radius: 50%; background: var(--ink-3);
  animation: typing 1.2s infinite ease-in-out;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing { 0%, 60%, 100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-8px); opacity: 1; } }

/* ── 输入区 ── */
.input-box {
  padding: 14px 20px 18px;
  border-top: 1px solid rgba(var(--acid-rgb), 0.12);
  background: rgba(10, 13, 10, 0.4);
}
.uploaded-files { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
.input-row { display: flex; align-items: flex-end; gap: 10px; }
.chat-input { flex: 1; }
.chat-input >>> .el-textarea__inner {
  background: rgba(30, 41, 59, 0.7);
  border: 1px solid rgba(var(--acid-rgb), 0.25);
  border-radius: 14px;
  color: #f1f5f9;
  font-size: 14px;
  padding: 10px 14px;
  resize: none;
}
.chat-input >>> .el-textarea__inner:focus { border-color: var(--ink-5); box-shadow: 0 0 0 2px rgba(var(--acid-rgb), 0.15); }
.chat-input >>> .el-textarea__inner::placeholder { color: #64748b; }

.icon-btn {
  background: rgba(30, 41, 59, 0.7) !important;
  border-color: rgba(var(--acid-rgb), 0.25) !important;
  color: #94a3b8 !important;
}
.icon-btn:hover { color: var(--ice) !important; border-color: var(--ink-5) !important; }

.send-btn {
  background: linear-gradient(135deg, var(--acid), var(--ice)) !important;
  border: none !important;
  height: 40px;
  border-radius: 14px !important;
  padding: 0 22px;
  font-weight: 600;
  transition: transform 0.2s, box-shadow 0.2s;
}
.send-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(var(--acid-rgb), 0.4); }
.send-btn i { margin-right: 4px; }

.upload-btn { display: inline-block; }

/* ── 响应式：窄屏隐藏侧栏 ── */
@media (max-width: 900px) {
  .history-sidebar { display: none; }
  .tutor-layout { max-width: 920px; }
}
</style>
