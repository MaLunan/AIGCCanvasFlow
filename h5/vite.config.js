import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    headers: {
      // Required for SharedArrayBuffer (ffmpeg.wasm multi-thread mode)
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'credentialless',
    },
    proxy: {
      '/canvas': {
        target: 'http://10.25.177.15:8084',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/canvas/, '/canvas'),
      },
    },
  },
  optimizeDeps: {
    exclude: ['@ffmpeg/ffmpeg', '@ffmpeg/util'],
  },
})
