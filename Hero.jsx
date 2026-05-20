import React from 'react'

export default function Hero({title,subtitle,highlight,primary,secondary,onPrimary,onSecondary,emoji='📦'}) {
  return (
    <section className="hero">
      <div className="container hero-inner">
        <div>
          <span className="badge">{highlight}</span>
          <h1 style={{fontSize:46,marginTop:16}}>{title}</h1>
          <p style={{fontSize:18}}>{subtitle}</p>
          <div style={{display:'flex',gap:12,flexWrap:'wrap',marginTop:22}}>
            {primary && <button className="btn btn-primary" onClick={onPrimary}>{primary}</button>}
            {secondary && <button className="btn btn-secondary" onClick={onSecondary}>{secondary}</button>}
          </div>
        </div>
        <div className="hero-art">{emoji}</div>
      </div>
    </section>
  )
}
