import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import FlowCanvas from '../components/FlowCanvas.vue'
import ProjectsPage from '../views/ProjectsPage.vue'

const routes = [
  { path: '/', name: 'home', component: HomePage },
  { path: '/canvas', name: 'canvas', component: FlowCanvas },
  { path: '/projects', name: 'projects', component: ProjectsPage },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
