const { request } = require('../../../utils/request')
const { resolveImageUrl } = require('../../../utils/config')

Page({
  data: {
    list: [],
    loading: false
  },

  onShow() {
    this.loadList()
  },

  async loadList() {
    this.setData({ loading: true })
    try {
      const data = await request('/api/me/favorites', 'GET', { page: 1, pageSize: 50 })
      const list = (data && Array.isArray(data.list) ? data.list : []).map(item => ({
        ...item,
        coverUrl: resolveImageUrl(item.coverUrl)
      }))
      this.setData({ list })
    } catch (error) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/news/detail?id=${id}` })
  },

  async unfavorite(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    try {
      await request(`/api/favorites/${id}/toggle`, 'POST')
      wx.showToast({ title: '已取消收藏', icon: 'none' })
      this.loadList()
    } catch (error) {
      wx.showToast({ title: '操作失败', icon: 'none' })
    }
  }
})