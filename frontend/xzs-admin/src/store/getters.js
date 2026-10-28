const getters = {
  sidebar: state => state.app.sidebar,
  device: state => state.app.device,
  visitedViews: state => state.tagsView.visitedViews,
  cachedViews: state => state.tagsView.cachedViews,
  userName: state => state.user.userName,
  userId: state => {
    try {
      const info = state.user.userInfo
      if (typeof info === 'string') return JSON.parse(info).id || 0
      return info ? info.id || 0 : 0
    } catch (e) { return 0 }
  },
  userAvatar: state => {
    try {
      const info = state.user.userInfo
      if (typeof info === 'string') return JSON.parse(info).imagePath || ''
      return info ? info.imagePath || '' : ''
    } catch (e) { return '' }
  },
  routes: state => state.router.routes
}
export default getters
