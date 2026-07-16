import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The dev server proxies /api to the gateway so the browser makes same-origin calls and
// there is no CORS to configure while developing.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.VITE_API_BASE ?? "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
