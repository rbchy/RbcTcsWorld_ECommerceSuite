import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The React dev server (5173) forwards every /api call to the Spring Boot backend (8081),
// so the browser talks to one origin and no CORS setup is needed.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
});
