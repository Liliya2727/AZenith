import { defineConfig } from "vite"
import mkcert from "vite-plugin-mkcert";
import tailwind from "tailwindcss";
import autoprefixer from "autoprefixer";
import { ViteMinifyPlugin } from 'vite-plugin-minify'
import obfuscator from "vite-plugin-javascript-obfuscator";
export default defineConfig({
  root: './src',
  plugins: [
    ViteMinifyPlugin({}),
    mkcert(),
    obfuscator({
      exclude: [
        'src/locales/strings/*',
        'src/scripts/languange.js',
      ],
      compact: true,
      stringArray: true,
      stringArrayThreshold: 0.75,
      controlFlowFlattening: false,
      deadCodeInjection: false,
    })
  ],
  server : {
    https: true,
  },
  css: {
    postcss: {
      plugins: [tailwind, autoprefixer],
    }
  },
  build: {
    outDir: '../dist',
  },
})
