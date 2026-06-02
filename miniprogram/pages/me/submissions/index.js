const { request } = require('../../../utils/request')
const { resolveImageUrl } = require('../../../utils/config')

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
        mediaUrl: resolveImageUrl(item.mediaUrl),
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
    wx.setStorageSync('submissionDraft', {
      title: item.title || '',
      content: item.content || ''
    })
    wx.navigateTo({ url: '/pages/submission/index' })
  },

  editItem(e) {
    const item = e.currentTarget.dataset.item
    if (!item) return
    wx.setStorageSync('submissionDraft', {
      editId: item.id,
      title: item.title || '',
      content: item.content || '',
      mediaType: item.mediaType || '',
      mediaUrl: item.mediaUrl || ''
    })
    wx.navigateTo({ url: '/pages/submission/index' })
  },

  deleteItem(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.showModal({
      title: '删除投稿',
      content: '确定要删除这条投稿吗？删除后不可恢复。',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await request(`/api/submissions/${id}`, 'DELETE')
          wx.showToast({ title: '已删除', icon: 'none' })
          this.loadList()
        } catch (error) {
          const msg = (error && error.message) || '删除失败'
          wx.showToast({ title: msg.slice(0, 20), icon: 'none' })
        }
      }
    })
  },

  goDetail(e) {
    const newsId = e.currentTarget.dataset.newsId
    if (!newsId) return
    wx.navigateTo({ url: `/pages/news/detail?id=${newsId}` })
  }
})
