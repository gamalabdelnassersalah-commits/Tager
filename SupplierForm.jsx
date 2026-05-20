import React from 'react'

export default function SupplierForm() {
  const fields = ['اسم الشركة أو النشاط التجاري','اسم المسؤول','رقم الهاتف','رقم واتساب','البريد الإلكتروني','المحافظة','مناطق التوصيل','نوع المنتجات','رقم السجل التجاري إن وجد','الرقم الضريبي إن وجد']
  return (
    <form className="card form" onSubmit={e=>{e.preventDefault(); alert('تم إرسال طلب التسجيل كمورد')}}>
      <h2>نموذج تسجيل المورد</h2>
      {fields.map(f=><input key={f} className="input" placeholder={f} />)}
      <textarea className="input" placeholder="ملاحظات إضافية" rows="4"></textarea>
      <button className="btn btn-primary">إرسال طلب التسجيل كمورد</button>
    </form>
  )
}
