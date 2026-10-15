import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Vite config. The important part is the "proxy":
// in development the frontend runs on port 5173 and the backend on port 4000.
// This proxy lets the frontend call "/api/..." and quietly forwards it to the
// backend, so we never have to worry about CORS while developing.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:4000",
        changeOrigin: true,
      },
    },
  },
});
