import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import 'mapbox-gl/dist/mapbox-gl.css'

// Create and mount the Vue app
const app = createApp(App)

// Use router
app.use(router)

// Mount app
app.mount('#app')
