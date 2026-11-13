import { useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import './admin.css'

export default function AdminShell() {
  useEffect(() => {
    const existing = document.querySelector<HTMLMetaElement>('meta[name="robots"]')
    const previous = existing?.content
    const meta = existing ?? document.createElement('meta')
    meta.name = 'robots'; meta.content = 'noindex'
    if (!existing) document.head.append(meta)
    return () => { if (existing) existing.content = previous ?? ''; else meta.remove() }
  }, [])
  return <main className="admin-shell" dir="rtl"><Outlet /></main>
}
