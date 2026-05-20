import React from 'react'

export default function DashboardSidebar({items}) {
  return (
    <aside className="sidebar">
      {items.map((item,i)=><button key={i} className={i===0?'btn btn-primary':'btn btn-secondary'}>{item}</button>)}
    </aside>
  )
}
