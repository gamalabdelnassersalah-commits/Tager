import React from 'react'

export default function InfoCard({title,desc,button,onClick,icon='✅'}) {
  return (
    <div className="card">
      <div style={{fontSize:36}}>{icon}</div>
      <h3>{title}</h3>
      <p>{desc}</p>
      {button && <button onClick={onClick} className="btn btn-secondary">{button}</button>}
    </div>
  )
}
