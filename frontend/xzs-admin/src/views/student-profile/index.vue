<template>
  <div class="profile-page">
    <!-- 顶部标题 -->
    <header class="hero">
      <div class="hero-badge">
        <span class="badge-dot" />
        <span>Student Profile System</span>
      </div>
      <h1 class="hero-title">
        <span class="title-accent">学生画像</span>
      </h1>
      <p class="hero-desc">8 维度学习特征 · 对话式构建 · AI 智能解读</p>
    </header>

    <!-- 学生选择与操作栏 -->
    <section class="toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="selectedUserId"
          filterable
          remote
          reserve-keyword
          placeholder="搜索并选择学生..."
          :remote-method="searchStudents"
          :loading="searching"
          size="medium"
          class="student-select"
          @change="onStudentChange"
        >
          <el-option
            v-for="s in studentList"
            :key="s.id"
            :label="s.userName + ' (' + (s.realName || '未实名') + ')'"
            :value="s.id"
          />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button size="medium" icon="el-icon-upload2" @click="showImportDialog = true">
          导入学生列表
        </el-button>
        <el-button
          size="medium"
          icon="el-icon-refresh"
          type="warning"
          :loading="reprofiling"
          @click="reprofile"
        >
          重新画像
        </el-button>
      </div>
    </section>

    <!-- 雷达图 -->
    <section class="radar-section" v-if="selectedUserId">
      <div class="card">
        <div class="card-header">
          <span>能力雷达图</span>
          <span class="tag" :class="profileLoaded ? 'tag-done' : 'tag-empty'">
            <span class="tag-dot" /> {{ profileLoaded ? '已构建' : '未构建' }}
          </span>
        </div>
        <div ref="radar" class="radar-chart" />
      </div>
    </section>

    <!-- 空状态 -->
    <section class="empty-state" v-if="!selectedUserId">
      <div class="empty-icon">👤</div>
      <h3>请选择学生以查看画像</h3>
      <p>从上方搜索框中搜索并选择学生，或导入新的学生列表</p>
    </section>

    <!-- 8 维度卡片网格 -->
    <section class="dims-section" v-if="selectedUserId">
      <div class="dims-header">
        <h3>画像维度</h3>
        <span class="dims-count">{{ filledCount }}/{{ dimensions.length }} 已填写</span>
      </div>
      <div class="dims-grid">
        <div
          v-for="dim in dimensions"
          :key="dim.key"
          class="dim"
          :class="{ on: hasValue(dim.field) }"
        >
          <div class="dim-top">
            <span class="dim-icon">{{ dim.icon }}</span>
            <div class="dim-check">{{ hasValue(dim.field) ? '✓' : '○' }}</div>
          </div>
          <div class="dim-label">{{ dim.label }}</div>
          <div class="dim-val">{{ getDisplayValue(dim) }}</div>
        </div>
      </div>
    </section>

    <!-- 操作 -->
    <section class="actions" v-if="selectedUserId">
      <el-button type="primary" class="btn-primary" @click="startDialogue">
        💬 开始画像对话
      </el-button>
      <el-button class="btn-ghost" :loading="explaining" @click="explainProfile">
        🤖 AI 解读画像
      </el-button>
    </section>

    <!-- AI 解读 -->
    <section v-if="explanation" class="card explain-card">
      <div class="card-header">
        <span>🧠 AI 解读</span>
        <span class="tag tag-done">{{ explaining ? '生成中...' : '解读完成' }}</span>
      </div>
      <div class="explain-body" v-html="renderedExplanation" />
      <span v-if="explaining" class="cursor">▊</span>
    </section>

    <!-- 导入学生对话框 -->
    <el-dialog title="导入学生列表" :visible.sync="showImportDialog" width="600px" :close-on-click-modal="false">
      <el-tabs v-model="importTab">
        <el-tab-pane label="粘贴数据" name="paste">
          <div class="import-tip">
            <p>按以下格式粘贴学生数据，每行一个学生：</p>
            <code>用户名,真实姓名,密码</code>
          </div>
          <el-input
            v-model="importText"
            type="textarea"
            :rows="10"
            placeholder="zhangsan,张三,123456
lisi,李四,123456"
          />
        </el-tab-pane>
        <el-tab-pane label="上传文件" name="file">
          <el-upload
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileSelect"
            accept=".csv,.txt"
          >
            <i class="el-icon-upload" />
            <div class="el-upload__text">将 CSV/TXT 文件拖到此处，或<em>点击上传</em></div>
          </el-upload>
        </el-tab-pane>
      </el-tabs>
      <div v-if="importPreview.length > 0" class="import-preview">
        <h4>预览（共 {{ importPreview.length }} 条）</h4>
        <el-table :data="importPreview.slice(0, 10)" size="mini" max-height="200">
          <el-table-column prop="userName" label="用户名" />
          <el-table-column prop="realName" label="姓名" />
        </el-table>
        <p v-if="importPreview.length > 10" style="color:#909399;font-size:12px">... 仅显示前10条</p>
      </div>
      <span slot="footer">
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { chartTheme } from '@/utils/fui-echarts-theme'
import studentProfileApi from '@/api/studentProfile'
import userApi from '@/api/user'

export default {
  name: 'StudentProfilePage',
  data () {
    return {
      selectedUserId: null,
      selectedStudent: null,
      studentList: [],
      searching: false,
      profile: {},
      profileLoaded: false,
      explaining: false,
      explanation: '',
      reprofiling: false,
      chart: null,
      showImportDialog: false,
      importTab: 'paste',
      importText: '',
      importFile: null,
      importPreview: [],
      importing: false,
      dimensions: [
        { key: 'knowledge', label: '知识基础', icon: '📚', field: 'knowledgeLevel' },
        { key: 'cognitive', label: '认知风格', icon: '🧠', field: 'cognitiveStyle' },
        { key: 'weak', label: '薄弱环节', icon: '⚠️', field: 'weakPoints' },
        { key: 'goals', label: '学习目标', icon: '🎯', field: 'learningGoals' },
        { key: 'pace', label: '学习节奏', icon: '⏱️', field: 'learningPace' },
        { key: 'interest', label: '兴趣方向', icon: '💡', field: 'interests' },
        { key: 'style', label: '学习风格', icon: '📖', field: 'learningStyle' },
        { key: 'motive', label: '动机水平', icon: '🔥', field: 'motivationLevel' }
      ]
    }
  },
  computed: {
    filledCount () {
      var self = this
      return this.dimensions.filter(function (d) { return self.hasValue(d.field) }).length
    },
    radarIndicators () {
      return this.dimensions.map(function (d) { return { name: d.label, max: 100 } })
    },
    radarValues () {
      var self = this
      return this.dimensions.map(function (d) {
        return self.hasValue(d.field) ? (self.dimensions.length - self.dimensions.indexOf(d)) * 12 : 20
      })
    },
    renderedExplanation () {
      if (!this.explanation) return ''
      return this.explanation
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n/g, '<br>')
    }
  },
  mounted () {
    var qId = this.$route.query.userId
    if (qId && !isNaN(parseInt(qId))) {
      this.selectedUserId = parseInt(qId)
      this.loadProfile()
    }
    this.initialSearch()
  },
  beforeDestroy () {
    if (this.chart) { this.chart.dispose(); this.chart = null }
  },
  methods: {
    hasValue (field) {
      var v = this.profile[field]
      if (v == null) return false
      if (typeof v === 'string') return v.trim().length > 0
      if (typeof v === 'number') return v > 0
      return false
    },
    getDisplayValue (dim) {
      var v = this.profile[dim.field]
      if (!this.hasValue(dim.field)) return '未填写'
      if (dim.field === 'motivationLevel') return v + ' 分'
      var s = String(v).trim()
      return s.length > 12 ? s.substring(0, 12) + '…' : s
    },
    initialSearch () {
      this.searchStudents('')
    },
    searchStudents (query) {
      var self = this
      this.searching = true
      userApi.getUserPageList({ pageIndex: 1, pageSize: 50, role: 1, userName: query || '' })
        .then(function (res) {
          self.searching = false
          if (res && res.code === 1 && res.response && res.response.list) {
            self.studentList = res.response.list.filter(function (u) { return u.role === 1 })
          }
        })
        .catch(function () {
          self.searching = false
        })
    },
    onStudentChange (userId) {
      if (userId) {
        this.profile = {}
        this.profileLoaded = false
        this.explanation = ''
        this.loadProfile()
      }
    },
    async loadProfile () {
      if (!this.selectedUserId) return
      try {
        var res = await studentProfileApi.getProfile(this.selectedUserId)
        if (res && res.code === 1 && res.response) {
          this.profile = res.response
          this.profileLoaded = true
          this.$nextTick(function () { this.initChart() }.bind(this))
        }
      } catch (e) { console.error(e) }
    },
    initChart () {
      if (!this.$refs.radar) return
      if (this.chart) this.chart.dispose()
      this.chart = echarts.init(this.$refs.radar, chartTheme())
      this.chart.setOption({
        backgroundColor: 'transparent',
        radar: {
          indicator: this.radarIndicators,
          shape: 'polygon',
          splitNumber: 4,
          axisName: { color: '#5c5548', fontSize: 11 },
          splitLine: { lineStyle: { color: '#e8dfcc' } },
          axisLine: { lineStyle: { color: '#e8dfcc' } }
        },
        series: [{
          type: 'radar',
          data: [{ value: this.radarValues, name: '画像' }],
          symbolSize: 4,
          lineStyle: { color: '#2A332A', width: 1.5 },
          areaStyle: { color: 'rgba(32, 40, 32, 0.06)' },
          itemStyle: { color: '#2A332A' }
        }]
      })
    },
    startDialogue () {
      if (!this.selectedUserId) return
      this.$router.push({ path: '/profile-agent/dialogue', query: { userId: this.selectedUserId } })
    },
    async explainProfile () {
      if (this.explaining) return
      this.explaining = true; this.explanation = ''
      try {
        await studentProfileApi.explainProfile(this.selectedUserId, function (chunk, full) {
          this.explanation = full
        }.bind(this))
      } catch (e) { this.$message.error('解读失败') } finally { this.explaining = false }
    },
    async reprofile () {
      if (!this.selectedUserId) {
        this.$message.warning('请先选择学生')
        return
      }
      try {
        await this.$confirm('确定要重置该学生的画像数据吗？所有已填维度将被清空。', '确认重新画像', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        this.reprofiling = true
        await studentProfileApi.updateProfile(this.selectedUserId, {
          knowledgeLevel: '',
          cognitiveStyle: '',
          weakPoints: '',
          learningGoals: '',
          learningPace: '',
          interests: '',
          learningStyle: '',
          motivationLevel: 5,
          profileSummary: '',
          profileVersion: 0
        })
        this.$message.success('画像已重置，请开始新的画像对话')
        this.profile = {}
        this.profileLoaded = false
        this.explanation = ''
        if (this.chart) { this.chart.dispose(); this.chart = null }
      } catch (e) {
        if (e !== 'cancel') { this.$message.error('重置失败') }
      } finally {
        this.reprofiling = false
      }
    },
    handleFileSelect (file) {
      var self = this
      var reader = new FileReader()
      reader.onload = function (e) {
        self.importText = e.target.result
        self.parseImportText()
      }
      reader.readAsText(file.raw)
    },
    parseImportText () {
      var lines = this.importText.trim().split(/\r?\n/)
      var result = []
      for (var i = 0; i < lines.length; i++) {
        var line = lines[i].trim()
        if (!line) continue
        var parts = line.split(/[,\t]+/)
        if (parts.length >= 1 && parts[0].trim()) {
          result.push({
            userName: (parts[0] || '').trim(),
            realName: (parts[1] || '').trim(),
            password: (parts[2] || '123456').trim()
          })
        }
      }
      this.importPreview = result
    },
    async doImport () {
      if (this.importPreview.length === 0) {
        this.$message.warning('没有可导入的学生数据')
        return
      }
      this.importing = true
      var success = 0
      var fail = 0
      for (var i = 0; i < this.importPreview.length; i++) {
        var s = this.importPreview[i]
        try {
          await userApi.createUser({
            userName: s.userName,
            realName: s.realName,
            password: s.password,
            role: 1,
            status: 1
          })
          success++
        } catch (e) {
          fail++
        }
      }
      this.importing = false
      this.$message.success('导入完成：' + success + ' 成功，' + fail + ' 失败')
      if (success > 0) {
        this.showImportDialog = false
        this.importText = ''
        this.importPreview = []
        this.searchStudents('')
      }
    }
  },
  watch: {
    importText: function () {
      this.parseImportText()
    }
  }
}
</script>

<style scoped>
.profile-page { min-height: 100vh; background: var(--ink-3); padding: 32px 40px 60px; max-width: 960px; margin: 0 auto; color: #3d3a35; }

/* Hero */
.hero { text-align: center; margin-bottom: 24px; }
.hero-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 14px; background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 100px; font-size: 11px; color: #8c8470; margin-bottom: 12px; }
.badge-dot { width: 6px; height: 6px; border-radius: 50%; background: #3d755b; }
.hero-title { font-size: 28px; font-weight: 700; margin: 0 0 6px; }
.title-accent { background: linear-gradient(135deg, var(--ink-4), var(--ink-5)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.hero-desc { font-size: 13px; color: #8c8470; margin: 0; }

/* Toolbar */
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }
.toolbar-left { flex: 1; min-width: 200px; }
.toolbar-right { display: flex; gap: 8px; }
.student-select { width: 100%; max-width: 360px; }

/* Empty State */
.empty-state { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-state h3 { font-size: 18px; color: #3d3a35; margin: 0 0 8px; }
.empty-state p { font-size: 13px; color: #8c8470; margin: 0; }

/* Card */
.card { background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 14px; padding: 20px 24px; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-size: 14px; font-weight: 600; color: #1e3a5f; margin-bottom: 12px; }
.tag { font-size: 11px; padding: 2px 8px; border-radius: 100px; font-weight: 500; }
.tag-done { background: var(--ink-3); color: #16a34a; }
.tag-empty { background: var(--ink-3); color: #94a3b8; }
.tag-dot { display: inline-block; width: 5px; height: 5px; border-radius: 50%; background: #22c55e; margin-right: 4px; }

/* Radar */
.radar-section { margin-bottom: 28px; }
.radar-chart { width: 100%; height: 320px; }

/* Dimensions */
.dims-section { margin-bottom: 24px; }
.dims-header { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 14px; }
.dims-header h3 { font-size: 16px; font-weight: 700; color: #1e3a5f; margin: 0; }
.dims-count { font-size: 12px; color: #8c8470; }
.dims-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.dim {
  background: var(--ink-3); border: 1px solid #e8dfcc; border-radius: 12px;
  padding: 14px; text-align: center; transition: all 0.2s;
}
.dim:hover { border-color: #c8a45c; transform: translateY(-1px); }
.dim.on { border-color: #3d755b; }
.dim-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.dim-icon { font-size: 22px; }
.dim-check { font-size: 13px; font-weight: 700; color: #c4bfb3; }
.dim.on .dim-check { color: #3d755b; }
.dim-label { font-size: 13px; font-weight: 600; color: #3d3a35; margin-bottom: 4px; }
.dim-val { font-size: 11px; color: #8c8470; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-height: 16px; }
.dim.on .dim-val { color: #3d755b; }

/* Actions */
.actions { display: flex; gap: 12px; justify-content: center; margin-bottom: 28px; }
.btn-primary { background: var(--ink-4); border-color: var(--ink-4); }
.btn-ghost { color: #1e3a5f; border-color: #c8a45c; }

/* Explain */
.explain-card { margin-bottom: 40px; }
.explain-body { font-size: 14px; color: #3d3a35; line-height: 1.8; }
.cursor { color: #1e3a5f; animation: blink 0.8s infinite; }
@keyframes blink { 50% { opacity: 0; } }

/* Import */
.import-tip { margin-bottom: 12px; font-size: 13px; color: var(--grey); }
.import-tip code { background: var(--ink-3); padding: 2px 6px; border-radius: 4px; }
.import-preview { margin-top: 16px; }
.import-preview h4 { font-size: 14px; margin: 0 0 8px; color: var(--grey); }

@media (max-width: 768px) { .dims-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .dims-grid { grid-template-columns: 1fr; } }
</style>
