import { createRouter, createWebHistory } from 'vue-router'

// Import the views
import MapView from '../views/MapView.vue'

// Define routes
const routes = [
  {
    path: '/',
    name: 'home',
    component: MapView
  },
  // Add more routes here as needed
]

// Create router instance
const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router