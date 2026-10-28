<template>
  <div class="mindmap-container">
    <!-- 顶部输入区 -->
    <div class="mm-header">
      <div class="mm-header-left">
        <h2 class="mm-title">
          <i class="el-icon-share"></i>
          思维导图
        </h2>
        <p class="mm-subtitle">baoyu-diagram · AI 驱动的知识图谱可视化</p>
      </div>
    </div>

    <!-- 输入行 -->
    <div class="mm-input-row">
      <div class="mm-input-wrap">
        <el-input
          v-model="topic"
          placeholder="输入学习主题，AI 自动生成知识图谱"
          size="large"
          @keyup.enter.native="generate"
          :disabled="generating"
        >
          <template slot="prepend">主题</template>
        </el-input>
      </div>
      <el-button
        type="primary"
        size="large"
        :loading="generating"
        :disabled="!topic.trim()"
        @click="generate"
        class="mm-gen-btn"
      >
        <i v-if="!generating" class="el-icon-magic-stick"></i>
        {{ generating ? 'AI 生成中...' : '生成导图' }}
      </el-button>
      <el-button
        v-if="generating"
        size="large"
        @click="cancelGenerate"
        class="mm-cancel-btn"
      >
        取消
      </el-button>
    </div>

    <!-- 主体内容 -->
    <div class="mm-body">
      <!-- 加载骨架屏 -->
      <div v-if="generating" class="mm-loading">
        <div class="mm-skeleton">
          <div class="sk-circle sk-root"></div>
          <div class="sk-line sk-l1"></div>
          <div class="sk-circle sk-c1"></div>
          <div class="sk-line sk-l2"></div>
          <div class="sk-circle sk-c2"></div>
          <div class="sk-line sk-l3"></div>
          <div class="sk-circle sk-c3"></div>
          <div class="sk-line sk-l4"></div>
          <div class="sk-circle sk-c4"></div>
        </div>
        <p class="mm-stream-hint" v-if="streamText">
          已生成 {{ streamText.length }} 字符...
        </p>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="errorMessage" class="mm-error">
        <div class="mm-error-icon">
          <i class="el-icon-warning-outline"></i>
        </div>
        <h3>生成失败</h3>
        <p>{{ errorMessage }}</p>
        <el-button type="primary" size="small" @click="retryGenerate">重试</el-button>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!treeData && history.length === 0" class="mm-empty">
        <div class="mm-empty-icon">
          <svg width="80" height="80" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="30" cy="40" r="8" stroke="#c8a45c" stroke-width="2"/>
            <circle cx="30" cy="40" r="3" fill="#c8a45c"/>
            <path d="M38 40 L52 22" stroke="#94a3b8" stroke-width="1.5"/>
            <circle cx="56" cy="20" r="6" stroke="#8c8470" stroke-width="1.5"/>
            <path d="M38 40 L52 58" stroke="#94a3b8" stroke-width="1.5"/>
            <circle cx="56" cy="60" r="6" stroke="#8c8470" stroke-width="1.5"/>
            <path d="M62 18 L72 12" stroke="#94a3b8" stroke-width="1.2"/>
            <circle cx="75" cy="10" r="4" stroke="#b0a68e" stroke-width="1.2"/>
            <path d="M62 18 L72 26" stroke="#94a3b8" stroke-width="1.2"/>
            <circle cx="75" cy="28" r="4" stroke="#b0a68e" stroke-width="1.2"/>
          </svg>
        </div>
        <h3>开始创建思维导图</h3>
        <p>输入学习主题，AI 将自动生成知识图谱</p>
        <div class="mm-empty-tips">
          <span>试试：</span>
          <el-tag
            v-for="t in demoTopics"
            :key="t"
            size="small"
            class="mm-tip-tag"
            @click="topic = t; generate()"
          >{{ t }}</el-tag>
        </div>
      </div>

      <!-- SVG 思维导图 -->
      <div v-else class="mm-svg-wrapper" ref="svgWrapper">
        <!-- 工具栏 -->
        <div class="mm-toolbar">
          <div class="mm-toolbar-left">
            <span class="mm-current-topic">{{ currentTopic }}</span>
            <span class="mm-toolbar-hint">双击编辑 · 右键增删 · 点击+/−折叠 · 滚轮缩放 · 拖拽平移</span>
          </div>
          <div class="mm-toolbar-right">
            <span v-if="nodeStats" class="mm-stats">
              共 {{ nodeStats.total }} 个节点，{{ nodeStats.leaves }} 个知识点
              <el-tooltip v-if="nodeStats.leaves < 15" content="知识点可能不够完整，建议重新生成" placement="top">
                <i class="el-icon-warning" style="color:#f59e0b;margin-left:4px"></i>
              </el-tooltip>
            </span>
            <el-button-group>
              <el-button size="mini" icon="el-icon-zoom-in" @click="zoomIn" :disabled="zoomLevel >= 3"></el-button>
              <el-button size="mini" icon="el-icon-zoom-out" @click="zoomOut" :disabled="zoomLevel <= 0.3"></el-button>
              <el-button size="mini" icon="el-icon-rank" @click="fitView"></el-button>
            </el-button-group>
            <el-button size="mini" icon="el-icon-download" @click="downloadSVG" style="margin-left:8px">导出 SVG</el-button>
          </div>
        </div>

        <!-- 渲染区域 -->
        <div class="mm-render-area" ref="renderArea">
          <div class="mm-svg-inner" ref="svgInner" :style="svgTransformStyle">
            <svg
              :viewBox="svgViewBox"
              width="100%"
              height="100%"
              preserveAspectRatio="xMidYMid meet"
              xmlns="http://www.w3.org/2000/svg"
              ref="svgEl"
              class="baoyu-mindmap-svg"
              style="max-width:100%;max-height:100%"
            ></svg>
          </div>
        </div>

        <!-- 图例 -->
        <div class="mm-legend-bar">
          <span v-for="(c, i) in levelColors.slice(0, 5)" :key="i" class="mm-legend-item">
            <span class="mm-legend-dot" :style="{ background: c.stroke }"></span>
            层级 {{ i + 1 }}
          </span>
        </div>
      </div>

      <!-- 右键上下文菜单 -->
      <div
        v-if="contextMenu.visible"
        class="mm-context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        @click.stop
      >
        <div class="mm-ctx-item" @click="menuAddChild"><i class="el-icon-plus"></i> 添加子节点</div>
        <div class="mm-ctx-item" @click="menuAddSibling"><i class="el-icon-share"></i> 添加同级节点</div>
        <div class="mm-ctx-item mm-ctx-danger" @click="menuDelete" v-if="canDeleteContextNode"><i class="el-icon-delete"></i> 删除节点</div>
      </div>
    </div>

    <!-- 历史侧边栏 -->
    <div class="mm-history" v-if="history.length > 0">
      <div class="mm-history-header">
        <i class="el-icon-time"></i>
        生成历史
        <span class="mm-history-count">{{ history.length }}</span>
      </div>
      <div
        v-for="r in history"
        :key="r.id"
        :class="['mm-history-item', { active: selectedHistoryId === r.id }]"
        @click="loadHistoryItem(r)"
      >
        <div class="mm-history-item-icon">
          <i class="el-icon-share"></i>
        </div>
        <div class="mm-history-item-body">
          <div class="mm-history-item-title">{{ r.title }}</div>
          <div class="mm-history-item-time">{{ formatTime(r.createTime) }}</div>
        </div>
        <el-button
          size="mini"
          type="text"
          icon="el-icon-delete"
          class="mm-history-item-del"
          @click.stop="deleteItem(r.id)"
        ></el-button>
      </div>
    </div>
  </div>
</template>

<script>
import resourceGenerationApi from '@/api/resourceGeneration'

// ==================== baoyu-diagram 配色方案 ====================
var LEVEL_COLORS = [
  { fill: 'rgba(204, 255, 0, 0.3)', stroke: '#A9F0FF' }, // 蓝色 — 根节点
  { fill: 'rgba(8, 51, 68, 0.4)', stroke: '#A9F0FF' }, // 青色 — 层级 1
  { fill: 'rgba(6, 78, 59, 0.4)', stroke: '#34d399' }, // 翠绿 — 层级 2
  { fill: 'rgba(76, 29, 149, 0.4)', stroke: '#A9F0FF' }, // 紫罗兰 — 层级 3
  { fill: 'rgba(120, 53, 15, 0.3)', stroke: '#fbbf24' }, // 琥珀色 — 层级 4
  { fill: 'rgba(136, 19, 55, 0.4)', stroke: '#fb7185' }, // 玫瑰色 — 层级 5
  { fill: 'rgba(251, 146, 60, 0.3)', stroke: '#fb923c' }, // 橙色 — 层级 6
  { fill: 'rgba(30, 41, 59, 0.5)', stroke: '#94a3b8' } // 石板色 — 7+
]

function getColor (depth) {
  return LEVEL_COLORS[Math.min(depth, LEVEL_COLORS.length - 1)]
}

function getFontSize (depth) {
  var sizes = [17, 14, 12, 11, 10, 9, 9]
  return sizes[Math.min(depth, sizes.length - 1)]
}

function getFontWeight (depth) {
  return depth === 0 ? 700 : depth === 1 ? 600 : 500
}

// ==================== 文本宽度估算 ====================
function estimateTextWidth (text, fontSize) {
  var cjk = 0; var latin = 0
  for (var i = 0; i < text.length; i++) {
    var code = text.charCodeAt(i)
    if (code > 0x4e00) { cjk++ } else { latin++ }
  }
  var cjkW = fontSize * 1.0
  var latinW = fontSize * 0.6
  return Math.ceil(cjk * cjkW + latin * latinW + 20)
}

// ==================== 树解析 ====================
function parseToTree (content) {
  if (!content) return null
  content = content.trim()

  // 尝试 JSON 解析
  try {
    var obj = JSON.parse(content)
    return normalizeNode(obj)
  } catch (e) {}

  // 尝试提取 JSON（去除可能的 markdown 包裹）
  var jsonMatch = content.match(/\{[\s\S]*"title"[\s\S]*\}/)
  if (jsonMatch) {
    try {
      return normalizeNode(JSON.parse(jsonMatch[0]))
    } catch (e) {}
  }

  // ═══ 预处理：AI可能把整棵树输出在一行，先拆成多行 ═══
  // pattern: " -一、核心定义" or " -二、关键属性" etc
  if (content.indexOf('\n') < 0 || content.split('\n').filter(function(l){return l.trim()}).length < 5) {
    // 单行或少行模式：按 " -一、" " -二、" 等中文序号拆分
    content = content.replace(/ ([  ]*-[一二三四五六七八九十]、)/g, '\n$1')
    // 也按 " -核心定义" " -关键属性" 拆分
    content = content.replace(/ ([  ]*-核心)/g, '\n$1')
    content = content.replace(/ ([  ]*-关键)/g, '\n$1')
    content = content.replace(/ ([  ]*-分类)/g, '\n$1')
    content = content.replace(/ ([  ]*-公式)/g, '\n$1')
    content = content.replace(/ ([  ]*-应用)/g, '\n$1')
    content = content.replace(/ ([  ]*-常见)/g, '\n$1')
    content = content.replace(/ ([  ]*-知识)/g, '\n$1')
  }

  // 降级：从 Markdown 标题 或 缩进列表 构建树
  var lines = content.split('\n')
  var root = { title: '思维导图', children: [] }
  var stack = [{ node: root, depth: 0 }]

  // 先尝试 # 标题格式
  var headingLines = 0
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i].trim()
    if (line.match(/^(#{1,6})\s+/)) headingLines++
  }

  if (headingLines > 0) {
    // 标题模式
    for (var i = 0; i < lines.length; i++) {
      var line = lines[i].trim()
      var m = line.match(/^(#{1,6})\s+(.+)/)
      if (!m) continue
      var depth = m[1].length
      var title = m[2].replace(/\*\*/g, '').trim()
      var newNode = { title: title, children: [] }
      while (stack.length > 0 && stack[stack.length - 1].depth >= depth) {
        stack.pop()
      }
      if (stack.length > 0) {
        stack[stack.length - 1].node.children.push(newNode)
      }
      stack.push({ node: newNode, depth: depth })
    }
  } else {
    // 缩进列表模式（- 中心主题 /  - 子节点 /    - 孙节点）
    for (var i = 0; i < lines.length; i++) {
      var line = lines[i]
      var trimmed = line.trim()
      if (!trimmed || !trimmed.startsWith('-')) continue
      // 计算缩进层级：数前导空格数
      var indent = line.search(/\S/)
      if (indent < 0) indent = 0
      var depth = Math.floor(indent / 2) + 1  // 每2空格一级
      if (depth > 6) depth = 6
      // 提取标题（去掉 - ** ** 等markdown标记）
      var title = trimmed.replace(/^-\s*/, '').replace(/\*\*/g, '').replace(/^[#]+\s*/, '').trim()
      if (!title) continue
      // 第一层（depth=1）作为根节点的直接子节点
      var newNode = { title: title, children: [] }
      while (stack.length > 0 && stack[stack.length - 1].depth >= depth) {
        stack.pop()
      }
      if (stack.length > 0) {
        stack[stack.length - 1].node.children.push(newNode)
      }
      stack.push({ node: newNode, depth: depth })
    }
  }
  return root.children.length > 0 ? root : null
}

function normalizeNode (node) {
  if (!node) return null
  var title = (node.title || node.name || node.text || node.label || '节点').replace(/[*#]/g, '').trim()
  var children = (node.children || node.childNodes || node.nodes || node.items || [])
  return {
    id: node.id || ('n_' + Math.random().toString(36).slice(2, 8)),
    title: title,
    children: children.map(function (c) { return normalizeNode(c) }).filter(Boolean),
    collapsed: false
  }
}

// ==================== SVG 工具函数 ====================
var SVG_NS = 'http://www.w3.org/2000/svg'

function createSvgEl (tag, attrs) {
  var el = document.createElementNS(SVG_NS, tag)
  for (var key in attrs) {
    if (attrs.hasOwnProperty(key)) {
      el.setAttribute(key, attrs[key])
    }
  }
  return el
}

// ==================== 布局计算 ====================
var NODE_H = 48
var NODE_MIN_W = 80
var NODE_RX = 8
var LEVEL_X_GAP = 220
var SUBTREE_Y_GAP = 24

function layoutTree (node, depth, x) {
  if (!node) return { width: 0, height: 0 }

  var textW = estimateTextWidth(node.title, getFontSize(depth))
  var w = Math.max(NODE_MIN_W, textW)
  var h = NODE_H

  var children = (node.children || []).filter(function (c) { return !c._hidden })
  if (children.length === 0) {
    node._layout = { x: x, y: 0, w: w, h: h, cx: x + w / 2, cy: 0, depth: depth, leaf: true }
    return { width: w + 20, height: h }
  }

  // 递归布局子节点
  var childX = x + w + LEVEL_X_GAP
  var totalH = 0
  var childLayouts = []

  for (var i = 0; i < children.length; i++) {
    var cr = layoutTree(children[i], depth + 1, childX)
    childLayouts.push({ node: children[i], result: cr })
    if (i > 0) totalH += SUBTREE_Y_GAP
    totalH += cr.height
  }

  // 当前节点的垂直中心 = 所有子节点的垂直范围中心
  var firstChild = childLayouts[0]
  var lastChild = childLayouts[childLayouts.length - 1]
  var childTopY = firstChild.result.height / 2
  var childBottomY = totalH - lastChild.result.height / 2
  var cy = (childTopY + childBottomY) / 2

  // 调整子节点位置（垂直偏移）
  var curY = cy - (totalH / 2)
  for (var j = 0; j < childLayouts.length; j++) {
    var cl = childLayouts[j]
    var childCy = curY + cl.result.height / 2
    curY += cl.result.height + SUBTREE_Y_GAP
    cl.node._layout = {
      x: childX,
      y: childCy - cl.result.height / 2,
      w: cl.result.width - 20,
      h: NODE_H,
      cx: childX + (cl.result.width - 20) / 2,
      cy: childCy,
      depth: depth + 1,
      leaf: false
    }
  }

  var fullH = Math.max(h + 40, totalH)
  var fullW = childX + Math.max.apply(null, childLayouts.map(function (c) { return c.result.width }))

  node._layout = { x: x, y: cy - h / 2, w: w, h: h, cx: x + w / 2, cy: cy, depth: depth, leaf: false }

  return { width: fullW, height: fullH }
}

export default {
  name: 'MindmapAgentPage',
  data: function () {
    return {
      topic: '',
      generating: false,
      streamText: '',
      errorMessage: '',
      treeData: null,
      currentTopic: '',
      selectedHistoryId: null,
      history: [],
      zoomLevel: 1,
      panX: 0,
      panY: 0,
      svgViewBox: '0 0 1200 700',
      svgWidth: '100%',
      svgHeight: '700',
      levelColors: LEVEL_COLORS,
      nodeStats: null,
      demoTopics: ['勾股定理', '牛顿力学', '化学元素周期表', '二次函数', '光合作用', '中国近代史', '细胞结构', '电磁感应'],
      abortController: null,
      contextMenu: { visible: false, x: 0, y: 0, nodeId: null },
      editing: false
    }
  },
  computed: {
    svgTransformStyle: function () {
      return {
        transform: 'translate(' + this.panX + 'px, ' + this.panY + 'px) scale(' + this.zoomLevel + ')',
        transformOrigin: 'center center'
      }
    },
    canDeleteContextNode: function () {
      if (!this.contextMenu.nodeId || !this.treeData) return false
      return this.contextMenu.nodeId !== this.treeData.id
    }
  },
  mounted: function () {
    this.loadHistory()
    this._setupInteraction()
  },
  beforeDestroy: function () {
    if (this.abortController) {
      this.abortController.abort()
    }
    this._teardownInteraction()
  },
  methods: {
    // ── 节点统计 ──
    computeNodeStats: function (node) {
      if (!node) return { total: 0, leaves: 0 }
      var total = 0
      var leaves = 0
      function walk(n) {
        total++
        if (!n.children || n.children.length === 0 || (n.children.length === 1 && (!n.children[0].children || n.children[0].children.length === 0))) {
          leaves++
        }
        if (n.children) {
          for (var i = 0; i < n.children.length; i++) {
            walk(n.children[i])
          }
        }
      }
      walk(node)
      // 减掉根节点（中心主题）
      this.nodeStats = { total: total, leaves: leaves }
    },

    // ── 历史列表 ──
    loadHistory: async function () {
      try {
        var res = await resourceGenerationApi.list({ userId: this.$store.getters.userId || 0 })
        if (res && res.code === 1 && Array.isArray(res.response)) {
          this.history = res.response.filter(function (r) { return r.resourceType === 'mind_map' })
        }
      } catch (e) {
        console.error('加载历史失败:', e)
      }
    },

    loadHistoryItem: async function (item) {
      this.selectedHistoryId = item.id
      this.topic = item.title || ''
      try {
        var res = await resourceGenerationApi.getDetail(item.id)
        if (res && res.code === 1 && res.response) {
          var tree = parseToTree(res.response.content)
          if (tree) {
            this.treeData = tree
            this.currentTopic = res.response.title || item.title || '思维导图'
            this.errorMessage = ''
            this.$nextTick(function () { this.renderSvg() })
          } else {
            this.errorMessage = '无法解析该资源的内容'
          }
        }
      } catch (e) {
        console.error('加载资源失败:', e)
        this.errorMessage = '加载失败: ' + (e.message || '网络错误')
      }
    },

    deleteItem: async function (id) {
      try {
        await this.$confirm('确定删除该思维导图？', '确认', { type: 'warning' })
        await resourceGenerationApi.delete(id)
        this.$message.success('已删除')
        if (this.selectedHistoryId === id) {
          this.treeData = null
          this.currentTopic = ''
          this.selectedHistoryId = null
        }
        this.loadHistory()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('删除失败')
        }
      }
    },

    // ── 生成 ──
    generate: async function () {
      if (!this.topic.trim()) {
        this.$message.warning('请输入学习主题')
        return
      }
      if (this.generating) return

      this.generating = true
      this.errorMessage = ''
      this.streamText = ''
      this.treeData = null
      this.currentTopic = this.topic.trim()

      var self = this
      this.abortController = new AbortController()

      try {
        var fullContent = await resourceGenerationApi.generate(
          { userId: self.$store.getters.userId || 0, resourceType: 'mind_map', topic: self.currentTopic },
          function (chunk, full) {
            self.streamText = full
          }
        )

        if (fullContent) {
          var tree = parseToTree(fullContent)
          if (tree) {
            self.treeData = tree
            self.errorMessage = ''
            self.zoomLevel = 1
            self.panX = 0
            self.panY = 0
            self.$nextTick(function () {
              self.renderSvg()
              self.computeNodeStats(tree)
            })
            self.$message.success('思维导图生成完成')
          } else {
            self.errorMessage = 'AI 返回的数据格式异常，请重试'
          }
        } else {
          self.errorMessage = '未收到 AI 响应内容'
        }
        self.loadHistory()
      } catch (e) {
        if (e.name === 'AbortError') {
          self.errorMessage = ''
        } else {
          console.error('生成失败:', e)
          self.errorMessage = e.message || '网络错误，请检查 API 配置并重试'
        }
      } finally {
        self.generating = false
        self.streamText = ''
        self.abortController = null
      }
    },

    cancelGenerate: function () {
      if (this.abortController) {
        this.abortController.abort()
      }
      this.generating = false
      this.streamText = ''
    },

    retryGenerate: function () {
      this.errorMessage = ''
      this.generate()
    },

    // ── SVG 渲染（baoyu-diagram 设计系统）──
    renderSvg: function () {
      var svgEl = this.$refs.svgEl
      if (!svgEl || !this.treeData) return

      // 清除旧内容
      while (svgEl.firstChild) {
        svgEl.removeChild(svgEl.firstChild)
      }

      // 计算布局（先深拷贝树避免污染原始数据）
      var treeCopy = JSON.parse(JSON.stringify(this.treeData))
      this._resetHidden(treeCopy)
      var layoutSize = layoutTree(treeCopy, 0, 60)

      // 计算 viewBox
      var padding = 60
      var vbW = Math.max(1200, layoutSize.width + padding * 2)
      var vbH = Math.max(700, layoutSize.height + padding * 2)
      this.svgViewBox = '0 0 ' + vbW + ' ' + vbH
      this.svgWidth = vbW
      this.svgHeight = vbH

      this._layoutedTree = treeCopy

      // ── <defs> ──
      var defs = createSvgEl('defs', {})

      // 网格图案
      var grid = createSvgEl('pattern', { id: 'mm-grid', width: '40', height: '40', patternUnits: 'userSpaceOnUse' })
      grid.appendChild(createSvgEl('path', { d: 'M 40 0 L 0 0 0 40', fill: 'none', stroke: '#141A14', 'stroke-width': '0.5' }))
      defs.appendChild(grid)

      // 投影滤镜
      var filter = createSvgEl('filter', { id: 'mm-shadow', x: '-10%', y: '-10%', width: '130%', height: '130%' })
      filter.appendChild(createSvgEl('feDropShadow', { dx: '2', dy: '3', stdDeviation: '4', 'flood-color': '#000000', 'flood-opacity': '0.3' }))
      defs.appendChild(filter)

      // 根节点发光
      var glowFilter = createSvgEl('filter', { id: 'mm-glow', x: '-20%', y: '-20%', width: '140%', height: '140%' })
      glowFilter.appendChild(createSvgEl('feGaussianBlur', { stdDeviation: '4', result: 'blur' }))
      var glowMerge = createSvgEl('feMerge', {})
      glowMerge.appendChild(createSvgEl('feMergeNode', { in: 'blur' }))
      glowMerge.appendChild(createSvgEl('feMergeNode', { in: 'SourceGraphic' }))
      glowFilter.appendChild(glowMerge)
      defs.appendChild(glowFilter)

      svgEl.appendChild(defs)

      // ── 背景 ──
      svgEl.appendChild(createSvgEl('rect', { x: '0', y: '0', width: vbW, height: vbH, fill: '#0A0D0A' }))
      svgEl.appendChild(createSvgEl('rect', { x: '0', y: '0', width: vbW, height: vbH, fill: 'url(#mm-grid)' }))

      // ── 收集所有节点与连线 ──
      var nodes = []
      var edges = []
      this._collectNodes(treeCopy, null, nodes, edges)

      // ── 连线层 ──
      var edgesG = createSvgEl('g', { id: 'mm-edges' })
      for (var ei = 0; ei < edges.length; ei++) {
        var edge = edges[ei]
        var color = getColor(edge.depth)
        var cpOffset = Math.min(80, Math.abs(edge.to._layout.cx - edge.from._layout.cx) * 0.4)
        var d = 'M ' + (edge.from._layout.x + edge.from._layout.w) + ' ' + edge.from._layout.cy +
          ' C ' + (edge.from._layout.x + edge.from._layout.w + cpOffset) + ' ' + edge.from._layout.cy +
          ', ' + (edge.to._layout.x - cpOffset) + ' ' + edge.to._layout.cy +
          ', ' + edge.to._layout.x + ' ' + edge.to._layout.cy
        var path = createSvgEl('path', {
          d: d,
          fill: 'none',
          stroke: color.stroke,
          'stroke-width': edge.depth === 0 ? '2.5' : '1.5',
          opacity: '0.55',
          class: 'mm-edge'
        })
        edgesG.appendChild(path)
      }
      svgEl.appendChild(edgesG)

      // ── 节点层 ──
      var nodesG = createSvgEl('g', { id: 'mm-nodes' })
      for (var ni = 0; ni < nodes.length; ni++) {
        var node = nodes[ni]
        var layout = node._layout
        var nc = getColor(layout.depth)
        var fontSize = getFontSize(layout.depth)
        var fontWeight = getFontWeight(layout.depth)

        // 节点组
        var ng = createSvgEl('g', {
          class: 'mm-node' + (layout.depth === 0 ? ' mm-node-root' : ''),
          'data-node-id': node.id,
          'data-depth': layout.depth,
          cursor: (node.children && node.children.length > 0) ? 'pointer' : 'default'
        })

        // 遮罩层
        ng.appendChild(createSvgEl('rect', {
          x: layout.x,
          y: layout.y,
          width: layout.w,
          height: layout.h,
          rx: NODE_RX,
          fill: '#0A0D0A'
        }))

        // 视觉层
        var visualRect = createSvgEl('rect', {
          x: layout.x,
          y: layout.y,
          width: layout.w,
          height: layout.h,
          rx: NODE_RX,
          fill: nc.fill,
          stroke: nc.stroke,
          'stroke-width': layout.depth === 0 ? '2.5' : '1.5',
          class: 'mm-node-rect'
        })
        if (layout.depth === 0) {
          visualRect.setAttribute('filter', 'url(#mm-glow)')
        }
        ng.appendChild(visualRect)

        // 文本
        ng.appendChild(createSvgEl('text', {
          x: layout.cx,
          y: layout.cy + fontSize * 0.35,
          fill: '#ffffff',
          'font-size': fontSize,
          'font-weight': fontWeight,
          'font-family': "'JetBrains Mono', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif",
          'text-anchor': 'middle',
          class: 'mm-node-text'
        }))
        var textEl = ng.lastChild
        textEl.textContent = node.title

        // 折叠指示器（有子节点时显示）
        if (node.children && node.children.length > 0) {
          var indicatorCx = layout.x + layout.w - 8
          var indicatorCy = layout.cy
          ng.appendChild(createSvgEl('circle', {
            cx: indicatorCx,
            cy: indicatorCy,
            r: '5',
            fill: node.collapsed ? nc.stroke : 'transparent',
            stroke: nc.stroke,
            'stroke-width': '1.5',
            class: 'mm-collapse-indicator'
          }))
          // 加号/减号
          var sign = createSvgEl('text', {
            x: indicatorCx,
            y: indicatorCy + 3,
            fill: node.collapsed ? '#0A0D0A' : nc.stroke,
            'font-size': '8',
            'font-weight': '700',
            'font-family': "'JetBrains Mono', monospace",
            'text-anchor': 'middle',
            class: 'mm-collapse-sign'
          })
          sign.textContent = node.collapsed ? '+' : '−'
          ng.appendChild(sign)
        }

        nodesG.appendChild(ng)
      }
      svgEl.appendChild(nodesG)

      // ── 图例 ──
      var legendX = vbW - 200
      var legendY = vbH - 90
      var legendG = createSvgEl('g', { id: 'mm-legend', transform: 'translate(' + legendX + ', ' + legendY + ')' })
      legendG.appendChild(createSvgEl('rect', {
        x: 0,
        y: 0,
        width: 170,
        height: 72,
        rx: 6,
        fill: 'rgba(10, 13, 10, 0.85)',
        stroke: '#141A14',
        'stroke-width': '1'
      }))
      var colorsToShow = Math.min(5, LEVEL_COLORS.length)
      for (var ci = 0; ci < colorsToShow; ci++) {
        var lc = LEVEL_COLORS[ci]
        var ly = 16 + ci * 12
        legendG.appendChild(createSvgEl('rect', {
          x: 10,
          y: ly - 4,
          width: 12,
          height: 8,
          rx: 3,
          fill: lc.fill,
          stroke: lc.stroke,
          'stroke-width': '1'
        }))
        var lt = createSvgEl('text', {
          x: 28,
          y: ly + 2,
          fill: '#94a3b8',
          'font-size': '8',
          'font-family': "'JetBrains Mono', monospace"
        })
        lt.textContent = 'Level ' + (ci + 1)
        legendG.appendChild(lt)
      }
      svgEl.appendChild(legendG)

      // 标题
      var titleG = createSvgEl('g', { transform: 'translate(20, 30)' })
      var titleText = createSvgEl('text', {
        x: 0,
        y: 0,
        fill: '#f8fafc',
        'font-size': '18',
        'font-weight': '700',
        'font-family': "'JetBrains Mono', 'Noto Sans SC', 'PingFang SC', sans-serif"
      })
      titleText.textContent = this.currentTopic || '思维导图'
      titleG.appendChild(titleText)
      var subtitle = createSvgEl('text', {
        x: 0,
        y: 16,
        fill: '#64748b',
        'font-size': '9',
        'font-family': "'JetBrains Mono', monospace"
      })
      subtitle.textContent = 'baoyu-diagram · AI Knowledge Graph'
      titleG.appendChild(subtitle)
      svgEl.appendChild(titleG)

      // 绑定节点点击事件
      this._bindNodeEvents()
    },

    _resetHidden: function (node) {
      if (node.collapsed && node.children) {
        for (var i = 0; i < node.children.length; i++) {
          this._markHidden(node.children[i])
        }
      } else if (node.children) {
        for (var j = 0; j < node.children.length; j++) {
          this._resetHidden(node.children[j])
        }
      }
    },

    _markHidden: function (node) {
      node._hidden = true
      if (node.children) {
        for (var i = 0; i < node.children.length; i++) {
          this._markHidden(node.children[i])
        }
      }
    },

    _collectNodes: function (node, parent, nodes, edges) {
      if (!node || node._hidden) return
      if (node._layout) {
        nodes.push(node)
      }
      if (parent && parent._layout && node._layout) {
        edges.push({ from: parent, to: node, depth: node._layout.depth - 1 })
      }
      if (!node.collapsed && node.children) {
        for (var i = 0; i < node.children.length; i++) {
          this._collectNodes(node.children[i], node, nodes, edges)
        }
      }
    },

    // ── 交互 ──
    _setupInteraction: function () {
      this._onWheel = this._handleWheel.bind(this)
      this._onMouseDown = this._handleMouseDown.bind(this)
      this._onMouseMove = this._handleMouseMove.bind(this)
      this._onMouseUp = this._handleMouseUp.bind(this)
      this._onDblClick = this._handleDblClick.bind(this)
      this._onContextMenu = this._handleContextMenu.bind(this)
      this._onDocClick = this.closeContextMenu.bind(this)

      var el = this.$refs.renderArea
      if (el) {
        el.addEventListener('wheel', this._onWheel, { passive: false })
        el.addEventListener('mousedown', this._onMouseDown)
        el.addEventListener('dblclick', this._onDblClick)
        el.addEventListener('contextmenu', this._onContextMenu)
      }
      document.addEventListener('click', this._onDocClick)
    },

    _teardownInteraction: function () {
      var el = this.$refs.renderArea
      if (el) {
        el.removeEventListener('wheel', this._onWheel)
        el.removeEventListener('mousedown', this._onMouseDown)
        el.removeEventListener('dblclick', this._onDblClick)
        el.removeEventListener('contextmenu', this._onContextMenu)
      }
      document.removeEventListener('mousemove', this._onMouseMove)
      document.removeEventListener('mouseup', this._onMouseUp)
      document.removeEventListener('click', this._onDocClick)
    },

    _handleWheel: function (e) {
      e.preventDefault()
      var delta = e.deltaY > 0 ? -0.08 : 0.08
      this.zoomLevel = Math.max(0.3, Math.min(3, this.zoomLevel + delta))
    },

    // 统一 mousedown：记录起点，统一进入拖拽判定，mouseup 时区分点击与拖拽
    _handleMouseDown: function (e) {
      if (this.editing) return // 编辑中不触发交互
      this._downX = e.clientX
      this._downY = e.clientY
      this._downTarget = e.target
      this._isPanning = false
      this._panStartX = this._downX - this.panX
      this._panStartY = this._downY - this.panY
      document.addEventListener('mousemove', this._onMouseMove)
      document.addEventListener('mouseup', this._onMouseUp)
    },

    _handleMouseMove: function (e) {
      if (this._isPanning) {
        this.panX = e.clientX - this._panStartX
        this.panY = e.clientY - this._panStartY
        return
      }
      // 超过阈值才开始平移（避免点击误判为拖拽）
      if (Math.abs(e.clientX - this._downX) > 5 || Math.abs(e.clientY - this._downY) > 5) {
        this._isPanning = true
      }
    },

    _handleMouseUp: function (e) {
      document.removeEventListener('mousemove', this._onMouseMove)
      document.removeEventListener('mouseup', this._onMouseUp)
      if (this._isPanning) { this._isPanning = false; return }
      // 视为点击：仅点击折叠指示器（+/−）才切换折叠，避免与双击编辑冲突
      var t = this._downTarget
      if (t && t.closest && (t.closest('.mm-collapse-indicator') || t.closest('.mm-collapse-sign'))) {
        var nodeEl = t.closest('.mm-node')
        if (nodeEl) {
          var nodeId = nodeEl.getAttribute('data-node-id')
          if (nodeId) this.toggleNode(nodeId)
        }
      }
      this._isPanning = false
    },

    _handleDblClick: function (e) {
      var t = e.target
      if (!t || !t.closest) return
      var nodeEl = t.closest('.mm-node')
      if (!nodeEl) return
      var nodeId = nodeEl.getAttribute('data-node-id')
      if (nodeId) this.startEditNode(nodeId)
    },

    _handleContextMenu: function (e) {
      var t = e.target
      if (!t || !t.closest || !t.closest('.mm-node')) return
      e.preventDefault()
      var nodeEl = t.closest('.mm-node')
      var nodeId = nodeEl.getAttribute('data-node-id')
      if (!nodeId) return
      this.contextMenu.visible = true
      this.contextMenu.x = e.clientX
      this.contextMenu.y = e.clientY
      this.contextMenu.nodeId = nodeId
    },

    closeContextMenu: function () {
      this.contextMenu.visible = false
    },

    _bindNodeEvents: function () {
      // 节点事件通过事件委托在 mousedown/dblclick/contextmenu 中处理
    },

    _handleNodeClick: function (nodeId) {
      this.toggleNode(nodeId)
    },

    toggleNode: function (nodeId) {
      var self = this
      function findAndToggle (node) {
        if (!node) return false
        if (node.id === nodeId) {
          node.collapsed = !node.collapsed
          return true
        }
        if (node.children) {
          for (var i = 0; i < node.children.length; i++) {
            if (findAndToggle(node.children[i])) return true
          }
        }
        return false
      }

      if (this._layoutedTree) {
        findAndToggle(this._layoutedTree)
        // 同步到 treeData（源数据）
        if (this.treeData) { findAndToggle(this.treeData) }
        this.$nextTick(function () { self.renderSvg() })
      }
    },

    // ── 节点查找工具 ──
    _findNode: function (nodeId, tree) {
      if (!tree) return null
      if (tree.id === nodeId) return tree
      if (tree.children) {
        for (var i = 0; i < tree.children.length; i++) {
          var found = this._findNode(nodeId, tree.children[i])
          if (found) return found
        }
      }
      return null
    },
    _findParent: function (nodeId, tree) {
      if (!tree || !tree.children) return null
      for (var i = 0; i < tree.children.length; i++) {
        if (tree.children[i].id === nodeId) return tree
        var found = this._findParent(nodeId, tree.children[i])
        if (found) return found
      }
      return null
    },
    _genId: function () {
      return 'n_' + Math.random().toString(36).slice(2, 8)
    },

    // ── 行内编辑（foreignObject + input）──
    startEditNode: function (nodeId) {
      var self = this
      if (!this._layoutedTree || !this.$refs.svgEl) return
      var layoutedNode = this._findNode(nodeId, this._layoutedTree)
      if (!layoutedNode || !layoutedNode._layout) return
      var L = layoutedNode._layout
      this.editing = true
      this.closeContextMenu()

      var svgEl = this.$refs.svgEl
      var SVG_NS = 'http://www.w3.org/2000/svg'
      var fo = document.createElementNS(SVG_NS, 'foreignObject')
      fo.setAttribute('x', L.x)
      fo.setAttribute('y', L.y)
      fo.setAttribute('width', Math.max(80, L.w))
      fo.setAttribute('height', L.h)
      fo.setAttribute('class', 'mm-edit-fo')
      var input = document.createElement('input')
      input.type = 'text'
      input.value = layoutedNode.title
      input.className = 'mm-edit-input'
      fo.appendChild(input)
      svgEl.appendChild(fo)
      this._editFo = fo
      this._editInput = input
      this._editNodeId = nodeId

      setTimeout(function () { input.focus(); input.select() }, 0)

      var commit = function () {
        var val = input.value.trim()
        if (val && self.treeData) {
          var target = self._findNode(nodeId, self.treeData)
          if (target) target.title = val
        }
        self._removeEditFo()
        self.editing = false
        self.$nextTick(function () { self.renderSvg() })
      }
      var cancel = function () {
        self._removeEditFo()
        self.editing = false
      }
      input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); commit() }
        else if (e.key === 'Escape') { e.preventDefault(); cancel() }
        e.stopPropagation()
      })
      input.addEventListener('blur', commit)
    },

    _removeEditFo: function () {
      if (this._editFo && this._editFo.parentNode) {
        this._editFo.parentNode.removeChild(this._editFo)
      }
      this._editFo = null
      this._editInput = null
    },

    // ── 右键菜单操作 ──
    menuAddChild: function () {
      var nodeId = this.contextMenu.nodeId
      this.closeContextMenu()
      if (!this.treeData) return
      var target = this._findNode(nodeId, this.treeData)
      if (!target) return
      if (!target.children) target.children = []
      target.collapsed = false
      target.children.push({ id: this._genId(), title: '新节点', children: [], collapsed: false })
      var self = this
      this.computeNodeStats(this.treeData)
      this.$nextTick(function () { self.renderSvg() })
      this.$message.success('已添加子节点，双击可编辑')
    },

    menuAddSibling: function () {
      var nodeId = this.contextMenu.nodeId
      this.closeContextMenu()
      if (!this.treeData) return
      if (nodeId === this.treeData.id) {
        this.$message.warning('根节点无法添加同级')
        return
      }
      var parent = this._findParent(nodeId, this.treeData)
      if (!parent) return
      var idx = -1
      for (var i = 0; i < parent.children.length; i++) {
        if (parent.children[i].id === nodeId) { idx = i; break }
      }
      parent.children.splice(idx + 1, 0, { id: this._genId(), title: '新节点', children: [], collapsed: false })
      var self = this
      this.computeNodeStats(this.treeData)
      this.$nextTick(function () { self.renderSvg() })
      this.$message.success('已添加同级节点，双击可编辑')
    },

    menuDelete: function () {
      var nodeId = this.contextMenu.nodeId
      this.closeContextMenu()
      if (!this.treeData || nodeId === this.treeData.id) return
      var parent = this._findParent(nodeId, this.treeData)
      if (!parent) return
      for (var i = 0; i < parent.children.length; i++) {
        if (parent.children[i].id === nodeId) { parent.children.splice(i, 1); break }
      }
      var self = this
      this.computeNodeStats(this.treeData)
      this.$nextTick(function () { self.renderSvg() })
      this.$message.success('已删除节点')
    },

    zoomIn: function () {
      this.zoomLevel = Math.min(3, +(this.zoomLevel + 0.15).toFixed(2))
    },

    zoomOut: function () {
      this.zoomLevel = Math.max(0.3, +(this.zoomLevel - 0.15).toFixed(2))
    },

    fitView: function () {
      this.zoomLevel = 1
      this.panX = 0
      this.panY = 0
    },

    // ── 导出 ──
    downloadSVG: function () {
      var svgEl = this.$refs.svgEl
      if (!svgEl) return

      var clone = svgEl.cloneNode(true)
      // 添加独立样式
      var style = createSvgEl('style', {})
      style.textContent =
        '@import url("https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap");' +
        '.mm-node-text { font-family: "JetBrains Mono", "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif; }' +
        '.mm-edge { transition: opacity 0.3s; }'

      clone.insertBefore(style, clone.firstChild)

      var serializer = new XMLSerializer()
      var svgString = serializer.serializeToString(clone)
      var blob = new Blob([svgString], { type: 'image/svg+xml' })
      var url = URL.createObjectURL(blob)

      var a = document.createElement('a')
      a.href = url
      a.download = (this.currentTopic || 'mindmap') + '.svg'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      this.$message.success('SVG 已导出')
    },

    // ── 工具 ──
    formatTime: function (ts) {
      if (!ts) return ''
      var d = new Date(ts)
      var pad = function (n) { return ('0' + n).slice(-2) }
      return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
    }
  }
}
</script>

<style scoped>
.mindmap-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
}

/* ── 头部 ── */
.mm-header {
  margin-bottom: 20px;
  text-align: center;
}
.mm-title {
  font-size: 26px;
  color: var(--grey);
  margin: 0 0 6px 0;
  font-weight: 600;
  letter-spacing: 1px;
}
.mm-title i {
  color: #c8a45c;
  margin-right: 8px;
}
.mm-subtitle {
  color: #8c8470;
  font-size: 13px;
  margin: 0;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
}

/* ── 输入行 ── */
.mm-input-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
.mm-input-wrap { flex: 1; }
.mm-gen-btn {
  background: linear-gradient(135deg, var(--ink-2), #141A14) !important;
  border-color: var(--ink-2) !important;
  color: var(--ice) !important;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.mm-gen-btn:hover {
  background: linear-gradient(135deg, #141A14, #334155) !important;
}
.mm-cancel-btn {
  color: #94a3b8 !important;
}

/* ── 主体 ── */
.mm-body {
  background: #fff;
  border: 1px solid #e8e4d9;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(10, 13, 10, 0.06);
}

/* ── 加载骨架屏 ── */
.mm-loading {
  padding: 60px 40px;
  background: var(--ink-2);
  min-height: 500px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.mm-skeleton {
  position: relative;
  width: 500px;
  height: 300px;
}
.sk-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.15);
  animation: mm-pulse 1.5s ease-in-out infinite;
}
.sk-root { width: 36px; height: 36px; left: 20px; top: 130px; }
.sk-c1 { width: 24px; height: 24px; left: 180px; top: 60px; }
.sk-c2 { width: 24px; height: 24px; left: 180px; top: 130px; }
.sk-c3 { width: 24px; height: 24px; left: 180px; top: 200px; }
.sk-c4 { width: 18px; height: 18px; left: 340px; top: 100px; }
.sk-line {
  position: absolute;
  height: 2px;
  background: rgba(148, 163, 184, 0.12);
  transform-origin: left center;
}
.sk-l1 { width: 130px; left: 56px; top: 148px; }
.sk-l2 { width: 130px; left: 56px; top: 148px; transform: rotate(-40deg); }
.sk-l3 { width: 130px; left: 56px; top: 148px; }
.sk-l4 { width: 130px; left: 56px; top: 148px; transform: rotate(40deg); }
@keyframes mm-pulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.7; }
}
.mm-stream-hint {
  color: #64748b;
  font-size: 13px;
  margin-top: 24px;
  font-family: 'JetBrains Mono', monospace;
}

/* ── 错误状态 ── */
.mm-error {
  padding: 80px 40px;
  text-align: center;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.mm-error-icon {
  font-size: 48px;
  color: #fb7185;
  margin-bottom: 16px;
}
.mm-error h3 {
  font-size: 18px;
  color: var(--grey);
  margin: 0 0 8px 0;
}
.mm-error p {
  color: #8c8470;
  font-size: 14px;
  margin: 0 0 20px 0;
  max-width: 400px;
}

/* ── 空状态 ── */
.mm-empty {
  padding: 80px 40px;
  text-align: center;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.mm-empty-icon {
  margin-bottom: 16px;
  opacity: 0.6;
}
.mm-empty h3 {
  font-size: 18px;
  color: var(--grey);
  margin: 0 0 8px 0;
}
.mm-empty p {
  color: #8c8470;
  font-size: 14px;
  margin: 0 0 16px 0;
}
.mm-empty-tips span {
  color: #8c8470;
  font-size: 13px;
}
.mm-tip-tag {
  margin: 4px;
  cursor: pointer;
  background: var(--ink-3);
  border-color: var(--ink-5);
  color: var(--grey);
  transition: all 0.2s;
}
.mm-tip-tag:hover {
  border-color: #c8a45c;
  color: #c8a45c;
}

/* ── SVG 包装器 ── */
.mm-svg-wrapper {
  background: var(--ink-2);
}

/* ── 工具栏 ── */
.mm-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: rgba(30, 41, 59, 0.95);
  border-bottom: 1px solid #141A14;
}
.mm-toolbar-left {
  display: flex;
  align-items: center;
}
.mm-current-topic {
  color: #f8fafc;
  font-size: 14px;
  font-weight: 600;
  font-family: 'JetBrains Mono', 'Noto Sans SC', 'PingFang SC', sans-serif;
}
.mm-stats {
  color: #94a3b8;
  font-size: 12px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: 'JetBrains Mono', monospace;
}
.mm-toolbar-right {
  display: flex;
  align-items: center;
}
.mm-toolbar .el-button-group .el-button {
  background: rgba(51, 65, 85, 0.6);
  border-color: #334155;
  color: #94a3b8;
}
.mm-toolbar .el-button-group .el-button:hover {
  color: var(--ice);
  border-color: var(--ink-5);
}
.mm-toolbar .el-button {
  background: rgba(51, 65, 85, 0.6);
  border-color: #334155;
  color: #94a3b8;
}
.mm-toolbar .el-button:hover {
  color: var(--ice);
  border-color: var(--ink-5);
}

/* ── 渲染区域 ── */
.mm-render-area {
  overflow: hidden;
  flex: 1;
  min-height: 0;
  cursor: grab;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mm-render-area:active {
  cursor: grabbing;
}
.mm-svg-inner {
  transition: transform 0.15s ease-out;
}
.baoyu-mindmap-svg {
  display: block;
}

/* ── 互动式：悬停高亮 + 过渡 ── */
.mm-node {
  cursor: pointer;
  transition: opacity 0.2s ease;
}
.mm-node .mm-node-rect {
  transition: filter 0.18s ease, stroke-width 0.18s ease;
}
.mm-node:hover .mm-node-rect {
  filter: brightness(1.35) drop-shadow(0 0 6px rgba(var(--ice-rgb), 0.55));
}
.mm-node:hover .mm-collapse-indicator {
  filter: brightness(1.4);
}
.mm-edge {
  transition: opacity 0.3s ease;
}

/* ── 工具栏提示 ── */
.mm-toolbar-hint {
  color: #64748b;
  font-size: 11px;
  margin-left: 14px;
  font-family: 'JetBrains Mono', monospace;
  opacity: 0.8;
}

/* ── 右键上下文菜单 ── */
.mm-context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 160px;
  background: rgba(10, 13, 10, 0.96);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(var(--acid-rgb), 0.3);
  border-radius: 10px;
  padding: 6px 0;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.5);
}
.mm-ctx-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  color: #e2e8f0;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}
.mm-ctx-item:hover {
  background: rgba(var(--acid-rgb), 0.25);
  color: var(--ice);
}
.mm-ctx-item.mm-ctx-danger:hover {
  background: rgba(251, 113, 133, 0.2);
  color: #fb7185;
}

/* ── 行内编辑输入框 ── */
.mm-edit-input {
  width: 100%;
  height: 100%;
  border: 2px solid var(--ice);
  border-radius: 6px;
  background: var(--ink-2);
  color: #f8fafc;
  font-size: 14px;
  font-family: 'JetBrains Mono', 'Noto Sans SC', 'PingFang SC', sans-serif;
  text-align: center;
  outline: none;
  padding: 0 6px;
  box-sizing: border-box;
  box-shadow: 0 0 12px rgba(var(--ice-rgb), 0.5);
}

/* ── 图例 ── */
.mm-legend-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 10px 16px;
  background: rgba(30, 41, 59, 0.95);
  border-top: 1px solid #141A14;
}
.mm-legend-item {
  color: #94a3b8;
  font-size: 11px;
  font-family: 'JetBrains Mono', monospace;
  display: flex;
  align-items: center;
  gap: 6px;
}
.mm-legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

/* ── 历史面板 ── */
.mm-history {
  margin-top: 24px;
}
.mm-history-header {
  font-size: 15px;
  font-weight: 600;
  color: var(--grey);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.mm-history-header i {
  color: #c8a45c;
}
.mm-history-count {
  font-size: 11px;
  color: #8c8470;
  background: var(--ink-3);
  padding: 1px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.mm-history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--ink-3);
  border: 1px solid #e8e4d9;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.mm-history-item:hover,
.mm-history-item.active {
  border-color: #c8a45c;
  background: var(--ink-3);
  box-shadow: 0 2px 8px rgba(200, 164, 92, 0.1);
}
.mm-history-item-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--ink-3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c8a45c;
  font-size: 16px;
}
.mm-history-item-body {
  flex: 1;
  min-width: 0;
}
.mm-history-item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--grey);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mm-history-item-time {
  font-size: 12px;
  color: #b0a68e;
  margin-top: 2px;
}
.mm-history-item-del {
  color: #b0a68e !important;
  font-size: 16px;
}
.mm-history-item-del:hover {
  color: #fb7185 !important;
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .mm-input-row {
    flex-direction: column;
  }
  .mm-toolbar {
    flex-direction: column;
    gap: 8px;
  }
  .mm-toolbar-left,
  .mm-toolbar-right {
    width: 100%;
    justify-content: center;
  }
  .mm-toolbar-right {
    flex-wrap: wrap;
  }
}
</style>
