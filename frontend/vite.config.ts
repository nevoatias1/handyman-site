import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import { seoPlugin } from './build/seo-plugin.ts'

export default defineConfig(({ mode }) => ({
  plugins: [react(), seoPlugin(loadEnv(mode, process.cwd(), '').VITE_SITE_URL)],
  server: {
    host: 'localhost',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
}))
