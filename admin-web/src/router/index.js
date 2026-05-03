import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import NewsManage from '../views/NewsManage.vue'
import VideoManage from '../views/VideoManage.vue'
import CommentManage from '../views/CommentManage.vue'
import SubmissionManage from '../views/SubmissionManage.vue'
import MediaLibrary from '../views/MediaLibrary.vue'
import UserManage from '../views/UserManage.vue'
import LogsManage from '../views/LogsManage.vue'

const routes = [
  { path: '/login', component: Login, meta: { title: '登录' } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: Dashboard, meta: { auth: true, title: '仪表盘' } },
  { path: '/news', component: NewsManage, meta: { auth: true, title: '新闻管理' } },
  { path: '/videos', component: VideoManage, meta: { auth: true, title: '视频管理' } },
  { path: '/comments', component: CommentManage, meta: { auth: true, title: '评论管理' } },
  { path: '/submissions', component: SubmissionManage, meta: { auth: true, title: '投稿审核' } },
  { path: '/media', component: MediaLibrary, meta: { auth: true, title: '媒体资源库' } },
  { path: '/users', component: UserManage, meta: { auth: true, adminOnly: true, title: '用户与角色' } },
  { path: '/logs', component: LogsManage, meta: { auth: true, adminOnly: true, title: '操作日志' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  const roles = JSON.parse(localStorage.getItem('roles') || '[]')

  if (to.meta.auth && !token) {
    next('/login')
    return
  }

  if (to.meta.adminOnly && !roles.includes('ADMIN')) {
    next('/dashboard')
    return
  }

  next()
})

export default router
