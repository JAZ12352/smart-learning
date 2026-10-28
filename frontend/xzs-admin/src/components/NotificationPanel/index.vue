<template>
  <div class="notification-wrapper">
    <!-- 通知图标按钮 -->
    <el-tooltip content="消息通知" placement="bottom" effect="light">
      <div class="notification-trigger" :class="{ 'is-active': visible }" @click="togglePanel">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="nav-icon-svg">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
          <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
        </svg>
        <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        <span v-else class="notification-dot" />
      </div>
    </el-tooltip>

    <!-- 通知面板 -->
    <transition name="notif-slide">
      <div v-show="visible" class="notification-panel" @click.stop>
        <!-- 头部 -->
        <div class="panel-header">
          <h3 class="panel-title">消息通知</h3>
          <button class="panel-clear-btn" v-if="unreadCount > 0" @click="markAllRead">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="clear-icon">
              <polyline points="9 11 12 14 22 4"/>
              <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
            </svg>
            全部已读
          </button>
        </div>

        <!-- 标签切换 -->
        <div class="panel-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            class="panel-tab"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span v-if="tab.key === 'unread' && unreadCount > 0" class="tab-count">{{ unreadCount }}</span>
          </div>
        </div>

        <!-- 列表 -->
        <div class="panel-body" ref="panelBody">
          <div v-if="filteredList.length === 0" class="panel-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
            <span>{{ emptyText }}</span>
          </div>
          <div
            v-for="(item) in filteredList"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
            @click="markRead(item)"
          >
            <div class="notif-icon-wrap" :class="item.type">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="notif-type-icon">
                <path v-if="item.type === 'system'" d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                <polyline v-if="item.type === 'success'" points="9 11 12 14 22 4"/>
                <circle v-if="item.type === 'warning'" cx="12" cy="12" r="10"/>
                <line v-if="item.type === 'warning'" x1="12" y1="8" x2="12" y2="12"/>
                <line v-if="item.type === 'warning'" x1="12" y1="16" x2="12.01" y2="16"/>
                <path v-if="item.type === 'info'" d="M21 12a9 9 0 1 1-9-9" />
                <line v-if="item.type === 'info'" x1="12" y1="16" x2="12" y2="12"/>
                <line v-if="item.type === 'info'" x1="12" y1="8" x2="12.01" y2="8"/>
              </svg>
            </div>
            <div class="notif-content">
              <div class="notif-text">{{ item.content }}</div>
              <div class="notif-time">{{ item.time }}</div>
            </div>
            <div v-if="!item.read" class="notif-unread-dot" />
          </div>
        </div>

        <!-- 底部 -->
        <div class="panel-footer">
          <button class="view-all-btn" @click="viewAll">查看全部通知</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'NotificationPanel',
  data () {
    return {
      visible: false,
      activeTab: 'all',
      tabs: [
        { key: 'all', label: '全部' },
        { key: 'unread', label: '未读' },
        { key: 'read', label: '已读' }
      ],
      notifications: [
        { id: 1, type: 'system', content: '系统检测到新的知识点更新，建议刷新知识库', time: '2 分钟前', read: false },
        { id: 2, type: 'success', content: 'AI 教案生成完成：《二次函数》教学设计已就绪', time: '15 分钟前', read: false },
        { id: 3, type: 'info', content: '学生张三的学习画像已更新，新增数学薄弱点分析', time: '1 小时前', read: false },
        { id: 4, type: 'warning', content: '知识库同步任务已完成，共更新 23 个文档', time: '3 小时前', read: false },
        { id: 5, type: 'system', content: '系统将于 02:00 进行维护升级，预计耗时 30 分钟', time: '昨天', read: true },
        { id: 6, type: 'success', content: '学习路径规划完成：李四的个性化学习方案已生成', time: '昨天', read: true },
        { id: 7, type: 'info', content: '评估报告已生成，请查看班级整体学习情况', time: '2 天前', read: true }
      ]
    }
  },
  computed: {
    unreadCount () {
      return this.notifications.filter(n => !n.read).length
    },
    filteredList () {
      if (this.activeTab === 'all') return this.notifications
      if (this.activeTab === 'unread') return this.notifications.filter(n => !n.read)
      return this.notifications.filter(n => n.read)
    },
    emptyText () {
      if (this.activeTab === 'all') return '暂无通知'
      if (this.activeTab === 'unread') return '暂无未读通知'
      return '暂无已读通知'
    }
  },
  mounted () {
    document.addEventListener('click', this.handleDocumentClick)
  },
  beforeDestroy () {
    document.removeEventListener('click', this.handleDocumentClick)
  },
  methods: {
    handleDocumentClick (e) {
      if (this.visible && !this.$el.contains(e.target)) {
        this.visible = false
      }
    },
    togglePanel (e) {
      e.stopPropagation()
      this.visible = !this.visible
    },
    closePanel () {
      this.visible = false
    },
    markRead (item) {
      if (!item.read) {
        item.read = true
      }
      // 可以跳转到详情或执行其他操作
    },
    markAllRead () {
      this.notifications.forEach(n => { n.read = true })
    },
    viewAll () {
      this.visible = false
      this.$message({
        message: '通知中心功能开发中...',
        type: 'info',
        customClass: 'premium-message'
      })
    }
  }
}
</script>

<style scoped>
.notification-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.notification-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  color: #64748b;
  position: relative;
}

.notification-trigger:hover,
.notification-trigger.is-active {
  background: var(--ink-3);
  color: var(--acid);
}

.nav-icon-svg {
  width: 18px;
  height: 18px;
}

.notification-badge {
  position: absolute;
  top: 3px;
  right: 3px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 10px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 650;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #ffffff;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2);
}

.notification-dot {
  position: absolute;
  top: 6px;
  right: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ef4444;
  border: 1.5px solid #ffffff;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2);
}

/* 面板 */
.notification-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: -8px;
  width: 380px;
  background: var(--ink-3);
  border-radius: 14px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(0, 0, 0, 0.04);
  z-index: 200;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 0;
}

.panel-title {
  font-size: 16px;
  font-weight: 650;
  color: #1e293b;
  margin: 0;
}

.panel-clear-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--acid);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.panel-clear-btn:hover {
  background: var(--ink-3);
}

.clear-icon {
  width: 14px;
  height: 14px;
}

/* 标签 */
.panel-tabs {
  display: flex;
  gap: 0;
  padding: 12px 20px 0;
  border-bottom: 1px solid #f1f5f9;
}

.panel-tab {
  position: relative;
  padding: 8px 16px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.panel-tab.active {
  color: var(--acid);
  font-weight: 500;
  border-bottom-color: var(--ink-5);
}

.panel-tab:hover:not(.active) {
  color: var(--grey);
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--ink-3);
  color: var(--acid);
  font-size: 10px;
  font-weight: 600;
  margin-left: 4px;
}

/* 列表 */
.panel-body {
  max-height: 340px;
  overflow-y: auto;
  padding: 6px;
}

.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 20px;
  color: #94a3b8;
  font-size: 13px;
}

.empty-icon {
  width: 36px;
  height: 36px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease;
  position: relative;
}

.notification-item:hover {
  background: var(--ink-3);
}

.notification-item.unread {
  background: var(--ink-3);
}

.notification-item.unread:hover {
  background: var(--ink-3);
}

.notif-icon-wrap {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notif-icon-wrap.system {
  background: var(--ink-3);
  color: var(--acid);
}

.notif-icon-wrap.success {
  background: var(--ink-3);
  color: #10b981;
}

.notif-icon-wrap.warning {
  background: var(--ink-3);
  color: #f59e0b;
}

.notif-icon-wrap.info {
  background: var(--ink-3);
  color: var(--ice);
}

.notif-type-icon {
  width: 16px;
  height: 16px;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-text {
  font-size: 13px;
  color: var(--grey);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-item.unread .notif-text {
  font-weight: 500;
  color: #1e293b;
}

.notif-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.notif-unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ink-3);
  flex-shrink: 0;
  margin-top: 6px;
}

/* 底部 */
.panel-footer {
  padding: 10px 20px;
  border-top: 1px solid #f1f5f9;
  text-align: center;
}

.view-all-btn {
  font-size: 13px;
  color: var(--acid);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  transition: background 0.15s;
}

.view-all-btn:hover {
  background: var(--ink-3);
}

/* 动画 */
.notif-slide-enter-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.notif-slide-leave-active {
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}
.notif-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.96);
}
.notif-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}
</style>
