const app = getApp()

Page({
  data: {
    isLoggedIn: false,
    profile: {},
    avatarText: '用',
    nicknameText: '点击登录',
    roleText: '',
    avatarUrl: '',
    favoriteCount: 0,
    submissionCount: 0,
    commentCount: 0
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 })
    }
    this.checkAndLoad()
  },

  checkAndLoad() {
    const loggedIn = app.isLoggedIn()
    this.setData({ isLoggedIn: loggedIn })

    if (loggedIn) {
      const nickname = wx.getStorageSync('nickname') || '微信用户'
      const avatar = wx.getStorageSync('avatar') || ''
      this.setData({
        nicknameText: nickname,
        avatarText: String(nickname).substring(0, 1),
        avatarUrl: avatar
      })
      this.loadData()
    }
  },

  async loadData() {
    const { request } = require('../../utils/request')
    try {
      const [profile, favorites, submissions, comments] = await Promise.all([
        request('/api/me/profile'),
        request('/api/me/favorites', 'GET', { page: 1, pageSize: 1 }),
        request('/api/me/submissions', 'GET', { page: 1, pageSize: 1 }),
        request('/api/me/comments', 'GET', { page: 1, pageSize: 1 })
      ])

      const roles = profile && Array.isArray(profile.roles) ? profile.roles : []
      const roleText = roles.length ? roles.join(' / ') : '普通用户'
      const nicknameText = profile && profile.nickname ? profile.nickname : '微信用户'
      const avatarText = String(nicknameText).substring(0, 1)
      const avatarUrl = profile && profile.avatar ? profile.avatar : ''

      this.setData({
        profile: profile || {},
        avatarText,
        nicknameText,
        roleText,
        avatarUrl,
        favoriteCount: favorites && favorites.total ? favorites.total : 0,
        submissionCount: submissions && submissions.total ? submissions.total : 0,
        commentCount: comments && comments.total ? comments.total : 0
      })
    } catch (error) {
      const msg = (error && error.message) || ''
      if (msg.indexOf('token') >= 0 || msg.indexOf('401') >= 0 || msg.indexOf('过期') >= 0) {
        app.logout()
        this.setData({ isLoggedIn: false })
      } else {
        wx.showToast({ title: '个人信息加载失败', icon: 'none' })
      }
    }
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  handleLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout()
          this.setData({
            isLoggedIn: false,
            avatarText: '用',
            nicknameText: '点击登录',
            roleText: '',
            avatarUrl: '',
            favoriteCount: 0,
            submissionCount: 0,
            commentCount: 0
          })
          wx.showToast({ title: '已退出', icon: 'none' })
        }
      }
    })
  },

  goFavorites() {
    if (!this.data.isLoggedIn) { this.goLogin(); return }
    wx.navigateTo({ url: '/pages/me/favorites/index' })
  },

  goSubmissions() {
    if (!this.data.isLoggedIn) { this.goLogin(); return }
    wx.navigateTo({ url: '/pages/me/submissions/index' })
  },

  goComments() {
    if (!this.data.isLoggedIn) { this.goLogin(); return }
    wx.navigateTo({ url: '/pages/me/comments/index' })
  },

  changeAvatar() {
    if (!this.data.isLoggedIn) return
    const { request, BASE_URL } = require('../../utils/request')
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file || !file.tempFilePath) return
        this.uploadAvatar(file.tempFilePath)
      }
    })
  },

  uploadAvatar(filePath) {
    const { request, BASE_URL } = require('../../utils/request')
    const token = wx.getStorageSync('accessToken')
    wx.showLoading({ title: '上传中...' })

    wx.uploadFile({
      url: `${BASE_URL}/api/files/upload`,
      filePath,
      name: 'file',
      header: { Authorization: token ? `Bearer ${token}` : '' },
      success: async (res) => {
        try {
          const payload = JSON.parse(res.data)
          if (payload.code !== 0 || !payload.data || !payload.data.url) {
            throw new Error(payload.message || '上传失败')
          }
          await this.updateProfile({ avatar: payload.data.url })
        } catch (err) {
          wx.showToast({ title: '头像更新失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.showToast({ title: '上传失败，请检查网络', icon: 'none' })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  },

  changeNickname() {
    if (!this.data.isLoggedIn) return
    wx.showModal({
      title: '修改昵称',
      editable: true,
      placeholderText: '请输入新昵称（最多16字）',
      content: this.data.nicknameText,
      success: async (res) => {
        if (!res.confirm) return
        const newNickname = (res.content || '').trim()
        if (!newNickname) {
          wx.showToast({ title: '昵称不能为空', icon: 'none' })
          return
        }
        if (newNickname.length > 16) {
          wx.showToast({ title: '昵称最多16个字', icon: 'none' })
          return
        }
        if (newNickname === this.data.nicknameText) return
        await this.updateProfile({ nickname: newNickname })
      }
    })
  },

  async updateProfile(data) {
    const { request } = require('../../utils/request')
    try {
      const result = await request('/api/me/profile', 'PUT', data)
      const nickname = result && result.nickname ? result.nickname : this.data.nicknameText
      const avatar = result && result.avatar ? result.avatar : this.data.avatarUrl

      wx.setStorageSync('nickname', nickname)
      wx.setStorageSync('avatar', avatar)

      this.setData({
        nicknameText: nickname,
        avatarText: String(nickname).substring(0, 1),
        avatarUrl: avatar
      })
      wx.showToast({ title: '资料已更新', icon: 'none' })
    } catch (error) {
      const msg = (error && error.message) || '更新失败'
      wx.showToast({ title: msg.slice(0, 20), icon: 'none' })
    }
  },

  goSubmissionCenter() {
    if (!this.data.isLoggedIn) { this.goLogin(); return }
    wx.navigateTo({ url: '/pages/submission/index' })
  }
})
