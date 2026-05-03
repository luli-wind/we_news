import request from './request'

export const adminLogin = (data) => request.post('/api/auth/admin/login', data)

export const fetchNews = (params) => request.get('/api/admin/news', { params })
export const createNews = (data) => request.post('/api/news', data)
export const updateNews = (id, data) => request.put(`/api/news/${id}`, data)
export const deleteNews = (id) => request.delete(`/api/news/${id}`)
export const syncDomesticNews = (data = {}) => request.post('/api/admin/news/sync/domestic', data)

export const fetchVideos = (params) => request.get('/api/admin/videos', { params })
export const createVideo = (data) => request.post('/api/videos', data)
export const updateVideo = (id, data) => request.put(`/api/videos/${id}`, data)
export const deleteVideo = (id) => request.delete(`/api/videos/${id}`)

export const fetchComments = (params) => request.get('/api/comments', { params })
export const fetchAllComments = (params) => request.get('/api/comments/admin/all', { params })
export const deleteComment = (id) => request.delete(`/api/comments/admin/${id}`)

export const fetchSubmissions = (params) => request.get('/api/submissions/admin', { params })
export const auditSubmission = (id, data) => request.put(`/api/submissions/admin/${id}/audit`, data)

export const fetchUsers = (params) => request.get('/api/admin/users', { params })
export const fetchRoles = () => request.get('/api/admin/roles')
export const assignUserRole = (data) => request.post('/api/admin/users/assign-role', data)

export const fetchOperationLogs = (params) => request.get('/api/admin/logs/operations', { params })

export const fetchDashboard = () => request.get('/api/admin/dashboard')

export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/files/upload', formData)
}

