import React from 'react'

export default function ProductFilters() {
  return (
    <div className="card">
      <h3>فلتر المنتجات حسب احتياجك</h3>
      <div className="filters">
        <select className="input"><option>التصنيف</option></select>
        <select className="input"><option>المحافظة</option></select>
        <select className="input"><option>المورد</option></select>
        <input className="input" placeholder="السعر من" />
        <input className="input" placeholder="الحد الأدنى للطلب" />
        <select className="input"><option>متاح للتوصيل</option></select>
      </div>
    </div>
  )
}
