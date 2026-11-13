import { Link, Outlet } from 'react-router-dom'
import { business, whatsappLink } from '../config/business'
const currentYear = new Date().getFullYear()

export default function Layout() {
 return <div className="layout">
  <a className="skip-link" href="#main-content">דילוג לתוכן</a>
  <header className="site-header"><div className="container header-inner">
   <Link to="/" className="brand"><span className="brand-mark" aria-hidden="true">י.א</span><span>{business.name}<small>אינסטלציה והנדימן</small></span></Link>
   <a className="button button-header" href={business.phoneLink}>☎ התקשרו עכשיו</a>
  </div></header>
  <main id="main-content" tabIndex={-1}><Outlet /></main>
  <footer className="site-footer"><div className="container footer-inner">
   <strong>{business.name}</strong><span>{business.area}</span><a href={business.phoneLink} dir="ltr">{business.phoneDisplay}</a><span>© {currentYear} {business.name}</span>
  </div></footer>
  <a className="floating-whatsapp" href={whatsappLink} target="_blank" rel="noopener noreferrer" aria-label="שליחת הודעת WhatsApp"><span aria-hidden="true">✆</span> WhatsApp</a>
 </div>
}
