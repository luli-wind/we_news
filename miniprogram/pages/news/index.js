const { request } = require('../../utils/request')
const { resolveImageUrl } = require('../../utils/config')

function highlightTitle(title, keyword) {
  if (!keyword || !title) return [{ text: title || '未命名新闻', hl: false }]
  const kw = keyword.trim()
  if (!kw) return [{ text: title || '未命名新闻', hl: false }]
  const parts = title.split(new RegExp(`(${kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi'))
  return parts.filter(Boolean).map(text => ({
    text,
    hl: text.toLowerCase() === kw.toLowerCase()
  }))
}

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

const CHANNEL_HEADLINE = 'headline'
const CHANNEL_DYNAMIC = 'dynamic'

Page({
  data: {
    keyword: '',
    loading: false,
    channel: CHANNEL_HEADLINE,
    headline: { feed: [], page: 1, total: 0, hasMore: true, showEmpty: false },
    dynamic: { feed: [], page: 1, total: 0, hasMore: true, showEmpty: false }
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 })
    }
    this.loadFeed()
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onConfirmSearch() {
    const channel = this.data.channel
    this.setData({
      [`${channel}.page`]: 1,
      [`${channel}.feed`]: [],
      [`${channel}.hasMore`]: true
    })
    this.loadFeed()
  },

  switchChannel(e) {
    const channel = e.currentTarget.dataset.channel
    if (channel === this.data.channel) return
    this.setData({ channel, keyword: '' })
    this.loadFeed()
  },

  loadMore() {
    const channel = this.data.channel
    const state = this.data[channel]
    if (!state.hasMore || this.data.loading) return
    this.setData({ [`${channel}.page`]: state.page + 1 })
    this.loadFeed()
  },

  onShareAppMessage() {
    return {
      title: '今日资讯 - 随时随地了解最新动态',
      path: '/pages/news/index'
    }
  },

  onShareTimeline() {
    return {
      title: '今日资讯 - 随时随地了解最新动态'
    }
  },

  onReachBottom() {
    this.loadMore()
  },

  async loadFeed() {
    const channel = this.data.channel
    const state = this.data[channel]
    if (this.data.loading) return
    this.setData({ loading: true })

    try {
      const params = { page: state.page, pageSize: 10, keyword: this.data.keyword }
      if (channel === CHANNEL_HEADLINE) {
        params.excludeCategory = '用户投稿'
      } else {
        params.category = '用户投稿'
      }

      const data = await request('/api/news', 'GET', params)
      const isDynamic = channel === CHANNEL_DYNAMIC
      const list = (data && Array.isArray(data.list) ? data.list : []).map((item) => ({
        id: item.id,
        title: item.title || '未命名新闻',
        titleParts: highlightTitle(item.title, this.data.keyword),
        summary: item.summary || '',
        categoryText: item.category || '推荐',
        coverUrl: resolveImageUrl(item.coverUrl),
        relativeTime: relativeTime(item.publishedAt || item.createdAt),
        sourceText: isDynamic
          ? (item.authorName || item.sourceName || '用户')
          : (item.sourceName || item.category || '新闻源')
      }))

      const isFirstPage = state.page === 1
      const feed = isFirstPage ? list : [...state.feed, ...list]
      const total = data && data.total ? data.total : 0

      this.setData({
        [`${channel}.feed`]: feed,
        [`${channel}.total`]: total,
        [`${channel}.hasMore`]: list.length >= 10,
        [`${channel}.showEmpty`]: isFirstPage && feed.length === 0
      })
    } catch (error) {
      const msg = (error && error.message) ? error.message.slice(0, 32) : '加载失败'
      wx.showToast({ title: msg, icon: 'none' })
      if (state.page === 1) {
        this.setData({
          [`${channel}.feed`]: [],
          [`${channel}.showEmpty`]: true
        })
      }
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/news/detail?id=${id}` })
  },

  goSubmit() {
    wx.navigateTo({ url: '/pages/submission/index' })
  },

  onPullDownRefresh() {
    const channel = this.data.channel
    this.setData({
      [`${channel}.page`]: 1,
      [`${channel}.feed`]: [],
      [`${channel}.hasMore`]: true
    })
    this.loadFeed().then(() => wx.stopPullDownRefresh())
  },

  async onRefreshTap() {
    try {
      wx.showLoading({ title: '刷新数据中...' })
      await request('/api/news/sync/refresh', 'GET')
      wx.hideLoading()
      wx.showToast({ title: '数据已刷新', icon: 'none' })
      const channel = this.data.channel
      this.setData({
        [`${channel}.page`]: 1,
        [`${channel}.feed`]: [],
        [`${channel}.hasMore`]: true
      })
      this.loadFeed()
    } catch (error) {
      wx.hideLoading()
      wx.showToast({ title: '刷新失败，请检查网络', icon: 'none' })
    }
  }
})
