import { createRouter, createWebHistory } from 'vue-router'
import TopView from '../views/TopView.vue'
import PosEditorView from '../views/PosEditorView.vue'
import HandyView from '../views/HandyView.vue'
import CatalogView from '../views/CatalogView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'top',
      component: TopView,
    },
    {
      path: '/pos',
      name: 'pos',
      component: PosEditorView,
    },
    {
      path: '/handy',
      name: 'handy',
      component: HandyView,
    },
    {
      path: '/catalog',
      name: 'catalog',
      component: CatalogView,
    },
  ],
})

export default router
