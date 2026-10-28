<template>
  <div :class="classObj" class="app-wrapper">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside" />
    <sidebar class="sidebar-container" />
    <div  class="hasTagsView main-container">
      <div class="fixed-header">
        <navbar />
        <tags-view v-if="showTagsView" />
      </div>
      <app-main />
      <floating-teacher />
      <div v-if="showFooter" class="foot-copyright">
        <span class="fc-live">● LIVE</span>
        <span class="fc-txt">POLYAGENT · 基于多智能体的教学培训系统 · NODE SH-04</span>
        <span class="fc-meta">RETINAL-04 / 0x2F9A</span>
      </div>
      <back-to-top :visibility-height="100" :back-position="0" transition-name="fade" ref="backTop"/>
    </div>

    <!-- HUD 装饰层（纯视觉，不参与交互） -->
    <div class="hud-frame" aria-hidden="true">
      <span class="hf tl"></span><span class="hf tr"></span>
      <span class="hf bl"></span><span class="hf br"></span>
      <span class="hud-vert">POLYAGENT // RETINAL-04 // 0x2F9A</span>
      <span class="hud-sweep"></span>
    </div>
  </div>
</template>

<script>
import { AppMain, Navbar, Sidebar, TagsView } from './components'
import ResizeMixin from './mixin/ResizeHandler'
import { mapState } from 'vuex'
import BackToTop from '@/components/BackToTop'
import FloatingTeacher from '@/components/FloatingTeacher'
export default {
  name: 'Layout',
  components: {
    AppMain,
    Navbar,
    Sidebar,
    TagsView,
    BackToTop,
    FloatingTeacher
  },
  mixins: [ResizeMixin],
  computed: {
    ...mapState({
      sidebar: state => state.app.sidebar,
      device: state => state.app.device,
      showTagsView: state => state.app.showTagsView !== false,
      showFooter: state => state.app.showFooter !== false
    }),
    classObj () {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    }
  },
  methods: {
    handleClickOutside () {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    }
  }
}
</script>

<style>
  .app-wrapper {
    position: relative;
    height: 100%;
    width: 100%;
  }

  .app-wrapper.mobile.openSidebar {
    position: fixed;
    top: 0;
  }

  .drawer-bg {
    background: #000;
    opacity: 0.3;
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 9;
    width: calc(100% - 240px);
    transition: width 0.32s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .hideSidebar .fixed-header {
    width: calc(100% - 68px)
  }

  .mobile .fixed-header {
    width: 100%;
  }

  /* ═══════════ FUI · HUD 装饰层 ═══════════ */
  .hud-frame {
    position: fixed;
    inset: 0;
    pointer-events: none;
    z-index: 9996;
  }

  .hud-frame .hf {
    position: absolute;
    width: 12px;
    height: 12px;
    border: 1px solid rgba(var(--acid-rgb), .35);
  }

  .hud-frame .tl { top: 4px; left: 4px; border-right: 0; border-bottom: 0; }
  .hud-frame .tr { top: 4px; right: 4px; border-left: 0; border-bottom: 0; }
  .hud-frame .bl { bottom: 4px; left: 4px; border-right: 0; border-top: 0; }
  .hud-frame .br { bottom: 4px; right: 4px; border-left: 0; border-top: 0; }

  .hud-vert {
    position: absolute;
    right: 7px;
    top: 50%;
    transform: rotate(90deg) translateX(50%);
    transform-origin: right center;
    font-family: "Cascadia Mono", Consolas, monospace;
    font-size: 6.5px;
    letter-spacing: .34em;
    color: rgba(var(--acid-rgb), .2);
    white-space: nowrap;
  }

  .hud-sweep {
    position: absolute;
    left: 0;
    right: 0;
    height: 140px;
    background: linear-gradient(180deg, transparent, rgba(var(--acid-rgb), .05) 50%, transparent);
    animation: fuiSweep 11s linear infinite;
  }

  @keyframes fuiSweep {
    0% { top: -150px; }
    100% { top: 100%; }
  }

  /* 底部系统状态条 */
  .foot-copyright {
    position: relative;
    width: 100% !important;
    max-width: none !important;
    flex: none;
    margin-top: auto !important;
    align-self: stretch;
    z-index: 9;
    display: flex !important;
    align-items: center;
    gap: 14px;
    height: 24px;
    box-sizing: border-box;
    text-align: left !important;
    padding: 0 16px !important;
    border-top: 1px solid rgba(var(--grid-rgb), .12);
    background: rgba(8, 10, 8, .96);
    font-family: "Cascadia Mono", Consolas, monospace;
    font-size: 8.5px;
    line-height: 1;
    letter-spacing: .14em;
    color: #525850 !important;
    pointer-events: none;
  }

  .foot-copyright .fc-live { color: var(--acid); }

  .foot-copyright .fc-meta {
    margin-left: auto;
    color: rgba(var(--acid-rgb), .3);
  }

</style>
