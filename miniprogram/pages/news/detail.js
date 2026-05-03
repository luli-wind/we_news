const { request } = require('../../utils/request')

function toParagraphs(content) {
  if (!content) return ['暂无正文内容。']
  const lines = String(content)
    .split(/\n+/)
    .map((line) => line.trim())
    .filter(Boolean)
    .filter(line => !line.startsWith('原文链接：') && !line.startsWith('Original URL:'))
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
    authorName: '新闻中心',
    paragraphs: [],
    mediaImages: [],
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
      const mediaImages = (detail && Array.isArray(detail.media)
        ? detail.media.filter(m => m.mediaType === 'IMAGE')
        : [])
      if (!detail.coverUrl && mediaImages.length > 0) {
        detail.coverUrl = mediaImages[0].url
      }
      this.setData({
        detail: detail || {},
        authorName,
        paragraphs: toParagraphs(detail && detail.content),
        mediaImages,
        publishTimeText: toRelativeTime(detail && (detail.publishedAt || detail.createdAt))
      })
    } catch (error) {
      wx.showToast({ title: '详情加载失败', icon: 'none' })
    }
  },

  async loadComments() {
    try {
      const list = await request('/api/comments', 'GET', { bizType: 'NEWS', bizId: this.data.id })
      const comments = flattenComments(list)
      this.setData({ comments, commentCount: comments.length })
    } catch (error) {
      // comments are optional
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

  ensureLogin() {
    if (getApp().isLoggedIn()) return true
    wx.showModal({
      title: '请先登录',
      content: '登录后即可使用收藏和评论功能',
      success: (res) => {
        if (res.confirm) wx.navigateTo({ url: '/pages/login/index' })
      }
    })
    return false
  },

  async toggleFavorite() {
    if (!this.ensureLogin()) return
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
    if (!this.ensureLogin()) return
    const content = (this.data.commentText || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' })
      return
    }
    if (content.length > 500) {
      wx.showToast({ title: '评论内容不能超过500字', icon: 'none' })
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
      const msg = (error && error.message) || '评论失败'
      if (msg.indexOf('401') >= 0 || msg.indexOf('token') >= 0) {
        wx.showToast({ title: '请先登录后再评论', icon: 'none' })
      } else {
        wx.showToast({ title: msg.slice(0, 20), icon: 'none' })
      }
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
          const msg = (error && error.message) || '删除失败'
          wx.showToast({ title: msg.slice(0, 20), icon: 'none' })
        }
      }
    })
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url
    const urls = this.data.mediaImages.map(m => m.url)
    wx.previewImage({ current: url, urls })
  },

  onCoverError() {
    this.setData({ 'detail.coverUrl': '' })
  },

  onImageError(e) {
    const idx = e.currentTarget.dataset.index
    if (idx !== undefined) {
      this.setData({ [`mediaImages[${idx}].url`]: '' })
    }
  }
})
