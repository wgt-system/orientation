import { defineConfig } from "vite";

export default defineConfig({
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
