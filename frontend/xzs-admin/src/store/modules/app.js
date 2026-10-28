import Cookies from 'js-cookie'

const state = {
  sidebar: {
    opened: Cookies.get('sidebarStatus') ? !!+Cookies.get('sidebarStatus') : true,
    withoutAnimation: false
  },
  device: 'desktop',
  size: Cookies.get('size') || 'medium',

  // 界面选项（持久化到 localStorage）
  showTagsView: localStorage.getItem('showTagsView') !== 'false',
  showFooter: localStorage.getItem('showFooter') !== 'false',
  showBreadcrumb: localStorage.getItem('showBreadcrumb') !== 'false'
}

const mutations = {
  TOGGLE_SIDEBAR: state => {
    state.sidebar.opened = !state.sidebar.opened
    state.sidebar.withoutAnimation = false
    if (state.sidebar.opened) {
      Cookies.set('sidebarStatus', 1)
    } else {
      Cookies.set('sidebarStatus', 0)
    }
  },
  CLOSE_SIDEBAR: (state, withoutAnimation) => {
    Cookies.set('sidebarStatus', 0)
    state.sidebar.opened = false
    state.sidebar.withoutAnimation = withoutAnimation
  },
  TOGGLE_DEVICE: (state, device) => {
    state.device = device
  },
  SET_SIZE: (state, size) => {
    state.size = size
    Cookies.set('size', size)
  },

  // ── 界面选项 ──
  SET_SHOW_TAGS_VIEW: (state, val) => {
    state.showTagsView = val
    localStorage.setItem('showTagsView', val)
  },
  SET_SHOW_FOOTER: (state, val) => {
    state.showFooter = val
    localStorage.setItem('showFooter', val)
  },
  SET_SHOW_BREADCRUMB: (state, val) => {
    state.showBreadcrumb = val
    localStorage.setItem('showBreadcrumb', val)
  }
}

const actions = {
  toggleSideBar ({ commit }) {
    commit('TOGGLE_SIDEBAR')
  },
  closeSideBar ({ commit }, { withoutAnimation }) {
    commit('CLOSE_SIDEBAR', withoutAnimation)
  },
  toggleDevice ({ commit }, device) {
    commit('TOGGLE_DEVICE', device)
  },
  setSize ({ commit }, size) {
    commit('SET_SIZE', size)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
