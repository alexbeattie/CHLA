import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import 'mapbox-gl/dist/mapbox-gl.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import { initAnalytics } from './utils/analytics'

// Create Pinia store
const pinia = createPinia()

// Create and mount the Vue app
const app = createApp(App)

// Use Pinia, router, and i18n
app.use(pinia)
app.use(router)
app.use(i18n)

// Mount app
app.mount('#app')

// Signal the prerender step that the initial route has resolved and the DOM
// (including useSeo head updates) is ready to be snapshotted.
router.isReady().then(() => {
  // Two ticks: first ensures the matched view is rendered, second ensures
  // useSeo's watchEffect has applied. Wrap in microtasks to keep it cheap.
  Promise.resolve()
    .then(() => Promise.resolve())
    .then(() => {
      document.dispatchEvent(new Event('render-event'))
    })
})

// Initialize Google Analytics (production only, requires VITE_GA_MEASUREMENT_ID in .env)
// To enable: Set VITE_GA_MEASUREMENT_ID=G-XXXXXXXXXX in .env.production
initAnalytics()
