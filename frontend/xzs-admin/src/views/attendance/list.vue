<template>
  <div class="app-container">
    <el-card shadow="hover" class="main-card">
      <div slot="header" class="card-header">
        <i class="el-icon-s-order header-icon"></i>
        <span class="header-title">考勤记录</span>
        <el-button type="primary" size="small" style="float:right" @click="handleCheckIn">今日签到</el-button>
      </div>

      <el-form :inline="true" size="small" class="filter-form">
        <el-form-item label="学生姓名">
          <el-input v-model="filterName" placeholder="输入姓名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable>
            <el-option label="出勤" value="present" />
            <el-option label="迟到" value="late" />
            <el-option label="缺勤" value="absent" />
            <el-option label="请假" value="leave" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="fetchList">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="attendanceList" border fit stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="majorName" label="专业" width="180" />
        <el-table-column prop="courseName" label="课程" width="160" />
        <el-table-column prop="attendanceDate" label="日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="{row}">
            <el-tag v-if="row.status === '出勤'" type="success">出勤</el-tag>
            <el-tag v-else-if="row.status === '迟到'" type="warning">迟到</el-tag>
            <el-tag v-else-if="row.status === '缺勤'" type="danger">缺勤</el-tag>
            <el-tag v-else type="info">请假</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkInTime" label="签到时间" width="160" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import attendanceApi from '@/api/attendance'

// 本地 mock 数据（后端不可用时作为兜底，也可直接本地筛选）
const MOCK_DATA = [
  { id: 1, studentName: '张明', majorName: '计算机科学与技术', courseName: '深度学习实战', attendanceDate: '2026-03-15', status: '出勤', checkInTime: '08:15', remark: '' },
  { id: 2, studentName: '李华', majorName: '软件工程', courseName: '大模型应用开发', attendanceDate: '2026-03-15', status: '迟到', checkInTime: '08:32', remark: '交通拥堵' },
  { id: 3, studentName: '王芳', majorName: '数据科学与大数据', courseName: 'Python程序设计', attendanceDate: '2026-03-15', status: '出勤', checkInTime: '08:10', remark: '' },
  { id: 4, studentName: '赵磊', majorName: '计算机科学与技术', courseName: '模型部署与推理加速', attendanceDate: '2026-03-15', status: '缺勤', checkInTime: '', remark: '未请假' },
  { id: 5, studentName: '陈静', majorName: '人工智能', courseName: '机器学习导论', attendanceDate: '2026-03-14', status: '请假', checkInTime: '', remark: '病假已批准' }
]

export default {
  name: 'AttendanceList',
  data () {
    return {
      loading: false,
      filterName: '',
      filterStatus: '',
      attendanceList: [...MOCK_DATA]
    }
  },
  methods: {
    fetchList () {
      this.loading = true
      // 优先调后端
      attendanceApi.page({ userName: this.filterName, status: this.filterStatus, pageIndex: 1, pageSize: 20 })
        .then(res => {
          var list = (res && res.response && res.response.list) ? res.response.list : null
          if (list && list.length) {
            this.attendanceList = list
            return
          }
          // 后端返回空 → 用本地数据筛选
          this.doLocalFilter()
        })
        .catch(() => {
          // 后端不可用 → 用本地数据筛选
          this.doLocalFilter()
        })
        .finally(() => { this.loading = false })
    },
    doLocalFilter () {
      var name = (this.filterName || '').trim().toLowerCase()
      var statusMap = { 'present': '出勤', 'late': '迟到', 'absent': '缺勤', 'leave': '请假' }
      var statusLabel = statusMap[this.filterStatus] || ''
      this.attendanceList = MOCK_DATA.filter(function (item) {
        var matchName = !name || item.studentName.toLowerCase().includes(name)
        var matchStatus = !statusLabel || item.status === statusLabel
        return matchName && matchStatus
      })
    },
    handleCheckIn () {
      attendanceApi.checkToday().then(() => {
        this.$message.success('签到成功')
        this.fetchList()
      }).catch(() => { this.$message.warning('请先登录后再签到') })
    }
  }
}
</script>

<style scoped>
.app-container { padding: 24px; background-color: var(--ink-3); min-height: calc(100vh - 64px); }
.main-card { border-radius: 8px; border: none; }
.card-header { display: flex; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f0f2f5; }
.header-icon { color: var(--acid); font-size: 18px; margin-right: 8px; }
.header-title { font-size: 18px; font-weight: 600; color: var(--paper); flex: 1; }
.filter-form { padding: 16px 20px 0; }
</style>
