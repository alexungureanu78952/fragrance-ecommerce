import react from '@vitejs/plugin-react'
import type { Plugin } from 'vite'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

// Public origin used for canonical/Open Graph URLs, robots.txt, and sitemap.xml.
function siteFiles(siteUrl: string): Plugin {
  const files: Record<string, string> = {
    'robots.txt': `User-agent: *\nAllow: /\nSitemap: ${siteUrl}/sitemap.xml\n`,
    'sitemap.xml': `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n  <url><loc>${siteUrl}/</loc><changefreq>weekly</changefreq></url>\n</urlset>\n`,
  }
  return {
    name: 'site-files',
    transformIndexHtml: (html) => html.replaceAll('%SITE_URL%', siteUrl),
    configureServer(server) {
      server.middlewares.use((request, response, next) => {
        const file = files[(request.url ?? '').replace(/^\//, '')]
        if (!file) return next()
        response.setHeader('Content-Type', request.url?.endsWith('.xml') ? 'application/xml' : 'text/plain')
        response.end(file)
      })
    },
    generateBundle() {
      for (const [fileName, source] of Object.entries(files)) this.emitFile({ type: 'asset', fileName, source })
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '..', 'VITE_')
  const siteUrl = (env.VITE_SITE_URL || 'http://localhost:5173').replace(/\/+$/, '')
  return {
    // GitHub Pages serves the project from a subpath such as /fragrance-ecommerce/.
    base: env.VITE_BASE_PATH || '/',
    plugins: [react(), siteFiles(siteUrl)],
    envDir: '..',
    server: {
      headers: {
        'Content-Security-Policy': "default-src 'self'; script-src 'self' https://hcaptcha.com https://*.hcaptcha.com; frame-src https://hcaptcha.com https://*.hcaptcha.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://hcaptcha.com https://*.hcaptcha.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self' http://localhost:8080 https://hcaptcha.com https://*.hcaptcha.com; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'",
        'X-Frame-Options': 'DENY',
        'X-Content-Type-Options': 'nosniff',
        'Referrer-Policy': 'strict-origin-when-cross-origin',
        'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()',
        'Cross-Origin-Opener-Policy': 'same-origin',
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: './src/test-setup.ts',
    },
  }
})
