<template>
  <div class="detail-container">
    <div class="detail-header">
      <div class="header-badge"><span class="badge-dot" /><span>Resource Detail</span></div>
      <el-button class="back-btn" size="small" @click="goBack"><i class="el-icon-arrow-left" /> 返回列表</el-button>
    </div>
    <div v-if="loading" class="loading-state"><div class="loading-spinner" /><p>加载资源中...</p></div>
    <div v-else-if="!resource" class="empty-state"><div class="empty-icon">📭</div><p>资源不存在</p></div>
    <div v-else class="detail-body">
      <div class="info-bar">
        <div class="info-left">
          <span class="type-badge">{{ getTypeIcon(resource.resourceType) }}</span>
          <div class="info-text">
            <h2 class="resource-title">{{ resource.title || '未命名资源' }}</h2>
            <div class="resource-meta">
              <span class="meta-item"><i class="el-icon-folder" /> {{ getTypeName(resource.resourceType) }}</span>
              <span class="meta-item" v-if="resource.createTime"><i class="el-icon-time" /> {{ resource.createTime }}</span>
              <span class="meta-item" v-if="resource.sourceAgent"><i class="el-icon-cpu" /> {{ resource.sourceAgent }}</span>
            </div>
          </div>
        </div>
        <div class="info-right">
          <div class="quality-ring" v-if="resource.qualityScore">
            <svg viewBox="0 0 36 36" class="quality-svg"><path class="ring-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/><path class="ring-fill" :stroke-dasharray="qualityPercent + ', 100'" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/></svg>
            <span class="quality-text">{{ resource.qualityScore }}</span>
          </div>
          <div class="word-count-badge">{{ wordCount }} 字</div>
        </div>
      </div>
      <div class="content-panel">
        <div v-if="isMarkdownType" class="content-markdown" v-html="renderedContent" />
        <div v-else-if="resource.resourceType === 'mind_map'" class="content-mindmap-wrapper">
          <div class="mindmap-toolbar">
            <span class="toolbar-title">思维导图</span>
            <el-button size="mini" @click="zoomMindmap(0.2)">放大</el-button>
            <el-button size="mini" @click="zoomMindmap(-0.2)">缩小</el-button>
            <el-button size="mini" @click="resetMindmap">重置</el-button>
          </div>
          <div ref="mindmapContainer" class="mindmap-container" />
        </div>
        <div v-else-if="resource.resourceType === 'exercise'" class="content-exercise">
          <div v-for="(q, idx) in parsedExercises" :key="idx" class="exercise-card">
            <div class="exercise-header"><span class="exercise-num">{{ idx + 1 }}</span><span class="exercise-type-tag">{{ q.type || '题目' }}</span></div>
            <div class="exercise-question">{{ q.question || q.title || '' }}</div>
            <div v-if="q.options && q.options.length" class="exercise-options">
              <div v-for="(opt, oi) in q.options" :key="oi" class="option-item"><span class="option-letter">{{ String.fromCharCode(65 + oi) }}.</span>{{ opt }}</div>
            </div>
            <div v-if="q.answer" class="exercise-answer"><span class="answer-label">答案：</span>{{ q.answer }}</div>
            <div v-if="q.explanation" class="exercise-explanation"><span class="answer-label">解析：</span>{{ q.explanation }}</div>
          </div>
        </div>
        <div v-else-if="resource.resourceType === 'video'" class="content-video">
          <div v-for="(v, idx) in parsedVideos" :key="idx" class="video-card">
            <div class="video-icon">🎬</div><div class="video-info">
              <h4 class="video-title">{{ v.title || '视频资源' }}</h4><p class="video-desc">{{ v.description || '' }}</p>
              <!-- B 站视频：官方 iframe 播放器内嵌播放（兼容 BV 号 / av 号 / 分P） -->
              <div v-if="bilibiliEmbedUrl(v.url)" class="video-embed">
                <iframe
                  :src="bilibiliEmbedUrl(v.url)"
                  scrolling="no" frameborder="no" allowfullscreen="true"
                  referrerpolicy="no-referrer-when-downgrade" />
              </div>
              <!-- 网易公开课：走后端解析直链后原生播放 -->
              <video v-else-if="neteasePlayUrl(v.url)" :src="neteasePlayUrl(v.url)" controls preload="metadata" class="video-native" />
              <!-- 通用 MP4：HTML5 原生播放 -->
              <video v-else-if="isMp4Url(v.url)" :src="v.url" controls preload="metadata" class="video-native" />
              <!-- 其他来源：保留外链 -->
              <a v-if="v.url" :href="v.url" target="_blank" class="video-link"><i class="el-icon-link" /> 打开链接</a>
            </div>
          </div>
        </div>
        <div v-else class="content-raw"><pre>{{ resource.content }}</pre></div>
      </div>
    </div>
  </div>
</template>

<script>
import resourceGenerationApi from '@/api/resourceGeneration'

export default {
  name: 'ResourceDetail',
  data () {
    return {
      loading: true,
      resource: null,
      mindmapScale: 1,
      resourceTypes: [
        { key: 'document', label: '课程文档', icon: '📄' }, { key: 'mind_map', label: '思维导图', icon: '🗺️' },
        { key: 'exercise', label: '练习题目', icon: '✏️' }, { key: 'extended_reading', label: '拓展阅读', icon: '📚' },
        { key: 'video', label: '视频资源', icon: '🎬' }, { key: 'code_example', label: '代码案例', icon: '💻' }
      ]
    }
  },
  computed: {
    isMarkdownType () { return ['document', 'extended_reading', 'code_example'].includes(this.resource?.resourceType) },
    renderedContent () {
      if (!this.resource?.content) return ''
      var html = this.resource.content.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      html = html.replace(/^### (.*$)/gm, '<h3 class="md-h3">$1</h3>').replace(/^## (.*$)/gm, '<h2 class="md-h2">$1</h2>').replace(/^# (.*$)/gm, '<h1 class="md-h1">$1</h1>')
      html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
      html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre class="code-block"><code>$2</code></pre>')
      html = html.replace(/^\s*[-*] (.*$)/gm, '<li class="md-li">$1</li>').replace(/(<li[^>]*>.*<\/li>\n?)+/g, '<ul class="md-ul">$&</ul>')
      html = html.replace(/\n\n/g, '</p><p class="md-p">').replace(/\n/g, '<br>')
      return '<p class="md-p">' + html + '</p>'
    },
    wordCount () { return (this.resource?.content || '').replace(/\s/g, '').length },
    qualityPercent () { return Math.min((this.resource?.qualityScore || 0) * 10, 100) },
    parsedExercises () { try { var p = JSON.parse(this.resource?.content || '[]'); return Array.isArray(p) ? p : [] } catch (e) { return [] } },
    parsedVideos () { try { var p = JSON.parse(this.resource?.content || '[]'); return Array.isArray(p) ? p : [] } catch (e) { return [] } },
  },
  mounted () { this.loadResource() },
  methods: {
    /** 构建 B 站官方播放器嵌入地址，兼容 BV 号 / av 号 / 分P / b23.tv */
    bilibiliEmbedUrl (url) {
      if (!url) return null
      var s = String(url)
      var pm = s.match(/[?&]p=(\d+)/i)
      var page = pm ? pm[1] : '1'
      // BV 号（www.bilibili.com/video/BVxxxx 或纯 BV 号）
      var bv = s.match(/BV[0-9A-Za-z]{10}/)
      if (bv) return 'https://player.bilibili.com/player.html?bvid=' + bv[0] + '&page=' + page + '&high_quality=1&danmaku=0'
      // av 号（bilibili.com/video/av12345 或纯 av12345）
      var av = s.match(/(?:bilibili\.com\/video\/av|(?:^|[^0-9])av)(\d+)/i)
      if (av) return 'https://player.bilibili.com/player.html?aid=' + av[1] + '&page=' + page + '&high_quality=1&danmaku=0'
      return null
    },
    /** 判断是否为可直接播放的 MP4 链接 */
    isMp4Url (url) {
      return !!url && /\.mp4(\?|$)/i.test(String(url))
    },
    /** 网易公开课（open.163.com）视频 → 后端解析直链播放代理地址 */
    neteasePlayUrl (url) {
      if (!url) return null
      var s = String(url)
      if (s.indexOf('open.163.com') === -1) return null
      var pid = (s.match(/[?&]pid=([A-Za-z0-9]+)/i) || [])[1]
      var mid = (s.match(/[?&]mid=([A-Za-z0-9]+)/i) || [])[1]
      if (!pid || !mid) return null
      return '/api/admin/netease/play?pid=' + pid + '&mid=' + mid
    },
    async loadResource () {
      var id = this.$route.query.id
      if (!id) { this.loading = false; return }
      try {
        var res = await resourceGenerationApi.getDetail(id)
        if (res?.code === 1 && res.response) {
          this.resource = res.response
          if (this.resource.resourceType === 'mind_map') this.$nextTick(() => this.renderSvgMindmap())
        }
      } catch (e) { console.error('加载资源详情失败:', e) } finally { this.loading = false }
    },

    // ====== 纯 SVG 思维导图渲染 ======
    renderSvgMindmap () {
      var container = this.$refs.mindmapContainer
      if (!container || !this.resource?.content) return
      var nodes = this.parseToTree(this.resource.content)

      var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
      svg.setAttribute('viewBox', '0 0 1200 700')
      svg.setAttribute('width', '100%')
      svg.setAttribute('height', '520')
      svg.style.overflow = 'visible'

      // 根节点
      this.drawMindmapNode(svg, nodes, 600, 60, 600, 0, 0)

      container.innerHTML = ''
      container.appendChild(svg)
    },

    drawMindmapNode (svg, node, x, y, parentX, parentY, depth) {
      if (!node) return
      // 连线
      if (depth > 0) {
        var line = document.createElementNS('http://www.w3.org/2000/svg', 'path')
        var midY = (parentY + y) / 2
        line.setAttribute('d', 'M' + parentX + ' ' + parentY + ' C' + parentX + ' ' + midY + ', ' + x + ' ' + midY + ', ' + x + ' ' + y)
        line.setAttribute('stroke', 'rgba(200,164,92,0.4)')
        line.setAttribute('stroke-width', '1.5')
        line.setAttribute('fill', 'none')
        svg.appendChild(line)
      }

      // 节点圆
      var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle')
      circle.setAttribute('cx', x)
      circle.setAttribute('cy', y)
      circle.setAttribute('r', depth === 0 ? 10 : 7)
      circle.setAttribute('fill', depth === 0 ? '#2A332A' : '#ffffff')
      circle.setAttribute('stroke', depth === 0 ? '#c8a45c' : '#2A332A')
      circle.setAttribute('stroke-width', depth === 0 ? '3' : '2')
      svg.appendChild(circle)

      // 文字
      var text = document.createElementNS('http://www.w3.org/2000/svg', 'text')
      text.setAttribute('x', x + 14)
      text.setAttribute('y', y + 5)
      text.setAttribute('fill', depth === 0 ? '#2A332A' : '#3d3a35')
      text.setAttribute('font-size', depth === 0 ? '16' : '13')
      text.setAttribute('font-weight', depth === 0 ? '700' : '400')
      text.setAttribute('font-family', 'Georgia, "Noto Serif SC", serif')
      text.textContent = (node.title || node.text || '').substring(0, 40)
      svg.appendChild(text)

      // 子节点
      var children = node.children || node.nodes || node.items || []
      if (!Array.isArray(children) || children.length === 0) return
      var childY = y + 50
      var childXStep = 1000 / Math.max(children.length, 1)
      var childStartX = x - (children.length - 1) * childXStep / 2
      for (var i = 0; i < children.length; i++) {
        var cx = Math.max(50, Math.min(1150, childStartX + i * childXStep))
        this.drawMindmapNode(svg, children[i], cx, childY, x, y, depth + 1)
      }
    },

    parseToTree (content) {
      content = (content || '').trim()
      try {
        var obj = JSON.parse(content)
        return { title: '思维导图', children: Array.isArray(obj) ? obj : obj.nodes || obj.children || [obj] }
      } catch (e) {}
      // Markdown 标题解析
      var lines = content.split('\n')
      var root = { title: '思维导图', children: [] }
      var stack = [{ node: root, level: 0 }]
      for (var i = 0; i < lines.length; i++) {
        var line = lines[i].trim()
        if (!line) continue
        var m = line.match(/^(#{1,6})\s+(.+)/)
        if (m) {
          var level = m[1].length; var title = m[2].replace(/\*\*/g, '')
          var newNode = { title: title, children: [] }
          while (stack.length > 0 && stack[stack.length - 1].level >= level) stack.pop()
          var parent = stack[stack.length - 1]
          if (parent) { parent.node.children.push(newNode); stack.push({ node: newNode, level: level }) }
        } else if (line.startsWith('- ') || line.startsWith('* ')) {
          var t = line.substring(2).trim()
          var leaf = { title: t }
          var p = stack[stack.length - 1]
          if (p) p.node.children.push(leaf)
        }
      }
      return root.children.length > 0 ? root : { title: content.substring(0, 60), children: [] }
    },

    zoomMindmap (delta) {
      this.mindmapScale = Math.max(0.3, Math.min(3, this.mindmapScale + delta))
      var svg = this.$refs.mindmapContainer?.querySelector('svg')
      if (svg) svg.style.transform = 'scale(' + this.mindmapScale + ')'
    },
    resetMindmap () { this.mindmapScale = 1; var svg = this.$refs.mindmapContainer?.querySelector('svg'); if (svg) svg.style.transform = 'scale(1)' },

    goBack () { this.$router.go(-1) },
    getTypeIcon (type) { var f = this.resourceTypes.find(t => t.key === type); return f?.icon || '📄' },
    getTypeName (type) { var f = this.resourceTypes.find(t => t.key === type); return f?.label || '未知' }
  }
}
</script>

<style scoped>
.detail-container { min-height: 100vh; background: var(--ink-3); color: #3d3a35; padding: 32px 40px; max-width: 1200px; margin: 0 auto; }
.detail-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.header-badge { display: inline-flex; align-items: center; gap: 8px; padding: 5px 16px; background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 100px; font-size: 12px; color: #8c8470; }
.badge-dot { width: 7px; height: 7px; border-radius: 50%; background: #3d755b; }
.back-btn { background: var(--ink-3); border: 1px solid #e8dfcc; color: #5c5548; border-radius: 8px; }
.info-bar { display: flex; align-items: center; justify-content: space-between; background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 14px; padding: 24px 28px; margin-bottom: 20px; }
.info-left { display: flex; align-items: center; gap: 16px; }
.type-badge { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; background: var(--ink-3); }
.resource-title { font-size: 20px; font-weight: 700; color: #1e3a5f; margin: 0 0 6px; }
.resource-meta { display: flex; gap: 16px; }
.meta-item { font-size: 13px; color: #a0988c; display: flex; align-items: center; gap: 4px; }
.info-right { display: flex; align-items: center; gap: 16px; }
.quality-ring { position: relative; width: 56px; height: 56px; }
.quality-svg { width: 56px; height: 56px; transform: rotate(-90deg); }
.ring-bg { fill: none; stroke: #f5f1e8; stroke-width: 3; }
.ring-fill { fill: none; stroke: #3d755b; stroke-width: 3; stroke-linecap: round; }
.quality-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); font-size: 14px; font-weight: 700; color: #3d755b; }
.word-count-badge { font-size: 12px; padding: 4px 12px; background: var(--ink-3); border-radius: 100px; color: #8c8470; }
.content-panel { background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 14px; padding: 28px; }
.content-mindmap-wrapper { position: relative; }
.mindmap-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; padding: 0 0 12px; border-bottom: 1px solid #f5f1e8; }
.toolbar-title { font-size: 14px; font-weight: 600; color: #1e3a5f; margin-right: auto; }
.mindmap-container { width: 100%; min-height: 520px; border: 1px solid #f0ece1; border-radius: 10px; background: var(--ink-3); overflow: auto; }
.mindmap-container svg { transform-origin: top left; transition: transform 0.2s; min-width: 1200px; }
.content-markdown { font-size: 14px; color: #3d3a35; line-height: 1.8; }
.content-markdown >>> .md-h1 { font-size: 22px; font-weight: 700; color: #1e3a5f; margin: 24px 0 12px; padding-bottom: 8px; border-bottom: 1px solid #f0ece1; }
.content-markdown >>> .md-h2 { font-size: 18px; font-weight: 700; color: #1e3a5f; margin: 20px 0 8px; }
.content-markdown >>> .md-h3 { font-size: 16px; font-weight: 600; color: #5c5548; margin: 14px 0 6px; }
.content-markdown >>> .md-p { margin: 0 0 10px; }
.content-markdown >>> .inline-code { background: var(--ink-3); color: #1e3a5f; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.content-markdown >>> .code-block { background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 8px; padding: 16px; margin: 12px 0; overflow-x: auto; font-size: 13px; color: #5c5548; }
.content-markdown >>> .md-ul { padding-left: 20px; margin: 8px 0; }
.content-markdown >>> .md-li { margin-bottom: 4px; }
.exercise-card { background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 12px; padding: 20px 24px; margin-bottom: 14px; }
.exercise-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.exercise-num { width: 26px; height: 26px; border-radius: 8px; background: var(--ink-4); color: #faf8f3; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.exercise-type-tag { font-size: 11px; padding: 2px 10px; background: var(--ink-3); color: #8c8470; border-radius: 100px; }
.exercise-question { font-size: 14px; color: #3d3a35; line-height: 1.7; margin-bottom: 10px; }
.option-item { padding: 4px 10px; font-size: 13px; color: #5c5548; border-radius: 6px; background: var(--ink-3); margin-bottom: 3px; }
.option-letter { color: #1e3a5f; font-weight: 600; margin-right: 6px; }
.exercise-answer, .exercise-explanation { font-size: 13px; color: #5c5548; margin-top: 8px; padding: 10px 14px; background: var(--ink-3); border-radius: 8px; border-left: 3px solid #c8a45c; }
.answer-label { font-weight: 600; color: #1e3a5f; }
.video-card { display: flex; gap: 14px; padding: 18px 22px; background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 12px; margin-bottom: 12px; }
.video-icon { font-size: 32px; flex-shrink: 0; }
.video-title { font-size: 15px; font-weight: 600; color: #1e3a5f; margin: 0 0 4px; }
.video-desc { font-size: 13px; color: #8c8470; margin-bottom: 6px; }
.video-link { font-size: 13px; color: #1e3a5f; }
.content-raw pre { font-size: 13px; color: #5c5548; white-space: pre-wrap; word-break: break-all; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; }
.loading-spinner { width: 40px; height: 40px; border: 3px solid #e8dfcc; border-top-color: var(--ink-4); border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-icon { font-size: 48px; margin-bottom: 16px; }
</style>
