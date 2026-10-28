<template>
  <div class="app-container">
    <el-card shadow="hover" class="main-card">
      <div slot="header" class="card-header">
        <i class="el-icon-data-line header-icon"></i>
        <span class="header-title">出勤统计</span>
      </div>

      <div class="statistics-section">
        <el-row :gutter="20" class="stat-cards">
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-number">{{ totalStudents }}</div>
                <div class="stat-label">学生总数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-number success-text">{{ normalCount }}</div>
                <div class="stat-label">正常出勤</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-number warning-text">{{ lateCount }}</div>
                <div class="stat-label">迟到/早退</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-number danger-text">{{ absentCount }}</div>
                <div class="stat-label">旷课</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-table :data="statisticsTableData" border fit highlight-current-row style="width: 100%; margin-top: 20px">
          <el-table-column prop="majorName" label="专业" width="200" />
          <el-table-column prop="totalStudents" label="人数" width="80" />
          <el-table-column prop="normalRate" label="出勤率" width="100">
            <template slot-scope="{row}">{{ row.normalRate }}%</template>
          </el-table-column>
          <el-table-column prop="lateCount" label="迟到人次" width="100" />
          <el-table-column prop="earlyLeaveCount" label="早退人次" width="100" />
          <el-table-column prop="absentCount" label="旷课人次" width="100" />
          <el-table-column prop="leaveCount" label="请假人次" width="100" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  data () {
    return {
      totalStudents: 186,
      normalCount: 172,
      lateCount: 10,
      absentCount: 4,
      statisticsTableData: [
        { majorName: '计算机科学与技术', totalStudents: 62, normalRate: 94, lateCount: 3, earlyLeaveCount: 1, absentCount: 1, leaveCount: 2 },
        { majorName: '软件工程', totalStudents: 55, normalRate: 93, lateCount: 4, earlyLeaveCount: 2, absentCount: 2, leaveCount: 1 },
        { majorName: '数据科学与大数据', totalStudents: 38, normalRate: 97, lateCount: 2, earlyLeaveCount: 1, absentCount: 0, leaveCount: 1 },
        { majorName: '人工智能', totalStudents: 31, normalRate: 91, lateCount: 1, earlyLeaveCount: 1, absentCount: 1, leaveCount: 2 }
      ]
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
.statistics-section { padding: 0 20px; }
.stat-cards { margin-bottom: 20px; }
.stat-card { text-align: center; cursor: pointer; }
.stat-content { padding: 20px; }
.stat-number { font-size: 32px; font-weight: 600; margin-bottom: 8px; }
.stat-label { font-size: 14px; color: var(--grey); }
.success-text { color: #67c23a; }
.warning-text { color: #e6a23c; }
.danger-text { color: #f56c6c; }
</style>
