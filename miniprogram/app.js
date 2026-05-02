const { request, BASE_URL } = require('./utils/request')

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
  })
}

App({
  onLaunch() {
    console.info('[app] active BASE_URL:', BASE_URL)
    this.silentLogin()
  },

  getDeviceId() {
    try {
      let deviceId = wx.getStorageSync('_deviceId')
      if (!deviceId) {
        deviceId = generateUUID().replace(/-/g, '')
        wx.setStorageSync('_deviceId', deviceId)
      }
      return deviceId
    } catch (e) {
      return generateUUID().replace(/-/g, '')
    }
  },

  silentLogin() {
    const deviceId = this.getDeviceId()
    wx.login({
      success: (res) => {
        request('/api/auth/wechat/login', 'POST', { code: deviceId })
          .then((data) => {
            if (!data) return
            wx.setStorageSync('accessToken', data.accessToken || '')
            wx.setStorageSync('refreshToken', data.refreshToken || '')
            wx.setStorageSync('nickname', data.nickname || '微信用户')
            wx.setStorageSync('avatar', data.avatar || '')
            wx.setStorageSync('userId', data.userId || '')
          })
          .catch((error) => {
            console.error('[app] 静默登录失败:', error && error.message)
          })
      },
      fail: (err) => {
        console.error('[app] wx.login 失败:', err)
      }
    })
  },

  doLogin(nickname, avatar) {
    const deviceId = this.getDeviceId()
    return request('/api/auth/wechat/login', 'POST', {
      code: deviceId,
      nickname: nickname || undefined,
      avatar: avatar || undefined
    }).then((data) => {
      wx.setStorageSync('accessToken', data.accessToken || '')
      wx.setStorageSync('refreshToken', data.refreshToken || '')
      wx.setStorageSync('nickname', data.nickname || '微信用户')
      wx.setStorageSync('avatar', data.avatar || '')
      wx.setStorageSync('userId', data.userId || '')
      return data
    })
  },

  isLoggedIn() {
    try {
      const token = wx.getStorageSync('accessToken')
      return !!token
    } catch (e) {
      return false
    }
  },

  logout() {
    try {
      wx.removeStorageSync('accessToken')
      wx.removeStorageSync('refreshToken')
      wx.removeStorageSync('nickname')
      wx.removeStorageSync('avatar')
      wx.removeStorageSync('userId')
      // 注意：不解锁 _deviceId，下次登录仍是同一用户
    } catch (e) {
      // ignore
    }
  }
})
