import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import { createServer, loadEnv } from 'vite'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { business } from '../src/config/business.ts'
import { resolveSiteUrl } from '../src/config/site.ts'
import { seo } from '../src/config/seo.ts'

const siteUrl = resolveSiteUrl(loadEnv('production', process.cwd(), '').VITE_SITE_URL)
const expected = seo(siteUrl)
const html = readFileSync('dist/index.html', 'utf8')
assert.match(html, /<html lang="he" dir="rtl">/)
assert.equal((html.match(/<title>/g) ?? []).length, 1)
assert.ok(html.includes(expected.title))
for (const tag of ['description', 'og:title', 'og:description', 'og:image', 'og:url', 'twitter:card', 'twitter:image']) assert.ok(html.includes('"'+tag+'"'))
assert.ok(html.includes('rel="canonical" href="'+siteUrl+'"'))
const match = html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/)
assert.ok(match, 'JSON-LD must exist in the built HTML without JS')
const data = JSON.parse(match[1])
assert.deepEqual(data, expected.structuredData)
assert.equal(data['@type'], 'Plumber')
assert.equal(data.founder.name, business.owner)
assert.equal(data.telephone, business.phoneLink.slice(4))
for (const field of ['address', 'aggregateRating', 'review']) assert.equal(data[field], undefined)
assert.equal(resolveSiteUrl('https://check.invalid'), 'https://check.invalid/')
assert.throws(() => resolveSiteUrl('javascript:alert(1)'))
for (const file of ['robots.txt', 'sitemap.xml', 'og-image.png']) assert.ok(existsSync('dist/'+file))
const robots = readFileSync('dist/robots.txt','utf8')
assert.ok(robots.includes('Disallow: /admin') && robots.includes('Disallow: /api') && robots.includes('Sitemap: '+siteUrl+'sitemap.xml'))
assert.ok(readFileSync('dist/sitemap.xml','utf8').includes('<loc>'+siteUrl+'</loc>'))
const png = readFileSync('dist/og-image.png')
assert.equal(png.subarray(1,4).toString(), 'PNG')
assert.equal(png.readUInt32BE(16), 1200)
assert.equal(png.readUInt32BE(20), 630)
const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
try {
 const { default: Home } = await server.ssrLoadModule('/src/pages/Home.tsx')
 const { default: Layout } = await server.ssrLoadModule('/src/components/Layout.tsx')
 const page = renderToStaticMarkup(createElement(MemoryRouter, null, createElement(Routes, null,
  createElement(Route, { element: createElement(Layout) }, createElement(Route, { index: true, element: createElement(Home) })))))
 const headings = [...page.matchAll(/<h([1-6])(?:\s|>)/g)].map(m => Number(m[1]))
 assert.equal(headings.filter(level => level === 1).length, 1)
 for (let i=1;i<headings.length;i++) assert.ok(headings[i] <= headings[i-1]+1, 'Heading levels must not skip')
 assert.ok(page.includes('href="'+business.phoneLink+'"'))
 const gallery = readFileSync('src/components/PublicGallery.tsx','utf8')
 for (const img of gallery.matchAll(/<img\b[^>]*>/g)) assert.match(img[0], /\balt=/)
 console.log('SEO checks passed: static metadata, parsed JSON-LD, crawler files, 1200x630 PNG, homepage semantics.')
} finally { await server.close() }
