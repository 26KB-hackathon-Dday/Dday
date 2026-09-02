import { createRouter, createWebHistory } from 'vue-router'
import PocketView from '../views/PocketView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'pockets',
      component: PocketView,
    },
  ],
})

export default router
