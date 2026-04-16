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

function shouldRetryWithNextHost(errMsg) {
  if (!errMsg) return false
  return errMsg.indexOf('timeout') >= 0 || errMsg.indexOf('request:fail') >= 0
}

function request(url, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    if (!hasLoggedBaseUrl) {
      hasLoggedBaseUrl = true
      console.info('[request] base URL:', BASE_URL)
    }

    const token = wx.getStorageSync('accessToken')

    const doRequest = (idx, lastError) => {
      const baseUrl = BASE_URL_CANDIDATES[idx]
      if (!baseUrl) {
        reject(lastError || new Error('network error'))
        return
      }

      const finalUrl = baseUrl + url
      wx.request({
        url: finalUrl,
        method,
        data,
        timeout: 12000,
        header: {
          Authorization: token ? `Bearer ${token}` : ''
        },
        success: (res) => {
          const payload = res.data || {}
          if (payload.code === 0) {
            resolve(payload.data)
            return
          }
          reject(new Error(payload.message || 'request failed'))
        },
        fail: (err) => {
          const message = (err && err.errMsg) || 'network error'
          if (shouldRetryWithNextHost(message) && idx + 1 < BASE_URL_CANDIDATES.length) {
            doRequest(idx + 1, new Error(`${message} | ${finalUrl}`))
            return
          }
          reject(new Error(`${message} | ${finalUrl}`))
        }
      })
    }

    doRequest(0, null)
  })
}

module.exports = {
  request,
  BASE_URL,
  BASE_URL_CANDIDATES
}
