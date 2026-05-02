const app = getApp()

Page({
  data: {
    loading: false,
    avatarUrl: '',
    nickname: ''
  },

  onLoad() {
    if (app.isLoggedIn()) {
      this.goBack()
    }
  },

  handleGetUserInfo(e) {
    const userInfo = e.detail && e.detail.userInfo
    if (userInfo) {
      this.setData({
        avatarUrl: userInfo.avatarUrl || '',
        nickname: userInfo.nickName || ''
      })
    }
  },

  async handleLogin() {
    this.setData({ loading: true })

    try {
      const data = await app.doLogin(
        this.data.nickname || undefined,
        this.data.avatarUrl || undefined
      )

      if (data) {
        wx.showToast({ title: '登录成功', icon: 'none' })
        setTimeout(() => this.goBack(), 600)
      }
    } catch (error) {
      const msg = (error && error.message) || '登录失败'
      wx.showToast({ title: msg.indexOf('fail') >= 0 ? '网络连接失败' : msg.slice(0, 20), icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({ url: '/pages/news/index' })
    }
  }
})
