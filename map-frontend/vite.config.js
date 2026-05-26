import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import vitePrerender from 'vite-plugin-prerender'

const PRERENDER_ROUTES = [
  '/',
  '/about',
  '/faq',
  '/clinicians',
  '/privacy',
  '/terms',
  '/regional-centers',
  '/regional-centers/san-gabriel-pomona',
  '/regional-centers/harbor',
  '/regional-centers/north-la-county',
  '/regional-centers/eastern-la',
  '/regional-centers/south-central-la',
  '/regional-centers/westside',
  '/regional-centers/lanterman',
]

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  // Load env file based on `mode` in the current working directory.
  // Set the third parameter to '' to load all env regardless of the `VITE_` prefix.
  const env = loadEnv(mode, process.cwd(), '')

  const isProductionBuild = command === 'build' && mode === 'production'
  const disablePrerender = env.DISABLE_PRERENDER === '1'

  const Renderer = vitePrerender.PuppeteerRenderer

  return {
    plugins: [
      vue(),
      ...(isProductionBuild && !disablePrerender
        ? [
          vitePrerender({
            staticDir: path.join(__dirname, 'dist'),
            routes: PRERENDER_ROUTES,
            renderer: new Renderer({
              renderAfterDocumentEvent: 'render-event',
              maxConcurrentRoutes: 2,
              skipThirdPartyRequests: true,
              headless: 'new',
              args: ['--no-sandbox', '--disable-setuid-sandbox'],
            }),
          }),
        ]
        : []),
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    build: {
      // Mapbox GL is intentionally large; keep it isolated in its own vendor
      // chunk while raising the warning threshold to avoid noisy deploy output.
      chunkSizeWarningLimit: 1700,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined
            }
            if (id.includes('mapbox-gl')) {
              return 'vendor-mapbox'
            }
            if (
              id.includes('jspdf') ||
              id.includes('html2canvas') ||
              id.includes('dompurify')
            ) {
              return 'vendor-pdf'
            }
            if (id.includes('vue') || id.includes('pinia')) {
              return 'vendor-vue'
            }
            return 'vendor'
          },
        },
      },
    },
    server: {
      port: env.VITE_PORT || 3000,
      proxy: {
        '/api': {
          target: (env.VITE_API_BASE_URL || 'http://127.0.0.1:8000').replace(/\/+$/, ''),
          changeOrigin: true,
          secure: false,
          ws: true,
        },
        '/graphql': {
          target: (env.VITE_API_BASE_URL || 'http://127.0.0.1:8000').replace(/\/+$/, ''),
          changeOrigin: true,
          secure: false,
          ws: true,
        }
      }
    },
    test: {
      globals: true,
      environment: 'happy-dom',
      setupFiles: ['./src/tests/setup.ts'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json', 'html'],
        exclude: [
          'node_modules/',
          'src/tests/',
          '**/*.spec.ts',
          '**/*.test.ts'
        ]
      }
    }
  }
})
