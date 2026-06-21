import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/survey',
      name: 'survey',
      component: () => import('@/views/SurveyView.vue'),
    },
    {
      path: '/result',
      name: 'result',
      component: () => import('@/views/ResultView.vue'),
    },
    {
      path: '/properties/:id',
      name: 'property-detail',
      component: () => import('@/views/PropertyDetailView.vue'),
      props: true,
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/FavoritesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/my-page',
      name: 'my-page',
      component: () => import('@/views/MyPageView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/agent/properties',
      name: 'agent-properties',
      component: () => import('@/views/AgentPropertiesView.vue'),
      meta: { requiresAuth: true, roles: ['AGENT'] },
    },
    {
      path: '/agent/properties/new',
      name: 'agent-property-new',
      component: () => import('@/views/PropertyFormView.vue'),
      meta: { requiresAuth: true, roles: ['AGENT'] },
    },
    {
      path: '/agent/properties/:id/edit',
      name: 'agent-property-edit',
      component: () => import('@/views/PropertyFormView.vue'),
      props: true,
      meta: { requiresAuth: true, roles: ['AGENT'] },
    },
    {
      path: '/admin',
      name: 'admin-dashboard',
      component: () => import('@/views/AdminDashboardView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/views/AdminUsersView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/admin/reports',
      name: 'admin-reports',
      component: () => import('@/views/AdminReportsView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/map',
      name: 'map',
      component: () => import('@/views/MapView.vue'),
    },
    {
      path: '/compare',
      name: 'compare',
      component: () => import('@/views/CompareView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/signup',
      name: 'signup',
      component: () => import('@/views/SignupView.vue'),
    },
    {
      path: '/forbidden',
      name: 'forbidden',
      component: () => import('@/views/ForbiddenView.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.isInitialized && (shouldInitializeAuth(to) || authStore.accessToken)) {
    await authStore.initializeAuth()
  }

  if ((to.name === 'login' || to.name === 'signup') && authStore.isAuthenticated) {
    return routeAfterAuth(to.query.redirect)
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  const allowedRoles = Array.isArray(to.meta.roles) ? to.meta.roles : []

  if (allowedRoles.length && !authStore.hasRole(allowedRoles)) {
    return {
      name: 'forbidden',
      query: { from: to.fullPath },
    }
  }

  return true
})

function routeAfterAuth(redirect) {
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect
  }

  return { name: 'home' }
}

function shouldInitializeAuth(to) {
  const hasRoleGuard = Array.isArray(to.meta.roles) && to.meta.roles.length > 0
  const isAuthPage = to.name === 'login' || to.name === 'signup'

  return to.meta.requiresAuth || hasRoleGuard || isAuthPage
}

export default router
