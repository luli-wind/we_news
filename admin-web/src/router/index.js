import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import NewsManage from '../views/NewsManage.vue'
import VideoManage from '../views/VideoManage.vue'
import CommentManage from '../views/CommentManage.vue'
import SubmissionManage from '../views/SubmissionManage.vue'
import UserManage from '../views/UserManage.vue'
import LogsManage from '../views/LogsManage.vue'

const routes = [
  { path: '/login', component: Login },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: Dashboard, meta: { auth: true } },
  { path: '/news', component: NewsManage, meta: { auth: true } },
  { path: '/videos', component: VideoManage, meta: { auth: true } },
  { path: '/comments', component: CommentManage, meta: { auth: true } },
  { path: '/submissions', component: SubmissionManage, meta: { auth: true } },
  { path: '/users', component: UserManage, meta: { auth: true, adminOnly: true } },
  { path: '/logs', component: LogsManage, meta: { auth: true, adminOnly: true } }
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
