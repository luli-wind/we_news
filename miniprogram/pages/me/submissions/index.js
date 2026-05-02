const { request } = require('../../../utils/request')

const STATUS_MAP = {
  PENDING: { label: '待审核', class: 'status--pending' },
  APPROVED: { label: '已通过', class: 'status--approved' },
  REJECTED: { label: '已驳回', class: 'status--rejected' }
}

function formatTime(source) {
  if (!source) return ''
  const d = new Date(source)
  if (Number.isNaN(d.getTime())) return ''
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

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
      const data = await request('/api/me/submissions', 'GET', { page: 1, pageSize: 50 })
      const list = (data && Array.isArray(data.list) ? data.list : []).map(item => ({
        ...item,
        statusInfo: STATUS_MAP[item.status] || { label: item.status, class: '' },
        timeText: formatTime(item.createdAt)
      }))
      this.setData({ list, loading: false })
    } catch (error) {
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    }
  },

  resubmit(e) {
    const item = e.currentTarget.dataset.item
    if (!item) return
    const pages = getCurrentPages()
    // Store draft data in the submission page
    wx.setStorageSync('submissionDraft', {
      title: item.title || '',
      content: item.content || ''
    })
    wx.navigateTo({ url: '/pages/submission/index' })
  },

  goDetail(e) {
    const newsId = e.currentTarget.dataset.newsId
    if (!newsId) return
    wx.navigateTo({ url: `/pages/news/detail?id=${newsId}` })
  }
})
