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
        <el-menu-item index="/comments">评论管理</el-menu-item>
        <el-menu-item index="/submissions">投稿审核</el-menu-item>
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
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  color: #fff;
  padding: 16px;
  display: flex;
  flex-direction: column;
  position: relative;
}

.side::before {
  content: '';
  position: absolute;
  top: -60px;
  right: -100px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(20, 184, 166, 0.12), transparent 70%);
  pointer-events: none;
}

.brand-wrap {
  padding: 14px 10px 20px;
}

.brand-pill {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(20, 184, 166, 0.2);
  font-size: 12px;
  letter-spacing: 0.8px;
  color: #5eead4;
}

.brand-title {
  margin: 12px 0 0;
  font-size: 24px;
  line-height: 1.1;
  background: linear-gradient(135deg, #ffffff, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-sub {
  margin: 8px 0 0;
  color: rgba(148, 163, 184, 0.7);
  font-size: 13px;
}

.side-menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #94a3b8;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-item-height: 44px;
  border-radius: 10px;
}

.side-menu :deep(.el-menu-item) {
  border-radius: 9px;
  margin-bottom: 2px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.side-menu :deep(.el-menu-item:hover) {
  color: #e2e8f0;
}

.side-menu :deep(.el-menu-item.is-active) {
  border-radius: 9px;
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.55), rgba(13, 148, 136, 0.55));
  box-shadow: 0 4px 15px rgba(20, 184, 166, 0.25);
  font-weight: 600;
}

.side-foot {
  margin-top: auto;
  padding: 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.side-foot__label {
  margin: 0;
  color: rgba(148, 163, 184, 0.6);
  font-size: 12px;
}

.side-foot__name {
  margin: 6px 0 0;
  font-size: 18px;
  font-weight: 700;
  color: #e2e8f0;
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
