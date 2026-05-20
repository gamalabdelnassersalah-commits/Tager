import React from 'react'
import Hero from '../components/common/Hero.jsx'
import SectionTitle from '../components/common/SectionTitle.jsx'
import CategoryCard from '../components/categories/CategoryCard.jsx'
import { categories } from '../data/categories.js'

export default function Categories({onNavigate}) {
  return (
    <>
      <Hero title="فئات المواد الغذائية على Tager" subtitle="اختر الفئة المناسبة وابدأ في تصفح منتجات الجملة وجملة الجملة من موردين معتمدين." highlight="فئات واضحة تساعدك على الوصول للمنتجات بسرعة" emoji="🧺" />
      <section className="section"><div className="container">
        <SectionTitle title="اختر الفئة التي تناسب نشاطك" subtitle="كل فئة تمثل مجموعة من المنتجات الغذائية المخصصة لتجار الجملة." />
        <div className="grid grid-3">{categories.map(c=><CategoryCard key={c.id} category={c} onNavigate={onNavigate} />)}</div>
      </div></section>
      <section className="section"><div className="container card">
        <h2>لم تجد الفئة التي تبحث عنها؟</h2>
        <p>تواصل مع فريق Tager وسنساعدك في الوصول إلى الموردين والمنتجات المناسبة لنشاطك.</p>
        <button className="btn btn-primary" onClick={()=>onNavigate('contact')}>تواصل معنا</button>
      </div></section>
    </>
  )
}
