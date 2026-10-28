/**
 * 界面主题引擎
 * ---------------------------------------------------------------
 * 通过 <html data-theme="..."> 切换，所有颜色走 CSS 变量，
 * 无需重载页面、不影响任何业务逻辑。
 * 持久化到 localStorage：xzs.ui.theme
 */

export const THEMES = [
  {
    key: 'acid',
    name: '酸性青柠',
    en: 'ACID LIME',
    desc: '90s 锐舞 / 视网膜投影 HUD，酸性青柠配深黑',
    bg: '#0C0F0C',
    accent: '#CCFF00',
    swatch: ['#CCFF00', '#A9F0FF', '#E8EDE4']
  },
  {
    key: 'cyber',
    name: '赛博霓虹',
    en: 'CYBER NEON',
    desc: '霓虹青紫 / 全息玻璃 / CRT 扫描线，high tech low life',
    bg: '#07091A',
    accent: '#00F0FF',
    swatch: ['#00F0FF', '#FF2BD6', '#A855F7']
  },
  {
    key: 'amber',
    name: '琥珀终端',
    en: 'AMBER CRT',
    desc: '单色琥珀荧光屏，复古终端机的余晖',
    bg: '#150F06',
    accent: '#FFB000',
    swatch: ['#FFB000', '#FFD47A', '#8A6B3A']
  },
  {
    key: 'ice',
    name: '深空冰蓝',
    en: 'DEEP SPACE',
    desc: '低饱和冰蓝，安静的深空指挥舱',
    bg: '#060B14',
    accent: '#7FD8F7',
    swatch: ['#7FD8F7', '#D9F2FF', '#587FA6']
  },
  {
    key: 'grass',
    name: '原野草绿',
    en: 'SOFT MEADOW',
    desc: '低饱和草绿配墨绿底，晨雾原野般的柔和自然',
    bg: '#0B110C',
    accent: '#8FD46B',
    swatch: ['#8FD46B', '#C8E8A6', '#5E9E45']
  }
]

const STORAGE_KEY = 'xzs.ui.theme'
const FX_KEY = 'xzs.ui.fx'
const DEFAULT_THEME = 'acid'

export function isValidTheme (key) {
  return THEMES.some(t => t.key === key)
}

export function currentTheme () {
  let saved = null
  try {
    saved = window.localStorage.getItem(STORAGE_KEY)
  } catch (e) {
    saved = null
  }
  return isValidTheme(saved) ? saved : DEFAULT_THEME
}

/** 特效等级：full（全效）| lite（轻量，关掉扫描线/网格/数据流/辉光） */
export function currentFx () {
  let saved = null
  try {
    saved = window.localStorage.getItem(FX_KEY)
  } catch (e) {
    saved = null
  }
  return saved === 'lite' ? 'lite' : 'full'
}

export function applyFx (level) {
  const lv = level === 'lite' ? 'lite' : 'full'
  document.documentElement.setAttribute('data-fx', lv)
  try {
    window.localStorage.setItem(FX_KEY, lv)
  } catch (e) { /* ignore */ }
  return lv
}

/** 切换主题：写入 data-theme + 持久化 + 派发事件 */
export function applyTheme (key) {
  const theme = isValidTheme(key) ? key : DEFAULT_THEME
  const root = document.documentElement

  // 切换瞬间挂过渡类，280ms 后移除，避免影响日常交互性能
  root.classList.add('theme-transition')
  window.clearTimeout(applyTheme._t)
  applyTheme._t = window.setTimeout(function () {
    root.classList.remove('theme-transition')
  }, 320)

  root.setAttribute('data-theme', theme)
  try {
    window.localStorage.setItem(STORAGE_KEY, theme)
  } catch (e) { /* 隐私模式下忽略 */ }
  syncFxLayer(theme)
  window.dispatchEvent(new CustomEvent('xzs:theme-change', { detail: { theme } }))
  return theme
}

/** 主题特效层（扫描线 / 噪点 / 光晕 / 数据流），只由 CSS 渲染 */
function syncFxLayer (theme) {
  let el = document.getElementById('theme-fx')
  if (!el) {
    el = document.createElement('div')
    el.id = 'theme-fx'
    el.setAttribute('aria-hidden', 'true')
    document.body.appendChild(el)
  }
  el.className = 'theme-fx theme-fx--' + theme
  el.innerHTML = ''

  // 赛博主题：铺若干条竖向数据流
  if (theme === 'cyber') {
    const frag = document.createDocumentFragment()
    for (let i = 0; i < 5; i++) {
      const s = document.createElement('span')
      s.className = 'fx-stream'
      s.style.left = (10 + i * 19 + (i % 2) * 3) + '%'
      s.style.animationDelay = (-i * 0.83).toFixed(2) + 's'
      s.style.opacity = (0.22 + (i % 3) * 0.16).toFixed(2)
      frag.appendChild(s)
    }
    el.appendChild(frag)
  }
}

/** 首屏应用（在 new Vue 之前调用，避免闪回） */
export function initTheme () {
  applyFx(currentFx())
  return applyTheme(currentTheme())
}

export default { THEMES, applyTheme, initTheme, currentTheme, applyFx, currentFx }
