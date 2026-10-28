<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}">
    <transition name="sidebarLogoFade">
      <router-link key="expand" class="sidebar-logo-link" to="/">
        <div class="logo-icon">
          <svg width="26" height="26" viewBox="0 0 26 26" fill="none" class="logo-svg" shape-rendering="crispEdges">
            <rect x="0.5" y="0.5" width="25" height="25" stroke="rgba(204,255,0,0.45)" />
            <rect x="5.5" y="5.5" width="15" height="15" stroke="rgba(204,255,0,0.7)" />
            <rect x="11" y="11" width="4" height="4" fill="#CCFF00" />
            <line x1="13" y1="0" x2="13" y2="5" stroke="#CCFF00" />
            <line x1="13" y1="21" x2="13" y2="26" stroke="#CCFF00" />
            <line x1="0" y1="13" x2="5" y2="13" stroke="#CCFF00" />
            <line x1="21" y1="13" x2="26" y2="13" stroke="#CCFF00" />
            <rect x="1.5" y="1.5" width="2" height="2" fill="rgba(204,255,0,0.7)" />
            <rect x="22.5" y="22.5" width="2" height="2" fill="rgba(204,255,0,0.7)" />
          </svg>
        </div>
        <div v-if="!collapse" class="logo-text">
          <span class="logo-title">POLYAGENT</span>
          <span class="logo-desc">多智能体教学培训系统</span>
        </div>
      </router-link>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'SidebarLogo',
  props: { collapse: { type: Boolean, required: true } },
  data () { return { title: '智慧乐学' } }
}
</script>

<style scoped>
.sidebarLogoFade-enter-active { transition: opacity 0.35s ease; }
.sidebarLogoFade-enter, .sidebarLogoFade-leave-to { opacity: 0; }

.sidebar-logo-container {
  width: 100%;
  height: 82px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  border-bottom: 1px solid rgba(var(--acid-rgb), 0.15);
  background: rgba(0,0,0,0.12);
}

.sidebar-logo-container::after {
  content: '';
  position: absolute; bottom: 0; left: 18px; right: 18px; height: 1px;
  background: linear-gradient(90deg,
    transparent, rgba(var(--acid-rgb), 0.25) 20%,
    rgba(var(--ice-rgb), 0.55) 50%,
    rgba(var(--acid-rgb), 0.25) 80%, transparent);
}

.sidebar-logo-container .sidebar-logo-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%; height: 100%;
  padding: 0; margin: 0; gap: 8px;
  text-decoration: none;
}

.logo-icon {
  width: 44px; height: 44px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; margin: 0 auto;
}

.logo-svg {
  display: block;
  filter: drop-shadow(0 2px 14px rgba(var(--acid-rgb), 0.5));
  transition: all 0.3s ease;
}
.sidebar-logo-link:hover .logo-svg {
  filter: drop-shadow(0 4px 22px rgba(var(--acid-rgb), 0.65));
  transform: scale(1.05);
}

.logo-text {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 2px; width: 100%; text-align: center;
}

.logo-title {
  font-size: 17px; font-weight: 700;
  color: #e0f2fe;
  letter-spacing: 4px; white-space: nowrap; text-align: center;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.logo-desc {
  font-size: 10px; font-weight: 400;
  color: rgba(var(--ice-rgb), 0.5);
  letter-spacing: 1.5px; white-space: nowrap; text-align: center;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

.sidebar-logo-container.collapse { height: 56px; }
.sidebar-logo-container.collapse .sidebar-logo-link {
  flex-direction: row; padding: 0; justify-content: center; gap: 0;
}
.sidebar-logo-container.collapse .logo-text { display: none; }
.sidebar-logo-container.collapse .logo-icon { width: 34px; height: 34px; }
.sidebar-logo-container.collapse::after { left: 6px; right: 6px; }
</style>
