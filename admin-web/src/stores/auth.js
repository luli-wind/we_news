import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    nickname: localStorage.getItem('nickname') || '',
    roles: JSON.parse(localStorage.getItem('roles') || '[]')
  }),
  actions: {
    saveAuth(payload) {
      this.accessToken = payload.accessToken
      this.refreshToken = payload.refreshToken
      this.nickname = payload.nickname
      this.roles = payload.roles || []
      localStorage.setItem('accessToken', this.accessToken)
      localStorage.setItem('refreshToken', this.refreshToken)
      localStorage.setItem('nickname', this.nickname)
      localStorage.setItem('roles', JSON.stringify(this.roles))
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.nickname = ''
      this.roles = []
      localStorage.clear()
    }
  }
})
