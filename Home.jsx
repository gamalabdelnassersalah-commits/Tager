import React from 'react'
import Hero from '../components/common/Hero.jsx'
import SectionTitle from '../components/common/SectionTitle.jsx'
import InfoCard from '../components/common/InfoCard.jsx'
import CategoryCard from '../components/categories/CategoryCard.jsx'
import { categories } from '../data/categories.js'

export default function Home({onNavigate}) {
  return (
    <>
      <Hero
        title="منصة Tager لتجارة الجملة للمواد الغذائية في مصر"
        subtitle="Tager يربط بين موردي المواد الغذائية المعتمدين وبين التجار، السوبرماركت، المطاعم، الكافيهات، الفنادق والشركات في جميع محافظات مصر."
        highlight="أسعار جملة وجملة الجملة – كميات كبيرة – توصيل للمحافظات"
        primary="تسجيل كمورد"
        secondary="تسجيل مشتري"
        onPrimary={()=>onNavigate('supplier')}
        onSecondary={()=>onNavigate('customer')}
        emoji="🥫"
      />
      <section className="section"><div className="container">
        <SectionTitle title="ما هي منصة Tager؟" subtitle="منصة إلكترونية متخصصة في تجارة الجملة وجملة الجملة للمواد الغذائية في مصر." />
        <p>نساعد الموردين على عرض منتجاتهم بكميات وأسعار جملة، ونوفر للمشترين طريقة سهلة للمقارنة، الطلب، وتتبع الطلبات من مكان واحد.</p>
      </div></section>
      <section className="section"><div className="container">
        <SectionTitle title="الفئات الرئيسية للمنتجات الغذائية" subtitle="استكشف أهم فئات المواد الغذائية المتاحة على Tager." />
        <div className="grid grid-3">{categories.slice(0,6).map(c=><CategoryCard key={c.id} category={c} onNavigate={onNavigate} />)}</div>
      </div></section>
      <section className="section"><div className="container">
        <SectionTitle title="كيف تعمل Tager؟" />
        <div className="grid grid-4">
          <InfoCard title="تسجيل سريع" desc="يسجل الموردون والمشترون حساباتهم على المنصة في دقائق." icon="1️⃣" />
          <InfoCard title="تأكيد الموردين" desc="يقوم فريق Tager بمراجعة بيانات الموردين واعتمادهم." icon="2️⃣" />
          <InfoCard title="تصفح المنتجات والطلب" desc="يختار المشتري المنتجات والكمية ويظهر السعر تلقائياً." icon="3️⃣" />
          <InfoCard title="إدارة الطلب والتسليم" desc="متابعة حالة الطلب مع خيارات دفع مرنة وتوصيل للمحافظات." icon="4️⃣" />
        </div>
      </div></section>
      <section className="section"><div className="container grid grid-2">
        <InfoCard title="مميزات الموردين" desc="عرض منتجاتك، تحديد أسعار الجملة، استقبال الطلبات، وإدارة المخزون بسهولة." button="انضم كمورد" onClick={()=>onNavigate('supplier')} icon="🏭" />
        <InfoCard title="مميزات المشترين" desc="قارن الأسعار، اختر الكمية، واحصل على سعر جملة أو جملة الجملة من موردين معتمدين." button="سجل كمشتري" onClick={()=>onNavigate('customer')} icon="🛒" />
      </div></section>
    </>
  )
}
