import { useEffect, useRef, useState } from 'react'
import { api } from '../lib/api'
import { getPhotos, type Category, type Photo } from '../lib/gallery'
import './gallery.css'

function Lightbox({ photos, index, name, close }: { photos: Photo[]; index: number; name: string; close: () => void }) {
 const [current, setCurrent] = useState(index)
 const dialog = useRef<HTMLDialogElement>(null)
 const start = useRef<number | null>(null)
 const move = (delta: number) => setCurrent(i => (i + delta + photos.length) % photos.length)
 useEffect(() => {
  const previous = document.activeElement as HTMLElement | null
  const modal = dialog.current!
  const overflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  modal.showModal()
  return () => { modal.close(); document.body.style.overflow = overflow; previous?.focus() }
 }, [])
 const photo = photos[current]
 return <dialog className="lightbox" ref={dialog} onCancel={e => { e.preventDefault(); close() }} aria-label="תצוגת תמונה"
  onKeyDown={e => {
   if (e.key === 'ArrowLeft') { e.preventDefault(); move(1) }
   if (e.key === 'ArrowRight') { e.preventDefault(); move(-1) }
   if (e.key === 'Tab') {
    const buttons = Array.from(dialog.current!.querySelectorAll<HTMLButtonElement>('button:not(:disabled)'))
    const first = buttons[0], last = buttons[buttons.length - 1]
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus() }
    if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus() }
   }
  }}>
  <button autoFocus onClick={close} aria-label="סגירת התמונה">סגירה ✕</button>
  <figure onTouchStart={e => { start.current = e.touches[0].clientX }} onTouchEnd={e => {
   if (start.current !== null) { const delta = e.changedTouches[0].clientX - start.current; if (Math.abs(delta) > 50) move(delta > 0 ? 1 : -1) }
   start.current = null
  }}>
   <img src={photo.url} alt={photo.caption || name} />
   <figcaption aria-live="polite">{photo.caption || name} · {current + 1} / {photos.length}</figcaption>
  </figure>
  <div className="gallery-actions"><button onClick={() => move(-1)} disabled={photos.length < 2}>→ הקודמת</button><button onClick={() => move(1)} disabled={photos.length < 2}>הבאה ←</button></div>
 </dialog>
}
export default function PublicGallery() {
 const [groups, setGroups] = useState<{ category: Category; photos: Photo[] }[]>([])
 const [selected, setSelected] = useState(0)
 const [index, setIndex] = useState<number | null>(null)
 useEffect(() => {
  let active = true
  api<Category[]>('/api/categories').then(categories => Promise.all(categories.map(async category => ({ category, photos: await getPhotos(category.slug) }))))
   .then(data => { if (active) setGroups(data.filter(group => group.photos.length)) }).catch(() => { /* Keep unavailable gallery out of the public page. */ })
  return () => { active = false }
 }, [])
 if (!groups.length) return null
 const group = groups[selected]
 return <section className="section" aria-labelledby="gallery-heading"><div className="container">
  <h2 id="gallery-heading">עבודות שביצענו</h2>
  <div className="gallery-tabs" aria-label="קטגוריות עבודות">{groups.map((g,i) => <button key={g.category.id} aria-pressed={selected === i} onClick={() => { setSelected(i); setIndex(null) }}>{g.category.name}</button>)}</div>
  <div className="gallery-grid">{group.photos.map((photo,i) => <button className="gallery-thumb" key={photo.id} onClick={() => setIndex(i)} aria-label={'פתיחת תמונה: ' + (photo.caption || group.category.name)}><img loading="lazy" src={photo.thumbUrl} alt={photo.caption || group.category.name}/></button>)}</div>
  {index !== null && <Lightbox photos={group.photos} index={index} name={group.category.name} close={() => setIndex(null)}/>}
 </div></section>
}
