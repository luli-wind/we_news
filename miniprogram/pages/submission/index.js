const { request, BASE_URL } = require('../../utils/request')

function classifyError(error) {
  const msg = (error && error.message) || ''
  if (!msg) return { type: 'unknown', text: '未知错误，请重试' }
  if (msg.indexOf('401') >= 0 || msg.indexOf('unauthorized') >= 0 || msg.indexOf('鉴权') >= 0 || msg.indexOf('token') >= 0) {
    return { type: 'auth', text: '登录已失效，请退出后重新进入小程序', retry: false }
  }
  if (msg.indexOf('timeout') >= 0 || msg.indexOf('network') >= 0 || msg.indexOf('fail') >= 0 || msg.indexOf('网络') >= 0) {
    return { type: 'network', text: '网络连接失败，请检查网络后重试', retry: true }
  }
  return { type: 'error', text: msg.slice(0, 40), retry: false }
}

Page({
  data: {
    form: { title: '', content: '', mediaType: 'NONE', mediaUrl: '' },
    hasImage: false,
    hasVideo: false,
    uploading: false,
    submitting: false,
    errorInfo: null
  },

  onLoad() {
    const draft = wx.getStorageSync('submissionDraft')
    if (draft) {
      this.setData({
        'form.title': draft.title || '',
        'form.content': draft.content || ''
      })
      wx.removeStorageSync('submissionDraft')
    }
  },

  onInputTitle(e) {
    this.setData({ 'form.title': e.detail.value, errorInfo: null })
  },

  onInputContent(e) {
    this.setData({ 'form.content': e.detail.value, errorInfo: null })
  },

  onInputMediaUrl(e) {
    this.setData({ 'form.mediaUrl': e.detail.value })
  },

  chooseMedia() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image', 'video'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file || !file.tempFilePath) return
        this.uploadMedia(file.tempFilePath, file.fileType)
      }
    })
  },

  uploadMedia(filePath, fileType) {
    this.setData({ uploading: true, errorInfo: null })
    const token = wx.getStorageSync('accessToken')

    wx.uploadFile({
      url: `${BASE_URL}/api/files/upload`,
      filePath,
      name: 'file',
      header: { Authorization: token ? `Bearer ${token}` : '' },
      success: (res) => {
        try {
          const payload = JSON.parse(res.data)
          if (payload.code !== 0 || !payload.data || !payload.data.url) {
            throw new Error(payload.message || '上传失败')
          }
          const isVideo = fileType === 'video'
          this.setData({
            'form.mediaUrl': payload.data.url,
            'form.mediaType': isVideo ? 'VIDEO' : 'IMAGE',
            hasImage: !isVideo,
            hasVideo: isVideo
          })
          wx.showToast({ title: '上传成功', icon: 'none' })
        } catch (err) {
          wx.showToast({ title: '上传失败，请重试', icon: 'none' })
        }
      },
      fail: () => {
        wx.showToast({ title: '上传失败，请检查网络', icon: 'none' })
      },
      complete: () => {
        this.setData({ uploading: false })
      }
    })
  },

  clearMedia() {
    this.setData({
      'form.mediaType': 'NONE', 'form.mediaUrl': '',
      hasImage: false, hasVideo: false
    })
  },

  ensureLogin() {
    if (getApp().isLoggedIn()) return true
    wx.showModal({
      title: '请先登录',
      content: '登录后即可发布投稿',
      success: (res) => {
        if (res.confirm) wx.navigateTo({ url: '/pages/login/index' })
      }
    })
    return false
  },

  async submit() {
    if (!this.ensureLogin()) return
    const title = (this.data.form.title || '').trim()
    const content = (this.data.form.content || '').trim()

    if (!title) {
      this.setData({ errorInfo: { type: 'validation', text: '请输入标题' } })
      return
    }
    if (title.length < 2) {
      this.setData({ errorInfo: { type: 'validation', text: '标题至少输入2个字' } })
      return
    }
    if (!content) {
      this.setData({ errorInfo: { type: 'validation', text: '请输入正文内容' } })
      return
    }
    if (content.length < 10) {
      this.setData({ errorInfo: { type: 'validation', text: '正文内容至少10个字' } })
      return
    }

    this.setData({ submitting: true, errorInfo: null })

    try {
      await request('/api/submissions', 'POST', this.data.form)
      wx.showToast({ title: '发布成功，等待审核', icon: 'none' })
      this.setData({
        form: { title: '', content: '', mediaType: 'NONE', mediaUrl: '' },
        hasImage: false, hasVideo: false, submitting: false
      })
      setTimeout(() => { wx.navigateBack() }, 800)
    } catch (error) {
      const info = classifyError(error)
      this.setData({ errorInfo: info, submitting: false })
      if (info.type !== 'auth') {
        wx.showToast({ title: info.text, icon: 'none' })
      }
    }
  },

  dismissError() {
    this.setData({ errorInfo: null })
  }
})
