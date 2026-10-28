<template>
  <div class="exam-page">
    <!-- 页面头部 -->
    <div class="exam-header">
      <div class="exam-header-left">
        <div class="exam-header-icon">
          <i class="el-icon-document"></i>
        </div>
        <div>
          <h2 class="exam-title">试卷管理</h2>
          <p class="exam-subtitle">管理试卷库 · 创建、编辑、删除试卷</p>
        </div>
      </div>
      <router-link :to="{path:'/exam/paper/edit'}">
        <el-button type="primary" icon="el-icon-plus" class="exam-add-btn">新建试卷</el-button>
      </router-link>
    </div>

    <!-- 筛选卡片 -->
    <div class="exam-filter-card">
      <el-form :model="queryParam" ref="queryForm" :inline="true" class="exam-filter-form">
        <el-form-item label="题目ID">
          <el-input v-model="queryParam.id" clearable placeholder="输入ID" style="width:120px"></el-input>
        </el-form-item>
        <el-form-item label="学历层次">
          <el-select v-model="queryParam.educationLevel" placeholder="全部" clearable style="width:130px">
            <el-option v-for="item in educationLevelEnum" :key="item.key" :value="item.key" :label="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="学科门类">
          <el-select v-model="queryParam.category" placeholder="全部" clearable style="width:130px">
            <el-option v-for="item in categoryEnum" :key="item.key" :label="item.value" :value="item.key"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="queryParam.subjectId" clearable filterable placeholder="搜索课程" style="width:200px">
            <el-option v-for="item in subjectFilter" :key="item.id" :value="item.id" :label="item.name + (item.levelName ? ' ('+item.levelName+')' : '')"></el-option>
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
        <span class="exam-table-count">共 {{ total }} 份试卷</span>
      </div>
      <el-table v-loading="listLoading" :data="tableData" fit highlight-current-row style="width: 100%" class="exam-table">
        <el-table-column prop="id" label="ID" width="80px" align="center"/>
        <el-table-column prop="subjectId" label="学科" :formatter="subjectFormatter" width="120px" align="center">
          <template slot-scope="{row}">
            <span class="exam-subject-tag">{{ subjectFormatter(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="试卷名称" min-width="200px" show-overflow-tooltip>
          <template slot-scope="{row}">
            <span class="exam-name-text">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170px" align="center">
          <template slot-scope="{row}">
            <span class="exam-time-text">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="170px">
          <template slot-scope="{row}">
            <el-button size="mini" @click="$router.push({path:'/exam/paper/edit',query:{id:row.id}})" icon="el-icon-edit" class="exam-action-edit">编辑</el-button>
            <el-button size="mini" type="danger" @click="deletePaper(row)" icon="el-icon-delete" class="exam-action-del">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParam.pageIndex" :limit.sync="queryParam.pageSize" @pagination="search" class="exam-pagination"/>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapState, mapActions } from 'vuex'
import Pagination from '@/components/Pagination'
import examPaperApi from '@/api/examPaper'

export default {
  components: { Pagination },
  data () {
    return {
      queryParam: {
        id: null,
        level: null,
        subjectId: null,
        pageIndex: 1,
        pageSize: 10
      },
      subjectFilter: null,
      listLoading: true,
      tableData: [],
      total: 0
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
    submitForm () {
      this.queryParam.pageIndex = 1
      this.search()
    },
    search () {
      this.listLoading = true
      examPaperApi.pageList(this.queryParam).then(data => {
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
    deletePaper (row) {
      let _this = this
      examPaperApi.deletePaper(row.id).then(re => {
        if (re.code === 1) {
          _this.search()
          _this.$message.success(re.message)
        } else {
          _this.$message.error(re.message)
        }
      })
    },
    levelChange () {
      this.queryParam.subjectId = null
      if (this.queryParam.level) {
        this.subjectFilter = this.subjects.filter(data => data.subjectType >= 3 && (data.level === this.queryParam.level || data.level == null))
      } else {
        this.subjectFilter = this.subjects.filter(function(d){ return d.subjectType >= 3 })
      }
    },
    subjectFormatter  (row, column, cellValue, index) {
      return this.subjectEnumFormat(cellValue)
    },
    ...mapActions('exam', { initSubject: 'initSubject' })
  },
  computed: {
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapState('enumItem', {
      educationLevelEnum: state => state.user.educationLevelEnum,
      levelEnum: state => state.user.levelEnum,
      categoryEnum: state => state.user.categoryEnum
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
.exam-name-text {
  color: #1e293b;
  font-weight: 500;
}
.exam-time-text {
  color: #94a3b8;
  font-size: 12px;
}

/* 操作按钮 */
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
