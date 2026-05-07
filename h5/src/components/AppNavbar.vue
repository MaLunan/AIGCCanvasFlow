<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { storeToRefs } from 'pinia'

const router = useRouter()
const route  = useRoute()
const authStore = useAuthStore()
const { isLoggedIn, username } = storeToRefs(authStore)

const navItems = [
  { label: '首页',     to: '/' },
  { label: '我的项目', to: '/projects' },
  { label: '模型库',   to: '/models' },
  { label: '帮助',     to: '/help' },
]

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <nav class="navbar">
    <div class="nav-inner">
      <a class="nav-logo" href="/">
        <span class="logo-hex">⬡</span>
        <span class="logo-name">CanvasFlow</span>
        <span class="logo-badge">AIGC</span>
      </a>

      <div class="nav-links">
        <a
          v-for="item in navItems"
          :key="item.label"
          :href="item.to"
          :class="['nav-link', route.path === item.to && 'active']"
        >{{ item.label }}</a>
      </div>

      <div class="nav-actions">
        <button class="btn-ghost" @click="router.push('/canvas')">开始创作</button>
        <template v-if="isLoggedIn">
          <span class="nav-username">{{ username }}</span>
          <button class="btn-ghost" @click="handleLogout">退出</button>
        </template>
        <button v-else class="btn-primary" @click="router.push('/login')">登录 / 注册</button>
      </div>
    </div>
  </nav>
</template>

<style lang="scss" scoped>
@use '../styles/variables' as *;

.navbar {
  position: sticky;
  top: 0;
  z-index: $z-navbar;
  background: rgba(11, 11, 22, 0.9);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid $border-subtle;
}

.nav-inner {
  max-width: $max-width;
  margin: 0 auto;
  padding: 0 24px;
  height: $navbar-height;
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-hex  { font-size: 22px; color: $accent-primary; }
.logo-name { font-size: 16px; font-weight: 700; color: $text-primary; }

.logo-badge {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1px;
  background: rgba($accent-primary, 0.13);
  border: 1px solid rgba($accent-primary, 0.27);
  color: #a0aaff;
  padding: 2px 5px;
  border-radius: $radius-sm;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 6px 12px;
  font-size: 13px;
  color: #8080a0;
  text-decoration: none;
  border-radius: $radius-sm;
  transition: color 0.15s, background 0.15s;

  &:hover { color: $text-primary; background: rgba(255, 255, 255, 0.031); }
  &.active { color: #a0aaff; background: rgba($accent-primary, 0.09); }
}

.nav-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.nav-username { font-size: 13px; color: $text-secondary; padding: 0 4px; }

.btn-ghost {
  padding: 7px 16px;
  background: none;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  color: $text-secondary;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
  font-family: inherit;

  &:hover { border-color: rgba($accent-primary, 0.53); color: #a0aaff; }
}

.btn-primary {
  padding: 7px 18px;
  background: $accent-primary;
  border: 1px solid $accent-primary;
  border-radius: $radius-md;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;

  &:hover { background: #7c83ff; }
}
</style>
