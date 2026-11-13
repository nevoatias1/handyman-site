import { useEffect, useState } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { api, ApiError } from '../lib/api'

export default function ProtectedRoute() {
  const [state, setState] = useState<{ username?: string; anonymous?: boolean; error?: boolean }>({})
  useEffect(() => {
    let active = true
    api<{username:string}>('/api/auth/me').then(user => { if (active) setState(user) })
      .catch(error => { if (active) setState(error instanceof ApiError && error.status === 401 ? { anonymous: true } : { error: true }) })
    return () => { active = false }
  }, [])
  if (state.anonymous) return <Navigate to="/admin/login" replace />
  if (state.error) return <p role="alert">לא ניתן לבדוק את ההתחברות. נסו לרענן.</p>
  if (!state.username) return <p role="status">בודק התחברות…</p>
  return <Outlet context={{ username: state.username }} />
}
