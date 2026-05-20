import React from 'react'
export default function EmptyState({title='لا توجد بيانات',text='أضف بيانات تجريبية للبدء.'}){
  return <div className="card"><h3>{title}</h3><p>{text}</p></div>
}
