<template>
  <div class="navbar">
    <!-- 左侧：汉堡菜单 + 面包屑 -->
    <div class="navbar-left">
      <div
        id="hamburger-container"
        class="hamburger-wrapper"
        :class="{ 'is-active': sidebar.opened }"
        @click="toggleSideBar"
      >
        <svg
          class="hamburger-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="4" y1="6" x2="20" y2="6" />
          <line x1="4" y1="12" x2="20" y2="12" />
          <line x1="4" y1="18" x2="20" y2="18" />
        </svg>
      </div>
      <breadcrumb v-if="showBreadcrumb" id="breadcrumb-container" class="breadcrumb-container" />
    </div>

    <!-- 右侧：操作区 -->
    <div class="navbar-right">
      <!-- 搜索按钮 -->
      <el-tooltip content="搜索 (Cmd+K)" placement="bottom" effect="light">
        <div class="nav-icon-btn" @click="openSearch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="nav-icon-svg">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </div>
      </el-tooltip>

      <!-- 消息通知 -->
      <notification-panel ref="notificationPanel" />

      <!-- 全屏按钮 -->
      <el-tooltip content="全屏" placement="bottom" effect="light">
        <div class="nav-icon-btn" @click="toggleFullscreen">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="nav-icon-svg">
            <path v-if="!isFullscreen" d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
            <path v-else d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"/>
          </svg>
        </div>
      </el-tooltip>

      <!-- 用户下拉菜单 -->
      <el-dropdown class="user-dropdown" trigger="click" @command="handleCommand">
        <div class="user-trigger">
          <div class="user-avatar-wrap">
            <img v-if="userAvatar" :src="userAvatar" class="user-avatar-img" />
            <img v-else :src="defaultAvatar" class="user-avatar-img" />
            <span class="avatar-ring" />
          </div>
          <span class="user-name-text">{{ userName }}</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="chevron-icon">
            <polyline points="6 9 12 15 18 9"/>
          </svg>
        </div>
        <el-dropdown-menu slot="dropdown" class="user-dropdown-menu">
          <div class="dropdown-header">
            <img v-if="userAvatar" :src="userAvatar" class="dropdown-avatar-img" />
            <img v-else :src="defaultAvatar" class="dropdown-avatar-img" />
            <div class="dropdown-user-info">
              <span class="dropdown-name">{{ userName }}</span>
              <span class="dropdown-role">管理员</span>
            </div>
          </div>
          <div class="dropdown-divider" />
          <el-dropdown-item command="profile" icon="el-icon-user-solid">
            个人信息
          </el-dropdown-item>
          <el-dropdown-item command="home" icon="el-icon-s-home">
            主页
          </el-dropdown-item>
          <div class="dropdown-divider" />
          <el-dropdown-item command="logout" icon="el-icon-switch-button" class="logout-item">
            退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>

    <!-- 搜索对话框（独立引入） -->
    <search-dialog ref="searchDialog" />
  </div>
</template>

<script>
import { mapGetters, mapMutations } from 'vuex'
import loginApi from '@/api/login'
import Breadcrumb from '@/components/Breadcrumb'
import SearchDialog from '@/components/SearchDialog'
import NotificationPanel from '@/components/NotificationPanel'

export default {
  components: {
    Breadcrumb,
    SearchDialog,
    NotificationPanel
  },
  data () {
    return {
      isFullscreen: false,
      defaultAvatar: require('@/assets/avatar.gif')
    }
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'device',
      'userName',
      'userAvatar'
    ]),
    showBreadcrumb () {
      return this.$store.state.app.showBreadcrumb !== false
    }
  },
  mounted () {
    document.addEventListener('fullscreenchange', this.handleFullscreenChange)
    document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange)
  },
  beforeDestroy () {
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('webkitfullscreenchange', this.handleFullscreenChange)
  },
  methods: {
    toggleSideBar () {
      this.$store.dispatch('app/toggleSideBar')
    },

    // ── 全局搜索 ──
    openSearch () {
      this.$refs.searchDialog.open()
    },

    // ── 全屏切换 ──
    toggleFullscreen () {
      if (!document.fullscreenElement && !document.webkitFullscreenElement) {
        const el = document.documentElement
        if (el.requestFullscreen) {
          el.requestFullscreen()
        } else if (el.webkitRequestFullscreen) {
          el.webkitRequestFullscreen()
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen()
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen()
        }
      }
    },
    handleFullscreenChange () {
      this.isFullscreen = !!document.fullscreenElement || !!document.webkitFullscreenElement
    },

    // ── 用户下拉 ──
    handleCommand (command) {
      if (command === 'logout') {
        this.logout()
      } else if (command === 'profile') {
        this.$router.push('/profile/index')
      } else if (command === 'home') {
        this.$router.push('/')
      }
    },
    logout () {
      const _this = this
      loginApi.logout().then(function () {
        _this.clearLogin()
        _this.$router.push({ path: '/login' })
      }).catch(function () {
        _this.clearLogin()
        _this.$router.push({ path: '/login' })
      })
    },
    ...mapMutations('user', ['clearLogin'])
  }
}
</script>

<style scoped>
/* ═══════════════════════════════════════
   Navbar — Premium Design
   ═══════════════════════════════════════ */
.navbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(226,232,240,0.6);
  box-shadow: 0 1px 4px rgba(0,0,0,0.02), 0 4px 16px rgba(0,0,0,0.03);
  position: relative;
  z-index: 10;
}

/* 底部渐变装饰线 */
.navbar::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(var(--acid-rgb), 0.3) 20%,
    rgba(var(--acid-rgb), 0.3) 50%,
    rgba(var(--acid-rgb), 0.3) 80%,
    transparent 100%);
  opacity: 0.5;
}

/* ── 左侧 ── */
.navbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ── 汉堡按钮 ── */
.hamburger-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  color: #64748b;
}

.hamburger-wrapper:hover {
  background: var(--ink-3);
  color: var(--acid);
}

.hamburger-wrapper.is-active {
  color: var(--acid);
  background: rgba(var(--acid-rgb), 0.06);
}

.hamburger-icon {
  width: 18px;
  height: 18px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.hamburger-wrapper.is-active .hamburger-icon {
  transform: rotate(90deg);
}

/* ── 面包屑 ── */
.breadcrumb-container {
  margin-left: 4px;
}

/* ── 右侧 ── */
.navbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ── 图标按钮通用 ── */
.nav-icon-btn {
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

.nav-icon-btn:hover {
  background: var(--ink-3);
  color: var(--acid);
}

.nav-icon-svg {
  width: 18px;
  height: 18px;
}

/* ── 用户下拉 ── */
.user-dropdown {
  margin-left: 4px;
  height: 100%;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.user-trigger:hover {
  background: var(--ink-3);
  border-color: var(--ink-5);
}

/* 头像 */
.user-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.user-avatar-img,
.user-avatar-default {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  object-fit: cover;
  position: relative;
  z-index: 1;
}

.user-avatar-default {
  background: linear-gradient(135deg, var(--acid), var(--ice));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 650;
}

/* 头像辉光环 */
.avatar-ring {
  position: absolute;
  inset: -2px;
  border-radius: 11px;
  background: linear-gradient(135deg, var(--acid), var(--ice));
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 0;
}

.user-trigger:hover .avatar-ring {
  opacity: 0.2;
}

/* 用户名 */
.user-name-text {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--grey);
  white-space: nowrap;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 下拉箭头 */
.chevron-icon {
  width: 14px;
  height: 14px;
  color: #94a3b8;
  transition: transform 0.25s ease;
}

.user-trigger:hover .chevron-icon {
  color: var(--acid);
}

/* ═══════════════════════════════════════
   下拉菜单
   ═══════════════════════════════════════ */
.user-dropdown-menu {
  border-radius: 12px !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow: 0 12px 40px rgba(0,0,0,0.08), 0 0 0 1px rgba(0,0,0,0.02) !important;
  padding: 6px !important;
  min-width: 200px !important;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
}

.dropdown-avatar,
.dropdown-avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.dropdown-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.dropdown-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-role {
  font-size: 12px;
  color: #94a3b8;
}

.dropdown-divider {
  height: 1px;
  background: var(--ink-3);
  margin: 4px 0;
}

.logout-item {
  color: #ef4444 !important;
}

.logout-item:hover {
  background: var(--ink-3) !important;
  color: #dc2626 !important;
}
</style>

<!-- 全局样式覆盖 (非 scoped) -->
<style>
/* Element UI 下拉菜单项样式增强 */
.user-dropdown-menu .el-dropdown-menu__item {
  border-radius: 7px;
  margin: 1px 4px;
  padding: 0 12px;
  height: 36px;
  line-height: 36px;
  font-size: 13px;
  color: var(--grey);
  transition: all 0.2s ease;
}

.user-dropdown-menu .el-dropdown-menu__item:hover {
  background: var(--ink-3);
  color: #1e293b;
}

.user-dropdown-menu .el-dropdown-menu__item i {
  color: #94a3b8;
  margin-right: 8px;
}

/* 面包屑样式增强 */
.navbar .app-breadcrumb.el-breadcrumb {
  font-size: 13px;
}

.navbar .app-breadcrumb.el-breadcrumb .el-breadcrumb__inner {
  color: #94a3b8;
  font-weight: 400;
  transition: color 0.2s;
}

.navbar .app-breadcrumb.el-breadcrumb .el-breadcrumb__inner.is-link:hover {
  color: var(--acid);
}
</style>
