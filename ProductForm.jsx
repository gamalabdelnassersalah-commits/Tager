import React from 'react'
export default function ProductForm(){
  const fields = ['اسم المنتج بالعربي','التصنيف','العلامة التجارية','بلد المنشأ','الكمية المتاحة','أقل كمية جملة','سعر الجملة','أقل كمية جملة الجملة','سعر جملة الجملة','المحافظة','مناطق التوصيل']
  return <form className="card form">{fields.map(f=><input className="input" key={f} placeholder={f}/>)}<button className="btn btn-primary">حفظ المنتج</button></form>
}
