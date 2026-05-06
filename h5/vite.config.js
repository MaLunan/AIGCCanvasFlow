import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5444,
    headers: {
      // Required for SharedArrayBuffer (ffmpeg.wasm multi-thread mode)
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'credentialless',
    },
    proxy: {
      '/canvas/': {
        target: 'http://127.0.0.1:8084',
        changeOrigin: true,
      },
      '/auth': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/user': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  optimizeDeps: {
    exclude: ['@ffmpeg/ffmpeg', '@ffmpeg/util'],
  },
})
