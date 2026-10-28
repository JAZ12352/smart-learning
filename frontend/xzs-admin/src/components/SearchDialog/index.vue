<template>
  <div>
    <!-- 遮罩层 -->
    <transition name="search-fade">
      <div v-show="visible" class="search-overlay" @click="close" />
    </transition>

    <!-- 对话框 -->
    <transition name="search-slide">
      <div v-show="visible" class="search-dialog" @keydown="handleKeydown">
        <div class="search-panel">
          <!-- 搜索输入框 -->
          <div class="search-input-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="search-input-icon">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input
              ref="searchInput"
              v-model="query"
              placeholder="搜索页面、功能... 输入关键字"
              class="search-input"
              @input="onInput"
            />
            <kbd class="search-esc-hint">ESC</kbd>
          </div>

          <!-- 搜索结果 -->
          <div class="search-results" v-if="query">
            <div v-if="filteredRoutes.length === 0" class="search-empty">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="search-empty-icon">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                <line x1="8" y1="11" x2="14" y2="11"/>
              </svg>
              <span>未找到匹配结果</span>
            </div>
            <div
              v-for="(item, index) in filteredRoutes"
              :key="item.path"
              class="search-result-item"
              :class="{ active: index === activeIndex }"
              @click="navigateTo(item)"
              @mouseenter="activeIndex = index"
            >
              <div class="result-item-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="result-icon-svg">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
                </svg>
              </div>
              <div class="result-item-content">
                <span class="result-item-title">{{ item.meta.title }}</span>
                <span class="result-item-path">{{ item.path }}</span>
              </div>
              <div class="result-item-badge">{{ item.meta.icon || 'page' }}</div>
            </div>
          </div>

          <!-- 快捷提示 -->
          <div v-if="!query" class="search-hints">
            <div class="hints-title">快捷导航</div>
            <div class="hints-list">
              <div class="hint-item" @click="quickNav('/dashboard')">
                <span class="hint-icon">🏠</span>
                <span>系统首页</span>
              </div>
              <div class="hint-item" @click="quickNav('/resource-agent/generate')">
                <span class="hint-icon">🤖</span>
                <span>智能体生成</span>
              </div>
              <div class="hint-item" @click="quickNav('/profile-agent/index')">
                <span class="hint-icon">📊</span>
                <span>学习画像</span>
              </div>
              <div class="hint-item" @click="quickNav('/tutoring-agent/chat')">
                <span class="hint-icon">💬</span>
                <span>AI 辅导</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { constantRoutes } from '@/router'

export default {
  name: 'SearchDialog',
  data () {
    return {
      visible: false,
      query: '',
      activeIndex: 0,
      flatRoutes: []
    }
  },
  computed: {
    filteredRoutes () {
      if (!this.query) return []
      const q = this.query.toLowerCase().trim()
      return this.flatRoutes.filter(item =>
        item.meta && item.meta.title && (
          item.meta.title.toLowerCase().includes(q) ||
          item.path.toLowerCase().includes(q) ||
          (item.meta.icon && item.meta.icon.toLowerCase().includes(q))
        )
      )
    }
  },
  created () {
    this.flattenRoutes(constantRoutes)
    // 监听键盘快捷键
    document.addEventListener('keydown', this.handleGlobalKeydown)
  },
  beforeDestroy () {
    document.removeEventListener('keydown', this.handleGlobalKeydown)
  },
  methods: {
    flattenRoutes (routes, parentPath = '') {
      routes.forEach(route => {
        const path = route.path.startsWith('/') ? route.path : `${parentPath}/${route.path}`
        if (route.children) {
          this.flattenRoutes(route.children, path)
        } else if (!route.hidden && route.meta && route.meta.title) {
          this.flatRoutes.push({
            ...route,
            path: path.replace(/\/{2,}/g, '/')
          })
        }
      })
    },
    open () {
      this.visible = true
      this.query = ''
      this.activeIndex = 0
      this.$nextTick(() => {
        this.$refs.searchInput && this.$refs.searchInput.focus()
      })
    },
    close () {
      this.visible = false
    },
    onInput () {
      this.activeIndex = 0
    },
    handleGlobalKeydown (e) {
      // Cmd+K / Ctrl+K 打开搜索
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        this.open()
      }
      // ESC 关闭
      if (e.key === 'Escape' && this.visible) {
        this.close()
      }
    },
    handleKeydown (e) {
      if (!this.visible) return
      if (e.key === 'ArrowDown') {
        e.preventDefault()
        this.activeIndex = Math.min(this.activeIndex + 1, this.filteredRoutes.length - 1)
      } else if (e.key === 'ArrowUp') {
        e.preventDefault()
        this.activeIndex = Math.max(this.activeIndex - 1, 0)
      } else if (e.key === 'Enter' && this.filteredRoutes[this.activeIndex]) {
        e.preventDefault()
        this.navigateTo(this.filteredRoutes[this.activeIndex])
      }
    },
    navigateTo (item) {
      this.close()
      this.$router.push(item.path)
    },
    quickNav (path) {
      this.close()
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
/* 遮罩 */
.search-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 2000;
}

/* 对话框 */
.search-dialog {
  position: fixed;
  top: 12vh;
  left: 50%;
  transform: translateX(-50%);
  width: 580px;
  max-width: calc(100vw - 40px);
  z-index: 2001;
  outline: none;
}

.search-panel {
  background: var(--ink-3);
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

/* 搜索输入框 */
.search-input-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.search-input-icon {
  width: 20px;
  height: 20px;
  color: #94a3b8;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  color: #1e293b;
  background: transparent;
  font-family: inherit;
}

.search-input::placeholder {
  color: #94a3b8;
}

.search-esc-hint {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 22px;
  padding: 0 6px;
  font-size: 11px;
  font-family: inherit;
  color: #94a3b8;
  background: var(--ink-3);
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

/* 搜索结果 */
.search-results {
  max-height: 380px;
  overflow-y: auto;
  padding: 6px;
}

.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 20px;
  color: #94a3b8;
  font-size: 14px;
}

.search-empty-icon {
  width: 40px;
  height: 40px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.search-result-item:hover,
.search-result-item.active {
  background: var(--ink-3);
}

.search-result-item.active {
  background: var(--ink-3);
}

.result-item-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--ink-3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.result-icon-svg {
  width: 16px;
  height: 16px;
  color: var(--acid);
}

.result-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.result-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-item-path {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-item-badge {
  font-size: 10px;
  font-weight: 500;
  color: var(--ice);
  background: var(--ink-3);
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

/* 快捷提示 */
.search-hints {
  padding: 16px 20px;
}

.hints-title {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.hints-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.hint-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--grey);
  transition: background 0.15s ease;
}

.hint-item:hover {
  background: var(--ink-3);
  color: #1e293b;
}

.hint-icon {
  font-size: 16px;
}

/* 过渡动画 */
.search-fade-enter-active,
.search-fade-leave-active {
  transition: opacity 0.2s ease;
}
.search-fade-enter-from,
.search-fade-leave-to {
  opacity: 0;
}

.search-slide-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.search-slide-leave-active {
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}
.search-slide-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-20px) scale(0.96);
}
.search-slide-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px) scale(0.98);
}
</style>
