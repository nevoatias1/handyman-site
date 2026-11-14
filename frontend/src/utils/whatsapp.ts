import { business } from '../config/business'

export type Fields = { name: string; locality: string; service: string; description: string }

export function createWhatsAppUrl(fields: Fields) {
  const message = `שלום יוסי, פנייה מהאתר:\nשם: ${fields.name.trim()}\nיישוב: ${fields.locality.trim()}\nשירות: ${fields.service.trim()}\nתיאור: ${fields.description.trim()}`
  return `${business.whatsappBase}?text=${encodeURIComponent(message)}`
}
