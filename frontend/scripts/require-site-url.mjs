import { resolveSiteUrl } from '../src/config/site.ts'
const value = process.env.VITE_SITE_URL
if (!value?.trim()) throw new Error('VITE_SITE_URL build argument is required')
const url = new URL(resolveSiteUrl(value))
if (url.protocol !== 'https:' || url.hostname.endsWith('.invalid') || url.hostname.endsWith('.example') || ['example.com','example.org','example.net','localhost','127.0.0.1','[::1]'].includes(url.hostname)) {
 throw new Error('VITE_SITE_URL must be the actual public HTTPS origin, not a placeholder')
}
