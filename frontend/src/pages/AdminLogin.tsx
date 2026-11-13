import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../lib/api'

export default function AdminLogin() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username: username.trim(), password }) })
      setPassword(''); navigate('/admin', { replace: true })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'לא ניתן להתחבר כרגע')
    } finally { setBusy(false) }
  }
  return <section><h1>כניסת מנהל</h1><form onSubmit={submit}>
    <label htmlFor="admin-user">שם משתמש</label><input id="admin-user" autoComplete="username" required maxLength={128} value={username} onChange={e => setUsername(e.target.value)} />
    <label htmlFor="admin-password">סיסמה</label><input id="admin-password" type="password" autoComplete="current-password" required maxLength={256} value={password} onChange={e => setPassword(e.target.value)} />
    {error && <p role="alert">{error}</p>}<button disabled={busy} type="submit">{busy ? 'מתחבר…' : 'כניסה'}</button>
  </form></section>
}
