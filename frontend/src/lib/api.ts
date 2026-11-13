export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) { super(message); this.status = status }
}
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body) headers.set('Content-Type', 'application/json')
  if (!['GET', 'HEAD', 'OPTIONS'].includes((options.method ?? 'GET').toUpperCase())) {
    const csrf = await fetch('/api/auth/csrf', { credentials: 'include', cache: 'no-store' })
    if (!csrf.ok) throw new ApiError(csrf.status, 'לא ניתן לאמת את הבקשה')
    const { token } = await csrf.json() as { token: string }
    headers.set('X-XSRF-TOKEN', token)
  }
  const response = await fetch(path, { ...options, headers, credentials: 'include', cache: 'no-store' })
  const data = await response.json()
  if (!response.ok) throw new ApiError(response.status, data.message ?? 'אירעה שגיאה')
  return data as T
}
