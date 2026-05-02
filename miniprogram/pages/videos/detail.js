const { request } = require('../../utils/request')

function toRelativeTime(source) {
  if (!source) return '刚刚'
  const date = new Date(source)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const gap = Date.now() - date.getTime()
  if (gap < 60 * 1000) return '刚刚'
  if (gap < 60 * 60 * 1000) return `${Math.floor(gap / 60000)}分钟前`
  if (gap < 24 * 60 * 60 * 1000) return `${Math.floor(gap / 3600000)}小时前`
  return `${Math.floor(gap / 86400000)}天前`
}

function flattenComments(roots) {
  if (!Array.isArray(roots)) return []
  const result = []
  roots.forEach((root) => {
    result.push({ ...root, isReply: false, displayName: root.userNickname || '用户', timeText: toRelativeTime(root.createdAt) })
    if (root.replies && root.replies.length) {
      root.replies.forEach((reply) => {
        result.push({ ...reply, isReply: true, displayName: reply.userNickname || '用户', timeText: toRelativeTime(reply.createdAt) })
      })
    }
  })
  return result
}

Page({
  data: {
    id: null,
    detail: {},
    descriptionLines: [],
    publishTimeText: '',
    comments: [],
    commentCount: 0,
    commentText: '',
    replyParentId: null,
    replyHint: ''
  },

  onLoad(options) {
    this.setData({ id: Number(options.id) || 0 })
    this.loadAll()
  },

  async loadAll() {
    await Promise.all([this.loadDetail(), this.loadComments()])
  },

  async loadDetail() {
    try {
      const detail = await request(`/api/videos/${this.data.id}`)
      this.setData({
        detail: detail || {},
        descriptionLines: detail && detail.description
          ? String(detail.description).split(/\n+/).filter(Boolean)
          : [],
        publishTimeText: toRelativeTime(detail && (detail.publishedAt || detail.createdAt))
      })
    } catch (error) {
      wx.showToast({ title: '视频加载失败', icon: 'none' })
    }
  },

  async loadComments() {
    try {
      const list = await request('/api/comments', 'GET', { bizType: 'VIDEO', bizId: this.data.id })
      const comments = flattenComments(list)
      this.setData({ comments, commentCount: comments.length })
    } catch (error) {
      // comments are optional
    }
  },

  ensureLogin() {
    if (getApp().isLoggedIn()) return true
    wx.showModal({
      title: '请先登录',
      content: '登录后即可使用评论功能',
      success: (res) => {
        if (res.confirm) wx.navigateTo({ url: '/pages/login/index' })
      }
    })
    return false
  },

  onCommentInput(e) {
    this.setData({ commentText: e.detail.value })
  },

  selectReply(e) {
    const id = Number(e.currentTarget.dataset.id)
    const nickname = e.currentTarget.dataset.nickname || '用户'
    this.setData({ replyParentId: id, replyHint: `回复 @${nickname}` })
  },

  clearReply() {
    this.setData({ replyParentId: null, replyHint: '' })
  },

  async submitComment() {
    if (!this.ensureLogin()) return
    const content = (this.data.commentText || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' })
      return
    }
    try {
      await request('/api/comments', 'POST', {
        bizType: 'VIDEO',
        bizId: this.data.id,
        parentId: this.data.replyParentId,
        content
      })
      this.setData({ commentText: '', replyParentId: null, replyHint: '' })
      wx.showToast({ title: '评论成功', icon: 'none' })
      this.loadComments()
    } catch (error) {
      const msg = (error && error.message) || '评论失败'
      wx.showToast({ title: msg.indexOf('401') >= 0 ? '请先登录' : '评论失败', icon: 'none' })
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
          this.loadComments()
        } catch (error) {
          wx.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    })
  }
})
