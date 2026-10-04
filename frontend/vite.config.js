import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Both the dev server (npm run dev) and the production preview (npm run preview, used by CI)
// forward every /api call to the Spring Boot backend on 8081, so the browser talks to one origin
// and no CORS setup is needed.
const proxy = { '/api': 'http://localhost:8081' };

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy },
  preview: { port: 5173, strictPort: true, proxy },
});
