<template>
  <div class="app-container">
    <el-form :model="queryParam" ref="queryForm" :inline="true">
      <el-form-item label="用户名：">
        <el-input v-model="queryParam.userName" @keyup.enter="submitForm"></el-input>
      </el-form-item>
      <el-form-item label="专业：">
        <el-cascader
          v-model="majorFilter"
          :options="cascadeOptions"
          :props="{ label: 'name', value: 'id', children: 'children', checkStrictly: true }"
          placeholder="门类→专业类→专业"
          clearable
          style="width:240px"
          @change="onMajorFilterChange"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">查询</el-button>
        <router-link :to="{path:'/user/student/edit'}" class="link-left">
          <el-button type="primary">添加</el-button>
        </router-link>
      </el-form-item>
    </el-form>

    <el-table v-loading="listLoading" :data="tableData" border fit highlight-current-row style="width: 100%">
      <el-table-column prop="id" label="Id" width="60" />
      <el-table-column prop="userName" label="用户名" width="120"/>
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="userLevel" label="年级" width="80" :formatter="levelFormatter"/>
      <el-table-column label="专业" width="200">
        <template slot-scope="{row}">
          <el-tag v-if="row.majorName" size="small" closable @close="clearMajor(row)">{{ row.majorName }}</el-tag>
          <span v-else style="color:#c0c4cc">未设置</span>
        </template>
      </el-table-column>
      <el-table-column prop="sex" label="性别" width="60" :formatter="sexFormatter"/>
      <el-table-column prop="phone" label="手机号" width="130"/>
      <el-table-column prop="createTime" label="创建时间" width="160"/>
      <el-table-column label="状态" prop="status" width="70">
        <template slot-scope="{row}">
          <el-tag :type="statusTagFormatter(row.status)">{{ statusFormatter(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column width="420" label="操作" align="center">
        <template slot-scope="{row}">
          <el-button size="mini" @click="showMajorDialog(row)">设置专业</el-button>
          <el-button size="mini" type="warning" @click="showRequiredCourses(row)">必修课程</el-button>
          <el-button size="mini" type="success" @click="showTextbookResources(row)">电子课本</el-button>
          <el-button size="mini" @click="changeStatus(row)">
            {{ statusBtnFormatter(row.status) }}
          </el-button>
          <router-link :to="{path:'/user/student/edit', query:{id:row.id}}" class="link-left">
            <el-button size="mini">编辑</el-button>
          </router-link>
          <el-button size="mini" type="danger" @click="deleteUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParam.pageIndex" :limit.sync="queryParam.pageSize" @pagination="search"/>

    <!-- 设置专业弹窗 -->
    <el-dialog title="设置专业" :visible.sync="majorDialogVisible" width="500px">
      <div style="margin-bottom:12px;color:#606266">
        为学生 <b>{{ majorTarget?.realName }}</b> 选择专业：
      </div>
      <el-cascader
        v-model="selectedMajorPath"
        :options="cascadeOptions"
        :props="{ label: 'name', value: 'id', children: 'children', checkStrictly: true, expandTrigger: 'hover' }"
        placeholder="请选择门类 → 专业类 → 专业"
        clearable
        style="width:100%"
      />
      <span slot="footer">
        <el-button @click="majorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMajor" :loading="majorSaving">保存</el-button>
      </span>
    </el-dialog>

    <!-- 必修课程弹窗 -->
    <el-dialog title="必修课程" :visible.sync="courseDialogVisible" width="780px" top="5vh" :before-close="handleDialogClose('courseDialogVisible')">
      <div v-if="courseTarget" style="margin-bottom:16px;padding:12px 16px;background:#f0f5ff;border-radius:8px;border-left:4px solid #6366f1">
        <b style="font-size:16px">{{ courseTarget.realName || courseTarget.userName }}</b>
        <span style="margin-left:12px;color:#64748b">{{ courseTarget.majorName || '未设置专业' }}</span>
        <span v-if="courseTarget.majorName" style="margin-left:8px;color:#94a3b8;font-size:13px">· {{ courseTarget.userLevel ? levelMap[courseTarget.userLevel] : '' }}年级</span>
      </div>

      <!-- 专业必修课程 -->
      <div v-if="majorRequiredCourses.length > 0" style="margin-bottom:20px">
        <h4 style="font-size:15px;color:#1e293b;margin:0 0 10px;padding-left:8px;border-left:3px solid #6366f1">
          📚 专业必修课程（{{ courseTarget?.majorName }}）
        </h4>
        <el-table :data="majorRequiredCourses" size="small" stripe max-height="280">
          <el-table-column prop="name" label="课程名称" min-width="160"/>
          <el-table-column prop="credit" label="学分" width="60"/>
          <el-table-column prop="hours" label="学时" width="60"/>
          <el-table-column prop="term" label="开课学期" width="100"/>
          <el-table-column prop="textbook" label="使用教材" min-width="160" show-overflow-tooltip/>
          <el-table-column label="电子资源" width="100">
            <template slot-scope="{row}">
              <el-button v-if="row.hasResource" size="mini" type="text" icon="el-icon-view" @click="viewResource(row)">查看</el-button>
              <span v-else style="color:#c0c4cc;font-size:12px">暂无</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 公共必修课程 -->
      <div v-if="generalRequiredCourses.length > 0">
        <h4 style="font-size:15px;color:#1e293b;margin:0 0 10px;padding-left:8px;border-left:3px solid #f59e0b">
          📖 公共必修课程
        </h4>
        <el-table :data="generalRequiredCourses" size="small" stripe>
          <el-table-column prop="name" label="课程名称" min-width="160"/>
          <el-table-column prop="credit" label="学分" width="60"/>
          <el-table-column prop="hours" label="学时" width="60"/>
          <el-table-column prop="term" label="开课学期" width="100"/>
          <el-table-column prop="textbook" label="使用教材" min-width="160" show-overflow-tooltip/>
          <el-table-column label="电子资源" width="100">
            <template slot-scope="{row}">
              <el-button v-if="row.hasResource" size="mini" type="text" icon="el-icon-view" @click="viewResource(row)">查看</el-button>
              <span v-else style="color:#c0c4cc;font-size:12px">暂无</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="majorRequiredCourses.length === 0 && generalRequiredCourses.length === 0" style="text-align:center;padding:40px;color:#94a3b8">
        <i class="el-icon-notebook-2" style="font-size:40px;display:block;margin-bottom:12px"></i>
        <p>正在加载课程数据...</p>
      </div>
    </el-dialog>

    <!-- 电子课本资源弹窗 -->
    <el-dialog title="电子课本" :visible.sync="textbookDialogVisible" width="880px" top="3vh" :destroy-on-close="true" :before-close="handleDialogClose('textbookDialogVisible')">
      <div v-if="textbookTarget">
        <div style="display:flex;gap:16px;margin-bottom:16px;padding:12px;background:#f8fafc;border-radius:8px;align-items:center;flex-wrap:wrap">
          <span style="font-weight:600">{{ textbookTarget.realName || textbookTarget.userName }}</span>
          <el-tag size="small">{{ textbookTarget.majorName || '未设置专业' }}</el-tag>
          <el-select v-model="textbookSubjectFilter" placeholder="筛选科目" clearable size="small" style="width:160px" @change="loadTextbooks">
            <el-option v-for="s in textbookSubjects" :key="s" :label="s" :value="s"/>
          </el-select>
          <el-input v-model="textbookSearch" placeholder="搜索课本名称..." size="small" prefix-icon="el-icon-search" style="width:200px" @input="loadTextbooks"/>
        </div>

        <div v-loading="textbookLoading" style="min-height:200px">
          <div v-if="textbookList.length === 0 && !textbookLoading" style="text-align:center;padding:60px;color:#94a3b8">
            <i class="el-icon-notebook-1" style="font-size:48px;display:block;margin-bottom:12px"></i>
            <p>暂无电子课本资源</p>
            <el-button type="primary" size="small" style="margin-top:12px" @click="showImportPlatform = true">从教学平台导入</el-button>
          </div>

          <div v-else class="textbook-grid">
            <div v-for="book in textbookList" :key="book.id" class="textbook-card" @click="openTextbook(book)">
              <div class="textbook-cover">
                <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.name"/>
                <div v-else class="textbook-cover-placeholder">
                  <i class="el-icon-notebook-2"></i>
                </div>
              </div>
              <div class="textbook-info">
                <h4>{{ book.name }}</h4>
                <p>{{ book.author || '' }}{{ book.publisher ? ' · ' + book.publisher : '' }}</p>
                <div class="textbook-meta">
                  <span>{{ book.edition || '最新版' }}</span>
                  <el-button v-if="book.hasOnline" size="mini" type="text" icon="el-icon-view">在线预览</el-button>
                  <el-button v-if="book.hasDownload" size="mini" type="text" icon="el-icon-download">下载</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <span slot="footer" style="display:flex;justify-content:space-between;align-items:center">
        <el-button size="small" type="primary" icon="el-icon-plus" @click="showImportPlatform = true">从教学平台导入</el-button>
        <span style="color:#94a3b8;font-size:12px">支持 PDF / EPUB 格式在线阅读</span>
      </span>
    </el-dialog>

    <!-- 教学平台导入对话框 -->
    <el-dialog title="从教学平台导入资源" :visible.sync="showImportPlatform" width="800px" top="5vh">
      <el-tabs v-model="platformTab">
        <el-tab-pane label="中国大学MOOC" name="mooc">
          <div style="padding:20px;text-align:center;color:#94a3b8">
            <i class="el-icon-link" style="font-size:36px;display:block;margin-bottom:12px"></i>
            <p>输入课程链接或名称搜索</p>
            <el-input v-model="platformSearch" placeholder="例如：深度学习实战 李沐" size="large" style="max-width:400px;margin:12px auto">
              <el-button slot="append" icon="el-icon-search" @click="searchPlatform">搜索</el-button>
            </el-input>
            <div style="margin-top:20px;font-size:13px;color:#c0c4cc">支持导入：课程大纲、课件PPT、习题集、教材电子版</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="学堂在线" name="xuetang">
          <div style="padding:20px;text-align:center;color:#94a3b8">
            <i class="el-icon-link" style="font-size:36px;display:block;margin-bottom:12px"></i>
            <p>输入课程名称或关键字搜索</p>
            <el-input v-model="platformSearch" placeholder="搜索课程资源..." size="large" style="max-width:400px;margin:12px auto">
              <el-button slot="append" icon="el-icon-search" @click="searchPlatform">搜索</el-button>
            </el-input>
          </div>
        </el-tab-pane>
        <el-tab-pane label="手动上传" name="upload">
          <el-upload drag :action="uploadAction" multiple :show-file-list="true" accept=".pdf,.epub,.doc,.docx,.pptx,.mp4,.avi,.flv">
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">拖拽文件或 <em>点击选择</em></div>
            <div slot="tip" class="el-upload__tip">支持 PDF / EPUB / Word / PPT / MP4 格式，单个文件不超过 200MB</div>
          </el-upload>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 课程资源查看弹窗 -->
    <el-dialog :title="resourceViewTitle" :visible.sync="resourceViewVisible" width="900px" top="3vh" :destroy-on-close="true" :before-close="handleDialogClose('resourceViewVisible')">
      <div v-if="resourceViewType === 'pdf'" style="height:70vh">
        <iframe :src="resourceViewUrl" style="width:100%;height:100%;border:none;border-radius:4px"></iframe>
      </div>
      <div v-else-if="resourceViewType === 'video'" style="text-align:center;padding:20px">
        <video :src="resourceViewUrl" controls style="max-width:100%;max-height:60vh;border-radius:8px" />
      </div>
      <div v-else-if="resourceViewType === 'inline'" v-html="resourceViewContent" style="padding:4px"></div>
      <div v-else-if="resourceViewType === 'ppt'" style="padding:20px;text-align:center;color:#94a3b8">
        <i class="el-icon-presentation" style="font-size:48px;display:block;margin-bottom:12px"></i>
        <p>课件资源准备中</p>
      </div>
      <div v-else style="padding:40px;text-align:center;color:#94a3b8">
        <iframe :src="resourceViewUrl" style="width:100%;min-height:60vh;border:none;border-radius:4px"></iframe>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import Pagination from '@/components/Pagination'
import userApi from '@/api/user'
import subjectApi from '@/api/subject'

// 公共必修课程模板
const GENERAL_REQUIRED_COURSES = [
  { name: '大学英语', credit: 4, hours: 64, term: '第1-2学期', textbook: '新视野大学英语', hasResource: true, type: 'general' },
  { name: '高等数学', credit: 5, hours: 80, term: '第1-2学期', textbook: '高等数学（同济版）', hasResource: true, type: 'general' },
  { name: '思想政治理论', credit: 3, hours: 48, term: '第1-4学期', textbook: '马克思主义基本原理', hasResource: true, type: 'general' },
  { name: '大学体育', credit: 2, hours: 32, term: '第1-4学期', textbook: '大学体育教程', hasResource: false, type: 'general' },
  { name: '计算机基础', credit: 3, hours: 48, term: '第1学期', textbook: '大学计算机基础', hasResource: true, type: 'general' }
]

export default {
  components: { Pagination },
  data () {
    return {
      queryParam: { userName: '', role: 1, pageIndex: 1, pageSize: 10 },
      listLoading: true,
      tableData: [],
      total: 0,
      majorFilter: [],
      cascadeOptions: [],
      // 专业设置弹窗
      majorDialogVisible: false,
      majorTarget: null,
      selectedMajorPath: [],
      majorSaving: false,
      // 必修课程弹窗
      courseDialogVisible: false,
      courseTarget: null,
      majorRequiredCourses: [],
      generalRequiredCourses: [],
      levelMap: { 1: '大一', 2: '大二', 3: '大三', 4: '大四' },
      // 电子课本弹窗
      textbookDialogVisible: false,
      textbookTarget: null,
      textbookList: [],
      textbookSubjects: [],
      textbookSubjectFilter: '',
      textbookSearch: '',
      textbookLoading: false,
      // 导入平台
      showImportPlatform: false,
      platformTab: 'mooc',
      platformSearch: '',
      uploadAction: '/api/admin/knowledge-base/document/upload',
      // 资源查看
      resourceViewVisible: false,
      resourceViewTitle: '',
      resourceViewUrl: '',
      resourceViewType: 'pdf',
      resourceViewContent: ''
    }
  },
  created () {
    this.search()
    this.loadCascadeData()
  },
  methods: {
    search () {
      this.listLoading = true
      userApi.getUserPageList(this.queryParam).then(data => {
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
    async loadCascadeData () {
      try {
        const res = await subjectApi.getTree(2)
        if (res.code === 1) this.cascadeOptions = res.response || []
      } catch (e) {}
    },
    submitForm () {
      this.queryParam.pageIndex = 1
      this.search()
    },
    onMajorFilterChange (vals) {
      if (vals && vals.length > 0) {
        this.queryParam.majorId = vals[vals.length - 1]
      } else {
        this.queryParam.majorId = null
      }
      this.submitForm()
    },

    // ── 专业设置 ──
    showMajorDialog (row) {
      this.majorTarget = row
      this.selectedMajorPath = []
      this.majorDialogVisible = true
    },
    async saveMajor () {
      if (!this.majorTarget || this.selectedMajorPath.length === 0) {
        this.$message.warning('请选择专业')
        return
      }
      this.majorSaving = true
      try {
        const majorId = this.selectedMajorPath[this.selectedMajorPath.length - 1]
        const res = await userApi.updateMajor(this.majorTarget.id, majorId)
        if (res.code === 1) {
          this.$message.success('专业设置成功')
          this.majorDialogVisible = false
          this.search()
        } else {
          this.$message.error(res.message || '设置失败')
        }
      } catch (e) {
        this.$message.error('设置失败')
      } finally {
        this.majorSaving = false
      }
    },
    async clearMajor (row) {
      try {
        const res = await userApi.updateMajor(row.id, null)
        if (res.code === 1) {
          this.$message.success('已清除专业')
          this.search()
        }
      } catch (e) { this.$message.error('清除失败') }
    },

    // ── 必修课程 ──
    async showRequiredCourses (row) {
      this.courseTarget = row
      this.majorRequiredCourses = []
      this.generalRequiredCourses = []
      this.courseDialogVisible = true

      // 加载公共必修课程
      this.generalRequiredCourses = [...GENERAL_REQUIRED_COURSES]

      // 加载专业必修课程
      if (row.majorId) {
        try {
          const res = await subjectApi.getMajorCourses(row.majorId)
          if (res && res.code === 1 && res.response) {
            this.majorRequiredCourses = res.response.map(c => ({
              ...c,
              hasResource: c.hasResource !== false,
              type: 'major'
            }))
          } else {
            // 模拟专业课程数据
            this.majorRequiredCourses = this.getMockMajorCourses(row.majorName)
          }
        } catch (e) {
          this.majorRequiredCourses = this.getMockMajorCourses(row.majorName)
        }
      }
    },
    getMockMajorCourses (majorName) {
      const courseMap = {
        '计算机科学与技术': [
          { name: '数据结构与算法', credit: 4, hours: 64, term: '第2学期', textbook: '数据结构（C语言版）', hasResource: true },
          { name: '操作系统', credit: 4, hours: 64, term: '第3学期', textbook: '计算机操作系统', hasResource: true },
          { name: '计算机网络', credit: 3, hours: 48, term: '第4学期', textbook: '计算机网络（谢希仁）', hasResource: true },
          { name: '数据库系统原理', credit: 3, hours: 48, term: '第3学期', textbook: '数据库系统概论', hasResource: true },
          { name: '软件工程', credit: 3, hours: 48, term: '第5学期', textbook: '软件工程导论', hasResource: false }
        ],
        '软件工程': [
          { name: '软件需求工程', credit: 3, hours: 48, term: '第3学期', textbook: '需求工程', hasResource: true },
          { name: '软件设计模式', credit: 3, hours: 48, term: '第4学期', textbook: '设计模式', hasResource: true },
          { name: 'Web应用开发', credit: 4, hours: 64, term: '第3学期', textbook: 'Web开发技术', hasResource: true },
          { name: '软件测试', credit: 3, hours: 48, term: '第5学期', textbook: '软件测试方法', hasResource: false }
        ],
        '数学与应用数学': [
          { name: '数学分析', credit: 6, hours: 96, term: '第1-2学期', textbook: '数学分析（华东师大）', hasResource: true },
          { name: '高等代数', credit: 5, hours: 80, term: '第1-2学期', textbook: '高等代数（北大）', hasResource: true },
          { name: '概率论与数理统计', credit: 4, hours: 64, term: '第3学期', textbook: '概率论与数理统计', hasResource: true },
          { name: '运筹学', credit: 3, hours: 48, term: '第4学期', textbook: '运筹学', hasResource: false }
        ],
        '英语': [
          { name: '综合英语', credit: 4, hours: 64, term: '第1-4学期', textbook: '综合教程', hasResource: true },
          { name: '英语听力', credit: 2, hours: 32, term: '第1-2学期', textbook: '听力教程', hasResource: true },
          { name: '英语口语', credit: 2, hours: 32, term: '第1-2学期', textbook: '口语教程', hasResource: false },
          { name: '英汉翻译', credit: 3, hours: 48, term: '第3学期', textbook: '英汉翻译教程', hasResource: true }
        ]
      }
      // 模糊匹配
      for (const key of Object.keys(courseMap)) {
        if (majorName && majorName.includes(key)) return courseMap[key]
      }
      return [
        { name: '专业导论', credit: 2, hours: 32, term: '第1学期', textbook: '专业导论教程', hasResource: true },
        { name: '专业核心课程1', credit: 4, hours: 64, term: '第2学期', textbook: '核心教材', hasResource: true },
        { name: '专业核心课程2', credit: 3, hours: 48, term: '第3学期', textbook: '专业教材', hasResource: true },
        { name: '专业选修', credit: 2, hours: 32, term: '第4学期', textbook: '选修教材', hasResource: false }
      ]
    },

    // ── 电子课本 ──
    async showTextbookResources (row) {
      this.textbookTarget = row
      this.textbookList = []
      this.textbookSubjects = []
      this.textbookSubjectFilter = ''
      this.textbookSearch = ''
      this.textbookDialogVisible = true
      this.loadTextbooks()
    },
    async loadTextbooks () {
      this.textbookLoading = true
      try {
        // 模拟课本数据 — 后续对接后端API
        const allBooks = [
          { id: 1, name: '动手学深度学习（第2版）', author: '阿斯顿·张等', publisher: '人民邮电出版社', edition: '第2版', coverUrl: '', hasOnline: true, hasDownload: true, subject: 'AI岗位技能' },
          { id: 2, name: '大模型应用开发实战', author: 'DataWhale', publisher: '电子工业出版社', edition: '第1版', coverUrl: '', hasOnline: true, hasDownload: true, subject: '大模型应用' },
          { id: 3, name: '机器学习（西瓜书）', author: '周志华', publisher: '清华大学出版社', edition: '第2版', coverUrl: '', hasOnline: false, hasDownload: false, subject: '计算机' },
          { id: 4, name: '深度学习入门：基于Python的理论与实现', author: '斋藤康毅', publisher: '人民邮电出版社', edition: '第1版', coverUrl: '', hasOnline: true, hasDownload: true, subject: 'AI岗位技能' },
          { id: 5, name: '工业机器视觉项目实战', author: '天池实训组', publisher: '机械工业出版社', edition: '第1版', coverUrl: '', hasOnline: true, hasDownload: false, subject: '工业AI' },
          { id: 6, name: 'MLOps工程实践指南', author: 'Mark Treveil等', publisher: 'O\'Reilly Media', edition: '第1版', coverUrl: '', hasOnline: true, hasDownload: false, subject: 'AI岗位技能' },
          { id: 7, name: '数据结构（C语言版）', author: '严蔚敏', publisher: '清华大学出版社', edition: '第2版', coverUrl: '', hasOnline: true, hasDownload: true, subject: '计算机' },
          { id: 8, name: '计算机操作系统', author: '汤小丹', publisher: '西安电子科技大学出版社', edition: '第4版', coverUrl: '', hasOnline: true, hasDownload: true, subject: '计算机' },
          { id: 9, name: '计算机网络', author: '谢希仁', publisher: '电子工业出版社', edition: '第8版', coverUrl: '', hasOnline: true, hasDownload: true, subject: '计算机' },
          { id: 10, name: '数据库系统概论', author: '王珊', publisher: '高等教育出版社', edition: '第5版', coverUrl: '', hasOnline: true, hasDownload: true, subject: '计算机' }
        ]

        let filtered = allBooks
        if (this.textbookSubjectFilter) {
          filtered = filtered.filter(b => b.subject === this.textbookSubjectFilter)
        }
        if (this.textbookSearch) {
          const kw = this.textbookSearch.toLowerCase()
          filtered = filtered.filter(b => b.name.toLowerCase().includes(kw) || (b.author && b.author.toLowerCase().includes(kw)))
        }

        this.textbookList = filtered
        this.textbookSubjects = [...new Set(allBooks.map(b => b.subject))]
      } catch (e) {
        this.$message.error('加载电子课本失败')
      } finally {
        this.textbookLoading = false
      }
    },
    openTextbook (book) {
      this.resourceViewTitle = book.name + ' - 在线预览'
      // 直接生成 HTML 内容预览，不需要后端接口
      this.resourceViewType = 'inline'
      this.resourceViewVisible = true
      this.resourceViewUrl = ''
      this.resourceViewContent = this.generateTextbookPreview(book)
    },
    viewResource (row) {
      this.resourceViewTitle = row.name + ' - 电子资源'
      this.resourceViewType = 'inline'
      this.resourceViewVisible = true
      this.resourceViewUrl = ''
      this.resourceViewContent = this.generateResourcePreview(row)
    },
    // 生成课本内联预览 HTML（丰富版）
    generateTextbookPreview (book) {
      var name = book.name || '教材'
      var author = book.author || '标准编写组'
      var publisher = book.publisher || '高等教育出版社'
      var edition = book.edition || '最新版'
      var chapters = ['绪论与学科概述', '基础概念与理论框架', '核心原理与定理', '方法论与分析工具', '经典案例研究', '应用实践指导', '前沿知识拓展', '复习总结与展望']
      var chpHtml = ''
      for (var i = 0; i < 8; i++) {
        var sn = '第' + (i + 1) + '章 ' + (chapters[i] || (name + '章节'))
        chpHtml += '<div style="padding:14px 16px;margin:8px 0;background:#f8fafc;border-radius:8px;border-left:3px solid #CCFF00;cursor:pointer;transition:background 0.15s" onmouseover="this.style.background=\'#eef2ff\'" onmouseout="this.style.background=\'#f8fafc\'">' +
          '<strong style="font-size:15px;color:#1e293b">' + sn + '</strong>' +
          '<p style="margin:6px 0 0;color:#64748b;font-size:13px;line-height:1.6">本章介绍' + sn + '的核心知识点，包括基础概念定义、关键理论推导和实践应用分析。通过本章的深入学习，学生能够建立完整的知识体系框架。</p>' +
          '<div style="margin-top:8px;display:flex;gap:8px"><span style="background:#eef2ff;color:#CCFF00;padding:1px 8px;border-radius:4px;font-size:11px">📖 理论</span><span style="background:#f0fdf4;color:#10b981;padding:1px 8px;border-radius:4px;font-size:11px">💡 案例</span><span style="background:#fef3c7;color:#d97706;padding:1px 8px;border-radius:4px;font-size:11px">✍️ 习题</span></div></div>'
      }
      return '<div style="max-height:68vh;overflow-y:auto;padding:20px 24px;line-height:1.8">' +
        '<div style="display:flex;gap:16px;margin-bottom:20px;padding-bottom:16px;border-bottom:2px solid #e8ecf1;align-items:center">' +
        '<div style="width:60px;height:75px;border-radius:6px;background:linear-gradient(135deg,#2A332A,#3A463A);display:flex;align-items:center;justify-content:center;color:#fff;font-size:24px;font-weight:700;flex-shrink:0">📖</div>' +
        '<div><h2 style="margin:0 0 4px;color:#1e293b;font-size:20px">' + name + '</h2>' +
        '<p style="margin:0;color:#64748b;font-size:14px">作者：' + author + ' · 出版社：' + publisher + ' · 版次：' + edition + '</p></div></div>' +
        '<div style="padding:14px;background:#f0f9ff;border-radius:8px;margin-bottom:16px;border:1px solid #e0efff">' +
        '<p style="margin:0;color:#0369a1;font-size:14px"><strong>📌 教材简介</strong></p>' +
        '<p style="margin:6px 0 0;color:#0369a1;font-size:13px;line-height:1.6">本书是高等教育权威教材，系统阐述' + name + '的理论体系和实践方法。全书共' + chapters.length + '章，结构严谨、逻辑清晰，配有大量例题和习题帮助读者掌握核心知识。适合本科生及同等学力人员学习使用。</p></div>' +
        '<h3 style="font-size:16px;color:#1e293b;margin:16px 0 12px">📑 目录</h3>' + chpHtml + '</div>'
    },
    generateResourcePreview (row) {
      var name = row.name || '课程资源'
      var textbook = row.textbook || ''
      var credit = row.credit || '—'
      var hours = row.hours || '—'
      var term = row.term || '—'
      var sections = ['第一章：概述与基础理论', '第二章：核心概念解析', '第三章：重点与难点分析', '第四章：经典案例讲解', '第五章：实践应用指导', '第六章：复习与总结']
      var secHtml = ''
      for (var i = 0; i < 6; i++) {
        secHtml += '<div style="padding:12px 14px;background:#f8fafc;border-radius:6px;border:1px solid #e8ecf1;font-size:13px;color:#475569;display:flex;align-items:center;gap:8px">' +
          '<span style="display:flex;width:24px;height:24px;border-radius:50%;background:#CCFF00;color:#fff;align-items:center;justify-content:center;font-size:11px;font-weight:600;flex-shrink:0">' + (i + 1) + '</span>' +
          '<span>' + sections[i] + '</span></div>'
      }
      return '<div style="max-height:68vh;overflow-y:auto;padding:20px 24px;line-height:1.8">' +
        '<h2 style="margin:0 0 12px;color:#1e293b;font-size:20px">📚 ' + name + '</h2>' +
        '<div style="display:flex;gap:12px;margin-bottom:20px;flex-wrap:wrap">' +
        '<span style="background:#eef2ff;color:#CCFF00;padding:4px 12px;border-radius:6px;font-size:13px">🎓 ' + credit + ' 学分</span>' +
        '<span style="background:#f0fdf4;color:#10b981;padding:4px 12px;border-radius:6px;font-size:13px">⏱ ' + hours + ' 学时</span>' +
        '<span style="background:#fef3c7;color:#d97706;padding:4px 12px;border-radius:6px;font-size:13px">📅 ' + term + '</span>' +
        (textbook ? '<span style="background:#f1f5f9;color:#475569;padding:4px 12px;border-radius:6px;font-size:13px">📖 教材：' + textbook + '</span>' : '') +
        '</div>' +
        '<div style="padding:16px;background:#f8fafc;border-radius:8px;margin-bottom:20px;border:1px solid #e8ecf1">' +
        '<p style="margin:0;color:#475569;font-size:14px;line-height:1.8">' + name + '是高等教育核心必修课程。课程涵盖基础理论、核心分析方法和实践应用三大模块。通过系统学习，学生能够全面掌握相关知识体系，培养分析问题和解决问题的能力。' +
        '本课程采用' + (textbook || '权威教材') + '作为主要教材，配合多媒体课件和在线资源辅助教学。</p></div>' +
        '<h3 style="font-size:15px;color:#1e293b;margin:0 0 12px">📑 课程章节</h3>' +
        '<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">' + secHtml + '</div>' +
        '<div style="margin-top:16px;padding:14px;background:#f0fdf4;border-radius:8px;border:1px solid #bbf7d0">' +
        '<p style="margin:0;color:#065f46;font-size:13px"><strong>✅ 教学提示</strong>：本课程建议每周' + (Math.round(hours / 16) || 3) + '学时学习，重点掌握每章的核心理念和关键公式，课后完成配套习题以巩固知识。</p></div></div>'
    },

    // ── 教学平台导入 ──
    handleDialogClose (key) {
      return (done) => { done() }
    },
    searchPlatform () {
      if (!this.platformSearch.trim()) {
        this.$message.warning('请输入搜索关键词')
        return
      }
      this.$message.success('正在搜索"' + this.platformSearch + '"相关资源...')
      // 此处对接后端平台搜索 API
      setTimeout(() => {
        this.$message.info('搜索结果将显示在下方（模拟数据）')
      }, 1000)
    },

    // ── 其他操作 ──
    changeStatus (row) {
      userApi.changeStatus(row.id).then(re => {
        if (re.code === 1) {
          row.status = re.response
          this.$message.success(re.message)
        } else {
          this.$message.error(re.message)
        }
      })
    },
    deleteUser (row) {
      userApi.deleteUser(row.id).then(re => {
        if (re.code === 1) {
          this.search()
          this.$message.success(re.message)
        } else {
          this.$message.error(re.message)
        }
      })
    },
    levelFormatter (row, column, cellValue) {
      return this.enumFormat(this.levelEnum, cellValue)
    },
    sexFormatter (row, column, cellValue) {
      return this.enumFormat(this.sexEnum, cellValue)
    },
    statusFormatter (status) {
      return this.enumFormat(this.statusEnum, status)
    },
    statusTagFormatter (status) {
      return this.enumFormat(this.statusTag, status)
    },
    statusBtnFormatter (status) {
      return this.enumFormat(this.statusBtn, status)
    }
  },
  computed: {
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapState('enumItem', {
      sexEnum: state => state.user.sexEnum,
      statusEnum: state => state.user.statusEnum,
      statusTag: state => state.user.statusTag,
      statusBtn: state => state.user.statusBtn,
      levelEnum: state => state.user.levelEnum
    })
  }
}
</script>

<style scoped>
.textbook-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.textbook-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
}

.textbook-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  border-color: var(--ink-5);
}

.textbook-cover {
  height: 140px;
  background: var(--ink-3);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.textbook-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.textbook-cover-placeholder {
  font-size: 48px;
  color: #94a3b8;
}

.textbook-info {
  padding: 12px 14px;
}

.textbook-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.textbook-info p {
  font-size: 12px;
  color: #94a3b8;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.textbook-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #94a3b8;
}
</style>
