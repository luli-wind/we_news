const DEVTOOLS_BASE_URL = 'http://127.0.0.1:8080'
const DEVICE_DEBUG_BASE_URL = 'http://10.27.219.96:8080'

// You can replace trial/release with your deployed HTTPS domain later.
const API_BASE_URL = {
  develop: DEVICE_DEBUG_BASE_URL,
  trial: 'https://your-api-domain.com',
  release: 'https://your-api-domain.com'
}

function resolveEnvVersion() {
  try {
    const info = wx.getAccountInfoSync()
    return (info && info.miniProgram && info.miniProgram.envVersion) || 'develop'
  } catch (error) {
    return 'develop'
  }
}

function isDevtoolsRuntime() {
  try {
    const sys = wx.getSystemInfoSync()
    return sys && sys.platform === 'devtools'
  } catch (error) {
    return true
  }
}

function normalizeOverrideBaseUrl(input) {
  if (typeof input !== 'string') return ''
  if (input !== input.trim()) return ''
  const value = input.trim()
  if (!value) return ''
  if (!/^https?:\/\/[^\s]+$/i.test(value)) return ''
  return value
}

function resolveBaseUrl() {
  try {
    const overrideBaseUrl = normalizeOverrideBaseUrl(wx.getStorageSync('apiBaseUrl'))
    if (overrideBaseUrl) return overrideBaseUrl
  } catch (error) {
    // ignore
  }

  if (isDevtoolsRuntime()) {
    return DEVTOOLS_BASE_URL
  }

  const envVersion = resolveEnvVersion()
  const target = API_BASE_URL[envVersion]
  if (!target || target.indexOf('your-api-domain.com') >= 0) {
    return DEVICE_DEBUG_BASE_URL
  }
  return target
}

module.exports = {
  DEVTOOLS_BASE_URL,
  DEVICE_DEBUG_BASE_URL,
  API_BASE_URL,
  isDevtoolsRuntime,
  normalizeOverrideBaseUrl,
  BASE_URL: resolveBaseUrl()
}
