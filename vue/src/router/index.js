import { createRouter, createWebHistory } from 'vue-router'
import PosEditorView from '../views/PosEditorView.vue'
import HandyView from '../views/HandyView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'pos',
      component: PosEditorView,
    },
    {
      path: '/handy',
      name: 'handy',
      component: HandyView,
    },
  ],
})

export default router
