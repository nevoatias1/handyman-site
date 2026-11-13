import { useState } from 'react'
import { useNavigate, useOutletContext } from 'react-router-dom'
import { api } from '../lib/api'

export default function AdminDashboard() {
  const { username } = useOutletContext<{username:string}>()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()
  async function logout() {
    setBusy(true); setError('')
    try { await api('/api/auth/logout', { method: 'POST' }); navigate('/admin/login', { replace: true }) }
    catch { setError('לא ניתן להתנתק כרגע'); setBusy(false) }
  }
  return <section><h1>שלום, {username}</h1><p>אזור הניהול</p>{error && <p role="alert">{error}</p>}<button onClick={logout} disabled={busy}>התנתקות</button></section>
}
