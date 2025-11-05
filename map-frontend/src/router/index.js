import { createRouter, createWebHistory } from 'vue-router'

// Import the views
import MapView from '../views/MapView.vue'
import FaqView from '../views/FaqView.vue'
import AboutView from '../views/AboutView.vue'
import ProviderManagement from '../components/ProviderManagement.vue'
import Login from '../components/Login.vue'
import OnboardingFlowTest from '../components/OnboardingFlowTest.vue'
import { authService } from '../services/auth'

// Define routes
const routes = [
  {
    path: '/',
    name: 'home',
    component: MapView
  },
  {
    path: '/faq',
    name: 'faq',
    component: FaqView
  },
  {
    path: '/about',
    name: 'about',
    component: AboutView
  },
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { requiresGuest: true }
  },
  {
    path: '/providers',
    name: 'providers',
    component: ProviderManagement,
    meta: { requiresAuth: true }
  },
  {
    path: '/test-onboarding',
    name: 'test-onboarding',
    component: OnboardingFlowTest
  },
  // Add more routes here as needed
]

// Create router instance
const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guards
router.beforeEach((to, from, next) => {
  const isAuthenticated = authService.isAuthenticated();
  
  if (to.meta.requiresAuth && !isAuthenticated) {
    // Redirect to login with return URL
    next({
      name: 'login',
      query: { redirect: to.fullPath }
    });
  } else if (to.meta.requiresGuest && isAuthenticated) {
    // Redirect authenticated users away from login
    next({ name: 'providers' });
  } else {
    next();
  }
});

export default router