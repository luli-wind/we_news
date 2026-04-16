const { request } = require('../../utils/request')

const fallbackCover = 'https://images.unsplash.com/photo-1470770903676-69b98201ea1c?auto=format&fit=crop&w=1200&q=60'

function relativeTime(source) {
  if (!source) return '刚刚'
  const date = new Date(source)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const gap = Date.now() - date.getTime()
  if (gap < 60 * 1000) return '刚刚'
  if (gap < 60 * 60 * 1000) return `${Math.floor(gap / 60000)}分钟前`
  if (gap < 24 * 60 * 60 * 1000) return `${Math.floor(gap / 3600000)}小时前`
  return `${Math.floor(gap / 86400000)}天前`
}

function buildTabs(active) {
  const names = ['推荐', '热点', '天津', '视频', '社会', '图片']
  return names.map((name, index) => ({
    name,
    index,
    activeClass: active === index ? 'tab-item--active' : ''
  }))
}

Page({
  data: {
    keyword: '',
    loading: false,
    activeTab: 0,
    tabs: buildTabs(0),
    hero: null,
    feed: [],
    showEmpty: false
  },

  onShow() {
    this.loadNews()
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  switchTab(e) {
    const active = Number(e.currentTarget.dataset.index) || 0
    this.setData({
      activeTab: active,
      tabs: buildTabs(active)
    })
  },

  search() {
    this.loadNews()
  },

  async loadNews() {
    this.setData({ loading: true })
    try {
      const data = await request('/api/news', 'GET', {
        page: 1,
        pageSize: 20,
        keyword: this.data.keyword
      })

      const list = data && Array.isArray(data.list) ? data.list : []
      if (!list.length) {
        this.setData({ hero: null, feed: [], showEmpty: true })
        return
      }

      const mapped = list.map((item, idx) => {
        const coverUrl = item.coverUrl || fallbackCover
        return {
          id: item.id,
          title: item.title || '未命名新闻',
          summary: item.summary || '暂无摘要',
          categoryText: item.category || '推荐',
          coverUrl,
          relativeTime: relativeTime(item.publishedAt || item.createdAt),
          commentCount: (item.id % 3000) + 20,
          sourceText: item.sourceName || item.category || '新闻源',
          isSingle: idx % 3 !== 0,
          isTriple: idx % 3 === 0,
          thumbs: [coverUrl, `${fallbackCover}&seed=2`, `${fallbackCover}&seed=3`]
        }
      })

      this.setData({
        hero: mapped[0],
        feed: mapped.slice(1),
        showEmpty: false
      })
    } catch (error) {
      const msg = (error && error.message) ? error.message.slice(0, 32) : '加载失败'
      wx.showToast({ title: msg, icon: 'none' })
      this.setData({ hero: null, feed: [], showEmpty: true })
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/news/detail?id=${id}` })
  }
})
