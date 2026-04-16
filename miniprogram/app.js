const { request, BASE_URL } = require('./utils/request')

App({
  onLaunch() {
    console.info('[app] active BASE_URL:', BASE_URL)
    this.login()
  },

  login() {
    wx.login({
      success: (res) => {
        request('/api/auth/wechat/login', 'POST', {
          code: res.code,
          nickname: 'MiniUser'
        })
          .then((data) => {
            if (!data) return
            wx.setStorageSync('accessToken', data.accessToken || '')
            wx.setStorageSync('refreshToken', data.refreshToken || '')
            wx.setStorageSync('nickname', data.nickname || 'MiniUser')
          })
          .catch((error) => {
            console.error('wechat login failed:', error && error.message)
          })
      }
    })
  }
})
