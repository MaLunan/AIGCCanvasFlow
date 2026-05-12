<script setup>
// 登录/注册页面：包含两个 Tab，共用 loading/error 状态，注册成功后自动登录
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router    = useRouter()
const route     = useRoute()   // 用于读取 redirect 参数（从受保护页面跳转过来时携带）
const authStore = useAuthStore()

const tab     = ref('login')  // 当前激活的 Tab：'login' | 'register'
const loading = ref(false)    // 请求进行中（禁用提交按钮）
const error   = ref('')       // 错误提示信息

// 用 reactive 管理表单数据，方便直接 v-model 绑定
const loginForm    = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '', email: '' })

/** 切换 Tab 时清空错误提示 */
function switchTab(t) {
  tab.value   = t
  error.value = ''
}

/** 登录处理：前端校验 → authStore.login → 跳转到目标页 */
async function handleLogin() {
  error.value = ''
  if (!loginForm.username || !loginForm.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    await authStore.login({ username: loginForm.username, password: loginForm.password })
    // 登录成功后跳回原本要访问的页面，没有 redirect 则默认去项目列表
    const redirect = route.query.redirect || '/projects'
    router.push(redirect)
  } catch (e) {
    error.value = e.message || '登录失败，请检查用户名或密码'
  } finally {
    loading.value = false
  }
}

/** 注册处理：前端校验 → authStore.register → 自动登录 → 跳转项目列表 */
async function handleRegister() {
  error.value = ''
  // 基础必填校验
  if (!registerForm.username || !registerForm.password || !registerForm.email) {
    error.value = '请填写所有必填项'
    return
  }
  // 两次密码一致性校验
  if (registerForm.password !== registerForm.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await authStore.register({
      username: registerForm.username,
      password: registerForm.password,
      email:    registerForm.email,
    })
    // 注册成功后自动登录，提升用户体验（无需二次填写）
    await authStore.login({ username: registerForm.username, password: registerForm.password })
    router.push('/projects')
  } catch (e) {
    error.value = e.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-bg">
      <div class="orb orb1" />
      <div class="orb orb2" />
      <div class="grid-lines" />
    </div>

    <!-- Logo -->
    <a class="logo" href="/">
      <span class="logo-hex">⬡</span>
      <span class="logo-name">CanvasFlow</span>
      <span class="logo-badge">AIGC</span>
    </a>

    <!-- Card -->
    <div class="card">
      <!-- Tabs -->
      <div class="tabs">
        <button :class="['tab', tab === 'login' && 'tab-active']" @click="switchTab('login')">登录</button>
        <button :class="['tab', tab === 'register' && 'tab-active']" @click="switchTab('register')">注册</button>
      </div>

      <!-- Login Form -->
      <form v-if="tab === 'login'" class="form" @submit.prevent="handleLogin">
        <div class="field">
          <label class="label">用户名</label>
          <input
            v-model="loginForm.username"
            class="input"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>
        <div class="field">
          <label class="label">密码</label>
          <input
            v-model="loginForm.password"
            class="input"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>
        <div v-if="error" class="error">{{ error }}</div>
        <button class="submit" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner" />
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <p class="switch-tip">
          还没有账号？
          <a class="switch-link" @click="switchTab('register')">立即注册</a>
        </p>
      </form>

      <!-- Register Form -->
      <form v-else class="form" @submit.prevent="handleRegister">
        <div class="field">
          <label class="label">用户名 <span class="required">*</span></label>
          <input
            v-model="registerForm.username"
            class="input"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>
        <div class="field">
          <label class="label">邮箱 <span class="required">*</span></label>
          <input
            v-model="registerForm.email"
            class="input"
            type="email"
            placeholder="请输入邮箱"
            autocomplete="email"
          />
        </div>
        <div class="field">
          <label class="label">密码 <span class="required">*</span></label>
          <input
            v-model="registerForm.password"
            class="input"
            type="password"
            placeholder="请设置密码"
            autocomplete="new-password"
          />
        </div>
        <div class="field">
          <label class="label">确认密码 <span class="required">*</span></label>
          <input
            v-model="registerForm.confirmPassword"
            class="input"
            type="password"
            placeholder="请再次输入密码"
            autocomplete="new-password"
          />
        </div>
        <div v-if="error" class="error">{{ error }}</div>
        <button class="submit" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner" />
          {{ loading ? '注册中...' : '注册并登录' }}
        </button>
        <p class="switch-tip">
          已有账号？
          <a class="switch-link" @click="switchTab('login')">立即登录</a>
        </p>
      </form>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.page {
  min-height: 100vh;
  background: $bg-base;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.page-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;

  &1 { width: 500px; height: 500px; background: $accent-primary; top: -150px; left: -100px; }
  &2 { width: 400px; height: 400px; background: $accent-green; bottom: -100px; right: -100px; }
}

.grid-lines {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(100, 108, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(100, 108, 255, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}

.logo {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  margin-bottom: 32px;

  &-hex { font-size: 24px; color: $accent-primary; }
  &-name { font-size: 18px; font-weight: 700; color: $text-primary; }
  &-badge {
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 1px;
    background: rgba($accent-primary, 0.13);
    border: 1px solid rgba($accent-primary, 0.27);
    color: #a0aaff;
    padding: 2px 5px;
    border-radius: $radius-sm;
  }
}

.card {
  position: relative;
  width: 100%;
  max-width: 400px;
  background: $bg-surface;
  border: 1px solid $border-subtle;
  border-radius: $radius-2xl;
  padding: 32px;
  box-shadow: 0 0 60px rgba(100, 108, 255, 0.08);
}

.tabs {
  display: flex;
  border-bottom: 1px solid $border-subtle;
  margin-bottom: 28px;
}

.tab {
  flex: 1;
  padding: 10px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: $text-muted;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: $transition-fast;
  margin-bottom: -1px;

  &:hover { color: $text-secondary; }
  &-active {
    color: $accent-primary;
    border-bottom-color: $accent-primary;
  }
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 12px;
  font-weight: 600;
  color: #8080a0;
  letter-spacing: 0.3px;
}

.required { color: $accent-red; }

.input {
  background: $bg-input;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-primary;
  font-size: 14px;
  padding: 10px 12px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s;

  &:focus { border-color: rgba($accent-primary, 0.53); }
  &::placeholder { color: $text-faint; }
}

.error {
  background: rgba($accent-red, 0.09);
  border: 1px solid rgba($accent-red, 0.27);
  border-radius: $radius-md;
  color: #ff9999;
  font-size: 12px;
  padding: 8px 12px;
}

.submit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
  padding: 11px;
  background: $accent-primary;
  border: 1px solid $accent-primary;
  border-radius: $radius-lg;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: $transition-fast;

  &:hover:not(:disabled) { background: #7c83ff; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  flex-shrink: 0;
}

@keyframes spin { to { transform: rotate(360deg); } }

.switch-tip {
  text-align: center;
  font-size: 12px;
  color: $text-dim;
  margin-top: 4px;
}

.switch-link {
  color: $accent-primary;
  cursor: pointer;

  &:hover { color: #a0aaff; }
}
</style>
