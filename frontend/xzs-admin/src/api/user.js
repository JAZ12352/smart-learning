import { post, get } from '@/utils/request'

export default {
  getUserPageList: query => post('/api/admin/user/page/list', query),
  getUserEventPageList: query => post('/api/admin/user/event/page/list', query),
  createUser: query => post('/api/admin/user/edit', query),
  selectUser: id => post('/api/admin/user/select/' + id),
  getCurrentUser: () => post('/api/admin/user/current'),
  updateUser: query => post('/api/admin/user/update', query),
  changeStatus: id => post('/api/admin/user/changeStatus/' + id),
  deleteUser: id => post('/api/admin/user/delete/' + id),
  selectByUserName: query => post('/api/admin/user/selectByUserName', query),

  // 专业管理
  updateMajor: (userId, majorId) => post('/api/admin/user/major/update', { userId, majorId }),
  listByMajor: majorId => get('/api/admin/user/list-by-major/' + majorId),
  allStudents: () => get('/api/admin/user/all-students')
}
