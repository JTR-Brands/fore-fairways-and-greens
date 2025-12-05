import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // Fix for sockjs-client "global is not defined" error
    global: 'globalThis',
  },
  server: {
    port: 3000,
  },
})
