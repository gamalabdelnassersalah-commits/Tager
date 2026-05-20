import React from 'react'
import { ShoppingCart } from 'lucide-react'

const items = [
  ['home','الرئيسية'],
  ['products','المنتجات'],
  ['categories','الفئات'],
  ['supplier','انضم كمورد'],
  ['customer','سجل كمشتري'],
  ['contact','تواصل معنا']
]

export default function Header({currentPage,onNavigate}) {
  return (
    <header className="card" style={{borderRadius:0,position:'sticky',top:0,zIndex:50}}>
      <div className="container" style={{display:'flex',alignItems:'center',justifyContent:'space-between',gap:16}}>
        <button onClick={()=>onNavigate('home')} style={{fontSize:28,fontWeight:900,color:'var(--green)',background:'none',border:0,cursor:'pointer'}}>Tager</button>
        <nav style={{display:'flex',gap:10,flexWrap:'wrap'}}>
          {items.map(([key,label])=>(
            <button key={key} onClick={()=>onNavigate(key)} className={currentPage===key?'btn btn-primary':'btn btn-secondary'}>{label}</button>
          ))}
        </nav>
        <button className="btn btn-gold"><ShoppingCart size={18}/> السلة</button>
      </div>
    </header>
  )
}
