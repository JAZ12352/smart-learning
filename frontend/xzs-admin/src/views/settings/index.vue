<template>
  <div class="settings-container">
    <div class="settings-header">
      <div class="header-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
      </div>
      <div>
        <h2>模型设置</h2>
        <p>配置 AI 语言模型的 API 接入参数，修改后全局生效</p>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="settings-tabs">
      <button :class="['settings-tab', { active: activeTab === 'text' }]" @click="activeTab = 'text'">
        <i class="el-icon-chat-dot-square"></i> 文本模型
      </button>
      <button :class="['settings-tab', { active: activeTab === 'video' }]" @click="activeTab = 'video'">
        <i class="el-icon-video-camera"></i> 视频模型
      </button>
      <button :class="['settings-tab', { active: activeTab === 'theme' }]" @click="activeTab = 'theme'">
        <i class="el-icon-magic-stick"></i> 界面主题
      </button>
    </div>

    <!-- ═══ 文本模型配置 ═══ -->
    <div v-show="activeTab === 'text'">
    <!-- 当前状态 -->
    <div class="status-bar" :class="connected ? 'connected' : 'disconnected'">
      <span class="status-dot" />
      <span>{{ connected ? 'API 连接正常' : '未测试连接' }}</span>
      <span class="current-model">当前模型: {{ form.model || '未配置' }}</span>
    </div>

    <!-- 配置表单 -->
    <div class="settings-card">
      <div class="form-group">
        <label class="form-label">API Key</label>
        <div class="input-wrapper">
          <input
            v-model="form.apiKey"
            :type="showKey ? 'text' : 'password'"
            class="form-input"
            placeholder="sk-xxxxxxxxxxxxxxxx"
          />
          <button class="input-suffix-btn" @click="showKey = !showKey" type="button">
            {{ showKey ? '隐藏' : '显示' }}
          </button>
        </div>
        <span class="form-hint">DeepSeek 平台的 API 密钥，可在 platform.deepseek.com 获取</span>
      </div>

      <div class="form-group">
        <label class="form-label">API 地址</label>
        <input
          v-model="form.apiUrl"
          class="form-input"
          placeholder="https://api.deepseek.com/chat/completions"
        />
        <span class="form-hint">OpenAI 兼容格式的 API 端点地址</span>
      </div>

      <div class="form-row">
        <div class="form-group flex-1">
          <label class="form-label">模型</label>
          <div class="model-select-wrapper">
            <select v-model="selectedPreset" class="form-select" @change="onPresetChange">
              <option value="">自定义输入</option>
              <option value="deepseek-v4-pro">DeepSeek V4 Pro</option>
              <option value="deepseek-chat">DeepSeek Chat (V3)</option>
              <option value="deepseek-reasoner">DeepSeek Reasoner (R1)</option>
              <option value="deepseek-coder">DeepSeek Coder</option>
              <option value="gpt-4o">GPT-4o</option>
              <option value="gpt-4o-mini">GPT-4o Mini</option>
              <option value="claude-3-5-sonnet">Claude 3.5 Sonnet</option>
            </select>
            <input
              v-if="!selectedPreset"
              v-model="form.model"
              class="form-input mt-8"
              placeholder="输入模型名称，如 deepseek-v4-pro"
            />
          </div>
          <span class="form-hint">选择预设模型或手动输入自定义模型 ID</span>
        </div>

        <div class="form-group" style="width: 180px;">
          <label class="form-label">温度 ({{ form.temperature }})</label>
          <div class="slider-wrapper">
            <input
              v-model.number="form.temperature"
              type="range"
              min="0"
              max="2"
              step="0.1"
              class="form-slider"
            />
            <div class="slider-labels">
              <span>精确</span>
              <span>创意</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <button class="btn btn-outline" @click="handleTest" :disabled="testing">
          <svg v-if="!testing" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <span v-if="testing" class="spinner" />
          {{ testing ? '测试中...' : '测试连接' }}
        </button>
        <button class="btn btn-primary" @click="handleSave" :disabled="saving">
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
      </div>
    </div>

    <!-- 测试结果 -->
    <div v-if="testResult" class="test-result" :class="testResult.success ? 'success' : 'error'">
      <div class="result-icon">{{ testResult.success ? 'OK' : '!' }}</div>
      <div>
        <div class="result-title">{{ testResult.message }}</div>
        <div v-if="testResult.detail" class="result-detail">{{ testResult.detail }}</div>
      </div>
    </div>
    </div><!-- end 文本模型配置 -->

    <!-- ═══ 视频模型配置 ═══ -->
    <div v-show="activeTab === 'video'">
      <div class="status-bar" :class="videoConnected ? 'connected' : 'disconnected'">
        <span class="status-dot" />
        <span>{{ videoConnected ? '视频 API 连接正常' : '未测试连接' }}</span>
        <span class="current-model">当前模型: {{ videoForm.videoModel || '未配置' }}</span>
      </div>

      <div class="settings-card">
        <div class="form-group">
          <label class="form-label">视频 API Key</label>
          <div class="input-wrapper">
            <input
              v-model="videoForm.videoApiKey"
              :type="showVideoKey ? 'text' : 'password'"
              class="form-input"
              placeholder="sk-xxxxxxxxxxxxxxxx"
            />
            <button class="input-suffix-btn" @click="showVideoKey = !showVideoKey" type="button">
              {{ showVideoKey ? '隐藏' : '显示' }}
            </button>
          </div>
          <span class="form-hint">Agnes AI 平台的 API 密钥，用于视频生成</span>
        </div>

        <div class="form-group">
          <label class="form-label">视频 API 地址</label>
          <input
            v-model="videoForm.videoApiUrl"
            class="form-input"
            placeholder="https://apihub.agnes-ai.cn/v1"
          />
          <span class="form-hint">视频生成 API 的基础地址（不含 /videos 后缀）</span>
        </div>

        <div class="form-group">
          <label class="form-label">视频模型</label>
          <select v-model="videoForm.videoModel" class="form-select">
            <option value="agnes-video-v2.0">Agnes Video V2.0（推荐，最高 10 秒）</option>
            <option value="agnes-video-v1.5">Agnes Video V1.5（较短视频）</option>
          </select>
          <span class="form-hint">视频生成模型 ID，V2.0 支持 5-10 秒视频（249 帧上限）</span>
        </div>

        <div class="form-actions">
          <button class="btn btn-outline" @click="handleVideoTest" :disabled="videoTesting">
            <svg v-if="!videoTesting" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span v-if="videoTesting" class="spinner" />
            {{ videoTesting ? '测试中...' : '测试连接' }}
          </button>
          <button class="btn btn-primary" @click="handleVideoSave" :disabled="videoSaving">
            {{ videoSaving ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>

      <div v-if="videoTestResult" class="test-result" :class="videoTestResult.success ? 'success' : 'error'">
        <div class="result-icon">{{ videoTestResult.success ? 'OK' : '!' }}</div>
        <div>
          <div class="result-title">{{ videoTestResult.message }}</div>
        </div>
      </div>
    </div><!-- end 视频模型配置 -->

    <!-- ═══ 界面主题 ═══ -->
    <div v-show="activeTab === 'theme'" class="theme-pane">
      <div class="status-bar connected">
        <span class="status-dot" />
        <span>当前主题 · {{ activeThemeName }}</span>
        <span class="current-model">切换即时生效 · 本地记忆</span>
      </div>

      <div class="settings-card">
        <div class="theme-grid">
          <button
            v-for="t in themes"
            :key="t.key"
            type="button"
            class="theme-card"
            :class="{ active: themeKey === t.key }"
            @click="handleThemeChange(t.key)"
          >
            <!-- 缩略 UI 预览 -->
            <div
              class="theme-preview"
              :style="{ '--p-bg': t.bg, '--p-a': t.accent, '--p-b': t.swatch[2] }"
            >
              <div class="tp-top">
                <i /><i /><i />
                <span class="tp-title" />
              </div>
              <div class="tp-body">
                <div class="tp-side">
                  <span /><span class="on" /><span />
                </div>
                <div class="tp-main">
                  <span class="tp-h" />
                  <span class="tp-line" style="width: 72%" />
                  <span class="tp-line" style="width: 46%" />
                  <div class="tp-row">
                    <span class="tp-chip" />
                    <span class="tp-chip ghost" />
                  </div>
                  <div class="tp-bars">
                    <b /><b /><b /><b /><b /><b /><b />
                  </div>
                </div>
              </div>
              <span class="tp-scan" />
            </div>

            <div class="theme-meta">
              <div class="theme-name">
                {{ t.name }}
                <span class="theme-en">{{ t.en }}</span>
              </div>
              <p class="theme-desc">{{ t.desc }}</p>
              <div class="theme-dots">
                <i v-for="(c, i) in t.swatch" :key="i" :style="{ background: c }" />
              </div>
            </div>

            <span v-if="themeKey === t.key" class="theme-active-tag">ACTIVE</span>
          </button>
        </div>

        <p class="theme-note">
          主题只影响配色与氛围层，不影响任何功能与数据。界面色调、网格、扫描线与霓虹描边会整体联动。
        </p>

        <div class="fx-row">
          <div class="fx-label">
            <strong>轻量模式</strong>
            <span>关闭扫描线、网格、数据流与玻璃模糊。低配设备或远控时开启，操作更跟手。</span>
          </div>
          <button
            type="button"
            class="fx-toggle"
            :class="{ on: fxLevel === 'lite' }"
            @click="toggleFx"
          >
            <span class="fx-knob" />
            <em>{{ fxLevel === 'lite' ? 'LITE' : 'FULL' }}</em>
          </button>
        </div>
      </div>
    </div><!-- end 界面主题 -->
  </div>
</template>

<script>
import modelConfigApi from '@/api/modelConfig'
import { THEMES, applyTheme, currentTheme, applyFx, currentFx } from '@/utils/theme'

export default {
  name: 'ModelSettings',
  data () {
    return {
      activeTab: 'text',
      themes: THEMES,
      themeKey: currentTheme(),
      fxLevel: currentFx(),
      form: {
        apiKey: '',
        apiUrl: 'https://api.deepseek.com/chat/completions',
        model: 'deepseek-v4-pro',
        temperature: 0.7
      },
      selectedPreset: 'deepseek-v4-pro',
      showKey: false,
      testing: false,
      saving: false,
      connected: false,
      testResult: null,
      // 视频模型配置
      videoForm: {
        videoApiKey: '',
        videoApiUrl: 'https://apihub.agnes-ai.cn/v1',
        videoModel: 'agnes-video-v2.0'
      },
      showVideoKey: false,
      videoTesting: false,
      videoSaving: false,
      videoConnected: false,
      videoTestResult: null
    }
  },
  computed: {
    activeThemeName () {
      const t = this.themes.find(x => x.key === this.themeKey)
      return t ? t.name + ' / ' + t.en : this.themeKey
    }
  },
  created () {
    this.loadConfig()
  },
  methods: {
    handleThemeChange (key) {
      this.themeKey = applyTheme(key)
    },
    toggleFx () {
      this.fxLevel = applyFx(this.fxLevel === 'lite' ? 'full' : 'lite')
    },
    async loadConfig () {
      try {
        const res = await modelConfigApi.getConfig()
        if (res && res.response) {
          const cfg = res.response
          this.form.apiKey = cfg.apiKeyRaw || ''
          this.form.apiUrl = cfg.apiUrl || ''
          this.form.model = cfg.model || ''
          this.form.temperature = cfg.temperature || 0.7
          // 匹配预设
          const presets = ['deepseek-v4-pro', 'deepseek-chat', 'deepseek-reasoner', 'deepseek-coder', 'gpt-4o', 'gpt-4o-mini', 'claude-3-5-sonnet']
          this.selectedPreset = presets.includes(this.form.model) ? this.form.model : ''
          // 视频模型配置
          this.videoForm.videoApiKey = cfg.videoApiKeyRaw || ''
          this.videoForm.videoApiUrl = cfg.videoApiUrl || 'https://apihub.agnes-ai.cn/v1'
          this.videoForm.videoModel = cfg.videoModel || 'agnes-video-v2.0'
        }
      } catch (e) {
        console.error('加载模型配置失败:', e)
      }
    },

    onPresetChange () {
      if (this.selectedPreset) {
        this.form.model = this.selectedPreset
      }
    },

    async handleTest () {
      this.testing = true
      this.testResult = null
      this.connected = false
      try {
        // 先保存当前配置再测试
        await modelConfigApi.updateConfig(this.form)
        const res = await modelConfigApi.testConnection()
        if (res && res.response) {
          this.testResult = res.response
          this.connected = !!res.response.success
        }
      } catch (e) {
        this.testResult = { success: false, message: '测试请求失败: ' + e.message }
      } finally {
        this.testing = false
      }
    },

    async handleSave () {
      this.saving = true
      try {
        const res = await modelConfigApi.updateConfig(this.form)
        if (res && res.response) {
          const cfg = res.response
          this.form.apiKey = cfg.apiKeyRaw || this.form.apiKey
          this.$message.success('模型配置已保存，全局生效')
        }
      } catch (e) {
        this.$message.error('保存失败: ' + e.message)
      } finally {
        this.saving = false
      }
    },

    async handleVideoSave () {
      this.videoSaving = true
      try {
        const res = await modelConfigApi.updateConfig(this.videoForm)
        if (res && res.response) {
          const cfg = res.response
          this.videoForm.videoApiKey = cfg.videoApiKeyRaw || this.videoForm.videoApiKey
          this.$message.success('视频模型配置已保存，全局生效')
        }
      } catch (e) {
        this.$message.error('保存失败: ' + e.message)
      } finally {
        this.videoSaving = false
      }
    },

    async handleVideoTest () {
      this.videoTesting = true
      this.videoTestResult = null
      this.videoConnected = false
      try {
        // 先保存当前配置再测试
        await modelConfigApi.updateConfig(this.videoForm)
        // 测试连接：提交一个最小帧数的测试请求
        const res = await modelConfigApi.testVideoConnection()
        if (res && res.response) {
          this.videoTestResult = res.response
          this.videoConnected = !!res.response.success
        }
      } catch (e) {
        this.videoTestResult = { success: false, message: '测试请求失败: ' + (e.message || '未知错误') }
      } finally {
        this.videoTesting = false
      }
    }
  }
}
</script>

<style scoped>
.settings-container {
  padding: 24px 32px;
  max-width: 1180px;
}

/* 表单不要随容器无限拉宽 */
.settings-card .form-input,
.settings-card .form-select,
.settings-card .input-wrapper {
  max-width: 720px;
}

.settings-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 0;
}

.settings-tab {
  padding: 10px 24px;
  border: none;
  background: none;
  font-size: 14px;
  font-weight: 600;
  color: #94a3b8;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.settings-tab:hover {
  color: var(--acid);
}

.settings-tab.active {
  color: var(--acid);
  border-bottom-color: var(--ink-5);
}

.settings-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.header-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ink-3);
  border-radius: 14px;
  color: var(--acid);
}

.settings-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

.settings-header p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 10px;
  font-size: 13px;
  margin-bottom: 20px;
}

.status-bar.connected {
  background: var(--ink-3);
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.status-bar.disconnected {
  background: var(--ink-3);
  color: #94a3b8;
  border: 1px solid #e2e8f0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.connected .status-dot {
  background: #22c55e;
  box-shadow: 0 0 6px rgba(34,197,94,0.4);
}

.disconnected .status-dot {
  background: #cbd5e1;
}

.current-model {
  margin-left: auto;
  font-weight: 500;
  font-family: 'Courier New', monospace;
}

.settings-card {
  background: var(--ink-3);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--grey);
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  color: #1e293b;
  background: var(--ink-3);
  transition: border-color 0.2s, box-shadow 0.2s;
  outline: none;
  font-family: 'Courier New', monospace;
}

.form-input:focus {
  border-color: #a5b4fc;
  box-shadow: 0 0 0 3px rgba(var(--acid-rgb), 0.08);
}

.form-input::placeholder {
  color: #cbd5e1;
}

.input-wrapper {
  position: relative;
}

.input-wrapper .form-input {
  padding-right: 60px;
}

.input-suffix-btn {
  position: absolute;
  right: 4px;
  top: 4px;
  height: 34px;
  padding: 0 12px;
  border: none;
  background: var(--ink-3);
  color: var(--acid);
  font-size: 12px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.input-suffix-btn:hover {
  background: var(--ink-3);
}

.form-select {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  color: #1e293b;
  background: var(--ink-3);
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
}

.form-select:focus {
  border-color: #a5b4fc;
  box-shadow: 0 0 0 3px rgba(var(--acid-rgb), 0.08);
}

.form-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.form-row {
  display: flex;
  gap: 16px;
}

.flex-1 {
  flex: 1;
}

.mt-8 {
  margin-top: 8px;
}

.slider-wrapper {
  padding-top: 4px;
}

.form-slider {
  width: 100%;
  height: 6px;
  appearance: none;
  background: var(--ink-3);
  border-radius: 3px;
  outline: none;
  cursor: pointer;
}

.form-slider::-webkit-slider-thumb {
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--ink-3);
  box-shadow: 0 2px 6px rgba(var(--acid-rgb), 0.3);
  cursor: pointer;
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 11px;
  color: #94a3b8;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, var(--acid), var(--ice));
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(var(--acid-rgb), 0.25);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(var(--acid-rgb), 0.35);
}

.btn-outline {
  background: var(--ink-3);
  color: var(--acid);
  border: 1px solid var(--ice);
}

.btn-outline:hover:not(:disabled) {
  background: var(--ink-3);
  border-color: #a5b4fc;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #e2e8f0;
  border-top-color: var(--ink-5);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.test-result {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 18px;
  border-radius: 12px;
  font-size: 13px;
}

.test-result.success {
  background: var(--ink-3);
  border: 1px solid #bbf7d0;
  color: #16a34a;
}

.test-result.error {
  background: var(--ink-3);
  border: 1px solid #fecaca;
  color: #dc2626;
}

.result-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.success .result-icon {
  background: var(--ink-3);
  color: #16a34a;
}

.error .result-icon {
  background: var(--ink-3);
  color: #dc2626;
}

.result-title {
  font-weight: 600;
}

.result-detail {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.8;
  word-break: break-all;
}

/* ═══════════════════════════════════════════════════════
   界面主题选择器
   ═══════════════════════════════════════════════════════ */
.theme-pane {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.theme-grid {
  display: grid;
  /* 5 套主题要在一行里并排，才方便横向比较。
     settings-card 内容宽度约 1060px，5 列 + 4 个 16px gap → 每张约 199px，
     所以 minmax 取 190px；auto-fit 保证主题数量变少时不会留下空列。 */
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 16px;
}

.theme-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  background: var(--ink-3);
  border: 1px solid var(--line);
  border-radius: 0;
  color: var(--paper);
  font-family: inherit;
  transition: border-color .18s ease, box-shadow .18s ease, transform .12s ease;
}

.theme-card:hover {
  border-color: var(--acid-55);
  box-shadow: 0 0 0 1px var(--acid-30), 0 10px 28px rgba(0, 0, 0, .45);
  transform: translateY(-1px);
}

.theme-card.active {
  border-color: var(--acid);
  box-shadow: 0 0 0 1px var(--acid), 0 0 26px var(--acid-16);
}

/* ---- 缩略 UI 预览 ---- */
.theme-preview {
  position: relative;
  overflow: hidden;
  aspect-ratio: 16 / 10;
  padding: 9px;
  background: var(--p-bg);
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
}

.theme-card.active .theme-preview {
  border-color: var(--acid-30);
}

.tp-top {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.tp-top i {
  width: 4px;
  height: 4px;
  background: var(--p-a);
  opacity: .7;
}

.tp-title {
  flex: 1;
  height: 3px;
  margin-left: 6px;
  background: var(--p-a);
  opacity: .32;
}

.tp-body {
  display: flex;
  gap: 7px;
  flex: 1;
  min-height: 0;
}

.tp-side {
  width: 21%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 6px;
  border-right: 1px solid var(--p-b);
}

.tp-side span {
  height: 4px;
  background: var(--p-a);
  opacity: .18;
}

.tp-side span.on {
  opacity: .9;
  box-shadow: 0 0 6px var(--p-a);
}

.tp-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.tp-h {
  width: 58%;
  height: 7px;
  background: var(--p-a);
  opacity: .92;
}

.tp-line {
  height: 3px;
  background: var(--p-a);
  opacity: .26;
}

.tp-row {
  display: flex;
  gap: 4px;
  margin-top: 2px;
}

.tp-chip {
  width: 28px;
  height: 8px;
  background: var(--p-a);
  opacity: .95;
}

.tp-chip.ghost {
  background: transparent;
  box-shadow: inset 0 0 0 1px var(--p-a);
  opacity: .55;
}

.tp-bars {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 18px;
  margin-top: auto;
}

.tp-bars b {
  flex: 1;
  background: var(--p-a);
  opacity: .42;
}

.tp-bars b:nth-child(1) { height: 42%; }
.tp-bars b:nth-child(2) { height: 68%; }
.tp-bars b:nth-child(3) { height: 34%; }
.tp-bars b:nth-child(4) { height: 86%; }
.tp-bars b:nth-child(5) { height: 55%; }
.tp-bars b:nth-child(6) { height: 74%; }
.tp-bars b:nth-child(7) { height: 48%; }

.tp-scan {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: repeating-linear-gradient(180deg, rgba(0, 0, 0, .22) 0 1px, transparent 1px 3px);
  opacity: .55;
  mix-blend-mode: multiply;
}

/* ---- 卡片文字 ---- */
.theme-meta {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.theme-name {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: .02em;
  color: var(--paper);
}

.theme-en {
  font-family: var(--f-mono);
  font-size: 9px;
  letter-spacing: .16em;
  color: var(--grey-3);
}

.theme-desc {
  margin: 0;
  font-size: 11px;
  line-height: 1.5;
  color: var(--grey);
}

.theme-dots {
  display: flex;
  gap: 4px;
  margin-top: 2px;
}

.theme-dots i {
  width: 14px;
  height: 4px;
}

.theme-active-tag {
  position: absolute;
  top: 0;
  right: 0;
  padding: 2px 7px;
  font-family: var(--f-mono);
  font-size: 8px;
  letter-spacing: .18em;
  color: var(--ink);
  background: var(--acid);
}

.theme-note {
  margin: 16px 0 0;
  padding-top: 12px;
  border-top: 1px solid var(--line-soft);
  font-size: 11px;
  line-height: 1.6;
  color: var(--grey-2);
}

/* ---- 轻量模式开关 ---- */
.fx-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--line-soft);
}

.fx-label {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fx-label strong {
  font-size: 13px;
  color: var(--paper);
  font-weight: 700;
}

.fx-label span {
  font-size: 11px;
  color: var(--grey);
  line-height: 1.5;
}

.fx-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  padding: 5px 10px 5px 6px;
  background: var(--ink-3);
  border: 1px solid var(--line);
  cursor: pointer;
  color: var(--grey-2);
  font-family: var(--f-mono);
  font-size: 10px;
  letter-spacing: .12em;
  transition: border-color .16s ease, color .16s ease;
}

.fx-toggle:hover {
  border-color: var(--acid-55);
}

.fx-toggle.on {
  border-color: var(--acid);
  color: var(--acid);
}

.fx-knob {
  width: 12px;
  height: 12px;
  background: var(--grey-3);
  transition: background .16s ease, transform .16s ease;
}

.fx-toggle.on .fx-knob {
  background: var(--acid);
  transform: translateX(2px);
  box-shadow: 0 0 8px var(--acid-55);
}

.fx-toggle em {
  font-style: normal;
}
</style>
