import Vue from 'vue'
import Router from 'vue-router'
import Layout from '@/layout'

Vue.use(Router)

const constantRoutes = [
  { path: '/redirect', component: Layout, hidden: true, children: [{ path: '/redirect/:path*', component: () => import('@/views/redirect/index') }] },
  { path: '/login', name: 'Login', hidden: true, component: () => import('@/views/login/index'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', hidden: true, component: () => import('@/views/login/register'), meta: { title: '注册' } },

  // ── 首页 ──
  { path: '/', component: Layout, redirect: '/dashboard', children: [{ path: 'dashboard', component: () => import('@/views/dashboard/index'), name: 'Dashboard', meta: { title: '系统首页', icon: 'home', affix: true } }] },

  // ── 资源平台（专业课程资源） ──
  { path: '/knowledge-base',
    component: Layout,
    name: 'KnowledgeBaseMain',
    meta: { title: '课程资源', icon: 'knowledge-base' },
    redirect: '/knowledge-base/index',
    children: [
      { path: 'index', component: () => import('@/views/knowledge-base/index'), name: 'KnowledgeBaseHomePage', meta: { title: '专业课程资源', noCache: false } },
      { path: 'list', component: () => import('@/views/knowledge-base/list'), name: 'KnowledgeBaseListPage', meta: { title: '知识库管理', noCache: false }, hidden: true }
    ] },

  // ── 1. 学习画像（核心必做 35%） ──
  { path: '/profile-agent',
    component: Layout,
    name: 'ProfilePage',
    meta: { title: '学习画像', icon: 'agent' },
    redirect: '/profile-agent/index',
    children: [
      { path: 'index', component: () => import('@/views/student-profile/index'), name: 'StudentProfilePage', meta: { title: '画像概览', noCache: false } },
      { path: 'dialogue', component: () => import('@/views/student-profile/dialogue'), name: 'StudentProfileDialogue', meta: { title: '对话构建', noCache: false }, hidden: true },
      { path: 'explain', component: () => import('@/views/student-profile/dialogue'), name: 'StudentProfileExplain', meta: { title: 'AI 解读', noCache: false }, hidden: true }
    ] },

  // ── 2. 多智能体资源生成（核心必做 45%） ──
  { path: '/resource-agent',
    component: Layout,
    name: 'ResourcePage',
    meta: { title: '资源生成', icon: 'component' },
    redirect: '/resource-agent/generate',
    children: [
      { path: 'generate', component: () => import('@/views/resource/generate'), name: 'ResourceGeneratePage', meta: { title: '智能体生成', noCache: false } },
      { path: 'list', component: () => import('@/views/resource/detail'), name: 'ResourceListPage', meta: { title: '资源库', noCache: false }, hidden: true },
      { path: 'courseware', component: () => import('@/views/courseware/optimize'), name: 'CoursewareOptimizePage', meta: { title: '课件优化', noCache: false }, hidden: true },
      { path: 'collaboration', component: () => import('@/views/agent/collaboration'), name: 'AgentCollaborationPage', meta: { title: '多智能体协作', noCache: false } },
      { path: 'ppt-agent', component: () => import('@/views/resource/ppt-agent'), name: 'PptAgentPage', meta: { title: 'PPT课件生成', noCache: false }, hidden: true }
    ] },

  // ── 3. 学习路径（核心必做） ──
  { path: '/learning-path-agent',
    component: Layout,
    name: 'LearningPathPage',
    meta: { title: '学习路径', icon: 'tree' },
    redirect: '/learning-path-agent/index',
    children: [
      { path: 'index', component: () => import('@/views/learning-path/index'), name: 'LearningPathPageV', meta: { title: '路径规划', noCache: false } }
    ] },

  // ── 4. 智能辅导（加分项） ──
  { path: '/tutoring-agent',
    component: Layout,
    name: 'TutoringPage',
    meta: { title: '智能辅导', icon: 'ai-chat' },
    redirect: '/tutoring-agent/chat',
    children: [
      { path: 'chat', component: () => import('@/views/ai-chat/chat'), name: 'TutoringChatPage', meta: { title: 'AI 辅导', noCache: false } },
      { path: 'intent', component: () => import('@/views/teaching-intent/analyze'), name: 'TeachingIntentPage', meta: { title: '意图分析', noCache: false }, hidden: true }
    ] },

  // ── 5. 学习评估（加分项） ──
  { path: '/evaluation-agent',
    component: Layout,
    name: 'EvaluationPage',
    meta: { title: '学习评估', icon: 'chart' },
    redirect: '/evaluation-agent/report',
    children: [
      { path: 'report', component: () => import('@/views/evaluation/index'), name: 'EvaluationReportPage', meta: { title: '评估报告', noCache: false } },
      { path: 'experiment', component: () => import('@/views/evaluation/experiment'), name: 'ExperimentReportPage', meta: { title: '可复现实验报告', noCache: false } }
    ] },

  // ── 6. 考试系统（配套工具） ──
  { path: '/exam',
    component: Layout,
    name: 'ExamPage',
    meta: { title: '考试管理', icon: 'exam' },
    children: [
      { path: 'paper/list', component: () => import('@/views/exam/paper/list'), name: 'ExamPaperList', meta: { title: '试卷列表', noCache: false } },
      { path: 'paper/edit', component: () => import('@/views/exam/paper/edit'), name: 'ExamPaperEdit', meta: { title: '试卷编辑', activeMenu: '/exam/paper/list' }, hidden: true },
      { path: 'question/list', component: () => import('@/views/exam/question/list'), name: 'QuestionList', meta: { title: '题目列表', noCache: false } },
      { path: 'question/edit/singleChoice', component: () => import('@/views/exam/question/edit/single-choice'), name: 'singleChoiceEdit', meta: { title: '单选题', activeMenu: '/exam/question/list' }, hidden: true },
      { path: 'question/edit/multipleChoice', component: () => import('@/views/exam/question/edit/multiple-choice'), name: 'multipleChoiceEdit', meta: { title: '多选题', activeMenu: '/exam/question/list' }, hidden: true },
      { path: 'question/edit/trueFalse', component: () => import('@/views/exam/question/edit/true-false'), name: 'trueFalseEdit', meta: { title: '判断题', activeMenu: '/exam/question/list' }, hidden: true },
      { path: 'question/edit/gapFilling', component: () => import('@/views/exam/question/edit/gap-filling'), name: 'gapFillingEdit', meta: { title: '填空题', activeMenu: '/exam/question/list' }, hidden: true },
      { path: 'question/edit/shortAnswer', component: () => import('@/views/exam/question/edit/short-answer'), name: 'shortAnswerEdit', meta: { title: '简答题', activeMenu: '/exam/question/list' }, hidden: true }
    ] },

  // ── 7. 基础管理（用户/学科） ──
  { path: '/user',
    component: Layout,
    name: 'UserPage',
    meta: { title: '用户管理', icon: 'users' },
    children: [
      { path: 'student/list', component: () => import('@/views/user/student/list'), name: 'StudentList', meta: { title: '学生列表', noCache: false } },
      { path: 'student/edit', component: () => import('@/views/user/student/edit'), name: 'StudentEdit', meta: { title: '学生编辑', activeMenu: '/user/student/list' }, hidden: true },
      { path: 'admin/list', component: () => import('@/views/user/admin/list'), name: 'AdminList', meta: { title: '管理员列表', noCache: false } },
      { path: 'admin/edit', component: () => import('@/views/user/admin/edit'), name: 'AdminEdit', meta: { title: '管理员编辑', activeMenu: '/user/admin/list' }, hidden: true }
    ] },

  // ── 考勤管理 ──
  { path: '/attendance',
    component: Layout,
    name: 'AttendancePage',
    meta: { title: '考勤管理', icon: 'attendance' },
    children: [
      { path: 'list', component: () => import('@/views/attendance/list'), name: 'AttendanceList', meta: { title: '考勤记录', noCache: false } },
      { path: 'approval', component: () => import('@/views/attendance/approval'), name: 'AttendanceApproval', meta: { title: '请假审批', noCache: false } },
      { path: 'statistics', component: () => import('@/views/attendance/statistics'), name: 'AttendanceStatistics', meta: { title: '出勤统计', noCache: false } },
      { path: 'warning', component: () => import('@/views/attendance/warning'), name: 'AttendanceWarning', meta: { title: '缺勤预警', noCache: false } }
    ] },

  { path: '/education',
    component: Layout,
    name: 'EducationPage',
    meta: { title: '学科管理', icon: 'education' },
    alwaysShow: true,
    children: [
      { path: 'subject/list', component: () => import('@/views/education/subject/list'), name: 'SubjectList', meta: { title: '学科列表', noCache: false } },
      { path: 'subject/edit', component: () => import('@/views/education/subject/edit'), name: 'SubjectEdit', meta: { title: '学科编辑', activeMenu: '/education/subject/list' }, hidden: true }
    ] },

  // ── 8. 模型设置 ──
  { path: '/settings',
    component: Layout,
    children: [
      { path: 'model', component: () => import('@/views/settings/index'), name: 'ModelSettingsPage', meta: { title: '模型配置', icon: 'settings', noCache: false } }
    ] },

  // ── 隐藏 ──
  { path: '/profile',
    component: Layout,
    hidden: true,
    children: [
      { path: 'index', component: () => import('@/views/profile/index'), name: 'PersonalProfile', meta: { title: '个人简介', icon: 'user', noCache: false } }
    ] }
]

const router = new Router({ routes: constantRoutes })
export { constantRoutes, router }
