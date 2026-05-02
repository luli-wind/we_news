const { BASE_URL } = require('./config')
let hasLoggedBaseUrl = false

function buildBaseUrlCandidates() {
  const list = [BASE_URL]
  if (BASE_URL.indexOf('127.0.0.1') >= 0) {
    list.push(BASE_URL.replace('127.0.0.1', 'localhost'))
  } else if (BASE_URL.indexOf('localhost') >= 0) {
    list.push(BASE_URL.replace('localhost', '127.0.0.1'))
  }
  return Array.from(new Set(list.filter(Boolean)))
}

const BASE_URL_CANDIDATES = buildBaseUrlCandidates()

let refreshPromise = null

function shouldRetryWithNextHost(errMsg) {
  if (!errMsg) return false
  return errMsg.indexOf('timeout') >= 0 || errMsg.indexOf('request:fail') >= 0 || errMsg.indexOf('refused') >= 0
}

function tryRefreshToken() {
  if (refreshPromise) return refreshPromise

  refreshPromise = new Promise((resolve) => {
    const refreshToken = wx.getStorageSync('refreshToken')
    if (!refreshToken) {
      refreshPromise = null
      resolve(false)
      return
    }

    const baseUrl = BASE_URL_CANDIDATES[0]
    wx.request({
      url: baseUrl + '/api/auth/refresh',
      method: 'POST',
      data: { refreshToken },
      timeout: 10000,
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        const payload = res.data || {}
        if (payload.code === 0 && payload.data) {
          wx.setStorageSync('accessToken', payload.data.accessToken || '')
          wx.setStorageSync('refreshToken', payload.data.refreshToken || '')
          resolve(true)
        } else {
          resolve(false)
        }
      },
      fail: () => {
        resolve(false)
      },
      complete: () => {
        refreshPromise = null
      }
    })
  })

  return refreshPromise
}

function clearAuth() {
  try {
    wx.removeStorageSync('accessToken')
    wx.removeStorageSync('refreshToken')
  } catch (e) {
    // ignore
  }
}

function request(url, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    if (!hasLoggedBaseUrl) {
      hasLoggedBaseUrl = true
      console.info('[request] 当前 API 地址:', BASE_URL)
      console.info('[request] 备用地址列表:', BASE_URL_CANDIDATES)
    }

    const doRequest = (idx, lastError, isRetry) => {
      const baseUrl = BASE_URL_CANDIDATES[idx]
      if (!baseUrl) {
        const err = lastError || new Error('无法连接到服务器，请检查网络或服务状态')
        reject(err)
        return
      }

      const token = wx.getStorageSync('accessToken')
      const finalUrl = baseUrl + url

      wx.request({
        url: finalUrl,
        method,
        data: method === 'GET' ? data : data,
        timeout: 15000,
        header: {
          'Content-Type': 'application/json',
          Authorization: token ? `Bearer ${token}` : ''
        },
        success: async (res) => {
          if (res.statusCode === 401 && !isRetry) {
            const refreshed = await tryRefreshToken()
            if (refreshed) {
              doRequest(idx, null, true)
            } else {
              clearAuth()
              reject(new Error('登录已过期，请重新登录'))
            }
            return
          }
          if (res.statusCode === 401) {
            clearAuth()
            reject(new Error('登录已过期，请重新登录'))
            return
          }
          if (res.statusCode >= 500) {
            reject(new Error('服务器内部错误'))
            return
          }
          const payload = res.data || {}
          if (payload.code === 0) {
            resolve(payload.data)
            return
          }
          const msg = payload.message || '请求失败'
          reject(new Error(msg))
        },
        fail: (err) => {
          const message = (err && err.errMsg) || 'network error'
          console.warn('[request] 请求失败:', finalUrl, message)
          if (shouldRetryWithNextHost(message) && idx + 1 < BASE_URL_CANDIDATES.length) {
            console.info('[request] 尝试下一个地址:', BASE_URL_CANDIDATES[idx + 1])
            doRequest(idx + 1, new Error(message), isRetry)
            return
          }
          reject(new Error(message))
        }
      })
    }

    doRequest(0, null, false)
  })
}

module.exports = {
  request,
  BASE_URL,
  BASE_URL_CANDIDATES,
  tryRefreshToken
}
