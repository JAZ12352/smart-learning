import Vue from 'vue'
import App from './App.vue'
import { router } from './router'
import store from './store'
import 'normalize.css/normalize.css'
import Element from 'element-ui'
import './styles/element-variables.scss'

import '@/styles/index.scss' // global css
import '@/styles/fui-theme.scss' // FUI / HUD theme overlay — must stay last
import '@/styles/themes.scss' // 多主题令牌 + 赛博霓虹特效层 — 必须最后
import { registerFuiChartTheme } from '@/utils/fui-echarts-theme'
import { initTheme } from '@/utils/theme'
import './icons' // icon
import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style

registerFuiChartTheme() // 注册 FUI 图表主题，所有 echarts.init(el, 'fui') 生效
initTheme() // 首屏应用已保存的界面主题（默认 酸性青柠）

Vue.use(Element, {
  size: 'medium' // set element-ui default size
})

Vue.config.productionTip = false

NProgress.configure({ showSpinner: false }) // NProgress Configuration

router.beforeEach(async (to, from, next) => {
  // start progress bar
  NProgress.start()
  if (to.meta.title !== undefined) {
    document.title = to.meta.title
  } else {
    document.title = '\u200E'
  }
  store.commit('router/initRoutes')

  if (to.path) {
    // eslint-disable-next-line no-undef
    _hmt.push(['_trackPageview', '/#' + to.fullPath])
  }

  // 检查登录状态
  const token = localStorage.getItem('token')
  const whiteList = ['/login', '/register'] // 不重定向白名单

  if (token) {
    // 已登录，如果访问登录页则跳转到主页
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      // 确保用户信息已加载（含头像），避免刷新后丢失
      if (!store.state.user.userInfo) {
        store.dispatch('user/initUserInfo')
      }
      next()
    }
  } else {
    // 未登录
    if (whiteList.indexOf(to.path) !== -1) {
      // 在免登录白名单，直接进入
      next()
    } else {
      // 其他页面跳转到登录页
      next(`/login?redirect=${to.path}`)
    }
  }
})

router.afterEach(() => {
  // finish progress bar
  NProgress.done()
})

Vue.prototype.$$router = router

new Vue({
  router: router,
  store: store,
  render: h => h(App)
}).$mount('#app')
