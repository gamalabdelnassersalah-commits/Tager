import React from 'react'
import Hero from '../components/common/Hero.jsx'
import SectionTitle from '../components/common/SectionTitle.jsx'
import InfoCard from '../components/common/InfoCard.jsx'
import CustomerForm from '../components/forms/CustomerForm.jsx'

export default function CustomerJoin({onNavigate}) {
  return (
    <>
      <Hero title="اشترِ مواد غذائية بالجملة من موردين معتمدين" subtitle="من خلال Tager يمكنك تصفح المنتجات الغذائية، مقارنة الأسعار، اختيار الكمية المناسبة، والحصول على سعر الجملة أو جملة الجملة تلقائياً." highlight="منصة واحدة تربطك بموردي الجملة في مصر" primary="سجل كمشتري الآن" secondary="تصفح المنتجات" onSecondary={()=>onNavigate('products')} emoji="🛒" />
      <section className="section"><div className="container">
        <SectionTitle title="لماذا تستخدم Tager كمشتري؟" />
        <div className="grid grid-4">
          <InfoCard title="مقارنة الأسعار" desc="قارن أسعار الموردين في مكان واحد." />
          <InfoCard title="أسعار حسب الكمية" desc="جملة وجملة الجملة حسب كمية الطلب." />
          <InfoCard title="موردين معتمدين" desc="موردون في مختلف المحافظات." />
          <InfoCard title="متابعة الطلبات" desc="تابع حالة الطلب حتى التسليم." />
        </div>
      </div></section>
      <section className="section"><div className="container"><CustomerForm /></div></section>
    </>
  )
}
