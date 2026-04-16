<template>
  <router-view v-if="!hasToken || isLoginPage" />

  <el-container v-else style="height: 100vh">
    <el-aside width="220px" class="aside">
      <div class="logo">WeNews Admin</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">仪表盘</el-menu-item>
        <el-menu-item index="/news">新闻管理</el-menu-item>
        <el-menu-item index="/videos">视频管理</el-menu-item>
        <el-menu-item index="/comments">评论管理</el-menu-item>
        <el-menu-item index="/submissions">投稿审核</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">用户与角色</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/logs">操作日志</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span>欢迎，{{ nickname || '管理员' }}</span>
        <el-button text type="danger" @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const hasToken = computed(() => !!auth.accessToken)
const isLoginPage = computed(() => route.path === '/login')
const nickname = computed(() => auth.nickname)
const isAdmin = computed(() => auth.roles.includes('ADMIN'))

const logout = () => {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.aside {
  background: #0f172a;
  color: #fff;
}
.logo {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
}
</style>
