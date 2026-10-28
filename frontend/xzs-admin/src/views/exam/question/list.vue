<template>
  <div class="exam-page">
    <!-- 页面头部 -->
    <div class="exam-header">
      <div class="exam-header-left">
        <div class="exam-header-icon">
          <i class="el-icon-edit-outline"></i>
        </div>
        <div>
          <h2 class="exam-title">题目管理</h2>
          <p class="exam-subtitle">管理题库 · 创建、编辑、删除题目</p>
        </div>
      </div>
      <el-popover placement="bottom" trigger="click" popper-class="question-add-popover">
        <div class="question-add-grid">
          <el-button
            v-for="item in editUrlEnum"
            :key="item.key"
            @click="$router.push({path:item.value})"
            class="question-add-item">
            <i class="el-icon-plus"></i>
            <span>{{item.name}}</span>
          </el-button>
        </div>
        <el-button type="primary" icon="el-icon-plus" slot="reference" class="exam-add-btn">新建题目</el-button>
      </el-popover>
    </div>

    <!-- 筛选卡片 -->
    <div class="exam-filter-card">
      <el-form :model="queryParam" ref="queryForm" :inline="true" class="exam-filter-form">
        <el-form-item label="题目ID">
          <el-input v-model="queryParam.id" clearable placeholder="输入ID" style="width:120px"></el-input>
        </el-form-item>
        <el-form-item label="题目内容">
          <el-input v-model="queryParam.content" clearable placeholder="输入关键字" style="width:180px"></el-input>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="queryParam.level" placeholder="全部" @change="levelChange" clearable style="width:130px">
            <el-option v-for="item in levelEnum" :key="item.key" :value="item.key" :label="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="queryParam.subjectId" clearable filterable placeholder="搜索学科" style="width:200px">
            <el-option v-for="item in subjectFilter" :key="item.id" :value="item.id"
                       :label="item.name+' ( '+item.levelName+' )'"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="queryParam.questionType" clearable placeholder="全部" style="width:140px">
            <el-option v-for="item in questionType" :key="item.key" :value="item.key" :label="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitForm" icon="el-icon-search">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格卡片 -->
    <div class="exam-table-card">
      <div class="exam-table-header">
        <span class="exam-table-count">共 {{ total }} 道题目</span>
      </div>
      <el-table v-loading="listLoading" :data="tableData" fit highlight-current-row style="width: 100%" class="exam-table">
        <el-table-column prop="id" label="ID" width="80px" align="center"/>
        <el-table-column prop="subjectId" label="学科" :formatter="subjectFormatter" width="130px" align="center">
          <template slot-scope="{row}">
            <span class="exam-subject-tag">{{ subjectFormatter(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="questionType" label="题型" :formatter="questionTypeFormatter" width="110px" align="center">
          <template slot-scope="{row}">
            <span class="exam-type-tag">{{ questionTypeFormatter(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="shortTitle" label="题干" min-width="220px" show-overflow-tooltip>
          <template slot-scope="{row}">
            <span class="exam-name-text">{{ row.shortTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="80px" align="center">
          <template slot-scope="{row}">
            <span class="exam-score-text">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="difficult" label="难度" width="90px" align="center">
          <template slot-scope="{row}">
            <span class="exam-diff-tag" :class="difficultClass(row.difficult)">{{ difficultText(row.difficult) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170px" align="center">
          <template slot-scope="{row}">
            <span class="exam-time-text">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="240px">
          <template slot-scope="{row}">
            <el-button size="mini" @click="showQuestion(row)" icon="el-icon-view" class="exam-action-view">预览</el-button>
            <el-button size="mini" @click="editQuestion(row)" icon="el-icon-edit" class="exam-action-edit">编辑</el-button>
            <el-button size="mini" type="danger" @click="deleteQuestion(row)" icon="el-icon-delete" class="exam-action-del">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParam.pageIndex" :limit.sync="queryParam.pageSize"
                  @pagination="search" class="exam-pagination"/>
    </div>

    <el-dialog :visible.sync="questionShow.dialog" style="width: 100%;height: 100%">
      <QuestionShow :qType="questionShow.qType" :question="questionShow.question" :qLoading="questionShow.loading"/>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters, mapState, mapActions } from 'vuex'
import Pagination from '@/components/Pagination'
import QuestionShow from './components/Show'
import questionApi from '@/api/question'

export default {
  components: { Pagination, QuestionShow },
  data () {
    return {
      queryParam: {
        id: null,
        questionType: null,
        level: null,
        subjectId: null,
        pageIndex: 1,
        pageSize: 10
      },
      subjectFilter: null,
      listLoading: true,
      tableData: [],
      total: 0,
      questionShow: {
        qType: 0,
        dialog: false,
        question: null,
        loading: false
      }
    }
  },
  created () {
    this.initSubject()
    this.search()
  },
  watch: {
    subjects: {
      handler (val) { if (val && val.length) this.subjectFilter = val.filter(function(d){ return d.subjectType >= 3 }) },
      immediate: true
    }
  },
  methods: {
    addQuestionWithType (url, questionType) {
      this.$router.push({
        path: url,
        query: {
          questionType: questionType
        }
      })
    },
    submitForm () {
      this.queryParam.pageIndex = 1
      this.search()
    },
    search () {
      this.listLoading = true
      questionApi.pageList(this.queryParam).then(data => {
        const re = data.response
        if (re) {
          this.tableData = re.list || []
          this.total = re.total || 0
          this.queryParam.pageIndex = re.pageNum || 1
        } else {
          this.tableData = []
          this.total = 0
        }
        this.listLoading = false
      })
    },
    levelChange () {
      this.queryParam.subjectId = null
      if (this.queryParam.level) {
        // 仅显示专业和课程(subjectType>=3)，匹配年级或未设年级
        this.subjectFilter = this.subjects.filter(data =>
          data.subjectType >= 3 && (data.level === this.queryParam.level || data.level == null)
        )
      } else {
        this.subjectFilter = this.subjects.filter(function(d){ return d.subjectType >= 3 })
      }
    },
    addQuestion () {
      this.$router.push('/exam/question/edit/singleChoice')
    },
    showQuestion (row) {
      let _this = this
      this.questionShow.dialog = true
      this.questionShow.loading = true
      questionApi.select(row.id).then(re => {
        _this.questionShow.qType = re.response.questionType
        _this.questionShow.question = re.response
        _this.questionShow.loading = false
      })
    },
    editQuestion (row) {
      let url = this.enumFormat(this.editUrlEnum, row.questionType)
      this.$router.push({ path: url, query: { id: row.id } })
    },
    deleteQuestion (row) {
      let _this = this
      questionApi.deleteQuestion(row.id).then(re => {
        if (re.code === 1) {
          _this.search()
          _this.$message.success(re.message)
        } else {
          _this.$message.error(re.message)
        }
      })
    },
    questionTypeFormatter (row, column, cellValue, index) {
      return this.enumFormat(this.questionType, cellValue)
    },
    subjectFormatter (row, column, cellValue, index) {
      return this.subjectEnumFormat(cellValue)
    },
    difficultText (val) {
      const map = { 1: '简单', 2: '中等', 3: '困难', 4: '挑战' }
      return map[val] || '-'
    },
    difficultClass (val) {
      const map = { 1: 'diff-easy', 2: 'diff-mid', 3: 'diff-hard', 4: 'diff-expert' }
      return map[val] || ''
    },
    ...mapActions('exam', { initSubject: 'initSubject' })
  },
  computed: {
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapState('enumItem', {
      questionType: state => state.exam.question.typeEnum,
      editUrlEnum: state => state.exam.question.editUrlEnum,
      levelEnum: state => state.user.levelEnum
    }),
    ...mapGetters('exam', ['subjectEnumFormat']),
    ...mapState('exam', { subjects: state => state.subjects })
  }
}
</script>

<style scoped>
.exam-page {
  padding: 20px 24px;
}

/* 头部 */
.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.exam-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.exam-header-icon {
  width: 46px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--ink-4), var(--acid));
  border-radius: 12px;
  color: #fff;
  font-size: 22px;
  box-shadow: 0 4px 12px rgba(var(--acid-rgb), 0.3);
}
.exam-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}
.exam-subtitle {
  margin: 2px 0 0;
  font-size: 13px;
  color: #94a3b8;
}
.exam-add-btn {
  background: linear-gradient(135deg, var(--acid), var(--ice));
  border: none;
  border-radius: 10px;
  font-weight: 600;
  padding: 10px 20px;
  box-shadow: 0 4px 12px rgba(var(--acid-rgb), 0.25);
  transition: all 0.2s;
}
.exam-add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--acid-rgb), 0.35);
}

/* 筛选卡片 */
.exam-filter-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 24px;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.exam-filter-form >>> .el-form-item {
  margin-bottom: 0;
  margin-right: 16px;
}
.exam-filter-form >>> .el-form-item__label {
  color: var(--grey);
  font-weight: 500;
  font-size: 13px;
}
.exam-filter-form >>> .el-input__inner,
.exam-filter-form >>> .el-select .el-input__inner {
  border-radius: 8px;
  border-color: var(--ink-5);
  transition: all 0.2s;
}
.exam-filter-form >>> .el-input__inner:focus {
  border-color: var(--ink-5);
  box-shadow: 0 0 0 3px rgba(var(--acid-rgb), 0.08);
}

/* 表格卡片 */
.exam-table-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 24px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.exam-table-header {
  margin-bottom: 12px;
}
.exam-table-count {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

/* 表格样式 */
.exam-table >>> .el-table__header th {
  background: var(--ink-3);
  color: var(--grey);
  font-weight: 600;
  font-size: 13px;
  border-bottom: 2px solid #e2e8f0;
}
.exam-table >>> .el-table__body tr {
  transition: background 0.18s;
}
.exam-table >>> .el-table__body tr:hover > td {
  background: var(--ink-3) !important;
}
.exam-table >>> .el-table__body td {
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}
.exam-table >>> .el-table__body tr:last-child td {
  border-bottom: none;
}

/* 标签和文字 */
.exam-subject-tag {
  display: inline-block;
  padding: 2px 10px;
  background: rgba(var(--acid-rgb), 0.08);
  color: var(--acid);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}
.exam-type-tag {
  display: inline-block;
  padding: 2px 10px;
  background: rgba(32, 40, 32, 0.08);
  color: #1e3a5f;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}
.exam-name-text {
  color: #1e293b;
  font-weight: 500;
}
.exam-score-text {
  color: #1e3a5f;
  font-weight: 700;
  font-size: 14px;
}
.exam-time-text {
  color: #94a3b8;
  font-size: 12px;
}

/* 难度标签 */
.exam-diff-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}
.exam-diff-tag.diff-easy {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}
.exam-diff-tag.diff-mid {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}
.exam-diff-tag.diff-hard {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}
.exam-diff-tag.diff-expert {
  background: rgba(var(--acid-rgb), 0.1);
  color: var(--acid);
}

/* 操作按钮 */
.exam-action-view {
  border-color: var(--ink-5);
  color: var(--grey);
  border-radius: 7px;
  transition: all 0.18s;
}
.exam-action-view:hover {
  background: #64748b;
  border-color: #64748b;
  color: #fff;
}
.exam-action-edit {
  border-color: var(--ink-5);
  color: var(--acid);
  border-radius: 7px;
  transition: all 0.18s;
}
.exam-action-edit:hover {
  background: var(--ink-3);
  border-color: var(--ink-5);
  color: #fff;
}
.exam-action-del {
  border-radius: 7px;
}

/* 分页 */
.exam-pagination {
  margin-top: 16px;
  text-align: right;
}
</style>

<style>
/* 添加题目弹出层（非 scoped） */
.question-add-popover {
  padding: 12px !important;
  border-radius: 12px !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow: 0 8px 24px rgba(10, 13, 10, 0.12) !important;
}
.question-add-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  min-width: 240px;
}
.question-add-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  border: 1px solid #e2e8f0 !important;
  background: var(--ink-3) !important;
  color: var(--grey) !important;
  border-radius: 8px !important;
  padding: 8px 12px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  transition: all 0.18s !important;
}
.question-add-item i {
  color: var(--acid);
  font-weight: 700;
}
.question-add-item:hover {
  background: var(--ink-3) !important;
  border-color: var(--ink-5) !important;
  color: #fff !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(var(--acid-rgb), 0.25);
}
.question-add-item:hover i {
  color: #fff;
}
</style>
