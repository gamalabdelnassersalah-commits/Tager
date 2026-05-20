import React from 'react'
import Hero from '../components/common/Hero.jsx'
import InfoCard from '../components/common/InfoCard.jsx'
import ContactForm from '../components/forms/ContactForm.jsx'

export default function Contact({onNavigate}) {
  return (
    <>
      <Hero title="تواصل مع فريق Tager" subtitle="نحن هنا لمساعدة الموردين والمشترين في التسجيل، إضافة المنتجات، متابعة الطلبات، أو أي استفسار." highlight="اختر طريقة التواصل المناسبة وسيرد عليك فريقنا في أقرب وقت" emoji="☎️" />
      <section className="section"><div className="container grid grid-4">
        <InfoCard title="واتساب" desc="للاستفسارات السريعة والدعم الفوري." button="تواصل عبر واتساب" />
        <InfoCard title="الهاتف" desc="اتصل بفريق Tager خلال أوقات العمل." button="اتصل بنا" />
        <InfoCard title="البريد الإلكتروني" desc="للاستفسارات التفصيلية والمراسلات الرسمية." button="إرسال بريد" />
        <InfoCard title="نموذج التواصل" desc="املأ النموذج وسنتواصل معك." button="افتح النموذج" />
      </div></section>
      <section className="section"><div className="container"><ContactForm /></div></section>
      <section className="section"><div className="container card">
        <h2>ابدأ رحلتك مع Tager اليوم</h2>
        <p>سواء كنت موردًا أو مشتريًا، فريق Tager جاهز لمساعدتك.</p>
        <button className="btn btn-primary" onClick={()=>onNavigate('supplier')}>انضم كمورد</button>{' '}
        <button className="btn btn-secondary" onClick={()=>onNavigate('customer')}>سجل كمشتري</button>
      </div></section>
    </>
  )
}
