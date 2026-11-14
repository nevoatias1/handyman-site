export class ApiError extends Error {
 status: number
 constructor(status: number, message: string) { super(message); this.status = status }
}
function fail(status: number, text: string): never {
 let message = 'אירעה שגיאה בבקשה'
 try { message = JSON.parse(text).message ?? message } catch { /* Server may return an empty/non-JSON error. */ }
 if (status === 401 && location.pathname.startsWith('/admin') && location.pathname !== '/admin/login') location.assign('/admin/login')
 throw new ApiError(status, message)
}
export async function api<T = void>(path: string, options: RequestInit & { onUploadProgress?: (percent: number) => void } = {}): Promise<T> {
 const { onUploadProgress, ...request } = options
 const headers = new Headers(request.headers)
 headers.set('Accept', 'application/json')
 const multipart = request.body instanceof FormData
 if (request.body && !multipart) headers.set('Content-Type', 'application/json')
 if (multipart) headers.delete('Content-Type')
 if (!['GET', 'HEAD', 'OPTIONS'].includes((request.method ?? 'GET').toUpperCase())) {
  const csrf = await fetch('/api/auth/csrf', { credentials: 'include', cache: 'no-store' })
  if (!csrf.ok) fail(csrf.status, await csrf.text())
  const { token } = await csrf.json() as { token: string }
  headers.set('X-XSRF-TOKEN', token)
 }
 if (multipart && onUploadProgress) {
  return new Promise<T>((resolve, reject) => {
   const xhr = new XMLHttpRequest()
   xhr.open(request.method ?? 'POST', path)
   xhr.withCredentials = true
   headers.forEach((value, key) => xhr.setRequestHeader(key, value))
   xhr.upload.onprogress = e => { if (e.lengthComputable) onUploadProgress(Math.round(e.loaded / e.total * 100)) }
   xhr.onerror = () => reject(new Error('שגיאת רשת בהעלאה'))
   xhr.ontimeout = () => reject(new Error('ההעלאה ארכה זמן רב מדי'))
   xhr.timeout = 180000
   xhr.onload = () => {
    try {
     if (xhr.status < 200 || xhr.status >= 300) fail(xhr.status, xhr.responseText)
     resolve(xhr.responseText ? JSON.parse(xhr.responseText) as T : undefined as T)
    } catch (error) { reject(error) }
   }
   xhr.send(request.body as FormData)
  })
 }
 const response = await fetch(path, { ...request, headers, credentials: 'include', cache: 'no-store' })
 const text = await response.text()
 if (!response.ok) fail(response.status, text)
 return text ? JSON.parse(text) as T : undefined as T
}
