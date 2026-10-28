/**
 * FUI / HUD 图表主题（多主题版）
 * ---------------------------------------------------------------------------
 * 每个界面主题对应一套 ECharts 色板，通过 chartTheme() 取当前应使用的主题名。
 * 用法：echarts.init(el, chartTheme())
 */
import * as echarts from 'echarts'

const PALETTES = {
  acid: {
    colors: ['#CCFF00', '#A9F0FF', '#E8EDE4', '#9ECC00', '#6E766C', '#FF6A52', '#D9FF63', '#7FD8E8'],
    paper: '#E8EDE4',
    grey: '#8B948A',
    line: 'rgba(174,182,171,.20)',
    split: 'rgba(174,182,171,.09)',
    tooltipBg: 'rgba(10,13,10,.96)',
    tooltipBorder: 'rgba(204,255,0,.35)',
    cursor: 'rgba(204,255,0,.45)',
    splitArea: ['rgba(14,18,14,.55)', 'rgba(20,26,20,.45)'],
    pieBorder: '#070807',
    detail: '#CCFF00'
  },
  cyber: {
    colors: ['#00F0FF', '#FF2BD6', '#A855F7', '#4D7CFF', '#7CF9FF', '#FFB000', '#9FE8F5', '#E9F6FF'],
    paper: '#E9F6FF',
    grey: '#93A9C9',
    line: 'rgba(120,160,220,.22)',
    split: 'rgba(120,160,220,.10)',
    tooltipBg: 'rgba(8,10,26,.96)',
    tooltipBorder: 'rgba(0,240,255,.45)',
    cursor: 'rgba(0,240,255,.55)',
    splitArea: ['rgba(10,12,28,.62)', 'rgba(16,20,44,.50)'],
    pieBorder: '#05060E',
    detail: '#00F0FF'
  },
  amber: {
    colors: ['#FFB000', '#FFD47A', '#FF8C5A', '#D18E00', '#8A6B3A', '#FFE7BE', '#C98A00', '#6A573A'],
    paper: '#FFE7BE',
    grey: '#D3B681',
    line: 'rgba(200,160,90,.22)',
    split: 'rgba(200,160,90,.10)',
    tooltipBg: 'rgba(20,15,8,.96)',
    tooltipBorder: 'rgba(255,176,0,.42)',
    cursor: 'rgba(255,176,0,.5)',
    splitArea: ['rgba(26,23,16,.6)', 'rgba(34,30,20,.45)'],
    pieBorder: '#0B0904',
    detail: '#FFB000'
  },
  ice: {
    colors: ['#7FD8F7', '#D9F2FF', '#5B87C6', '#A8C8E8', '#4C6480', '#FF5E6E', '#9FE0F7', '#DCEEFB'],
    paper: '#DCEEFB',
    grey: '#9DB4CB',
    line: 'rgba(150,190,225,.22)',
    split: 'rgba(150,190,225,.10)',
    tooltipBg: 'rgba(8,14,24,.96)',
    tooltipBorder: 'rgba(127,216,247,.4)',
    cursor: 'rgba(127,216,247,.5)',
    splitArea: ['rgba(14,24,39,.6)', 'rgba(20,34,52,.45)'],
    pieBorder: '#060B14',
    detail: '#7FD8F7'
  },
  grass: {
    colors: ['#8FD46B', '#C8E8A6', '#5E9E45', '#A9CF7E', '#6E7F66', '#E8623F', '#B7E08F', '#E6F0DE'],
    paper: '#E6F0DE',
    grey: '#93A48C',
    line: 'rgba(150,176,145,.22)',
    split: 'rgba(150,176,145,.10)',
    tooltipBg: 'rgba(9,15,10,.96)',
    tooltipBorder: 'rgba(143,212,107,.42)',
    cursor: 'rgba(143,212,107,.5)',
    splitArea: ['rgba(11,17,12,.6)', 'rgba(18,26,19,.45)'],
    pieBorder: '#070C08',
    detail: '#8FD46B'
  }
}

function buildTheme (p) {
  const axis = {
    axisLine: { show: true, lineStyle: { color: p.line } },
    axisTick: { show: false },
    axisLabel: { color: p.grey, fontFamily: 'monospace', fontSize: 11 },
    splitLine: { show: true, lineStyle: { color: p.split, type: 'dashed' } }
  }

  return {
    color: p.colors,
    backgroundColor: 'transparent',
    textStyle: { color: p.grey, fontFamily: 'monospace' },
    title: {
      textStyle: { color: p.paper, fontWeight: 700 },
      subtextStyle: { color: p.grey }
    },
    grid: { borderColor: p.line },
    legend: {
      textStyle: { color: p.grey, fontFamily: 'monospace', fontSize: 11 },
      icon: 'roundRect',
      itemWidth: 10,
      itemHeight: 4
    },
    tooltip: {
      backgroundColor: p.tooltipBg,
      borderColor: p.tooltipBorder,
      borderWidth: 1,
      textStyle: { color: p.paper, fontFamily: 'monospace', fontSize: 11 },
      axisPointer: { lineStyle: { color: p.cursor } }
    },
    categoryAxis: axis,
    valueAxis: axis,
    logAxis: axis,
    timeAxis: axis,
    line: {
      itemStyle: { borderWidth: 1 },
      lineStyle: { width: 2 },
      symbolSize: 5,
      symbol: 'rect',
      smooth: false
    },
    bar: { itemStyle: { barBorderWidth: 0 } },
    pie: { itemStyle: { borderColor: p.pieBorder, borderWidth: 2 } },
    radar: {
      axisLine: { lineStyle: { color: p.line } },
      splitLine: { lineStyle: { color: p.split } },
      splitArea: { areaStyle: { color: p.splitArea } },
      axisName: { color: p.grey, fontFamily: 'monospace', fontSize: 11 },
      symbolSize: 4
    },
    gauge: {
      axisLine: { lineStyle: { color: [[1, p.split]] } },
      splitLine: { lineStyle: { color: p.line } },
      axisLabel: { color: p.grey },
      detail: { color: p.detail }
    }
  }
}

let registered = false

/** 注册全部界面主题对应的图表主题 */
export function registerFuiChartTheme () {
  if (registered) return
  Object.keys(PALETTES).forEach(function (key) {
    echarts.registerTheme('fui-' + key, buildTheme(PALETTES[key]))
  })
  registered = true
}

/** 当前应使用的 ECharts 主题名（与 <html data-theme> 联动） */
export function chartTheme () {
  const cur = document.documentElement.getAttribute('data-theme') || 'acid'
  return PALETTES[cur] ? 'fui-' + cur : 'fui-acid'
}

/**
 * 运行时主题色（canvas 不认 CSS 变量，图表 option 里必须用真实色值）
 * 用法：itemStyle: { color: tc('acid') }
 */
const TOKENS = {
  acid: { acid: '#CCFF00', acidDim: '#A8D400', ice: '#A9F0FF', paper: '#E8EDE4', grey: '#8B948A', grey2: '#6E766C', danger: '#FF6A52' },
  cyber: { acid: '#00F0FF', acidDim: '#4D7CFF', ice: '#FF2BD6', paper: '#E9F6FF', grey: '#93A9C9', grey2: '#6E85A6', danger: '#FF3355' },
  amber: { acid: '#FFB000', acidDim: '#D18E00', ice: '#FFD47A', paper: '#FFE7BE', grey: '#D3B681', grey2: '#9E855A', danger: '#FF6B35' },
  ice: { acid: '#7FD8F7', acidDim: '#57B8DD', ice: '#D9F2FF', paper: '#DCEEFB', grey: '#9DB4CB', grey2: '#728CA6', danger: '#FF5E6E' },
  grass: { acid: '#8FD46B', acidDim: '#69A94A', ice: '#C8E8A6', paper: '#E6F0DE', grey: '#93A48C', grey2: '#728369', danger: '#E8623F' }
}

export function tc (name) {
  const cur = document.documentElement.getAttribute('data-theme') || 'acid'
  const t = TOKENS[cur] || TOKENS.acid
  return t[name] || t.acid
}

/** 带透明度的主题色，返回 rgba() 字符串 */
export function tca (name, alpha) {
  const hex = tc(name).replace('#', '')
  const r = parseInt(hex.slice(0, 2), 16)
  const g = parseInt(hex.slice(2, 4), 16)
  const b = parseInt(hex.slice(4, 6), 16)
  return 'rgba(' + r + ', ' + g + ', ' + b + ', ' + alpha + ')'
}

export default PALETTES
