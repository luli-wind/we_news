<template>
  <div class="login-page">
    <section class="login-hero">
      <p class="hero-badge">WE NEWS 管理后台</p>
      <h1 class="hero-title">专为运营与审核设计的一体化控制台。</h1>
      <p class="hero-sub">在一个界面中完成新闻、视频、评论和投稿审核管理。</p>
      <div class="hero-bubbles">
        <span></span><span></span><span></span>
      </div>
    </section>

    <section class="login-box">
      <el-card class="login-card">
        <template #header>
          <div class="login-card__header">
            <h2>登录后台</h2>
            <p>使用管理员账号继续操作</p>
          </div>
        </template>

        <el-form :model="form" @submit.prevent class="login-form">
          <el-form-item label="用户名">
            <el-input v-model="form.username" size="large" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" size="large" show-password />
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%" @click="submit">立即登录</el-button>
        </el-form>

        <p class="hint">默认账号：admin / Admin@123</p>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { adminLogin } from '../api/modules'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  username: 'admin',
  password: 'Admin@123'
})

const submit = async () => {
  try {
    const data = await adminLogin(form)
    auth.saveAuth(data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  background: linear-gradient(130deg, #1f2b3f 0%, #112138 40%, #0a1628 100%);
}

.login-hero {
  color: #f7fbff;
  padding: 72px 66px;
  position: relative;
  overflow: hidden;
}

.login-hero::before {
  content: '';
  position: absolute;
  inset: auto -120px -120px auto;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(14, 165, 164, 0.7), transparent 65%);
}

.hero-badge {
  margin: 0;
  display: inline-block;
  font-size: 12px;
  letter-spacing: 0.8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
}

.hero-title {
  margin: 18px 0 0;
  font-size: clamp(30px, 4vw, 48px);
  line-height: 1.15;
  max-width: 580px;
}

.hero-sub {
  margin: 16px 0 0;
  max-width: 500px;
  color: rgba(230, 243, 255, 0.85);
  line-height: 1.7;
}

.hero-bubbles {
  margin-top: 30px;
  display: flex;
  gap: 10px;
}

.hero-bubbles span {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: linear-gradient(130deg, #f59e0b, #e14b3b);
}

.login-box {
  display: grid;
  place-items: center;
  padding: 26px;
  background: linear-gradient(145deg, #f6f8fd 0%, #e9eef9 100%);
}

.login-card {
  width: min(470px, 100%);
  border: 1px solid #e2e8f5;
  border-radius: 20px;
  box-shadow: 0 30px 50px rgba(18, 33, 52, 0.16);
}

.login-card__header h2 {
  margin: 0;
  font-size: 28px;
}

.login-card__header p {
  margin: 8px 0 0;
  color: #5e6f89;
}

.login-form {
  margin-top: 6px;
}

.hint {
  margin: 16px 0 4px;
  color: #7182a0;
  font-size: 13px;
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    padding: 26px;
  }
}
</style>
