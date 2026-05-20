import React from 'react'
import Hero from '../components/common/Hero.jsx'
import SectionTitle from '../components/common/SectionTitle.jsx'
import InfoCard from '../components/common/InfoCard.jsx'
import SupplierForm from '../components/forms/SupplierForm.jsx'

export default function SupplierJoin() {
  return (
    <>
      <Hero title="انضم إلى Tager كمورد مواد غذائية" subtitle="اعرض منتجاتك الغذائية بالجملة وجملة الجملة، ووصل إلى التجار والسوبرماركت والمطاعم في مصر." highlight="وصول لعملاء جملة حقيقيين – أسعار واضحة – حد أدنى للطلب" primary="تسجيل كمورد الآن" secondary="تواصل واتساب" emoji="🏭" />
      <section className="section"><div className="container">
        <SectionTitle title="لماذا تنضم إلى Tager كمورد؟" />
        <div className="grid grid-3">
          <InfoCard title="عملاء جدد" desc="الوصول إلى تجار ومطاعم وسوبرماركت في محافظات مصر." />
          <InfoCard title="أسعار واضحة" desc="تحديد سعر الجملة وجملة الجملة والحد الأدنى لكل منتج." />
          <InfoCard title="إدارة سهلة" desc="متابعة المنتجات والطلبات والمخزون من لوحة المورد." />
        </div>
      </div></section>
      <section className="section"><div className="container"><SupplierForm /></div></section>
    </>
  )
}
