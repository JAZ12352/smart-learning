<template>
  <div class="app-container">
    <el-card shadow="hover" class="main-card">
      <div slot="header" class="card-header">
        <i class="el-icon-edit header-icon"></i>
        <span class="header-title">请假审批</span>
      </div>

      <el-table v-loading="leaveLoading" :data="leaveTableData" border fit highlight-current-row style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="majorName" label="专业" width="180" />
        <el-table-column prop="leaveType" label="请假类型" width="100">
          <template slot-scope="{row}">
            <el-tag v-if="row.leaveType === '病假'" type="warning">病假</el-tag>
            <el-tag v-else-if="row.leaveType === '事假'" type="info">事假</el-tag>
            <el-tag v-else type="default">其他</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="reason" label="请假原因" min-width="200" />
        <el-table-column label="审批状态" width="100">
          <template slot-scope="{row}">
            <el-tag v-if="row.status === 'pending'" type="warning">待审批</el-tag>
            <el-tag v-else-if="row.status === 'approved'" type="success">已通过</el-tag>
            <el-tag v-else-if="row.status === 'rejected'" type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="{row}">
            <el-button v-if="row.status === 'pending'" type="text" size="small" style="color:#67c23a" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'pending'" type="text" size="small" style="color:#f56c6c" @click="handleReject(row)">拒绝</el-button>
            <el-button type="text" size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import attendanceApi from '@/api/attendance'

export default {
  data () {
    return {
      leaveLoading: false,
      leaveTableData: [
        { id: 1, studentName: '张明', majorName: '计算机科学与技术', leaveType: '病假', startDate: '2026-05-20', endDate: '2026-05-21', reason: '感冒发烧需就医', status: 'pending' },
        { id: 2, studentName: '李华', majorName: '软件工程', leaveType: '事假', startDate: '2026-05-22', endDate: '2026-05-22', reason: '参加ACM省赛', status: 'pending' },
        { id: 3, studentName: '王芳', majorName: '数据科学与大数据', leaveType: '病假', startDate: '2026-05-18', endDate: '2026-05-19', reason: '牙科手术复查', status: 'approved' }
      ]
    }
  },
  methods: {
    handleApprove (row) {
      this.$confirm('确认通过 ' + row.studentName + ' 的请假申请？', '审批确认', { type: 'success' }).then(() => {
        attendanceApi.approveLeave({ id: row.id, status: 'approved', approvalRemark: '同意' }).then(() => {
          row.status = 'approved'
          this.$message.success('已通过')
        })
      })
    },
    handleReject (row) {
      this.$prompt('请输入拒绝原因', '审批拒绝', { inputType: 'textarea' }).then(({ value }) => {
        attendanceApi.approveLeave({ id: row.id, status: 'rejected', approvalRemark: value }).then(() => {
          row.status = 'rejected'
          this.$message.success('已拒绝')
        })
      }).catch(() => {})
    },
    viewDetail (row) {
      const statusMap = { pending: '待审批', approved: '已通过', rejected: '已拒绝' }
      this.$alert(
        '学生：' + row.studentName + '\n专业：' + row.majorName + '\n类型：' + row.leaveType +
        '\n日期：' + row.startDate + ' ~ ' + row.endDate + '\n原因：' + row.reason +
        '\n状态：' + (statusMap[row.status] || row.status),
        '请假详情'
      )
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
</style>
