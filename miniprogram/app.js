App({
  onLaunch() {
    this.login()
  },

  login() {
    wx.login({
      success: (res) => {
        wx.request({
          url: 'http://127.0.0.1:8080/api/auth/wechat/login',
          method: 'POST',
          data: {
            code: res.code,
            nickname: '小程序用户'
          },
          success: (resp) => {
            const payload = resp.data
            if (payload && payload.code === 0 && payload.data) {
              wx.setStorageSync('accessToken', payload.data.accessToken)
              wx.setStorageSync('refreshToken', payload.data.refreshToken)
              wx.setStorageSync('nickname', payload.data.nickname)
            }
          }
        })
      }
    })
  }
})