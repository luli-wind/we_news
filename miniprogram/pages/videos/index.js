const { request } = require('../../utils/request')

const fallbackCover = 'https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=1200&q=60'

function formatPlayCount(count) {
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万次播放`
  if (count >= 1000) return `${(count / 1000).toFixed(0)}千次播放`
  return `${count || 0}次播放`
}

Page({
  data: {
    keyword: '',
    loading: false,
    featured: null,
    list: []
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 })
    }
    this.loadVideos()
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  search() {
    this.loadVideos()
  },

  async loadVideos() {
    this.setData({ loading: true })
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
        authorText: item.category || '创作者',
        playCount: formatPlayCount((idx + 8) * 1200)
      })) : []

      this.setData({
        featured: list.length ? list[0] : null,
        list: list.slice(1)
      })
    } catch (error) {
      wx.showToast({ title: '加载失败，请检查网络连接', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/videos/detail?id=${id}` })
  },

  goFeaturedDetail() {
    const id = this.data.featured && this.data.featured.id
    if (!id) return
    wx.navigateTo({ url: `/pages/videos/detail?id=${id}` })
  }
})