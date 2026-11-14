import { business } from './business.ts'
export function seo(siteUrl: string) {
 const specialty = business.services.map(group => group.group).join(' ו')
 const title = `${business.name} | ${specialty} | ${business.area} | ${business.highlights[0]}`
 const description = `${business.name} של ${business.owner}: שירותי ${specialty} ב${business.area}. ${business.highlights.join(', ')}. לפרטים: ${business.phoneDisplay}.`
 const image = new URL('og-image.png', siteUrl).href
 return {
  title, description, image,
  structuredData: {
   '@context': 'https://schema.org',
   '@type': 'Plumber',
   name: business.name,
   founder: { '@type': 'Person', name: business.owner },
   telephone: business.phoneLink.slice(4),
   url: siteUrl,
   areaServed: business.areaServed.map(name => ({ '@type': 'Place', name })),
   knowsAbout: business.services.flatMap(group => group.items.map(item => item.name)),
   image,
  },
 }
}
