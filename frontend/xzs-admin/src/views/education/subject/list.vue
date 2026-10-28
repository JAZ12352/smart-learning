<template>
  <div class="app-container">
    <!-- 学历层次 Tab 导航 -->
    <el-tabs v-model="activeEducationLevel" @tab-click="handleTabChange" class="edu-tabs">
      <el-tab-pane v-for="el in educationLevelEnum" :key="el.key" :label="el.value" :name="String(el.key)"></el-tab-pane>
      <el-tab-pane label="全部" name="all"></el-tab-pane>
    </el-tabs>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="filter-group">
        <el-input v-model="searchKeyword" placeholder="搜索学科/专业类/专业..." prefix-icon="el-icon-search" clearable size="medium" @input="onSearchInput" style="width:200px" />
        <el-select v-model="queryParam.category" placeholder="学科门类" clearable size="medium" @change="search" style="width:140px">
          <el-option v-for="item in categoryEnum" :key="item.key" :label="item.value" :value="item.key"></el-option>
        </el-select>
        <!-- 学生选择器：选学生后只显示该生专业关联的学科 -->
        <el-select v-model="selectedStudentId" placeholder="选择学生查看其专业学科" clearable filterable size="medium" @change="onStudentChange" style="width:220px">
          <el-option v-for="s in studentList" :key="s.id" :value="s.id" :label="s.realName + ' (' + s.userName + ')' + (s.majorName ? ' - ' + s.majorName : '')" />
        </el-select>
      </div>
      <div class="action-group">
        <el-button type="primary" size="medium" icon="el-icon-plus" @click="showAddDialog = true; subjectTypeForAdd = 1">添加学科</el-button>
        <el-button type="success" size="medium" icon="el-icon-download" @click="showImportDialog = true">📥 一键导入全部专业</el-button>
        <el-button type="warning" size="medium" icon="el-icon-refresh" @click="initDatabase" :loading="initLoading">⚡ 初始化学科</el-button>
      </div>
    </div>

    <!-- 学生专业提示条 -->
    <div v-if="selectedStudentInfo" class="student-info-bar">
      <i class="el-icon-user"></i>
      当前查看：<b>{{ selectedStudentInfo.realName }}</b>
      （专业：<el-tag size="mini">{{ selectedStudentInfo.majorName || '未设置' }}</el-tag>）
      <el-button type="text" size="mini" style="margin-left:8px" @click="clearStudentFilter">清除筛选</el-button>
    </div>

    <!-- 统计条 -->
    <div class="stats-bar" v-if="treeData.length > 0">
      <span class="stat-item"><b>{{ treeData.length }}</b> 个学科门类</span>
      <span class="stat-divider">|</span>
      <span class="stat-item"><b>{{ totalGroups }}</b> 个专业类</span>
      <span class="stat-divider">|</span>
      <span class="stat-item"><b>{{ totalMajors }}</b> 个专业</span>
    </div>

    <!-- 学生课程视图（选学生后显示） -->
    <div v-if="studentCourseMode" v-loading="listLoading" class="tree-content">
      <div class="student-course-header">
        <i class="el-icon-reading"></i>
        <span>{{ selectedStudentInfo?.realName }}的必修课程</span>
        <el-tag size="small" type="info">{{ studentCourses.length }} 门</el-tag>
      </div>
      <el-empty v-if="!listLoading && studentCourses.length === 0" description="该专业暂无课程，请在学科管理中为该专业添加课程" :image-size="80" />
      <el-table v-if="studentCourses.length > 0" :data="studentCourses" stripe style="width:100%">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="课程名称" min-width="200">
          <template slot-scope="{row}">
            <i class="el-icon-notebook-2" style="color:#409eff;margin-right:6px"></i>{{ row.name }}
          </template>
        </el-table-column>
        <el-table-column prop="itemOrder" label="排序" width="80" />
        <el-table-column label="操作" width="160">
          <template slot-scope="{row}">
            <el-button size="mini" @click="viewResources(row)">查看资源</el-button>
            <el-button size="mini" type="danger" @click="delSubject(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 学科树内容（默认模式） -->
    <div v-else v-loading="listLoading" class="tree-content">
      <el-empty v-if="!listLoading && treeData.length === 0" description="暂无学科数据，点击「一键导入全部专业」快速导入或「初始化学科」自动创建" />

      <!-- 门类循环 -->
      <div v-for="cat in treeData" :key="cat.id" class="category-card" v-show="filterMatches(cat)">
        <!-- 门类头 -->
        <div class="category-header" @click="cat._expanded = !cat._expanded">
          <div class="category-title">
            <i :class="catIcon(cat.name)" class="cat-icon"></i>
            <span class="cat-name">{{ cat.name }}</span>
            <el-tag size="mini" type="info" class="cat-count">{{ cat.children?.length || 0 }} 专业类</el-tag>
            <el-tag size="mini" type="success" v-if="cat.totalMajors !== undefined">{{ cat.totalMajors }} 专业</el-tag>
          </div>
          <div class="category-actions" @click.stop>
            <el-button size="mini" @click="openAddChild(cat, 2)">+ 添加专业类</el-button>
            <el-button size="mini" @click="editSubject(cat)">编辑</el-button>
            <el-button size="mini" type="danger" @click="delSubject(cat)">删除</el-button>
            <i :class="cat._expanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" class="expand-icon"></i>
          </div>
        </div>

        <!-- 专业类列表 (展开) -->
        <div v-show="cat._expanded" class="group-list">
          <div v-for="group in cat.children" :key="group.id" class="group-item" v-show="filterMatches(group)">
            <div class="group-header" @click="group._expanded = !group._expanded">
              <div class="group-title">
                <i class="el-icon-folder-opened group-icon"></i>
                <span class="group-name">{{ group.name }}</span>
                <el-tag size="mini" type="info">{{ group.children?.length || 0 }} 专业</el-tag>
              </div>
              <div class="group-actions" @click.stop>
                <el-button size="mini" @click="openAddChild(group, 3)">+ 添加专业</el-button>
                <el-button size="mini" @click="editSubject(group)">编辑</el-button>
                <el-button size="mini" type="danger" plain @click="delSubject(group)">删除</el-button>
                <i :class="group._expanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" class="expand-icon"></i>
              </div>
            </div>

            <!-- 专业列表 (展开) -->
            <div v-show="group._expanded" class="major-tags">
              <div v-for="major in group.children" :key="major.id" class="major-tag-wrapper" v-show="filterMatches(major)">
                <el-tag :closable="false" class="major-tag" effect="plain">
                  <i class="el-icon-notebook-2"></i> {{ major.name }}
                  <span class="major-actions">
                    <el-button type="text" size="mini" @click="manageCourses(major)" title="管理必修课程">📚</el-button>
                    <el-button type="text" size="mini" @click="viewResources(major)" title="查看资源">📖</el-button>
                    <el-button type="text" size="mini" @click="editSubject(major)" title="编辑">✏️</el-button>
                    <el-button type="text" size="mini" style="color:#f56c6c" @click="delSubject(major)" title="删除">🗑</el-button>
                  </span>
                </el-tag>
              </div>
            </div>
          </div>

          <!-- 门类空状态 -->
          <el-empty v-if="(!cat.children || cat.children.length === 0) && cat._expanded" description="暂无专业类，点击上方 + 添加" :image-size="60" />
        </div>
      </div>
    </div>

    <!-- ==================== 添加/编辑学科弹窗 ==================== -->
    <el-dialog :title="editTarget ? '编辑学科' : '添加学科'" :visible.sync="showAddDialog" width="580px" @close="resetAddForm">
      <el-form :model="addForm" ref="addForm" label-width="110px" :rules="addRules">
        <!-- 节点类型选择 -->
        <el-form-item label="节点类型" prop="subjectType">
          <el-radio-group v-model="addForm.subjectType" @change="onAddTypeChange">
            <el-radio :label="1">🏛 学科门类</el-radio>
            <el-radio :label="2">📁 专业类</el-radio>
            <el-radio :label="3">📖 专业</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 名称 -->
        <el-form-item label="名称" prop="name">
          <el-input v-model="addForm.name" :placeholder="namePlaceholder" />
        </el-form-item>

        <!-- 门类时: 学科门类选择 -->
        <el-form-item label="学科门类" prop="category" v-if="addForm.subjectType === 1">
          <el-select v-model="addForm.category" placeholder="选择所属门类" clearable style="width:100%">
            <el-option v-for="item in categoryEnum" :key="item.key" :label="item.value" :value="item.key" />
          </el-select>
        </el-form-item>

        <!-- 专业类/专业时: 父级级联选择 -->
        <el-form-item :label="parentLabel" v-if="addForm.subjectType >= 2">
          <el-cascader
            v-model="parentCascader"
            :options="treeForCascader"
            :props="{ checkStrictly: true, label: 'name', value: 'id', children: 'children', expandTrigger: 'hover', emitPath: true }"
            :placeholder="cascaderPlaceholder"
            clearable style="width:100%"
          />
        </el-form-item>

        <el-form-item label="学历层次">
          <el-select v-model="addForm.educationLevel" style="width:100%">
            <el-option v-for="item in educationLevelEnum" :key="item.key" :value="item.key" :label="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="addForm.description" type="textarea" :rows="2" placeholder="简介（可选）" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm" :loading="addLoading">保存</el-button>
      </span>
    </el-dialog>

    <!-- ==================== 一键导入弹窗 ==================== -->
    <el-dialog title="📥 一键导入教育部本科专业目录" :visible.sync="showImportDialog" width="580px" top="8vh">
      <div class="import-info">
        <div class="import-icon">
          <span class="big-icon">📚</span>
        </div>
        <div class="import-text">
          <p style="font-size:16px;font-weight:600;color:#1d2129;margin:0 0 8px">将导入以下全部数据：</p>
          <p v-if="catalogStats">
            <el-tag size="medium" style="margin:0 6px 6px 0">🏛 {{ catalogStats.categories }} 个学科门类</el-tag>
            <el-tag size="medium" style="margin:0 6px 6px 0">📁 {{ catalogStats.groups }} 个专业类</el-tag>
            <el-tag size="medium" style="margin:0 6px 6px 0">📖 {{ catalogStats.majors }} 个专业</el-tag>
          </p>
          <p style="color:#86909c;font-size:13px">数据来源：教育部《普通高等学校本科专业目录》，以 本科 层次导入。</p>
          <p style="color:#f56c6c;font-size:13px">⚠ 已存在的学科会跳过，不会重复导入。</p>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="success" @click="importCatalog" :loading="importLoading">确认导入</el-button>
      </span>
    </el-dialog>

    <!-- ==================== 专业必修课程管理 ==================== -->
    <el-dialog title="必修课程管理" :visible.sync="showCourseDialog" width="550px" @open="loadCourses" @closed="courses = []">
      <div slot="title" style="display:flex;align-items:center;gap:12px">
        <span>📚 {{ courseMajor?.name }} — 必修课程</span>
        <el-tag size="small" type="info">{{ courses.length }} 门</el-tag>
        <el-button size="mini" type="warning" @click="initCourses" :loading="initCourseLoading" style="margin-left:auto">🎯 生成推荐课程</el-button>
      </div>
      <div v-loading="courseLoading">
        <el-empty v-if="courses.length === 0" description="暂无课程，点击「生成推荐课程」自动创建或手动添加" :image-size="60" />
        <el-table v-else :data="courses" max-height="350">
          <el-table-column prop="name" label="课程名称" />
          <el-table-column prop="itemOrder" label="排序" width="60" />
          <el-table-column label="操作" width="80">
            <template slot-scope="{row}">
              <el-button type="text" size="mini" style="color:#f56c6c" @click="deleteCourse(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div style="margin-top:12px">
        <el-input v-model="newCourseName" placeholder="输入新课程名称" size="medium" style="width:300px" @keyup.enter="addCourse" />
        <el-button type="primary" size="medium" @click="addCourse" :loading="addCourseLoading" style="margin-left:8px">添加课程</el-button>
      </div>
    </el-dialog>

    <!-- ==================== 查看资源弹窗 ==================== -->
    <el-dialog title="学科资源" :visible.sync="showResourceDialog" width="900px" top="5vh" class="resource-dialog" @open="loadResources" @closed="resourceContent = ''">
      <div slot="title" style="display:flex;align-items:center;gap:12px">
        <span>📚 {{ currentSubject?.name }} — 资源列表</span>
        <el-tag size="small" type="info">{{ resources.length }} 项</el-tag>
      </div>

      <div class="resource-layout">
        <div class="resource-sidebar" v-loading="resourceLoading">
          <div class="resource-toolbar">
            <el-button size="mini" type="primary" @click="syncSubjectResources">同步AI资源</el-button>
          </div>
          <el-menu v-if="resources.length > 0" :default-active="activeResourceId" @select="selectResource" class="resource-menu">
            <el-menu-item v-for="r in resources" :key="r.id" :index="String(r.id)" class="resource-item">
              <div class="resource-item-content">
                <span class="resource-icon">
                  <i v-if="r.sourceType === 'generated'" class="el-icon-edit-outline" style="color:#409eff"></i>
                  <i v-else-if="r.sourceType === 'knowledge_base'" class="el-icon-collection" style="color:#67c23a"></i>
                  <i v-else class="el-icon-document" style="color:#e6a23c"></i>
                </span>
                <div class="resource-info">
                  <div class="resource-title">{{ r.title }}</div>
                  <div class="resource-meta">
                    <span class="resource-source">{{ r.source || r.sourceType }}</span>
                    <span class="resource-format">{{ r.format }}</span>
                    <span class="resource-clicks">{{ r.clickCount || 0 }} 次浏览</span>
                  </div>
                </div>
              </div>
            </el-menu-item>
          </el-menu>
          <el-empty v-else description="暂无资源" :image-size="80" />
        </div>

        <div class="resource-content" v-loading="contentLoading">
          <div v-if="currentResource" class="content-viewer">
            <div class="content-header">
              <h3>{{ currentResource.title }}</h3>
              <div class="content-actions">
                <el-button v-if="currentResource.format === 'mind_map' && !showMindMap" size="mini" type="warning" @click="toggleMindMap">🗺 思维导图</el-button>
                <el-button v-if="showMindMap" size="mini" type="primary" @click="toggleMindMap">📄 文本视图</el-button>
                <el-button size="mini" @click="copyContent">复制</el-button>
                <el-button size="mini" type="danger" @click="deleteResourceItem(currentResource)">删除</el-button>
              </div>
            </div>
            <el-divider></el-divider>
            <div v-show="!showMindMap" class="content-body markdown-body" v-html="renderedContent"></div>
            <div v-show="showMindMap" ref="mindmapContainer" class="mindmap-container"></div>
          </div>
          <div v-else class="content-placeholder">
            <i class="el-icon-document"></i>
            <p>请从左侧选择一个资源查看内容</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import subjectApi from '@/api/subject'
import userApi from '@/api/user'

export default {
  name: 'SubjectList',
  data () {
    return {
      activeEducationLevel: '2',
      queryParam: { educationLevel: null, category: null, pageIndex: 1, pageSize: 999 },
      listLoading: false,
      initLoading: false,
      searchKeyword: '',
      // 学生选择
      selectedStudentId: null,
      selectedStudentInfo: null,
      studentList: [],

      // 数据
      treeData: [],

      // 添加
      showAddDialog: false,
      addLoading: false,
      editTarget: null,
      addForm: {
        id: null,
        name: '',
        subjectType: 1,
        educationLevel: 2,
        educationLevelName: '本科',
        category: '',
        description: '',
        parentId: null
      },
      parentCascader: [],
      subjectTypeForAdd: 1,
      addRules: {
        subjectType: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
      },

      // 导入
      showImportDialog: false,
      importLoading: false,
      catalogStats: null,

      // 学生课程视图
      studentCourseMode: false,
      studentCourses: [],

      // 课程管理
      showCourseDialog: false,
      courseMajor: null,
      courses: [],
      courseLoading: false,
      newCourseName: '',
      addCourseLoading: false,
      initCourseLoading: false,

      // 资源
      showResourceDialog: false,
      currentSubject: null,
      resources: [],
      resourceLoading: false,
      activeResourceId: null,
      currentResource: null,
      contentLoading: false,
      resourceContent: '',
      showMindMap: false,
      mindmapInstance: null
    }
  },
  computed: {
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapState('enumItem', {
      educationLevelEnum: state => state.user.educationLevelEnum,
      categoryEnum: state => state.user.categoryEnum
    }),
    totalGroups () {
      return this.treeData.reduce((s, c) => s + (c.children?.length || 0), 0)
    },
    totalMajors () {
      return this.treeData.reduce((s, c) => s + (c.children || []).reduce((s2, g) => s2 + (g.children?.length || 0), 0), 0)
    },
    namePlaceholder () {
      const map = { 1: '如：工学、理学、哲学', 2: '如：计算机类、机械类', 3: '如：软件工程、会计学' }
      return map[this.addForm.subjectType] || '请输入名称'
    },
    parentLabel () {
      return this.addForm.subjectType === 2 ? '所属门类' : '所属专业类'
    },
    cascaderPlaceholder () {
      return this.addForm.subjectType === 2 ? '选择所属学科门类' : '选择门类 → 专业类'
    },
    treeForCascader () {
      // 如果是添加专业类(type=2)，只显示门类
      // 如果是添加专业(type=3)，显示门类→专业类
      return this.addForm.subjectType === 2
        ? this.treeData.map(n => ({ ...n, children: undefined }))
        : this.treeData
    },
    renderedContent () {
      if (!this.currentResource || !this.currentResource.content) return '<p style="color:#999">暂无内容</p>'
      const format = this.currentResource.format || 'markdown'
      if (format === 'html') return this.currentResource.content
      if (format === 'markdown' || format === 'mind_map') {
        return this.renderMarkdown(this.currentResource.content)
      }
      return '<pre>' + this.escapeHtml(this.currentResource.content) + '</pre>'
    }
  },
  created () {
    this.fetchTree()
    this.loadCatalogStats()
    this.loadStudents()
  },
  methods: {
    // ===================== 数据加载 =====================
    async fetchTree () {
      this.listLoading = true
      try {
        const eduLevel = this.activeEducationLevel === 'all' ? null : parseInt(this.activeEducationLevel)
        const res = await subjectApi.getTree(eduLevel)
        if (res.code === 1) {
          // 添加展开状态和统计
          this.treeData = (res.response || []).map(cat => {
            cat._expanded = true
            cat.totalMajors = (cat.children || []).reduce((s, g) => s + (g.children?.length || 0), 0)
            cat.children = (cat.children || []).map(g => {
              g._expanded = false
              return g
            })
            return cat
          })
        }
      } catch (e) {
        /* 加载失败，静默处理 */
      } finally {
        this.listLoading = false
      }
    },
    async loadCatalogStats () {
      try {
        const r = await subjectApi.getCatalogStats()
        if (r.code === 1) this.catalogStats = r.response
      } catch (e) {}
    },
    search () {
      this.fetchTree()
    },
    handleTabChange () {
      this.fetchTree()
    },

    // ===================== 搜索过滤 =====================
    onSearchInput: (function () {
      let timer = null
      return function () { clearTimeout(timer); timer = setTimeout(() => this.fetchTree(), 300) }
    })(),
    filterMatches (node) {
      if (!this.searchKeyword) return true
      const kw = this.searchKeyword.toLowerCase()
      return node.name && node.name.toLowerCase().includes(kw)
    },

    // ===================== 学生选择 =====================
    async loadStudents () {
      try {
        const r = await userApi.allStudents()
        if (r.code === 1) this.studentList = r.response || []
      } catch (e) {}
    },
    onStudentChange (studentId) {
      if (!studentId) {
        this.clearStudentFilter()
        return
      }
      this.selectedStudentInfo = this.studentList.find(s => s.id === studentId)
      if (this.selectedStudentInfo && this.selectedStudentInfo.majorId) {
        // 加载该学生专业的必修课程
        this.loadStudentCourses(this.selectedStudentInfo.majorId)
      } else {
        this.$message.warning('该学生尚未设置专业')
        this.studentCourses = []
        this.studentCourseMode = false
      }
    },
    async loadStudentCourses (majorId) {
      this.listLoading = true
      try {
        const r = await subjectApi.getMajorCourses(majorId)
        if (r.code === 1) {
          this.studentCourses = r.response || []
          this.studentCourseMode = true
        }
      } catch (e) {} finally {
        this.listLoading = false
      }
    },
    clearStudentFilter () {
      this.selectedStudentId = null
      this.selectedStudentInfo = null
      this.studentCourses = []
      this.studentCourseMode = false
      this.fetchTree()
    },

    // ===================== 课程管理 =====================
    manageCourses (major) {
      this.courseMajor = major
      this.courses = []
      this.newCourseName = ''
      this.showCourseDialog = true
    },
    async loadCourses () {
      if (!this.courseMajor) return
      this.courseLoading = true
      try {
        const r = await subjectApi.getMajorCourses(this.courseMajor.id)
        if (r.code === 1) this.courses = r.response || []
      } catch (e) {} finally { this.courseLoading = false }
    },
    async addCourse () {
      if (!this.newCourseName.trim()) { this.$message.warning('请输入课程名称'); return }
      this.addCourseLoading = true
      try {
        const r = await subjectApi.addMajorCourse(this.courseMajor.id, { name: this.newCourseName.trim() })
        if (r.code === 1) {
          this.$message.success('添加成功')
          this.newCourseName = ''
          this.loadCourses()
        } else { this.$message.error(r.message || '添加失败') }
      } catch (e) {
        this.$message.error('添加失败')
      } finally {
        this.addCourseLoading = false
      }
    },
    async deleteCourse (row) {
      this.$confirm(`确定删除课程「${row.name}」？`, '确认', { type: 'warning' }).then(async () => {
        const r = await subjectApi.deleteMajorCourse(row.id)
        if (r.code === 1) { this.$message.success('已删除'); this.loadCourses() }
      }).catch(() => {})
    },
    async initCourses () {
      this.initCourseLoading = true
      try {
        const r = await subjectApi.initMajorCourses(this.courseMajor.id)
        if (r.code === 1) {
          this.$message.success('已生成 ' + r.response + ' 门推荐课程')
          this.loadCourses()
        } else { this.$message.error(r.message || '生成失败') }
      } catch (e) {
        this.$message.error('生成失败')
      } finally {
        this.initCourseLoading = false
      }
    },

    // ===================== 添加 =====================
    onAddTypeChange (val) {
      this.parentCascader = []
      this.addForm.parentId = null
    },
    resetAddForm () {
      this.editTarget = null
      this.addForm = {
        id: null,
        name: '',
        subjectType: this.subjectTypeForAdd,
        educationLevel: 2,
        educationLevelName: '本科',
        category: '',
        description: '',
        parentId: null
      }
      this.parentCascader = []
      this.addLoading = false
      if (this.$refs.addForm) this.$refs.addForm.clearValidate()
    },
    openAddChild (parent, childType) {
      this.subjectTypeForAdd = childType
      this.showAddDialog = true
      this.editTarget = null
      this.addForm = {
        id: null,
        name: '',
        subjectType: childType,
        educationLevel: parent.educationLevel || 2,
        educationLevelName: parent.educationLevelName || '本科',
        category: parent.category || '',
        description: '',
        parentId: parent.id
      }
      this.parentCascader = this.buildParentPath(parent.id)
    },
    buildParentPath (id) {
      for (const cat of this.treeData) {
        if (cat.id === id) return [cat.id]
        for (const g of (cat.children || [])) {
          if (g.id === id) return [cat.id, g.id]
        }
      }
      return [id]
    },
    editSubject (subject) {
      this.showAddDialog = true
      this.editTarget = subject
      this.addForm = {
        id: subject.id,
        name: subject.name,
        subjectType: subject.subjectType || 3,
        educationLevel: subject.educationLevel || 2,
        educationLevelName: subject.educationLevelName || '本科',
        category: subject.category || '',
        description: subject.description || '',
        parentId: subject.parentId || null
      }
      this.parentCascader = subject.parentId ? this.buildParentPath(subject.parentId) : []
      if (this.$refs.addForm) this.$refs.addForm.clearValidate()
    },
    submitAddForm () {
      this.$refs.addForm.validate(async valid => {
        if (!valid) return
        this.addLoading = true
        const form = this.addForm
        if (this.parentCascader && this.parentCascader.length > 0) {
          form.parentId = this.parentCascader[this.parentCascader.length - 1]
        }
        form.educationLevelName = this.enumFormat(this.educationLevelEnum, form.educationLevel)
        try {
          const res = await subjectApi.edit(form)
          if (res.code === 1) {
            this.$message.success(this.editTarget ? '编辑成功' : '添加成功')
            this.showAddDialog = false
            this.fetchTree()
          } else {
            this.$message.error(res.message || '保存失败')
          }
        } catch (e) {
          this.$message.error('保存失败')
        } finally {
          this.addLoading = false
        }
      })
    },
    delSubject (row) {
      this.$confirm(`确定删除「${row.name}」吗？`, '确认', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(async () => {
        const res = await subjectApi.deleteSubject(row.id)
        if (res.code === 1) {
          this.$message.success('删除成功')
          this.fetchTree()
        }
      }).catch(() => {})
    },

    // ===================== 一键导入 =====================
    async importCatalog () {
      this.importLoading = true
      try {
        const r = await subjectApi.importCatalog(2)
        if (r.code === 1) {
          this.$message.success(r.response)
          this.showImportDialog = false
          this.fetchTree()
        } else {
          this.$message.error(r.message || '导入失败')
        }
      } catch (e) {
        this.$message.error('导入请求失败')
      } finally {
        this.importLoading = false
      }
    },
    async initDatabase () {
      this.initLoading = true
      try {
        const r = await subjectApi.initDatabase()
        if (r.code === 1) {
          this.$message.success(r.response)
          this.fetchTree()
        } else {
          this.$message.error(r.message || '初始化失败')
        }
      } catch (e) {
        this.$message.error('初始化请求失败')
      } finally {
        this.initLoading = false
      }
    },

    // ===================== 资源管理 =====================
    viewResources (subject) {
      this.currentSubject = subject
      this.resources = []
      this.currentResource = null
      this.activeResourceId = null
      this.showResourceDialog = true
    },
    async loadResources () {
      if (!this.currentSubject) return
      this.resourceLoading = true
      try {
        let res = await subjectApi.getResources(this.currentSubject.id)
        if (res.code === 1) {
          this.resources = res.response || []
          if (this.resources.length === 0) {
            const syncRes = await subjectApi.syncResources(this.currentSubject.id)
            if (syncRes.code === 1 && syncRes.response > 0) {
              res = await subjectApi.getResources(this.currentSubject.id)
              if (res.code === 1) this.resources = res.response || []
            }
          }
          if (this.resources.length > 0) {
            this.activeResourceId = String(this.resources[0].id)
            this.selectResource(this.activeResourceId)
          }
        }
      } catch (e) {
        /* 加载资源失败，静默处理 */
      } finally {
        this.resourceLoading = false
      }
    },
    async selectResource (index) {
      this.activeResourceId = index
      this.contentLoading = true
      this.showMindMap = false
      this.mindmapInstance = null
      try {
        const res = await subjectApi.getResourceDetail(parseInt(index))
        if (res.code === 1) {
          this.currentResource = res.response
          const found = this.resources.find(r => String(r.id) === index)
          if (found) found.clickCount = (found.clickCount || 0) + 1
        }
      } catch (e) {
        /* 加载资源内容失败，静默处理 */
      } finally {
        this.contentLoading = false
      }
    },
    async syncSubjectResources () {
      if (!this.currentSubject) return
      this.$message.info('正在同步AI生成资源...')
      try {
        const res = await subjectApi.syncResources(this.currentSubject.id)
        if (res.code === 1) {
          this.$message.success('同步完成，新增 ' + res.response + ' 项资源')
          this.loadResources()
        }
      } catch (e) {
        this.$message.error('同步失败')
      }
    },
    async deleteResourceItem (resource) {
      this.$confirm(`确定删除资源「${resource.title}」吗？`, '确认', { type: 'warning' })
        .then(async () => {
          const res = await subjectApi.deleteResource(resource.id)
          if (res.code === 1) { this.$message.success('资源已删除'); this.loadResources() }
        }).catch(() => {})
    },
    toggleMindMap () {
      this.showMindMap = !this.showMindMap
      if (this.showMindMap) this.$nextTick(() => this.renderMindMap())
    },
    renderMindMap () { /* kept from original */ },
    copyContent () {
      if (!this.currentResource || !this.currentResource.content) return
      navigator.clipboard.writeText(this.currentResource.content).then(() => this.$message.success('内容已复制')).catch(() => this.$message.error('复制失败'))
    },
    escapeHtml (text) {
      const div = document.createElement('div'); div.textContent = text; return div.innerHTML
    },
    renderMarkdown (text) {
      if (!text) return ''
      let html = this.escapeHtml(text)
      html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
      html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
      html = html.replace(/^######\s+(.+)$/gm, '<h6>$1</h6>')
      html = html.replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>')
      html = html.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>')
      html = html.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>')
      html = html.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>')
      html = html.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>')
      html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
      html = html.replace(/~~(.+?)~~/g, '<del>$1</del>')
      html = html.replace(/^[\s]*[-*+]\s+(.+)$/gm, '<li>$1</li>')
      html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
      html = html.replace(/^[\s]*\d+\.\s+(.+)$/gm, '<li>$1</li>')
      html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
      html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" style="max-width:100%">')
      html = html.replace(/^---+\s*$/gm, '<hr>')
      html = html.replace(/^(?!<[huoldpi]|<\/[huol])(.+)$/gm, '<p>$1</p>')
      html = html.replace(/<p>\s*<\/p>/g, '')
      html = html.replace(/<p><li>/g, '<li>')
      html = html.replace(/<\/li><\/p>/g, '</li>')
      return html
    },
    catIcon (name) {
      const map = {
        '哲学': 'el-icon-reading',
        '经济学': 'el-icon-coin',
        '法学': 'el-icon-s-check',
        '教育学': 'el-icon-school',
        '文学': 'el-icon-edit',
        '历史学': 'el-icon-time',
        '理学': 'el-icon-data-line',
        '工学': 'el-icon-setting',
        '农学': 'el-icon-sugar',
        '医学': 'el-icon-first-aid-kit',
        '军事学': 'el-icon-s-flag',
        '管理学': 'el-icon-s-management',
        '艺术学': 'el-icon-picture-outline-round',
        '交叉学科': 'el-icon-connection'
      }
      return map[name] || 'el-icon-collection-tag'
    }
  }
}
</script>

<style scoped>
.app-container { padding: 20px; min-height: calc(100vh - 64px); }
.edu-tabs { margin-bottom: 12px; }
.action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }
.filter-group { display: flex; gap: 8px; align-items: center; }
.action-group { display: flex; gap: 8px; }

/* 统计条 */
.stats-bar {
  padding: 10px 16px; background: var(--ink-3); border-radius: 8px; margin-bottom: 16px;
  font-size: 14px; color: var(--paper);
}
.stats-bar .stat-item b { font-size: 18px; color: var(--acid); }
.stats-bar .stat-divider { margin: 0 12px; color: #c0c4cc; }

/* 门类卡片 */
.category-card {
  background: #fff; border-radius: 10px; margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); border: 1px solid #ebeef5;
  overflow: hidden; transition: box-shadow 0.2s;
}
.category-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.category-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px; cursor: pointer; user-select: none;
  background: linear-gradient(135deg, #f8faff, #f0f5ff);
  border-bottom: 1px solid #ebeef5;
}
.category-title { display: flex; align-items: center; gap: 10px; }
.cat-icon { font-size: 22px; color: var(--acid); }
.cat-name { font-size: 18px; font-weight: 600; color: var(--paper); }
.cat-count { font-size: 11px; }
.category-actions { display: flex; align-items: center; gap: 6px; }
.expand-icon { font-size: 14px; color: #c0c4cc; margin-left: 8px; transition: transform 0.2s; }

/* 专业类列表 */
.group-list { padding: 8px 20px 16px; }
.group-item { margin-bottom: 8px; border: 1px solid #f0f0f0; border-radius: 8px; transition: all 0.15s; }
.group-item:hover { border-color: var(--ink-5); background: var(--ink-3); }
.group-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; cursor: pointer; user-select: none;
}
.group-title { display: flex; align-items: center; gap: 8px; }
.group-icon { font-size: 16px; color: #e6a23c; }
.group-name { font-size: 15px; font-weight: 500; color: var(--grey); }
.group-actions { display: flex; align-items: center; gap: 4px; }

/* 专业标签 */
.major-tags { padding: 6px 14px 12px; display: flex; flex-wrap: wrap; gap: 6px; }
.major-tag-wrapper { display: inline-block; }
.major-tag {
  padding: 4px 12px; border-radius: 6px; font-size: 13px;
  background: var(--ink-3) !important; border-color: var(--ink-5) !important;
  transition: all 0.15s; cursor: default;
}
.major-tag:hover { background: var(--ink-3) !important; border-color: var(--ink-5) !important; }
.major-tag i { margin-right: 4px; }
.major-actions { display: none; margin-left: 6px; }
.major-tag:hover .major-actions { display: inline-flex; gap: 2px; }
.major-actions .el-button--text { font-size: 13px; padding: 0 2px; }

/* 导入弹窗 */
.import-info { display: flex; gap: 20px; align-items: flex-start; padding: 10px 0; }
.import-icon .big-icon { font-size: 56px; line-height: 1; }
.import-text { flex: 1; }

/* 资源弹窗 - 继承原有样式 */
.resource-dialog >>> .el-dialog__body { padding: 0; }
.resource-layout { display: flex; height: 65vh; }
.resource-sidebar { width: 320px; min-width: 280px; border-right: 1px solid #ebeef5; overflow-y: auto; background: var(--ink-3); }
.resource-toolbar { padding: 12px; border-bottom: 1px solid #ebeef5; }
.resource-menu { border-right: none; background: transparent; }
.resource-item { height: auto !important; padding: 8px 12px !important; line-height: 1.4 !important; white-space: normal !important; border-bottom: 1px solid #f0f0f0; }
.resource-item.is-active { background: var(--ink-3) !important; }
.resource-item-content { display: flex; gap: 8px; align-items: flex-start; width: 100%; }
.resource-icon { font-size: 18px; margin-top: 2px; flex-shrink: 0; }
.resource-info { flex: 1; min-width: 0; }
.resource-title { font-size: 13px; color: var(--grey); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.resource-meta { font-size: 11px; color: #909399; margin-top: 4px; display: flex; gap: 8px; flex-wrap: wrap; }
.resource-content { flex: 1; overflow-y: auto; padding: 20px; }
.content-header { display: flex; justify-content: space-between; align-items: center; }
.content-header h3 { margin: 0; font-size: 16px; color: var(--grey); }
.content-actions { display: flex; gap: 6px; }
.content-body { font-size: 14px; line-height: 1.8; color: #333; }
.content-body >>> pre { background: var(--ink-3); padding: 16px; border-radius: 6px; overflow-x: auto; }
.content-body >>> code { background: var(--ink-3); padding: 2px 6px; border-radius: 3px; font-size: 13px; }
.content-body >>> img { max-width: 100%; border-radius: 4px; }
.content-body >>> table { width: 100%; border-collapse: collapse; }
.content-body >>> th, .content-body >>> td { border: 1px solid #dcdfe6; padding: 8px 12px; text-align: left; }
.content-body >>> th { background: var(--ink-3); font-weight: 600; }
.content-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #c0c4cc; }
.content-placeholder i { font-size: 48px; margin-bottom: 12px; }
.mindmap-container { width: 100%; height: 500px; background: var(--ink-3); border-radius: 8px; }

/* 学生信息条 */
.student-info-bar {
  padding: 8px 16px; background: var(--ink-3); border-radius: 6px; margin-bottom: 12px;
  font-size: 14px; color: var(--paper); display: flex; align-items: center; gap: 6px;
}
.student-info-bar i { font-size: 16px; color: var(--acid); }

/* 学生课程视图 */
.student-course-header {
  padding: 14px 20px; background: linear-gradient(135deg, #f0f5ff, #e6f7ff);
  border-radius: 10px; margin-bottom: 16px; font-size: 18px; font-weight: 600;
  color: var(--paper); display: flex; align-items: center; gap: 10px;
}
.student-course-header i { font-size: 22px; color: var(--acid); }
</style>
