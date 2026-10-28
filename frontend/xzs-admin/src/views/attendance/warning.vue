<template>
  <div class="app-container">
    <el-card shadow="hover" class="main-card">
      <div slot="header" class="card-header">
        <i class="el-icon-warning header-icon"></i>
        <span class="header-title">缺勤预警</span>
      </div>

      <div class="warning-section">
        <el-alert
          title="预警规则：近7天旷课 ≥ 3 次或迟到/早退 ≥ 5 次的学生将被预警。预警信息同步推送至辅导员。"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-table :data="warningTableData" border fit highlight-current-row style="width: 100%">
          <el-table-column prop="studentName" label="学生姓名" width="120" />
          <el-table-column prop="majorName" label="专业" width="180" />
          <el-table-column prop="grade" label="年级" width="100" />
          <el-table-column prop="absentCount" label="近7天旷课次数" width="140">
            <template slot-scope="{row}">
              <span class="danger-text">{{ row.absentCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="lateCount" label="近7天迟到/早退" width="140">
            <template slot-scope="{row}">
              <span class="warning-text">{{ row.lateCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="lastAttendDate" label="最近考勤日期" width="130" />
          <el-table-column label="操作" width="180">
            <template slot-scope="{row}">
              <el-button type="text" size="small" @click="viewDetail(row)">查看详情</el-button>
              <el-button type="text" size="small" @click="notifyAdvisor(row)">通知辅导员</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  data () {
    return {
      warningTableData: [
        { studentName: '赵磊', majorName: '计算机科学与技术', grade: '大三', absentCount: 4, lateCount: 3, lastAttendDate: '2026-05-18' },
        { studentName: '孙浩', majorName: '软件工程', grade: '大二', absentCount: 2, lateCount: 6, lastAttendDate: '2026-05-20' },
        { studentName: '周婷', majorName: '人工智能', grade: '大三', absentCount: 3, lateCount: 4, lastAttendDate: '2026-05-19' }
      ]
    }
  },
  methods: {
    viewDetail (row) {
      this.$message.info('查看 ' + row.studentName + ' 的详细考勤记录')
    },
    notifyAdvisor (row) {
      this.$confirm('确认通知 ' + row.studentName + ' 的辅导员？', '发送预警通知', { type: 'warning' }).then(() => {
        this.$message.success('已向辅导员发送 ' + row.studentName + ' 的缺勤预警通知')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.app-container { padding: 24px; background-color: var(--ink-3); min-height: calc(100vh - 64px); }
.main-card { border-radius: 8px; border: none; }
.card-header { display: flex; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f0f2f5; }
.header-icon { color: var(--acid); font-size: 18px; margin-right: 8px; }
.header-title { font-size: 18px; font-weight: 600; color: var(--paper); }
.warning-section { padding: 0 20px; }
.warning-text { color: #e6a23c; font-weight: 600; }
.danger-text { color: #f56c6c; font-weight: 600; }
</style>
