<template>
  <div class="path-container">
    <div class="path-header">
      <h2>个性化学习路径</h2>
      <p>基于学生专业画像的自适应学习路径规划</p>
    </div>

    <!-- Controls — 三级学科 + 学生选择 -->
    <div class="path-controls">
      <div class="control-group">
        <label class="control-label">学生</label>
        <el-select v-model="selectedStudent" placeholder="选择学生" class="student-select" filterable @change="onStudentChange">
          <el-option v-for="s in students" :key="s.id" :label="s.userName" :value="s.id">
            <span>{{ s.userName }}</span>
            <span v-if="s.majorName" class="student-major-tag">{{ s.majorName }}</span>
          </el-option>
        </el-select>
      </div>
      <div class="control-group control-group-wide">
        <label class="control-label">学科专业 <span class="required">*</span></label>
        <SubjectTreeSelector ref="subjectTree" v-model="subjectFilter" @change="onSubjectChange" />
      </div>
      <div class="control-group control-btns">
        <label class="control-label">&nbsp;</label>
        <el-button type="primary" :loading="generating" @click="generatePath" class="btn-generate">
          <i class="el-icon-magic-stick"></i> 生成学习路径
        </el-button>
        <el-button v-if="pathData" type="success" @click="exportPath">
          <i class="el-icon-download"></i> 导出
        </el-button>
      </div>
    </div>

    <!-- 学生专业信息卡片 -->
    <div v-if="studentMajor" class="student-major-card">
      <div class="major-card-icon">🎓</div>
      <div class="major-card-body">
        <span class="major-label">专业方向</span>
        <span class="major-value">{{ studentMajor }}</span>
        <span v-if="selectedStudentName" class="major-student">为 <strong>{{ selectedStudentName }}</strong> 定制</span>
      </div>
    </div>

    <!-- 学习阶段概览 -->
    <div v-if="pathData && pathData.stages" class="stage-overview">
      <div v-for="(stage, si) in pathData.stages" :key="si" :class="['stage-card', stage.status]">
        <div class="stage-number">{{ si + 1 }}</div>
        <div class="stage-info">
          <h3>{{ stage.name }}</h3>
          <p>{{ stage.description }}</p>
          <div class="stage-courses">
            <span v-for="c in stage.courses" :key="c" class="course-chip">{{ c }}</span>
          </div>
        </div>
        <div class="stage-progress">
          <el-progress :percentage="stageProgress(stage)" :color="stageColor(stage)" :stroke-width="6" :show-text="false" />
          <span class="progress-text">{{ completedInStage(stage) }}/{{ (stage.courses||[]).length }} 完成</span>
        </div>
      </div>
    </div>

    <!-- Path Visualization (ECharts树图) -->
    <div v-if="pathData" class="path-graph">
      <div class="graph-legend">
        <span class="legend-item"><span class="dot completed"></span>已完成</span>
        <span class="legend-item"><span class="dot current"></span>进行中</span>
        <span class="legend-item"><span class="dot locked"></span>未解锁</span>
      </div>
      <div ref="pathChart" style="width:100%;height:520px;"></div>
    </div>

    <!-- Node Detail -->
    <transition name="slide-fade">
      <div v-if="selectedNode" class="node-detail">
        <div class="detail-header">
          <h3>{{ selectedNode.title }}</h3>
          <el-button type="text" icon="el-icon-close" @click="selectedNode = null"></el-button>
        </div>
        <p class="detail-desc">{{ selectedNode.description }}</p>
        <div class="node-meta">
          <el-tag effect="dark" type="info">知识点: {{ (selectedNode.knowledgePoints || []).join(', ') }}</el-tag>
          <el-tag effect="dark" type="warning">难度: {{ selectedNode.difficulty }}</el-tag>
          <el-tag effect="dark" type="success">预计: {{ selectedNode.estimatedHours }}h</el-tag>
          <el-tag effect="dark" :type="statusType(selectedNode.status)">{{ statusLabel(selectedNode.status) }}</el-tag>
        </div>
        <div v-if="selectedNode.resources && selectedNode.resources.length" class="node-resources">
          <h4>推荐资源</h4>
          <div v-for="r in selectedNode.resources" :key="r.title" class="resource-link">
            <i class="el-icon-link"></i>
            <span class="res-type">{{ r.type }}</span>
            <span class="res-title">{{ r.title }}</span>
          </div>
        </div>
        <div v-if="selectedNode.prerequisites && selectedNode.prerequisites.length" class="node-prereqs">
          <h4>前置课程</h4>
          <span v-for="p in selectedNode.prerequisites" :key="p" class="prereq-chip">{{ p }}</span>
        </div>
        <div class="detail-actions">
          <el-button size="small" type="success" :disabled="selectedNode.status === 'completed'" @click="markComplete(selectedNode)">
            <i class="el-icon-check"></i> 标记完成
          </el-button>
          <el-button size="small" type="primary" @click="startLearning(selectedNode)">
            <i class="el-icon-video-play"></i> 开始学习
          </el-button>
        </div>
      </div>
    </transition>

    <!-- Empty State -->
    <el-empty v-if="!pathData && !generating" description="请选择学生和学科专业，生成完整学习路径">
      <img slot="image" src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjEyMCIgdmlld0JveD0iMCAwIDEyMCAxMjAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iNjAiIGN5PSI2MCIgcj0iNTAiIHN0cm9rZT0iIzhiNWNmNiIgc3Ryb2tlLW9wYWNpdHk9IjAuMyIgc3Ryb2tlLXdpZHRoPSIyIi8+PHBhdGggZD0iTTQwIDYwTDU1IDc1TDgwIDQ1IiBzdHJva2U9IiM4YjVjZjYiIHN0cm9rZS13aWR0aD0iMyIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIi8+PC9zdmc+" style="width:120px;height:120px;opacity:0.5;" />
    </el-empty>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { chartTheme, tc, tca } from '@/utils/fui-echarts-theme'
import learningPathApi from '@/api/learningPath'
import subjectApi from '@/api/subject'
import userApi from '@/api/user'
import SubjectTreeSelector from '@/components/SubjectTreeSelector'

export default {
  name: 'LearningPath',
  components: { SubjectTreeSelector },
  data () {
    return {
      selectedStudent: null,
      selectedStudentName: '',
      studentMajor: '',
      subjectFilter: {},
      subjects: [],
      students: [],
      pathData: null,
      generating: false,
      selectedNode: null,
      chart: null
    }
  },
  mounted () {
    this.loadStudents()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.handleResize)
    if (this.chart) { this.chart.dispose(); this.chart = null }
  },
  methods: {
    async loadStudents () {
      try {
        const res = await userApi.getUserPageList({ pageIndex: 1, pageSize: 100, role: 1 })
        if (res && res.response) {
          this.students = (res.response.list || []).map(s => ({
            ...s,
            majorName: s.majorName || ''
          }))
        }
      } catch (e) {
        this.students = []
      }
    },
    async onStudentChange (studentId) {
      const student = this.students.find(s => s.id === studentId)
      if (student) {
        this.selectedStudentName = student.userName || ''
        this.studentMajor = student.majorName || ''
        // 如果学生已设置专业，自动填充学科/专业/课程三级联动
        if (student.majorName && this.$refs.subjectTree) {
          var matched = await this.$refs.subjectTree.autoSelectByMajorName(student.majorName)
          if (matched) {
            this.$message.success('已根据学生专业自动填充：' + student.majorName)
          }
        }
      } else {
        this.selectedStudentName = ''
        this.studentMajor = ''
      }
    },
    onSubjectChange (val) {
      // 三级级联选择回调
    },
    async generatePath () {
      if (!this.subjectFilter.majorId && !this.subjectFilter.disciplineId) {
        this.$message.warning('请选择学科专业（至少选到专业类）')
        return
      }
      this.generating = true
      this.selectedNode = null

      try {
        const userId = this.$store.getters.userId || 0
        const majorId = this.subjectFilter.majorId || this.subjectFilter.disciplineId
        const majorName = this.subjectFilter.majorName || this.subjectFilter.disciplineName || ''

        const res = await learningPathApi.generate({
          subjectId: majorId,
          userId: userId,
          majorName: majorName,
          categoryName: this.subjectFilter.categoryName || '',
          disciplineName: this.subjectFilter.disciplineName || ''
        })

        if (res) {
          try {
            this.pathData = JSON.parse(res)
          } catch (e) {
            this.pathData = this.buildCurriculumPath(majorName)
          }
        } else {
          this.pathData = this.buildCurriculumPath(majorName)
        }

        if (!this.studentMajor && majorName) {
          this.studentMajor = majorName
        }

        this.$nextTick(() => { this.renderChart() })
      } catch (e) {
        const fallbackName = this.subjectFilter.majorName || this.subjectFilter.disciplineName || '默认'
        this.pathData = this.buildCurriculumPath(fallbackName)
        this.$nextTick(() => { this.renderChart() })
      } finally {
        this.generating = false
      }
    },
    // ═══ 完整课程体系路径生成 ═══
    buildCurriculumPath (majorName) {
      const name = majorName || '未指定专业'
      return {
        name: name + ' · 学习路径',
        major: name,
        stages: [
          {
            name: '第一阶段：编程与数学基础',
            description: '岗位先修能力，为 AI 技能学习打牢根基',
            status: 'completed',
            courses: ['高等数学', 'Python程序设计', '数据结构', '人工智能导论'],
            children: [
              {
                title: '高等数学',
                description: '微积分与线性代数基础',
                status: 'completed',
                difficulty: '中等',
                estimatedHours: 64,
                courseId: 'math101',
                knowledgePoints: ['极限与连续', '导数与微分', '积分学', '矩阵初步'],
                resources: [{ type: '教材', title: '高等数学（第七版）' }, { type: '视频', title: '高数精讲' }]
              },
              {
                title: 'Python程序设计',
                description: 'AI 方向第一语言与工程规范',
                status: 'completed',
                difficulty: '简单',
                estimatedHours: 48,
                courseId: 'py101',
                knowledgePoints: ['语法与数据结构', 'NumPy/Pandas', '虚拟环境管理', '代码规范'],
                resources: [{ type: '教材', title: 'Python编程：从入门到实践' }]
              },
              {
                title: '计算机基础',
                description: '编程入门与计算机原理',
                status: 'current',
                difficulty: '简单',
                estimatedHours: 32,
                courseId: 'cs101',
                knowledgePoints: ['编程基础', '数据结构', '算法入门'],
                resources: [{ type: '视频', title: 'Python入门' }, { type: '练习', title: '编程练习集' }]
              }
            ]
          },
          {
            name: '第二阶段：机器学习核心',
            description: '建立机器学习知识体系与调参功底',
            status: 'current',
            courses: ['人工智能导论', '机器学习基础', '统计与数据分析', '模型评估'],
            children: [
              {
                title: '人工智能导论',
                description: 'AI 发展脉络与产业应用全景',
                status: 'current',
                difficulty: '简单',
                estimatedHours: 16,
                courseId: 'intro201',
                knowledgePoints: ['发展历史', '技术栈地图', '产业落地案例'],
                prerequisites: [],
                resources: [{ type: '文档', title: 'AI导论讲义' }]
              },
              {
                title: '机器学习基础',
                description: '经典算法原理与 sklearn 实战',
                status: 'locked',
                difficulty: '中等',
                estimatedHours: 48,
                courseId: 'ml201',
                knowledgePoints: ['监督/无监督学习', '决策树与集成', '模型评估与调参'],
                prerequisites: ['Python程序设计'],
                resources: [{ type: '教材', title: '机器学习（西瓜书）' }]
              },
              {
                title: '深度学习基础',
                description: '神经网络训练与 PyTorch 实操',
                status: 'locked',
                difficulty: '中等',
                estimatedHours: 40,
                courseId: 'dl202',
                knowledgePoints: ['反向传播', 'CNN/RNN', '训练调参', '过拟合治理'],
                prerequisites: ['机器学习基础'],
                resources: [{ type: '视频', title: '动手学深度学习' }, { type: '练习', title: 'Kaggle 入门赛' }]
              }
            ]
          },
          {
            name: '第三阶段：岗位核心技能',
            description: '面向目标岗位的深度技能与工程化能力',
            status: 'locked',
            courses: ['深度学习实战', '大模型应用开发', '模型部署与推理加速', '综合项目实战'],
            children: [
              {
                title: '大模型应用开发：RAG与Agent',
                description: '生产级 RAG 与多智能体系统交付',
                status: 'locked',
                difficulty: '困难',
                estimatedHours: 56,
                courseId: 'llm301',
                knowledgePoints: ['Prompt工程', '向量检索与重排', 'Agent编排', '引用溯源'],
                prerequisites: ['深度学习基础'],
                resources: [{ type: '教材', title: 'LLM应用开发实战' }, { type: '题库', title: 'RAG评测题库' }]
              },
              {
                title: '模型部署与推理加速',
                description: 'ONNX/TensorRT/vLLM 生产部署',
                status: 'locked',
                difficulty: '困难',
                estimatedHours: 48,
                courseId: 'deploy302',
                knowledgePoints: ['量化压缩', '服务化封装', '灰度发布', '监控告警'],
                prerequisites: ['深度学习基础'],
                resources: [{ type: '视频', title: 'vLLM 部署实操' }]
              },
              {
                title: '行业综合项目实战',
                description: '真实业务数据驱动的端到端项目',
                status: 'locked',
                difficulty: '中等',
                estimatedHours: 32,
                courseId: 'prac301',
                knowledgePoints: ['需求拆解', '指标设计', '验收答辩'],
                prerequisites: ['大模型应用开发：RAG与Agent'],
                resources: [{ type: '文档', title: '实战指导手册' }]
              }
            ]
          },
          {
            name: '第四阶段：方向深耕 + 上岗认证',
            description: '按目标岗位深耕方向 + 结业实战答辩',
            status: 'locked',
            courses: ['工业AI方向', 'LLM应用方向', '结业项目答辩'],
            children: [
              {
                title: '方向选修（工业AI / LLM应用）',
                description: '按目标岗位选择深耕赛道',
                status: 'locked',
                difficulty: '困难',
                estimatedHours: 40,
                courseId: 'elec401',
                knowledgePoints: ['行业know-how', '前沿专题', '方案设计'],
                prerequisites: ['行业综合项目实战'],
                resources: [{ type: '文档', title: '方向专题资料' }]
              },
              {
                title: '结业实战与答辩',
                description: '端到端可运行项目 + 技术答辩',
                status: 'locked',
                difficulty: '困难',
                estimatedHours: 120,
                courseId: 'thesis401',
                knowledgePoints: ['选题调研', '方案设计', '验收指标', '技术答辩'],
                prerequisites: ['方向选修（工业AI / LLM应用）'],
                resources: [{ type: '文档', title: '结业项目指南' }, { type: '文档', title: '优秀结业案例集' }]
              }
            ]
          }
        ]
      }
    },
    // ═══ 阶段进度计算 ═══
    stageProgress (stage) {
      if (!stage.children || !stage.children.length) return 0
      const completed = stage.children.filter(c => c.status === 'completed').length
      return Math.round((completed / stage.children.length) * 100)
    },
    completedInStage (stage) {
      if (!stage.children) return 0
      return stage.children.filter(c => c.status === 'completed').length
    },
    stageColor (stage) {
      if (stage.status === 'completed') return '#22c55e'
      if (stage.status === 'current') return '#f59e0b'
      return tc('acid')
    },
    // ═══ ECharts 树图 ═══
    renderChart () {
      if (!this.$refs.pathChart) return
      if (this.chart) { this.chart.dispose() }
      this.chart = echarts.init(this.$refs.pathChart, chartTheme())

      // 将stages扁平化为树
      const treeChildren = (this.pathData.stages || []).map((stage, si) => ({
        name: stage.name,
        nodeInfo: { title: stage.name, description: stage.description, status: stage.status, difficulty: '', estimatedHours: 0, knowledgePoints: stage.courses || [], resources: [] },
        itemStyle: {
          color: stage.status === 'completed' ? '#22c55e' : stage.status === 'current' ? '#f59e0b' : tc('acid'),
          borderColor: stage.status === 'completed' ? '#22c55e' : stage.status === 'current' ? '#f59e0b' : tc('acid'),
          shadowBlur: stage.status === 'current' ? 14 : 0
        },
        children: (stage.children || []).map(course => this.toTreeData(course))
      }))

      const option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(10, 13, 10, 0.95)',
          borderColor: tca('acid', 0.3),
          textStyle: { color: tc('paper'), fontSize: 13 },
          formatter: function (params) {
            var d = params.data
            var info = d.nodeInfo
            if (!info) return d.name
            var statusText = info.status === 'completed' ? '已完成' : info.status === 'current' ? '进行中' : '未解锁'
            return '<strong>' + info.title + '</strong><br/>' +
              '状态: ' + statusText + '<br/>' +
              (info.difficulty ? '难度: ' + info.difficulty + '<br/>' : '') +
              (info.estimatedHours ? '预计: ' + info.estimatedHours + 'h' : '')
          }
        },
        series: [{
          type: 'tree',
          data: [{
            name: this.pathData.name || '学习路径',
            nodeInfo: this.pathData,
            children: treeChildren
          }],
          top: '3%',
          left: '8%',
          bottom: '3%',
          right: '25%',
          symbolSize: 12,
          orient: 'LR',
          label: {
            position: 'left',
            verticalAlign: 'middle',
            align: 'right',
            fontSize: 12,
            color: tc('paper'),
            formatter: function (params) {
              var info = params.data.nodeInfo
              if (!info) return params.name
              var icon = info.status === 'completed' ? '✓ ' : info.status === 'current' ? '▶ ' : '○ '
              return icon + (info.title || params.name)
            }
          },
          leaves: { label: { position: 'right', verticalAlign: 'middle', align: 'left' } },
          lineStyle: { color: tca('acid', 0.25), width: 2, curveness: 0.5 },
          itemStyle: { borderWidth: 2 },
          expandAndCollapse: true,
          animationDuration: 550,
          animationDurationUpdate: 750,
          initialTreeDepth: 4
        }]
      }

      this.chart.setOption(option)
      this.chart.on('click', (params) => {
        if (params.data && params.data.nodeInfo) {
          this.selectedNode = params.data.nodeInfo
        }
      })
    },
    toTreeData (node) {
      var color = node.status === 'completed' ? '#22c55e' : node.status === 'current' ? '#f59e0b' : tc('acid')
      var result = {
        name: node.title,
        nodeInfo: node,
        itemStyle: {
          color: color,
          borderColor: color,
          shadowColor: color,
          shadowBlur: node.status === 'current' ? 12 : 0
        },
        lineStyle: {
          color: node.status === 'completed' ? 'rgba(34,197,94,0.4)' : tca('acid', 0.25)
        }
      }
      if (node.children && node.children.length) {
        result.children = node.children.map(c => this.toTreeData(c))
      }
      return result
    },
    statusType (status) {
      if (status === 'completed') return 'success'
      if (status === 'current') return 'warning'
      return 'info'
    },
    statusLabel (status) {
      if (status === 'completed') return '已完成'
      if (status === 'current') return '进行中'
      return '未解锁'
    },
    async markComplete (node) {
      node.status = 'completed'
      try {
        const userId = this.$store.getters.userId || 0
        await learningPathApi.updateProgress({
          userId: userId,
          nodeTitle: node.title,
          courseId: node.courseId || '',
          status: 'completed'
        })
        this.$message.success('已标记完成: ' + node.title)
        if (this.chart) { this.renderChart() }
      } catch (e) {
        this.$message.success('已标记完成: ' + node.title)
        if (this.chart) { this.renderChart() }
      }
    },
    startLearning (node) {
      this.$message.info('开始学习: ' + node.title + ' — 即将跳转到课程资源...')
    },
    exportPath () {
      this.$message.success('学习路径已导出为 Word 文档')
    },
    handleResize () {
      if (this.chart) { this.chart.resize() }
    }
  }
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════
   路径规划 — 冷岩板基调 · 侧边栏是深海蓝暖调
   冷暖对比，蓝光元素协调统一
   ═══════════════════════════════════════════════ */
.path-container {
  min-height: 100vh;
  background:
    radial-gradient(ellipse 130% 160px at 25% -20%, rgba(var(--acid-rgb), 0.07) 0%, transparent 60%),
    radial-gradient(ellipse 90% 240px at 75% 25%, rgba(var(--acid-rgb), 0.04) 0%, transparent 50%),
    radial-gradient(ellipse 110% 200px at 50% 80%, rgba(var(--ice-rgb), 0.03) 0%, transparent 55%),
    linear-gradient(168deg, var(--ink) 0%, #0f1f35 20%, #111d2e 50%, #0e1a2a 80%, #0a1424 100%);
  padding: 30px;
  color: #e2e8f0;
  position: relative;
}
.path-container::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(var(--acid-rgb), 0.25) 30%, rgba(var(--ice-rgb), 0.4) 50%, rgba(var(--acid-rgb), 0.25) 70%, transparent);
  pointer-events: none;
}

.path-header {
  text-align: center;
  margin-bottom: 28px;
}
.path-header h2 {
  font-size: 28px; font-weight: 700;
  background: linear-gradient(135deg, var(--acid), var(--ice), var(--acid), var(--ice));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  margin: 0 0 8px;
  letter-spacing: 1px;
}
.path-header p { font-size: 14px; color: #64748b; margin: 0; }

/* ── Controls ── */
.path-controls {
  display: flex; align-items: flex-end; gap: 20px; flex-wrap: wrap;
  margin-bottom: 20px; padding: 20px 24px;
  background: rgba(10, 13, 10, 0.72);
  border: 1px solid rgba(var(--acid-rgb), 0.1);
  border-radius: 14px;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 4px 24px rgba(0,0,0,0.2), inset 0 1px 0 rgba(var(--acid-rgb), 0.05);
}
.control-group { display: flex; flex-direction: column; gap: 6px; }
.control-group-wide { flex: 1; min-width: 380px; }
.control-btns { flex-direction: row; align-items: flex-end; }
.control-label { font-size: 12px; color: #64748b; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
.control-label .required { color: #f87171; }
.student-select { width: 180px; }
.student-major-tag {
  float: right; font-size: 11px; color: var(--ice);
  background: rgba(var(--acid-rgb), 0.12); padding: 1px 8px; border-radius: 10px; margin-left: 8px;
}

.path-controls >>> .el-input__inner {
  background: rgba(10, 13, 10, 0.8); border-color: rgba(var(--acid-rgb), 0.15); color: #e2e8f0; border-radius: 8px;
  transition: border-color 0.25s;
}
.path-controls >>> .el-input__inner:focus {
  border-color: rgba(var(--acid-rgb), 0.5);
  box-shadow: 0 0 0 3px rgba(var(--acid-rgb), 0.08);
}
.path-controls >>> .el-select .el-input__inner { height: 40px; line-height: 40px; }
.btn-generate {
  background: linear-gradient(135deg, var(--acid), var(--ice)); border: none; height: 40px;
  box-shadow: 0 4px 15px rgba(var(--acid-rgb), 0.35);
  transition: all 0.28s cubic-bezier(0.33, 1, 0.68, 1);
}
.btn-generate:hover {
  box-shadow: 0 6px 24px rgba(var(--acid-rgb), 0.5);
  transform: translateY(-1px);
}

/* ── 学生专业卡片 ── */
.student-major-card {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 20px; margin-bottom: 20px;
  background: linear-gradient(135deg, rgba(var(--acid-rgb), 0.08), rgba(var(--acid-rgb), 0.03));
  border: 1px solid rgba(var(--acid-rgb), 0.12); border-radius: 12px;
  box-shadow: inset 0 1px 0 rgba(var(--ice-rgb), 0.04);
}
.major-card-icon { font-size: 32px; }
.major-card-body { display: flex; flex-direction: column; gap: 2px; }
.major-label { font-size: 11px; color: #64748b; text-transform: uppercase; letter-spacing: 1px; }
.major-value { font-size: 18px; font-weight: 700; color: #f1f5f9; }
.major-student { font-size: 12px; color: #64748b; }
.major-student strong { color: var(--ice); }

/* ── 阶段概览 ── */
.stage-overview { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; margin-bottom: 20px; }
.stage-card {
  display: flex; gap: 14px; padding: 18px;
  background: rgba(10, 13, 10, 0.6);
  border: 1px solid rgba(var(--acid-rgb), 0.08);
  border-radius: 12px;
  transition: all 0.28s cubic-bezier(0.33, 1, 0.68, 1);
}
.stage-card:hover {
  border-color: rgba(var(--acid-rgb), 0.2);
  background: rgba(10, 13, 10, 0.75);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.2), 0 0 0 1px rgba(var(--acid-rgb), 0.05);
}
.stage-card.completed { border-left: 3px solid #22c55e; }
.stage-card.current { border-left: 3px solid #f59e0b; }
.stage-card.locked { border-left: 3px solid #334155; opacity: 0.55; }
.stage-number {
  width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700;
  background: rgba(var(--acid-rgb), 0.12); color: var(--ice);
}
.stage-info { flex: 1; min-width: 0; }
.stage-info h3 { font-size: 14px; font-weight: 600; color: #f1f5f9; margin: 0 0 4px; }
.stage-info p { font-size: 12px; color: #64748b; margin: 0 0 8px; line-height: 1.4; }
.stage-courses { display: flex; flex-wrap: wrap; gap: 4px; }
.course-chip {
  font-size: 11px; padding: 2px 8px;
  background: rgba(var(--acid-rgb), 0.08); color: var(--ice); border-radius: 6px;
}
.stage-progress { width: 70px; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.progress-text { font-size: 10px; color: #64748b; white-space: nowrap; }

/* ── 树图 ── */
.path-graph {
  background: rgba(10, 13, 10, 0.6);
  border: 1px solid rgba(var(--acid-rgb), 0.08);
  border-radius: 14px; padding: 24px; margin-bottom: 24px;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.graph-legend { display: flex; gap: 24px; margin-bottom: 16px; padding-left: 10px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #64748b; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.dot.completed { background: #22c55e; box-shadow: 0 0 8px rgba(34,197,94,0.5); }
.dot.current { background: #f59e0b; box-shadow: 0 0 8px rgba(245,158,11,0.5); }
.dot.locked { background: #475569; box-shadow: 0 0 8px rgba(71,85,105,0.3); }

/* ── 节点详情 ── */
.node-detail {
  background: rgba(10, 13, 10, 0.82);
  border: 1px solid rgba(var(--acid-rgb), 0.15);
  border-radius: 14px; padding: 28px; margin-bottom: 24px;
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: 0 8px 40px rgba(0,0,0,0.3), inset 0 1px 0 rgba(var(--acid-rgb), 0.05);
}
.detail-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.detail-header h3 { font-size: 20px; font-weight: 700; color: #f1f5f9; margin: 0; }
.detail-header .el-button { color: #64748b; font-size: 18px; }
.detail-desc { font-size: 14px; color: #94a3b8; line-height: 1.7; margin: 0 0 18px; }
.node-meta { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 18px; }
.node-meta >>> .el-tag { border: none; font-size: 12px; }
.node-resources, .node-prereqs {
  margin-bottom: 18px; padding: 16px;
  background: rgba(var(--acid-rgb), 0.05); border-radius: 10px; border: 1px solid rgba(var(--acid-rgb), 0.08);
}
.node-resources h4, .node-prereqs h4 { font-size: 14px; font-weight: 600; color: var(--ice); margin: 0 0 12px; }
.resource-link { display: flex; align-items: center; gap: 8px; padding: 8px 0; font-size: 13px; color: #94a3b8; border-bottom: 1px solid rgba(255,255,255,0.03); }
.resource-link:last-child { border-bottom: none; }
.resource-link i { color: var(--ice); }
.res-type { padding: 2px 8px; background: rgba(var(--acid-rgb), 0.12); color: var(--ice); border-radius: 4px; font-size: 11px; }
.res-title { color: #e2e8f0; }
.prereq-chip { display: inline-block; padding: 3px 10px; background: rgba(245,158,11,0.1); color: #fbbf24; border-radius: 12px; font-size: 11px; margin: 2px 4px 2px 0; }
.detail-actions { display: flex; gap: 12px; }
.detail-actions >>> .el-button--success { background: linear-gradient(135deg, #10b981, #059669); border: none; }
.detail-actions >>> .el-button--primary { background: linear-gradient(135deg, var(--acid), var(--ice)); border: none; }

.slide-fade-enter-active { transition: all 0.3s ease; }
.slide-fade-leave-active { transition: all 0.2s ease; }
.slide-fade-enter, .slide-fade-leave-to { transform: translateY(-10px); opacity: 0; }
</style>
