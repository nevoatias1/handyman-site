export const business = {
 name:'י.א אינסטלציה', owner:'יוסי אטיאס', phoneDisplay:'054-873-4772', phoneLink:'tel:+972548734772',
 whatsappBase:'https://wa.me/972548734772', whatsappMessage:'שלום, הגעתי דרך האתר ואשמח לקבל הצעת מחיר',
 area:'עתלית ויישובי חוף הכרמל', highlights:['20 שנות ניסיון','אחריות על העבודה'],
 areaServed:['עתלית','יישובי חוף הכרמל'],
 services:[
  {group:'אינסטלציה',items:[
   {icon:'◉',name:'טיפול בניאגרות וניאגרות סמויות',description:'טיפול בניאגרות גלויות וסמויות.'},
   {icon:'◈',name:'איתור ותיקון נזילות',description:'איתור נזילות ותיקון מקור הנזילה.'},
   {icon:'≋',name:'פתיחת סתימות',description:'פתיחת סתימות במערכת האינסטלציה.'},
   {icon:'⌁',name:'החלפת ברזים',description:'החלפת ברזים בבית.'},
   {icon:'☼',name:'דודי שמש וחשמל והחלפת גופי חימום',description:'טיפול בדודי שמש וחשמל והחלפת גופי חימום.'},
   {icon:'◇',name:'התקנת כלים סניטריים',description:'התקנת כלים סניטריים בחדרי הרחצה.'},
  ]},
  {group:'הנדימן',items:[
   {icon:'▦',name:'הרכבת רהיטים',description:'הרכבת רהיטי איקאה ורהיטים דומים.'},
   {icon:'✦',name:'תיקונים כלליים',description:'תיקונים כלליים ברחבי הבית.'},
  ]},
 ],
} as const
export const whatsappLink = `${business.whatsappBase}?text=${encodeURIComponent(business.whatsappMessage)}`
