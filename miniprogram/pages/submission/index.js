const { request, BASE_URL } = require('../../utils/request')

Page({
  data: {
    form: {
      title: '',
      content: '',
      mediaType: 'NONE',
      mediaUrl: ''
    },
    hasImage: false,
    hasVideo: false,
    uploading: false
  },

  onInputTitle(e) {
    this.setData({ 'form.title': e.detail.value })
  },

  onInputContent(e) {
    this.setData({ 'form.content': e.detail.value })
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
    this.setData({ uploading: true })
    const token = wx.getStorageSync('accessToken')

    wx.uploadFile({
      url: `${BASE_URL}/api/files/upload`,
      filePath,
      name: 'file',
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
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
          wx.showToast({ title: '上传失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.showToast({ title: '上传失败', icon: 'none' })
      },
      complete: () => {
        this.setData({ uploading: false })
      }
    })
  },

  clearMedia() {
    this.setData({
      'form.mediaType': 'NONE',
      'form.mediaUrl': '',
      hasImage: false,
      hasVideo: false
    })
  },

  async submit() {
    const title = (this.data.form.title || '').trim()
    const content = (this.data.form.content || '').trim()
    if (!title) {
      wx.showToast({ title: '标题必填', icon: 'none' })
      return
    }
    if (!content) {
      wx.showToast({ title: '内容必填', icon: 'none' })
      return
    }

    try {
      await request('/api/submissions', 'POST', this.data.form)
      wx.showToast({ title: '发布成功，等待审核', icon: 'none' })
      this.setData({
        form: {
          title: '',
          content: '',
          mediaType: 'NONE',
          mediaUrl: ''
        },
        hasImage: false,
        hasVideo: false
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 500)
    } catch (error) {
      wx.showToast({ title: '发布失败', icon: 'none' })
    }
  }
})