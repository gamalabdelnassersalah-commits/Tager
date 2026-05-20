import React from 'react'
export default function CTA({title,text,button,onClick}){
  return <div className="card" style={{background:'var(--green)',color:'white'}}>
    <h2 style={{color:'white'}}>{title}</h2>
    <p style={{color:'#e5e7eb'}}>{text}</p>
    <button className="btn btn-gold" onClick={onClick}>{button}</button>
  </div>
}
