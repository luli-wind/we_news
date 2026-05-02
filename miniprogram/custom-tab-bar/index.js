Component({
  data: {
    selected: 0,
    list: [
      {
        pagePath: '/pages/news/index',
        text: '首页',
        icon: 'home'
      },
      {
        pagePath: '/pages/videos/index',
        text: '视频',
        icon: 'video'
      },
      {
        pagePath: '/pages/me/index',
        text: '我的',
        icon: 'user'
      }
    ]
  },

  methods: {
    switchTab(e) {
      const index = e.currentTarget.dataset.index
      const item = this.data.list[index]
      wx.switchTab({ url: item.pagePath })
    }
  }
})
