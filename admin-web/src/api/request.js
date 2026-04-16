import axios from 'axios'

const request = axios.create({
  baseURL: '/',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload.code !== 0) {
      return Promise.reject(new Error(payload.message || 'Request failed'))
    }
    return payload.data
  },
  (error) => Promise.reject(error)
)

export default request
