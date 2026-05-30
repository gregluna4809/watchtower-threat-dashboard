/// <reference types="vitest/config" />

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8088',
      },
      '/ws': {
        target: 'http://127.0.0.1:8088',
        ws: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
  },
});
