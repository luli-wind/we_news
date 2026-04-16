const { request } = require('../../../utils/request')

Page({
  data: {
    list: []
  },

  onShow() {
    this.loadList()
  },

  async loadList() {
    try {
      const data = await request('/api/me/submissions', 'GET', { page: 1, pageSize: 50 })
      this.setData({ list: data && Array.isArray(data.list) ? data.list : [] })
    } catch (error) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  }
})