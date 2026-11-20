import { createRouter, createWebHashHistory } from 'vue-router'
import { h } from 'vue'

// hash 路由：单 jar 静态托管时无需后端 fallback 配置
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/books' },
    { path: '/books', component: () => import('./views/BookView.vue'), meta: { title: '图书管理' } },
    { path: '/readers', component: () => import('./views/ReaderView.vue'), meta: { title: '读者管理' } },
    { path: '/loans', component: () => import('./views/LoanView.vue'), meta: { title: '借还办理' } },
    { path: '/shelvings', component: () => import('./views/ShelvingView.vue'), meta: { title: '上架办理' } },
    { path: '/dicts', component: () => import('./views/DictView.vue'), meta: { title: '字典管理' } },
    // 页签全部关闭后的空白页：不生成页签（hiddenTab）
    { path: '/blank', component: { render: () => h('div') }, meta: { hiddenTab: true } }
  ]
})

export default router
