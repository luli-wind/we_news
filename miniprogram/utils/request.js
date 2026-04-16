const BASE_URL = 'http://127.0.0.1:8080'

function request(url, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('accessToken')
    wx.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        const payload = res.data
        if (payload.code === 0) {
          resolve(payload.data)
        } else {
          reject(new Error(payload.message || '请求失败'))
        }
      },
      fail: (err) => reject(err)
    })
  })
}

module.exports = {
  request,
  BASE_URL
}