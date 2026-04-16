const { request } = require('../../utils/request')

Page({
  data: {
    profile: {},
    avatarText: '用',
    nicknameText: '用户123',
    roleText: '普通用户',
    favoriteCount: 0,
    submissionCount: 0,
    commentCount: 0
  },

  onShow() {
    this.loadData()
  },

  async loadData() {
    try {
      const [profile, favorites, submissions, comments] = await Promise.all([
        request('/api/me/profile'),
        request('/api/me/favorites', 'GET', { page: 1, pageSize: 1 }),
        request('/api/me/submissions', 'GET', { page: 1, pageSize: 1 }),
        request('/api/me/comments', 'GET', { page: 1, pageSize: 1 })
      ])

      const roles = profile && Array.isArray(profile.roles) ? profile.roles : []
      const roleText = roles.length ? roles.join(' / ') : '普通用户'
      const nicknameText = profile && profile.nickname ? profile.nickname : '用户123'
      const avatarText = nicknameText ? String(nicknameText).substring(0, 1) : '用'

      this.setData({
        profile: profile || {},
        avatarText,
        nicknameText,
        roleText,
        favoriteCount: favorites && favorites.total ? favorites.total : 0,
        submissionCount: submissions && submissions.total ? submissions.total : 0,
        commentCount: comments && comments.total ? comments.total : 0
      })
    } catch (error) {
      wx.showToast({ title: '个人信息加载失败', icon: 'none' })
    }
  },

  goFavorites() {
    wx.navigateTo({ url: '/pages/me/favorites/index' })
  },

  goSubmissions() {
    wx.navigateTo({ url: '/pages/me/submissions/index' })
  },

  goComments() {
    wx.navigateTo({ url: '/pages/me/comments/index' })
  },

  goSubmissionCenter() {
    wx.navigateTo({ url: '/pages/submission/index' })
  }
})