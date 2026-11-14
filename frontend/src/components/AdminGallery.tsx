import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../lib/api'
import { checkFiles, getPhotos, type Category, type Photo } from '../lib/gallery'
import './gallery.css'

function Preview({ file }: { file: File }) {
 const image = useRef<HTMLImageElement>(null)
 const [failed, setFailed] = useState(false)
 useEffect(() => {
  const url = URL.createObjectURL(file)
  if (image.current) image.current.src = url
  return () => URL.revokeObjectURL(url)
 }, [file])
 return failed ? <span className="preview-placeholder">תצוגה מקדימה אינה זמינה (למשל HEIC). ניתן להעלות את הקובץ.</span> : <img ref={image} alt="תצוגה מקדימה" onError={() => setFailed(true)}/>
}
function CaptionEditor({ photo, busy, save }: { photo: Photo; busy: boolean; save: (caption: string) => void }) {
 const [value, setValue] = useState(photo.caption ?? '')
 return <form onSubmit={e => { e.preventDefault(); save(value) }}><label htmlFor={'caption-'+photo.id}>תיאור התמונה</label><textarea id={'caption-'+photo.id} maxLength={200} value={value} onChange={e => setValue(e.target.value)} disabled={busy}/><small>{value.length}/200</small><button disabled={busy} type="submit">שמירת תיאור</button></form>
}
type Upload = { file: File; percent: number; status: string }
export default function AdminGallery() {
 const [categories, setCategories] = useState<Category[]>([])
 const [selected, setSelected] = useState<number | null>(null)
 const [photos, setPhotos] = useState<Photo[]>([])
 const [busy, setBusy] = useState(false)
 const [loading, setLoading] = useState(true)
 const [error, setError] = useState('')
 const [uploads, setUploads] = useState<Upload[]>([])
 const [name, setName] = useState('')
 const [slug, setSlug] = useState('')
 const [renames, setRenames] = useState<Record<number,string>>({})
 const input = useRef<HTMLInputElement>(null)
 const category = categories.find(c => c.id === selected)
 const message = (e: unknown) => e instanceof Error ? e.message : 'אירעה שגיאה. נסו שוב'
 useEffect(() => {
  let active = true
  api<Category[]>('/api/categories').then(data => { if (active) { setCategories(data); setSelected(data[0]?.id ?? null) } })
   .catch(e => { if (active) setError(message(e)) }).finally(() => { if (active) setLoading(false) })
  return () => { active = false }
 }, [])
 useEffect(() => {
  let active = true
  if (!category) return
  getPhotos(category.slug).then(data => { if (active) setPhotos(data) }).catch(e => { if (active) setError(message(e)) })
  return () => { active = false }
 }, [category])
 async function refresh() {
  const data = await api<Category[]>('/api/categories')
  setCategories(data)
  const current = data.find(c => c.id === selected) ?? data[0]
  setSelected(current?.id ?? null)
  setPhotos(current ? await getPhotos(current.slug) : [])
 }
 async function action(work: () => Promise<unknown>) {
  setBusy(true); setError('')
  try { await work(); await refresh() } catch(e) { setError(message(e)) } finally { setBusy(false) }
 }
 function reorder<T extends {id:number}>(items: T[], index: number, delta: number) {
  const next = [...items]; [next[index], next[index+delta]] = [next[index+delta], next[index]]
  return JSON.stringify({ ids: next.map(item => item.id) })
 }
 async function choose(files: File[]) {
  setBusy(true); setError(''); setUploads([])
  try { await checkFiles(files); setUploads(files.map(file => ({file,percent:0,status:'מוכן להעלאה'}))) }
  catch(e) { setError(message(e)); if (input.current) input.current.value = '' }
  finally { setBusy(false) }
 }
 async function upload(event: FormEvent) {
  event.preventDefault()
  if (!category || !uploads.length) return
  setBusy(true); setError('')
  const update = (i: number, patch: Partial<Upload>) => setUploads(items => items.map((item,j) => i === j ? {...item,...patch} : item))
  try {
   for (let i=0;i<uploads.length;i++) {
    if (uploads[i].status === 'הועלה בהצלחה') continue
    const body = new FormData(); body.append('categoryId', String(category.id)); body.append('files', uploads[i].file)
    update(i,{status:'מעלה…',percent:0})
    try {
     await api('/api/admin/photos',{method:'POST',body,onUploadProgress:percent => update(i,{percent,status:percent === 100 ? 'מעבד בשרת…' : 'מעלה…'})})
     update(i,{status:'הועלה בהצלחה',percent:100})
    } catch(e) { update(i,{status:message(e)}); setError('חלק מהתמונות לא הועלו. ניתן לנסות שוב.'); }
   }
   await refresh()
  } catch(e) { setError(message(e)) } finally { setBusy(false) }
 }
 return <section className="admin-gallery"><h2>ניהול הגלריה</h2>
  {error && <p role="alert">{error}</p>}{loading && <p role="status">טוען קטגוריות…</p>}
  <form onSubmit={e => { e.preventDefault(); void action(async () => { await api('/api/admin/categories',{method:'POST',body:JSON.stringify({name:name.trim(),slug})}); setName('');setSlug('') }) }}>
   <h3>קטגוריה חדשה</h3><label htmlFor="category-name">שם</label><input id="category-name" required maxLength={120} value={name} onChange={e => setName(e.target.value)} disabled={busy}/>
   <label htmlFor="category-slug">מזהה באנגלית (מילים עם מקף)</label><input id="category-slug" required pattern="[a-z0-9]+(-[a-z0-9]+)*" maxLength={100} dir="ltr" value={slug} onChange={e => setSlug(e.target.value)} disabled={busy}/><button disabled={busy}>יצירת קטגוריה</button>
  </form>
  <ul className="admin-category-list">{categories.map((c,i) => <li key={c.id}>
   <label htmlFor={'rename-'+c.id}>שם קטגוריה</label><input id={'rename-'+c.id} maxLength={120} value={renames[c.id] ?? c.name} onChange={e => setRenames({...renames,[c.id]:e.target.value})} disabled={busy}/>
   <div className="gallery-actions"><button disabled={busy || !(renames[c.id] ?? c.name).trim()} onClick={() => void action(() => api('/api/admin/categories/'+c.id,{method:'PATCH',body:JSON.stringify({name:(renames[c.id] ?? c.name).trim()})}))}>שמירת שם</button>
   <button disabled={busy} onClick={() => { if (confirm('למחוק את הקטגוריה '+c.name+'?')) void action(() => api('/api/admin/categories/'+c.id,{method:'DELETE'})) }}>מחיקה</button>
   <button aria-label={'הזזת '+c.name+' למעלה'} disabled={busy || i===0} onClick={() => void action(() => api('/api/admin/categories/reorder',{method:'PUT',body:reorder(categories,i,-1)}))}>↑ למעלה</button>
   <button aria-label={'הזזת '+c.name+' למטה'} disabled={busy || i===categories.length-1} onClick={() => void action(() => api('/api/admin/categories/reorder',{method:'PUT',body:reorder(categories,i,1)}))}>↓ למטה</button></div>
  </li>)}</ul>
  <label htmlFor="photo-category">תמונות בקטגוריה</label><select id="photo-category" disabled={busy || !categories.length} value={selected ?? ''} onChange={e => { setSelected(Number(e.target.value));setPhotos([]);setUploads([]);if(input.current) input.current.value='' }}>{!categories.length && <option value="">צרו קטגוריה תחילה</option>}{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
  <form onSubmit={upload}><label htmlFor="photo-files">בחירת תמונות מהטלפון</label><input ref={input} id="photo-files" type="file" accept="image/*" multiple disabled={busy || !category} onChange={e => void choose(Array.from(e.target.files ?? []))}/><small>עד 10 תמונות, 10MB לתמונה. JPEG, PNG, WebP או HEIC.</small>
   {uploads.map((item,i) => <div className="upload-preview" key={i}><Preview file={item.file}/><span>{item.file.name}</span><progress max={100} value={item.percent} aria-label={'התקדמות העלאת '+item.file.name}/><span role="status">{item.status}</span></div>)}
   <button disabled={busy || !uploads.length || !category || uploads.every(u=>u.status==='הועלה בהצלחה')}>העלאת התמונות</button>
  </form>
  {!photos.length && <p>אין תמונות להצגה בקטגוריה זו.</p>}
  <div className="admin-photo-grid">{photos.map((p,i) => <article key={p.id}><img loading="lazy" src={p.thumbUrl} alt={p.caption || category?.name || 'תמונה'}/>
   <CaptionEditor photo={p} busy={busy} save={caption => void action(() => api('/api/admin/photos/'+p.id,{method:'PATCH',body:JSON.stringify({caption})}))}/>
   <div className="gallery-actions"><button disabled={busy} onClick={() => { if(confirm('למחוק את התמונה לצמיתות?')) void action(() => api('/api/admin/photos/'+p.id,{method:'DELETE'})) }}>מחיקת תמונה</button>
   <button disabled={busy || i===0} aria-label="הזזת תמונה למעלה" onClick={() => void action(() => api('/api/admin/categories/'+selected+'/photos/reorder',{method:'PUT',body:reorder(photos,i,-1)}))}>↑ למעלה</button>
   <button disabled={busy || i===photos.length-1} aria-label="הזזת תמונה למטה" onClick={() => void action(() => api('/api/admin/categories/'+selected+'/photos/reorder',{method:'PUT',body:reorder(photos,i,1)}))}>↓ למטה</button></div>
  </article>)}</div>
 </section>
}
