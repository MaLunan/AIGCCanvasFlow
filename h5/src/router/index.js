// 前端路由配置：基于 HTML5 History API，定义页面路径和守卫逻辑
import { createRouter, createWebHistory } from 'vue-router'
import HomePage       from '../views/HomePage.vue'
import FlowCanvas     from '../components/FlowCanvas.vue'
import ProjectsPage   from '../views/ProjectsPage.vue'
import LoginPage      from '../views/LoginPage.vue'
import ModelPage      from '../views/ModelPage.vue'

// 路由表：path → 组件映射
// meta.requiresAuth = true 表示需要登录才能访问
const routes = [
  { path: '/',         name: 'home',     component: HomePage },
  { path: '/login',    name: 'login',    component: LoginPage },
  { path: '/canvas',   name: 'canvas',   component: FlowCanvas,   meta: { requiresAuth: true } },
  { path: '/projects', name: 'projects', component: ProjectsPage, meta: { requiresAuth: true } },
  { path: '/models',   name: 'models',   component: ModelPage },  // 模型广场无需登录浏览
]

const router = createRouter({
  history: createWebHistory(),  // 使用 HTML5 History 模式（URL 无 # 号）
  routes,
})

// 全局前置守卫：处理登录鉴权逻辑
router.beforeEach((to) => {
  const token = localStorage.getItem('access_token')

  // 访问需要认证的页面但未登录 → 跳转到登录页，并保留目标路径用于登录后重定向
  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 已登录用户访问登录页 → 直接跳转到项目列表（避免重复登录）
  if (to.name === 'login' && token) {
    return { name: 'projects' }
  }
})

export default router
