import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guest: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { guest: true }
    },
    {
      path: '/consumer',
      component: () => import('@/components/AppLayout.vue'),
      meta: { requiresAuth: true, role: 'ROLE_CONSUMER' },
      children: [
        { path: '', redirect: '/consumer/home' },
        { path: 'home', name: 'home', component: () => import('@/views/consumer/HomeView.vue') },
        { path: 'merchant/:id', name: 'merchant-detail', component: () => import('@/views/consumer/MerchantDetail.vue') },
        { path: 'checkout/:merchantId', name: 'checkout', component: () => import('@/views/consumer/CheckoutView.vue') },
        { path: 'orders', name: 'orders', component: () => import('@/views/consumer/OrdersView.vue') },
        { path: 'order/:id', name: 'order-detail', component: () => import('@/views/consumer/OrderDetail.vue') },
        { path: 'order/:id/review', name: 'review', component: () => import('@/views/consumer/ReviewView.vue') },
        { path: 'coupons', name: 'coupons', component: () => import('@/views/consumer/CouponsView.vue') },
        { path: 'search', name: 'search', component: () => import('@/views/consumer/SearchView.vue') },
        { path: 'profile', name: 'profile', component: () => import('@/views/consumer/ProfileView.vue') },
        { path: 'address', name: 'address', component: () => import('@/views/consumer/AddressView.vue') },
        { path: 'balance', name: 'balance', component: () => import('@/views/consumer/BalanceView.vue') }
      ]
    },
    {
      path: '/merchant',
      component: () => import('@/components/MerchantLayout.vue'),
      meta: { requiresAuth: true, role: 'ROLE_MERCHANT' },
      children: [
        { path: '', redirect: '/merchant/orders' },
        { path: 'orders', name: 'merchant-orders', component: () => import('@/views/merchant/DashboardView.vue') },
        { path: 'order/:id', name: 'merchant-order-detail', component: () => import('@/views/merchant/OrderDetail.vue') },
        { path: 'products', name: 'merchant-products', component: () => import('@/views/merchant/ProductsView.vue') },
        { path: 'reviews', name: 'merchant-reviews', component: () => import('@/views/merchant/ReviewsView.vue') },
        { path: 'earnings', name: 'merchant-earnings', component: () => import('@/views/merchant/EarningsView.vue') },
        { path: 'shop', name: 'merchant-shop', component: () => import('@/views/merchant/ShopEdit.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' }
  ]
})

// 检查 JWT 是否过期
function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 < Date.now()
  } catch { return true }
}

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('accessToken')
  const role = localStorage.getItem('role')

  // 如果 token 过期，清除并跳转登录
  if (token && isTokenExpired(token)) {
    localStorage.clear()
    if (!to.meta.guest) return next('/login')
  }

  // 已登录访问游客页 → 按角色跳转
  if (to.meta.guest && token && !isTokenExpired(token)) {
    return next(role === 'ROLE_CONSUMER' ? '/consumer/home' : '/merchant/orders')
  }

  // 需要认证
  if (to.meta.requiresAuth) {
    if (!token || isTokenExpired(token)) return next('/login')
    if (to.meta.role && to.meta.role !== role) {
      return next(role === 'ROLE_CONSUMER' ? '/consumer/home' : '/merchant/orders')
    }
  }

  next()
})

export default router
