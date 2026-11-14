import { api } from './api'
export type Category = { id: number; name: string; slug: string; sortOrder: number }
export type Photo = { id: number; url: string; thumbUrl: string; caption: string | null }
export const getPhotos = (slug: string) => api<Photo[]>('/api/photos?category=' + encodeURIComponent(slug))
export async function checkFiles(files: File[]) {
 if (!files.length || files.length > 10) throw new Error('יש לבחור בין תמונה אחת ל־10 תמונות')
 for (const file of files) {
  if (!file.size) throw new Error('קובץ ריק: ' + file.name)
  if (file.size > 10 * 1024 * 1024) throw new Error('הקובץ גדול מ־10MB: ' + file.name)
  const b = new Uint8Array(await file.slice(0, 128).arrayBuffer())
  const text = (offset: number, size: number) => new TextDecoder().decode(b.slice(offset, offset + size))
  const jpeg = b.length >= 3 && b[0] === 255 && b[1] === 216 && b[2] === 255
  const png = b.length >= 8 && [137,80,78,71,13,10,26,10].every((x,i) => b[i] === x)
  const webp = b.length >= 12 && text(0,4) === 'RIFF' && text(8,4) === 'WEBP'
  let heic = false
  if (b.length >= 16 && text(4,4) === 'ftyp') {
   const size = new DataView(b.buffer).getUint32(0)
   const brands = ['heic','heix','hevc','hevx']
   if (size >= 16 && size <= b.length) {
    heic = brands.includes(text(8,4))
    for (let i=16;i+4<=size;i+=4) heic ||= brands.includes(text(i,4))
   }
  }
  if (!jpeg && !png && !webp && !heic) throw new Error('סוג קובץ לא נתמך: ' + file.name + '. יש לבחור JPEG, PNG, WebP או HEIC')
 }
}
