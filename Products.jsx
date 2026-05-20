import React from 'react'
import Hero from '../components/common/Hero.jsx'
import SectionTitle from '../components/common/SectionTitle.jsx'
import ProductCard from '../components/products/ProductCard.jsx'
import ProductFilters from '../components/products/ProductFilters.jsx'
import PriceCalculator from '../components/products/PriceCalculator.jsx'
import { products } from '../data/products.js'

export default function Products({onNavigate}) {
  return (
    <>
      <Hero title="تصفح منتجات الجملة على Tager" subtitle="اكتشف مجموعة متنوعة من المواد الغذائية بأسعار جملة وجملة الجملة من موردين معتمدين في مصر." highlight="اختر المنتج، حدد الكمية، وشاهد السعر المناسب تلقائياً" emoji="🛍️" />
      <section className="section"><div className="container"><ProductFilters /></div></section>
      <section className="section"><div className="container">
        <SectionTitle title="نتائج المنتجات" subtitle="اطلع على تفاصيل كل منتج ومعرفة أسعار الجملة وجملة الجملة." />
        <div className="grid grid-3">{products.map(p=><ProductCard key={p.id} product={p} />)}</div>
      </div></section>
      <section className="section"><div className="container grid grid-2">
        <PriceCalculator product={products[0]} />
        <div className="card">
          <h2>هل أنت مورد وتريد إضافة منتجاتك؟</h2>
          <p>انضم إلى Tager واعرض منتجاتك الغذائية أمام تجار الجملة والمطاعم والسوبرماركت.</p>
          <button className="btn btn-primary" onClick={()=>onNavigate('supplier')}>انضم كمورد</button>
        </div>
      </div></section>
    </>
  )
}
