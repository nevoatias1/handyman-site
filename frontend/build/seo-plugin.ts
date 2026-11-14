import { mkdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { Resvg } from '@resvg/resvg-js'
import type { Plugin } from 'vite'
import { business } from '../src/config/business.ts'
import { seo } from '../src/config/seo.ts'
import { resolveSiteUrl } from '../src/config/site.ts'

function escape(value: string) {
 return value.replace(/[&<>"']/g, character => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&apos;' })[character]!)
}
export function seoPlugin(value?: string): Plugin {
 const siteUrl = resolveSiteUrl(value)
 const metadata = seo(siteUrl)
 let publicDir = ''
 return {
  name: 'local-business-seo',
  configResolved(config) { publicDir = config.publicDir || resolve(config.root, 'public') },
  buildStart() {
   mkdirSync(publicDir, { recursive: true })
   writeFileSync(resolve(publicDir, 'robots.txt'), `User-agent: *\nDisallow: /admin\nDisallow: /api\n\nSitemap: ${siteUrl}sitemap.xml\n`)
   writeFileSync(resolve(publicDir, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>${escape(siteUrl)}</loc></url></urlset>\n`)
   const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630">
    <rect width="1200" height="630" fill="#064b7a"/>
    <rect x="50" y="50" width="1100" height="530" rx="28" fill="#fff"/>
    <rect x="1030" y="120" width="12" height="390" rx="6" fill="#0b8fbc"/>
    <text x="600" y="300" text-anchor="middle" direction="rtl" font-family="Arial,DejaVu Sans,sans-serif" font-weight="700" font-size="85" fill="#064b7a">${escape(business.name)}</text>
    <text x="600" y="430" text-anchor="middle" font-family="Arial,DejaVu Sans,sans-serif" font-size="68" fill="#12334d">${escape(business.phoneDisplay)}</text>
   </svg>`
   writeFileSync(resolve(publicDir, 'og-image.png'), new Resvg(svg, { font: { loadSystemFonts: true } }).render().asPng())
  },
  transformIndexHtml() {
   const meta = (name: string, content: string, property = false) => ({ tag: 'meta', attrs: { [property ? 'property' : 'name']: name, content } })
   return [
    { tag: 'title', children: escape(metadata.title) },
    meta('description', metadata.description),
    { tag: 'link', attrs: { rel: 'canonical', href: siteUrl } },
    meta('og:type', 'website', true), meta('og:locale', 'he_IL', true),
    meta('og:site_name', business.name, true), meta('og:title', metadata.title, true),
    meta('og:description', metadata.description, true), meta('og:url', siteUrl, true),
    meta('og:image', metadata.image, true), meta('og:image:type','image/png',true),
    meta('og:image:width','1200',true), meta('og:image:height','630',true),
    meta('og:image:alt', business.name + ' — ' + business.phoneDisplay, true),
    meta('twitter:card','summary_large_image'), meta('twitter:title',metadata.title),
    meta('twitter:description',metadata.description), meta('twitter:image',metadata.image),
    meta('twitter:image:alt',business.name + ' — ' + business.phoneDisplay),
    { tag: 'script', attrs: { type: 'application/ld+json' }, children: JSON.stringify(metadata.structuredData).replace(/</g, '\\u003c').replace(/>/g, '\\u003e').replace(/&/g, '\\u0026') },
   ].map(tag => ({ ...tag, injectTo: 'head' as const }))
  },
 }
}
