import { useState, type FormEvent } from 'react'
import { business } from '../config/business'

type Fields = { name: string; locality: string; service: string; description: string }
const empty: Fields = { name: '', locality: '', service: '', description: '' }
const labels: Record<keyof Fields, string> = { name: 'שם', locality: 'יישוב', service: 'סוג השירות', description: 'תיאור התקלה' }

export function createWhatsAppUrl(fields: Fields) {
  const message = `שלום יוסי, פנייה מהאתר:\nשם: ${fields.name.trim()}\nיישוב: ${fields.locality.trim()}\nשירות: ${fields.service.trim()}\nתיאור: ${fields.description.trim()}`
  return `${business.whatsappBase}?text=${encodeURIComponent(message)}`
}

export default function ContactForm() {
  const [fields, setFields] = useState<Fields>(empty)
  const [errors, setErrors] = useState<Partial<Record<keyof Fields, string>>>({})
  function update(key: keyof Fields, value: string) {
    setFields(previous => ({ ...previous, [key]: value }))
    setErrors(previous => ({ ...previous, [key]: undefined }))
  }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next: Partial<Record<keyof Fields, string>> = {}
    for (const key of Object.keys(labels) as (keyof Fields)[]) {
      if (!fields[key].trim()) next[key] = `יש למלא ${labels[key]}`
    }
    if (fields.description.length > 500) next.description = 'התיאור מוגבל ל־500 תווים'
    setErrors(next)
    if (Object.keys(next).length === 0) window.open(createWhatsAppUrl(fields), '_blank', 'noopener,noreferrer')
  }
  return <form className="contact-form" onSubmit={submit} noValidate>
    <label htmlFor="contact-name">שם</label><input id="contact-name" value={fields.name} onChange={e => update('name', e.target.value)} aria-required="true" aria-invalid={!!errors.name} aria-describedby={errors.name ? 'error-name' : undefined} />{errors.name && <span id="error-name" className="form-error">{errors.name}</span>}
    <label htmlFor="contact-locality">יישוב</label><input id="contact-locality" value={fields.locality} onChange={e => update('locality', e.target.value)} aria-required="true" aria-invalid={!!errors.locality} aria-describedby={errors.locality ? 'error-locality' : undefined} />{errors.locality && <span id="error-locality" className="form-error">{errors.locality}</span>}
    <label htmlFor="contact-service">סוג השירות</label><select id="contact-service" value={fields.service} onChange={e => update('service', e.target.value)} aria-required="true" aria-invalid={!!errors.service} aria-describedby={errors.service ? 'error-service' : undefined}><option value="">בחרו שירות</option>{business.services.map(group => <optgroup key={group.group} label={group.group}>{group.items.map(item => <option key={item.name} value={item.name}>{item.name}</option>)}</optgroup>)}<option value="אחר">אחר</option></select>{errors.service && <span id="error-service" className="form-error">{errors.service}</span>}
    <label htmlFor="contact-description">תיאור התקלה</label><textarea id="contact-description" maxLength={500} rows={4} value={fields.description} onChange={e => update('description', e.target.value)} aria-required="true" aria-invalid={!!errors.description} aria-describedby={errors.description ? 'description-count error-description' : 'description-count'} /><span id="description-count" className="form-counter" aria-live="polite">{fields.description.length} / 500</span>{errors.description && <span id="error-description" className="form-error">{errors.description}</span>}
    <button className="button button-white" type="submit">שליחת פנייה בוואטסאפ</button>
    <p className="form-phone">מעדיפים לדבר? התקשרו: <a href={business.phoneLink} dir="ltr">{business.phoneDisplay}</a></p>
  </form>
}
