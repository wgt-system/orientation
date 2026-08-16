import { defineConfig } from "vite";

export default defineConfig({
  base: "./",
  server: {
    proxy: {
      "/api": {
        target: process.env.ORIENTATION_BACKEND_URL ?? "http://127.0.0.1:8080",
      },
    },
  },
  preview: {
    proxy: {
      "/api": {
        target: process.env.ORIENTATION_BACKEND_URL ?? "http://127.0.0.1:8080",
      },
    },
  },
  build: {
    sourcemap: true,
    rollupOptions: {
      input: {
        reference: "index.html",
        embed: "embed.html",
      },
    },
  },
});
