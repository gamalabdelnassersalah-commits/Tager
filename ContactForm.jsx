import React from 'react'

export default function ContactForm() {
  return (
    <form className="card form" onSubmit={e=>{e.preventDefault(); alert('تم إرسال رسالتك بنجاح')}}>
      <h2>نموذج التواصل</h2>
      <input className="input" placeholder="الاسم" />
      <input className="input" placeholder="رقم الهاتف" />
      <input className="input" placeholder="البريد الإلكتروني" />
      <select className="input"><option>نوع المستخدم</option><option>مورد</option><option>مشتري</option><option>استفسار عام</option></select>
      <input className="input" placeholder="موضوع الرسالة" />
      <textarea className="input" placeholder="الرسالة" rows="5"></textarea>
      <button className="btn btn-primary">إرسال الرسالة</button>
    </form>
  )
}
