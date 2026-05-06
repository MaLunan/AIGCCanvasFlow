import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import FlowCanvas from '../components/FlowCanvas.vue'
import ProjectsPage from '../views/ProjectsPage.vue'
import LoginPage from '../views/LoginPage.vue'
import ModelPage from '../views/ModelPage.vue'

const routes = [
  { path: '/', name: 'home', component: HomePage },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/canvas', name: 'canvas', component: FlowCanvas, meta: { requiresAuth: true } },
  { path: '/projects', name: 'projects', component: ProjectsPage, meta: { requiresAuth: true } },
  { path: '/models', name: 'models', component: ModelPage },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem('access_token')
  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // Redirect logged-in user away from login page
  if (to.name === 'login' && token) {
    return { name: 'projects' }
  }
})

export default router
