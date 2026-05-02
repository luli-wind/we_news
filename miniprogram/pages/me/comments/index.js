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
      const data = await request('/api/me/comments', 'GET', { page: 1, pageSize: 50 })
      this.setData({ list: data && Array.isArray(data.list) ? data.list : [] })
    } catch (error) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  goSource(e) {
    const item = e.currentTarget.dataset.item
    if (!item) return
    if (item.bizType === 'NEWS') {
      wx.navigateTo({ url: `/pages/news/detail?id=${item.bizId}` })
    } else if (item.bizType === 'VIDEO') {
      wx.navigateTo({ url: `/pages/videos/detail?id=${item.bizId}` })
    }
  },

  async deleteComment(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.showModal({
      title: '删除评论',
      content: '确定要删除这条评论吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await request(`/api/comments/${id}`, 'DELETE')
          wx.showToast({ title: '已删除', icon: 'none' })
          this.loadList()
        } catch (error) {
          wx.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    })
  }
})
