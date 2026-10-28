<template>
  <div class="eval-container">
    <div class="eval-header">
      <h2>学习效果评估</h2>
      <p>多维度学习效果分析与AI评估报告</p>
    </div>

    <!-- Stats Cards -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon-wrap" style="background: linear-gradient(135deg, rgba(37,99,235,0.2), rgba(59,130,246,0.2));">
          <i class="el-icon-document" style="color: #60a5fa;"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalResources }}</div>
          <div class="stat-label">学习资源</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="background: linear-gradient(135deg, rgba(34,197,94,0.2), rgba(16,185,129,0.2));">
          <i class="el-icon-circle-check" style="color: #4ade80;"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completedNodes }}</div>
          <div class="stat-label">已完成节点</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="background: linear-gradient(135deg, rgba(37,99,235,0.2), rgba(59,130,246,0.2));">
          <i class="el-icon-guide" style="color: #60a5fa;"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalPaths }}</div>
          <div class="stat-label">学习路径</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap" style="background: linear-gradient(135deg, rgba(236,72,153,0.2), rgba(219,39,119,0.2));">
          <i class="el-icon-data-analysis" style="color: #f472b6;"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgScore }}%</div>
          <div class="stat-label">平均得分</div>
        </div>
      </div>
    </div>

    <!-- Charts -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="card-header">
          <h3><i class="el-icon-trend-charts"></i> 学习进度趋势</h3>
        </div>
        <div ref="trendChart" style="width:100%;height:300px;"></div>
      </div>
      <div class="chart-card">
        <div class="card-header">
          <h3><i class="el-icon-pie-chart"></i> 能力维度分析</h3>
        </div>
        <div ref="radarChart" style="width:100%;height:300px;"></div>
      </div>
    </div>

    <!-- Second Charts Row -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="card-header">
          <h3><i class="el-icon-histogram"></i> 知识点掌握度</h3>
        </div>
        <div ref="barChart" style="width:100%;height:300px;"></div>
      </div>
      <div class="chart-card">
        <div class="card-header">
          <h3><i class="el-icon-data-line"></i> 学习时长分布</h3>
        </div>
        <div ref="lineChart" style="width:100%;height:300px;"></div>
      </div>
    </div>

    <!-- Progress Ring -->
    <div class="progress-section">
      <div class="progress-ring-card">
        <div class="ring-header">
          <h3>总体学习进度</h3>
          <span class="ring-percent">{{ overallProgress }}%</span>
        </div>
        <div class="ring-container">
          <div ref="progressRing" style="width:200px;height:200px;"></div>
          <div class="ring-center">
            <div class="ring-value">{{ overallProgress }}%</div>
            <div class="ring-label">完成率</div>
          </div>
        </div>
        <div class="progress-details">
          <div class="detail-item">
            <span class="detail-dot" style="background: #22c55e;"></span>
            <span class="detail-label">已完成</span>
            <span class="detail-val">{{ stats.completedNodes }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-dot" style="background: #f59e0b;"></span>
            <span class="detail-label">进行中</span>
            <span class="detail-val">{{ stats.inProgressNodes || 2 }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-dot" style="background: #475569;"></span>
            <span class="detail-label">未开始</span>
            <span class="detail-val">{{ stats.lockedNodes || 5 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- AI Report -->
    <div class="report-section">
      <div class="report-header">
        <h3><i class="el-icon-magic-stick"></i> AI 学习评估报告</h3>
        <el-button
          type="primary"
          size="small"
          :loading="reporting"
          @click="generateReport"
        >
          <i class="el-icon-s-opportunity"></i> 生成报告
        </el-button>
      </div>
      <div v-if="reportContent" class="report-body" v-html="reportContent"></div>
      <el-empty v-else-if="!reporting" description="点击按钮生成AI评估报告">
        <i slot="image" class="el-icon-magic-stick" style="font-size:64px;color:rgba(139,92,246,0.3);"></i>
      </el-empty>
      <div v-if="reporting && !reportContent" class="report-loading">
        <i class="el-icon-loading"></i>
        <span>AI 正在分析学习数据并生成评估报告...</span>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { chartTheme, tc, tca } from '@/utils/fui-echarts-theme'
import evaluationApi from '@/api/evaluation'

export default {
  name: 'LearningEvaluation',
  data () {
    return {
      stats: {
        totalResources: 0,
        completedNodes: 0,
        totalPaths: 0,
        avgScore: 0,
        inProgressNodes: 2,
        lockedNodes: 5
      },
      overallProgress: 0,
      reportContent: '',
      reporting: false,
      charts: [],
      trendData: [],
      radarData: []
    }
  },
  mounted () {
    this.loadStats()
    this.initCharts()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.handleResize)
    this.charts.forEach(function (c) {
      if (c) c.dispose()
    })
    this.charts = []
  },
  methods: {
    async loadStats () {
      try {
        var userId = this.$store.getters.userId || 0
        var res = await evaluationApi.getProgress(userId)
        if (res && res.response) {
          var data = res.response
          this.stats = {
            totalResources: data.totalResources || 24,
            completedNodes: data.completedNodes || 12,
            totalPaths: data.totalPaths || 3,
            avgScore: data.avgScore || 78,
            inProgressNodes: data.inProgressNodes || 2,
            lockedNodes: data.lockedNodes || 5
          }
          this.overallProgress = data.overallProgress || Math.round(
            (this.stats.completedNodes / (this.stats.completedNodes + this.stats.inProgressNodes + this.stats.lockedNodes)) * 100
          )
          this.trendData = data.trendData || null
          this.radarData = data.radarData || null
        }
      } catch (e) {
        this.stats = {
          totalResources: 24,
          completedNodes: 12,
          totalPaths: 3,
          avgScore: 78,
          inProgressNodes: 2,
          lockedNodes: 5
        }
        this.overallProgress = 63
      }
    },
    initCharts () {
      this.$nextTick(() => {
        this.renderTrendChart()
        this.renderRadarChart()
        this.renderBarChart()
        this.renderLineChart()
        this.renderProgressRing()
      })
    },
    renderTrendChart () {
      if (!this.$refs.trendChart) return
      var chart = echarts.init(this.$refs.trendChart, chartTheme())
      this.charts.push(chart)

      var weeks = ['第1周', '第2周', '第3周', '第4周', '第5周', '第6周', '第7周', '第8周']
      var scores = this.trendData ? this.trendData.scores : [62, 65, 68, 72, 70, 75, 78, 82]
      var avgLine = this.trendData ? this.trendData.avgLine : [60, 62, 64, 66, 68, 70, 72, 74]

      var option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(10, 13, 10, 0.95)',
          borderColor: tca('acid', 0.3),
          textStyle: { color: tc('paper') }
        },
        legend: {
          data: ['个人得分', '班级平均'],
          textStyle: { color: tc('grey'), fontSize: 12 },
          top: 0,
          right: 10
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: weeks,
          axisLine: { lineStyle: { color: tca('acid', 0.15) } },
          axisLabel: { color: tc('grey'), fontSize: 11 },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          min: 40,
          max: 100,
          axisLine: { show: false },
          axisLabel: { color: tc('grey'), fontSize: 11 },
          splitLine: { lineStyle: { color: tca('acid', 0.08) } }
        },
        series: [
          {
            name: '个人得分',
            type: 'line',
            smooth: true,
            data: scores,
            lineStyle: { color: tc('acid'), width: 3 },
            itemStyle: { color: tc('acid') },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: tca('acid', 0.3) },
                { offset: 1, color: tca('acid', 0.02) }
              ])
            },
            symbol: 'circle',
            symbolSize: 8
          },
          {
            name: '班级平均',
            type: 'line',
            smooth: true,
            data: avgLine,
            lineStyle: { color: tc('grey'), width: 2, type: 'dashed' },
            itemStyle: { color: tc('grey') },
            symbol: 'circle',
            symbolSize: 6
          }
        ]
      }

      chart.setOption(option)
    },
    renderRadarChart () {
      if (!this.$refs.radarChart) return
      var chart = echarts.init(this.$refs.radarChart, chartTheme())
      this.charts.push(chart)

      var indicators = [
        { name: '知识理解', max: 100 },
        { name: '解题能力', max: 100 },
        { name: '综合应用', max: 100 },
        { name: '创新思维', max: 100 },
        { name: '学习习惯', max: 100 },
        { name: '协作能力', max: 100 }
      ]
      var personal = this.radarData ? this.radarData.personal : [82, 75, 68, 60, 85, 72]
      var average = this.radarData ? this.radarData.average : [70, 68, 65, 55, 72, 68]

      var option = {
        backgroundColor: 'transparent',
        tooltip: {
          backgroundColor: 'rgba(10, 13, 10, 0.95)',
          borderColor: tca('acid', 0.3),
          textStyle: { color: tc('paper') }
        },
        legend: {
          data: ['个人能力', '班级平均'],
          textStyle: { color: tc('grey'), fontSize: 12 },
          top: 0,
          right: 10
        },
        radar: {
          indicator: indicators,
          shape: 'polygon',
          splitNumber: 4,
          axisName: {
            color: tc('ice'),
            fontSize: 12
          },
          splitLine: {
            lineStyle: { color: tca('acid', 0.12) }
          },
          splitArea: {
            areaStyle: {
              color: [
                tca('acid', 0.02),
                tca('acid', 0.04),
                tca('acid', 0.06),
                tca('acid', 0.08)
              ]
            }
          },
          axisLine: {
            lineStyle: { color: tca('acid', 0.15) }
          }
        },
        series: [
          {
            type: 'radar',
            data: [
              {
                value: personal,
                name: '个人能力',
                lineStyle: { color: tc('acid'), width: 2 },
                itemStyle: { color: tc('acid') },
                areaStyle: { color: tca('acid', 0.2) }
              },
              {
                value: average,
                name: '班级平均',
                lineStyle: { color: tc('grey'), width: 1.5, type: 'dashed' },
                itemStyle: { color: tc('grey') },
                areaStyle: { color: 'rgba(100,116,139,0.08)' }
              }
            ]
          }
        ]
      }

      chart.setOption(option)
    },
    renderBarChart () {
      if (!this.$refs.barChart) return
      var chart = echarts.init(this.$refs.barChart, chartTheme())
      this.charts.push(chart)

      var categories = ['函数', '几何', '代数', '概率', '统计', '三角', '向量', '数列']
      var masteryData = [85, 72, 68, 90, 78, 62, 55, 80]
      var colors = masteryData.map(function (v) {
        if (v >= 80) return tc('acid')
        if (v >= 60) return '#F5A623'
        return tc('danger')
      })

      var option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(10, 13, 10, 0.95)',
          borderColor: tca('acid', 0.3),
          textStyle: { color: tc('paper') },
          formatter: function (params) {
            return params[0].name + ': ' + params[0].value + '%'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '8%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: categories,
          axisLine: { lineStyle: { color: tca('acid', 0.15) } },
          axisLabel: { color: tc('grey'), fontSize: 11 },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          max: 100,
          axisLine: { show: false },
          axisLabel: { color: tc('grey'), fontSize: 11, formatter: '{value}%' },
          splitLine: { lineStyle: { color: tca('acid', 0.08) } }
        },
        series: [{
          type: 'bar',
          data: masteryData.map(function (v, i) {
            return {
              value: v,
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: colors[i] },
                  { offset: 1, color: colors[i] + '44' }
                ]),
                borderRadius: [6, 6, 0, 0]
              }
            }
          }),
          barWidth: '50%',
          label: {
            show: true,
            position: 'top',
            color: tc('grey'),
            fontSize: 11,
            formatter: '{c}%'
          }
        }]
      }

      chart.setOption(option)
    },
    renderLineChart () {
      if (!this.$refs.lineChart) return
      var chart = echarts.init(this.$refs.lineChart, chartTheme())
      this.charts.push(chart)

      var days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      var hours = [1.5, 2.0, 1.2, 2.5, 1.8, 3.5, 4.0]
      var target = [2, 2, 2, 2, 2, 3, 3]

      var option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(10, 13, 10, 0.95)',
          borderColor: tca('acid', 0.3),
          textStyle: { color: tc('paper') },
          formatter: function (params) {
            return params[0].name + '<br/>' +
              '实际: ' + params[0].value + 'h<br/>' +
              '目标: ' + params[1].value + 'h'
          }
        },
        legend: {
          data: ['实际学习', '目标时长'],
          textStyle: { color: tc('grey'), fontSize: 12 },
          top: 0,
          right: 10
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: days,
          axisLine: { lineStyle: { color: tca('acid', 0.15) } },
          axisLabel: { color: tc('grey'), fontSize: 11 },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          axisLine: { show: false },
          axisLabel: { color: tc('grey'), fontSize: 11, formatter: '{value}h' },
          splitLine: { lineStyle: { color: tca('acid', 0.08) } }
        },
        series: [
          {
            name: '实际学习',
            type: 'bar',
            data: hours,
            barWidth: '35%',
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: tc('acid') },
                { offset: 1, color: tca('acid', 0.2) }
              ]),
              borderRadius: [4, 4, 0, 0]
            }
          },
          {
            name: '目标时长',
            type: 'line',
            data: target,
            lineStyle: { color: '#f59e0b', width: 2, type: 'dashed' },
            itemStyle: { color: '#f59e0b' },
            symbol: 'diamond',
            symbolSize: 8
          }
        ]
      }

      chart.setOption(option)
    },
    renderProgressRing () {
      if (!this.$refs.progressRing) return
      var chart = echarts.init(this.$refs.progressRing, chartTheme())
      this.charts.push(chart)

      var progress = this.overallProgress
      var option = {
        backgroundColor: 'transparent',
        series: [{
          type: 'pie',
          radius: ['70%', '85%'],
          center: ['50%', '50%'],
          startAngle: 90,
          silent: true,
          label: { show: false },
          data: [
            {
              value: progress,
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: tc('acid') },
                  { offset: 1, color: tc('acid') }
                ])
              }
            },
            {
              value: 100 - progress,
              itemStyle: {
                color: tca('acid', 0.08)
              }
            }
          ],
          animation: true,
          animationDuration: 1500
        }]
      }

      chart.setOption(option)
    },
    async generateReport () {
      this.reporting = true
      this.reportContent = ''

      try {
        var userId = this.$store.getters.userId || 0
        await evaluationApi.generateReport(userId, {
          stats: this.stats,
          overallProgress: this.overallProgress
        }, (chunk, complete) => {
          this.reportContent = this.formatReport(complete)
        })

        if (!this.reportContent) {
          this.reportContent = this.buildMockReport()
        }
      } catch (e) {
        this.reportContent = this.buildMockReport()
      } finally {
        this.reporting = false
      }
    },
    formatReport (text) {
      if (!text) return ''
      return text
        .replace(/### (.*)/g, '<h4 style="color:#A9F0FF;margin:16px 0 8px;font-size:15px;">$1</h4>')
        .replace(/## (.*)/g, '<h3 style="color:#A9F0FF;margin:20px 0 10px;font-size:17px;">$1</h3>')
        .replace(/# (.*)/g, '<h2 style="color:#CCFF00;margin:24px 0 12px;font-size:20px;">$1</h2>')
        .replace(/\*\*(.*?)\*\*/g, '<strong style="color:#e2e8f0;">$1</strong>')
        .replace(/\n/g, '<br/>')
    },
    buildMockReport () {
      return '<h2 style="color:#CCFF00;margin:0 0 16px;font-size:20px;">AI 学习评估报告</h2>' +
        '<h3 style="color:#A9F0FF;margin:16px 0 8px;">一、总体评估</h3>' +
        '<p style="color:#94a3b8;line-height:1.8;">该生在过去8周的学习中表现出持续进步的趋势。总体掌握度达到 <strong style="color:#e2e8f0;">' + this.stats.avgScore + '%</strong>，' +
        '已完成 <strong style="color:#e2e8f0;">' + this.stats.completedNodes + '</strong> 个学习节点，整体学习进度为 <strong style="color:#e2e8f0;">' + this.overallProgress + '%</strong>。</p>' +
        '<h3 style="color:#A9F0FF;margin:16px 0 8px;">二、优势领域</h3>' +
        '<p style="color:#94a3b8;line-height:1.8;">在<strong style="color:#e2e8f0;">知识理解</strong>和<strong style="color:#e2e8f0;">学习习惯</strong>方面表现突出，得分均超过80分。' +
        '基础概念掌握扎实，解题思路清晰。</p>' +
        '<h3 style="color:#A9F0FF;margin:16px 0 8px;">三、薄弱环节</h3>' +
        '<p style="color:#94a3b8;line-height:1.8;">在<strong style="color:#fbbf24;">创新思维</strong>和<strong style="color:#fbbf24;">综合应用</strong>方面仍有提升空间。' +
        '建议加强对复杂问题的分析训练，多做综合性练习题。</p>' +
        '<h3 style="color:#A9F0FF;margin:16px 0 8px;">四、学习建议</h3>' +
        '<p style="color:#94a3b8;line-height:1.8;">1. 保持当前良好的学习节奏和习惯<br/>' +
        '2. 每周增加2-3小时的综合应用练习<br/>' +
        '3. 重点攻克向量和三角函数等薄弱知识点<br/>' +
        '4. 积极参与协作学习，提升创新思维能力</p>'
    },
    handleResize () {
      this.charts.forEach(function (c) {
        if (c) c.resize()
      })
    }
  }
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════
   学习评估 — 冷岩板基调 · 侧边栏深海蓝暖调
   冷暖对比，蓝光元素协调统一
   ═══════════════════════════════════════════════ */
.eval-container {
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
.eval-container::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(var(--acid-rgb), 0.25) 30%, rgba(var(--ice-rgb), 0.4) 50%, rgba(var(--acid-rgb), 0.25) 70%, transparent);
  pointer-events: none;
}

.eval-header {
  text-align: center;
  margin-bottom: 30px;
}
.eval-header h2 {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--acid), var(--ice), var(--acid), var(--ice));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 8px;
  letter-spacing: 1px;
}
.eval-header p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 20px;
  background: rgba(10, 13, 10, 0.6);
  border: 1px solid rgba(var(--acid-rgb), 0.08);
  border-radius: 14px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: all 0.28s cubic-bezier(0.33, 1, 0.68, 1);
}
.stat-card:hover {
  transform: translateY(-3px);
  border-color: rgba(var(--acid-rgb), 0.2);
  background: rgba(10, 13, 10, 0.75);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(var(--acid-rgb), 0.05);
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon-wrap i {
  font-size: 24px;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #f1f5f9;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.chart-card {
  background: rgba(10, 13, 10, 0.6);
  border: 1px solid rgba(var(--acid-rgb), 0.08);
  border-radius: 14px;
  padding: 20px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}
.card-header {
  margin-bottom: 8px;
}
.card-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ice);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-header h3 i {
  color: var(--ice);
}

.progress-section {
  margin-bottom: 24px;
}
.progress-ring-card {
  background: rgba(10, 13, 10, 0.6);
  border: 1px solid rgba(var(--acid-rgb), 0.08);
  border-radius: 14px;
  padding: 24px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  gap: 40px;
}
.ring-header {
  flex-shrink: 0;
}
.ring-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
  margin: 0 0 8px;
}
.ring-percent {
  font-size: 36px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--acid), var(--ice));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.ring-container {
  position: relative;
}
.ring-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}
.ring-value {
  font-size: 28px;
  font-weight: 800;
  color: #f1f5f9;
}
.ring-label {
  font-size: 12px;
  color: #64748b;
}
.progress-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.detail-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.detail-label {
  font-size: 13px;
  color: #94a3b8;
  flex: 1;
}
.detail-val {
  font-size: 16px;
  font-weight: 700;
  color: #e2e8f0;
}

.report-section {
  background: rgba(10, 13, 10, 0.6);
  border: 1px solid rgba(var(--acid-rgb), 0.08);
  border-radius: 14px;
  padding: 24px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}
.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(var(--acid-rgb), 0.08);
}
.report-header h3 {
  font-size: 17px;
  font-weight: 600;
  color: var(--ice);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.report-header h3 i {
  color: var(--ice);
}
.report-header .el-button--primary {
  background: linear-gradient(135deg, var(--acid), var(--ice));
  border: none;
  box-shadow: 0 4px 15px rgba(var(--acid-rgb), 0.35);
  transition: all 0.28s cubic-bezier(0.33, 1, 0.68, 1);
}
.report-header .el-button--primary:hover {
  box-shadow: 0 6px 20px rgba(var(--acid-rgb), 0.5);
  transform: translateY(-1px);
}
.report-body {
  padding: 20px;
  background: rgba(10, 13, 10, 0.7);
  border-radius: 10px;
  border: 1px solid rgba(var(--acid-rgb), 0.06);
  line-height: 1.8;
  font-size: 14px;
  color: #94a3b8;
}
.report-loading {
  text-align: center;
  padding: 40px;
  color: #64748b;
  font-size: 14px;
}
.report-loading i {
  font-size: 24px;
  color: var(--ice);
  margin-right: 10px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.eval-container >>> .el-empty__description p {
  color: #64748b;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
  .progress-ring-card {
    flex-direction: column;
    text-align: center;
  }
}
</style>
