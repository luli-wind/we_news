const { request } = require('../../utils/request')

function toParagraphs(content) {
  if (!content) return ['暂无正文内容。']
  const lines = String(content)
    .split(/\n+/)
    .map((line) => line.trim())
    .filter(Boolean)
  return lines.length ? lines : ['暂无正文内容。']
}

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

Page({
  data: {
    id: null,
    detail: {},
    authorName: '新闻中心',
    paragraphs: [],
    comments: [],
    commentCount: 0,
    commentText: '',
    replyParentId: null,
    replyHint: '',
    favorited: false,
    favoriteButtonText: '收藏',
    supportCount: 29,
    opposeCount: 2,
    publishTimeText: '刚刚'
  },

  onLoad(options) {
    this.setData({ id: Number(options.id) || 0 })
    this.loadAll()
  },

  async loadAll() {
    await Promise.all([this.loadDetail(), this.loadComments(), this.loadFavoriteStatus()])
  },

  async loadDetail() {
    try {
      const detail = await request(`/api/news/${this.data.id}`)
      const authorName = (detail && (detail.sourceName || detail.category)) || '新闻中心'
      this.setData({
        detail: detail || {},
        authorName,
        paragraphs: toParagraphs(detail && detail.content),
        publishTimeText: toRelativeTime(detail && (detail.publishedAt || detail.createdAt))
      })
    } catch (error) {
      wx.showToast({ title: '详情加载失败', icon: 'none' })
    }
  },

  async loadComments() {
    try {
      const list = await request('/api/comments', 'GET', { bizType: 'NEWS', bizId: this.data.id })
      const comments = Array.isArray(list)
        ? list.map((item, idx) => ({
            ...item,
            displayName: item.userNickname || '用户',
            timeText: toRelativeTime(item.createdAt),
            likeCount: (idx + 1) * 17
          }))
        : []
      this.setData({ comments, commentCount: comments.length })
    } catch (error) {
      wx.showToast({ title: '评论加载失败', icon: 'none' })
    }
  },

  async loadFavoriteStatus() {
    try {
      const result = await request(`/api/favorites/${this.data.id}/status`)
      this.setFavoriteState(Boolean(result && result.favorited))
    } catch (error) {
      this.setFavoriteState(false)
    }
  },

  setFavoriteState(favorited) {
    this.setData({
      favorited,
      favoriteButtonText: favorited ? '已收藏' : '收藏'
    })
  },

  async toggleFavorite() {
    try {
      const result = await request(`/api/favorites/${this.data.id}/toggle`, 'POST')
      const favorited = Boolean(result && result.favorited)
      this.setFavoriteState(favorited)
      wx.showToast({ title: favorited ? '收藏成功' : '取消收藏', icon: 'none' })
    } catch (error) {
      wx.showToast({ title: '操作失败', icon: 'none' })
    }
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
    const content = (this.data.commentText || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' })
      return
    }
    try {
      await request('/api/comments', 'POST', {
        bizType: 'NEWS',
        bizId: this.data.id,
        parentId: this.data.replyParentId,
        content
      })
      this.setData({ commentText: '', replyParentId: null, replyHint: '' })
      wx.showToast({ title: '评论成功', icon: 'none' })
      this.loadComments()
    } catch (error) {
      wx.showToast({ title: '评论失败', icon: 'none' })
    }
  }
})
