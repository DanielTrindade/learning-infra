import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // strictPort faz o Vite falhar se a porta estiver ocupada, em vez de deslizar
    // silenciosamente para outra — o que faria você abrir o app errado.
    port: 5180,
    strictPort: true,
    proxy: {
      '/api': 'http://127.0.0.1:8099',
    },
  },
})
