import { post, get, del } from '@/utils/request'

export default {
  list: query => post('/api/admin/education/subject/list'),
  pageList: query => post('/api/admin/education/subject/page', query),
  edit: query => post('/api/admin/education/subject/edit', query),
  select: id => post('/api/admin/education/subject/select/' + id),
  deleteSubject: id => post('/api/admin/education/subject/delete/' + id),
  getByEducationLevel: query => post('/api/admin/education/subject/by-education-level', query),
  getTree: (educationLevel) => get('/api/admin/education/subject/tree', { educationLevel }),
  getByParent: parentId => get('/api/admin/education/subject/by-parent/' + parentId),
  getByType: subjectType => get('/api/admin/education/subject/by-type/' + subjectType),

  // 学科资源
  getResources: subjectId => get('/api/admin/education/subject/' + subjectId + '/resources'),
  getResourceDetail: resourceId => get('/api/admin/education/subject/resource/' + resourceId),
  addResource: (subjectId, data) => post('/api/admin/education/subject/' + subjectId + '/resource/add', data),
  deleteResource: resourceId => del('/api/admin/education/subject/resource/' + resourceId),
  syncResources: subjectId => post('/api/admin/education/subject/' + subjectId + '/sync-resources'),

  // 学科目录导入
  getCatalogStats: () => get('/api/admin/education/catalog/stats'),
  importCatalog: (educationLevel) => post('/api/admin/education/catalog/import', null, { params: { educationLevel } }),
  initDatabase: () => post('/api/admin/education/init'),

  // 专业课程管理
  getMajorCourses: majorId => get('/api/admin/education/major/' + majorId + '/courses'),
  addMajorCourse: (majorId, data) => post('/api/admin/education/major/' + majorId + '/course/add', data),
  deleteMajorCourse: courseId => del('/api/admin/education/major/course/' + courseId),
  initMajorCourses: majorId => post('/api/admin/education/major/' + majorId + '/init-courses')
}
