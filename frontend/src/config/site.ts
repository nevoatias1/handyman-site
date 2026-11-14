/// <reference types="vite/client" />
export const PLACEHOLDER_SITE_URL = 'https://example.invalid'
export function resolveSiteUrl(value?: string) {
 const url = new URL(value?.trim() || PLACEHOLDER_SITE_URL)
 if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash || url.pathname !== '/') {
  throw new Error('VITE_SITE_URL must be an HTTP(S) site origin without credentials, path, query or fragment')
 }
 return url.origin + '/'
}
export const SITE_URL = resolveSiteUrl(import.meta.env?.VITE_SITE_URL)
