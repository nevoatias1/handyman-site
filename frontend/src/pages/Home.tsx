import { business, whatsappLink } from '../config/business'
import ContactForm from '../components/ContactForm'
export default function Home() {
 return <>
  <section className="hero"><div className="container hero-inner"><div className="hero-content">
   <p className="eyebrow hero-eyebrow">אינסטלציה והנדימן באזור חוף הכרמל</p>
   <h1>{business.name}<br/><span>פתרונות אינסטלציה והנדימן לבית</span></h1>
   <p className="hero-sub">שירות ב{business.area}</p>
   <div className="actions"><a className="button button-white" href={business.phoneLink}>☎ התקשרו עכשיו</a><a className="button button-outline" href={whatsappLink} target="_blank" rel="noopener noreferrer">✆ שלחו הודעה בוואטסאפ</a></div>
  </div><div className="hero-graphic" aria-hidden="true"><div className="graphic-ring">✳</div><div className="graphic-label">אינסטלציה + הנדימן</div></div></div></section>
  <section className="highlights" aria-label="פרטי השירות"><div className="container highlights-inner">{[...business.highlights,business.area].map((item,i)=><div className="highlight" key={item}><span aria-hidden="true">{['◷','✓','⌖'][i]}</span><strong>{item}</strong></div>)}</div></section>
  <section className="section services" id="services"><div className="container"><p className="eyebrow">מה אפשר לעשות בשבילכם</p><h2>השירותים שלנו</h2><p className="section-intro">מגוון עבודות אינסטלציה והנדימן לבית.</p>
   {business.services.map(group=><div className="service-group" key={group.group}><div className="group-title"><h3>{group.group}</h3><span /></div><div className="service-grid">{group.items.map(item=><article className="service-card" key={item.name}><span className="service-icon" aria-hidden="true">{item.icon}</span><h4>{item.name}</h4><p>{item.description}</p></article>)}</div></div>)}
  </div></section>
  <section className="section about"><div className="container about-grid"><div><p className="eyebrow">נעים להכיר</p><h2>קצת עלינו</h2></div><p className="about-copy">{business.owner} הוא בעל {business.name}, ומציע שירותי אינסטלציה והנדימן ב{business.area}. עם {business.highlights[0]} ו{business.highlights[1]}, אפשר לפנות אליו לעבודות אינסטלציה, הרכבת רהיטים ותיקונים כלליים.</p></div></section>
  <section className="section area"><div className="container area-inner"><div className="area-icon" aria-hidden="true">⌖</div><div><p className="eyebrow">קרובים אליכם</p><h2>אזור השירות</h2><p>{business.area}</p></div></div></section>
  <section className="section contact"><div className="container contact-inner"><div><p className="eyebrow">מדברים ומתקדמים</p><h2>צריכים עזרה בבית?</h2><p>ספרו לנו על העבודה ונשמח לשמוע מכם.</p></div><div><div className="actions"><a className="button button-white" href={business.phoneLink}>☎ <span dir="ltr">{business.phoneDisplay}</span></a><a className="button button-outline" href={whatsappLink} target="_blank" rel="noopener noreferrer">✆ שלחו הודעה בוואטסאפ</a></div><ContactForm /></div></div></section>
 </>
}
