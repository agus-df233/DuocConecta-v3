// Configuración del servidor de desarrollo.
// El puerto 5173 está fijado a propósito: es el que quedó registrado como URI de redirección
// en Azure AD y el que el BFF acepta en su lista de orígenes CORS. Si cambia, hay que
// actualizarlo en los dos lados o el login deja de funcionar.
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * Las etiquetas Open Graph necesitan una URL absoluta (la del API Gateway en la nube). La pone
 * aws.sh en VITE_SITIO_URL al construir; en local queda la de Vite.
 */
function urlDelSitio(modo) {
  const sitio = loadEnv(modo, process.cwd(), 'VITE_').VITE_SITIO_URL || process.env.VITE_SITIO_URL || 'http://localhost:5173'
  return {
    name: 'url-del-sitio',
    transformIndexHtml: (html) => html.replaceAll('__SITIO_URL__', sitio.replace(/\/$/, '')),
  }
}

export default defineConfig(({ mode }) => ({
  plugins: [react(), urlDelSitio(mode)],
  server: { port: 5173, strictPort: true },
}))
