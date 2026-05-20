import React from 'react'

export default function CustomerForm() {
  const fields = ['اسم النشاط التجاري','اسم المسؤول','رقم الهاتف','رقم واتساب','البريد الإلكتروني','المحافظة','العنوان','المنتجات التي تهتم بها','متوسط حجم الطلب الشهري']
  return (
    <form className="card form" onSubmit={e=>{e.preventDefault(); alert('تم إرسال طلب التسجيل كمشتري')}}>
      <h2>نموذج تسجيل المشتري</h2>
      <select className="input"><option>نوع النشاط</option><option>بقالة</option><option>سوبر ماركت</option><option>مطعم</option><option>موزع</option></select>
      {fields.map(f=><input key={f} className="input" placeholder={f} />)}
      <textarea className="input" placeholder="ملاحظات إضافية" rows="4"></textarea>
      <button className="btn btn-primary">إرسال طلب التسجيل كمشتري</button>
    </form>
  )
}
