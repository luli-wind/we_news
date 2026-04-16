const { request } = require('../../utils/request')

const fallbackCover = 'https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=1200&q=60'

Page({
  data: {
    keyword: '',
    featured: null,
    list: []
  },

  onShow() {
    this.loadVideos()
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  search() {
    this.loadVideos()
  },

  async loadVideos() {
    try {
      const data = await request('/api/videos', 'GET', {
        page: 1,
        pageSize: 20,
        keyword: this.data.keyword
      })
      const list = data && Array.isArray(data.list) ? data.list.map((item, idx) => ({
        id: item.id,
        title: item.title || '未命名视频',
        description: item.description || '暂无描述',
        url: item.url,
        coverUrl: item.coverUrl || fallbackCover,
        authorText: idx % 2 === 0 ? '创作者甲' : '创作者乙',
        playCount: `${(idx + 8) * 12}万次播放`
      })) : []

      this.setData({
        featured: list.length ? list[0] : null,
        list: list.slice(1)
      })
    } catch (error) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  }
})