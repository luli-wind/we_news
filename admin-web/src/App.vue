<template>
  <router-view v-if="!hasToken || isLoginPage" />

  <div v-else class="shell">
    <aside class="side">
      <div class="brand-wrap">
        <div class="brand-pill">后台管理</div>
        <h1 class="brand-title">WeNews 控制台</h1>
        <p class="brand-sub">内容运营与审核中心</p>
      </div>

      <el-menu
        class="side-menu"
        :default-active="route.path"
        router
      >
        <el-menu-item index="/dashboard">仪表盘</el-menu-item>
        <el-menu-item index="/news">新闻管理</el-menu-item>
        <el-menu-item index="/videos">视频管理</el-menu-item>
        <el-menu-item index="/comments">评论管理</el-menu-item>
        <el-menu-item index="/submissions">投稿审核</el-menu-item>
        <el-menu-item index="/media">媒体资源库</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">用户与角色</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/logs">操作日志</el-menu-item>
      </el-menu>

      <div class="side-foot">
        <p class="side-foot__label">当前账号</p>
        <p class="side-foot__name">{{ nickname || '管理员' }}</p>
      </div>
    </aside>

    <section class="main">
      <header class="top">
        <div>
          <p class="top-sub">管理后台</p>
<!--          <h2 class="top-title">{{ pageTitle }}</h2>-->
        </div>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </header>

      <main class="content">
        <router-view />
      </main>
    </section>
  </div>
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
const pageTitle = computed(() => route.meta.title || '仪表盘')

const logout = () => {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 258px 1fr;
}

.side {
  background: linear-gradient(170deg, #1f2b3f, #141e2e);
  color: #fff;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.brand-wrap {
  padding: 14px 10px 20px;
}

.brand-pill {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.15);
  font-size: 12px;
  letter-spacing: 0.8px;
}

.brand-title {
  margin: 12px 0 0;
  font-size: 24px;
  line-height: 1.1;
}

.brand-sub {
  margin: 8px 0 0;
  color: rgba(230, 239, 255, 0.72);
  font-size: 13px;
}

.side-menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #b9c7df;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.1);
  --el-menu-item-height: 44px;
  border-radius: 10px;
}

.side-menu :deep(.el-menu-item.is-active) {
  border-radius: 9px;
  background: linear-gradient(125deg, rgba(225, 75, 59, 0.95), rgba(183, 52, 41, 0.95));
}

.side-foot {
  margin-top: auto;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
}

.side-foot__label {
  margin: 0;
  color: rgba(237, 244, 255, 0.6);
  font-size: 12px;
}

.side-foot__name {
  margin: 6px 0 0;
  font-size: 18px;
  font-weight: 700;
}

.main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.top {
  height: 82px;
  padding: 12px 22px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top-sub {
  margin: 0;
  color: var(--ink-2);
  font-size: 12px;
  letter-spacing: 0.6px;
  text-transform: uppercase;
}

.top-title {
  margin: 4px 0 0;
  font-size: 24px;
}

.content {
  padding: 18px 22px 20px;
}

@media (max-width: 1024px) {
  .shell {
    grid-template-columns: 86px 1fr;
  }

  .brand-title,
  .brand-sub,
  .side-foot {
    display: none;
  }

  .brand-wrap {
    text-align: center;
    padding-bottom: 12px;
  }
}
</style>
