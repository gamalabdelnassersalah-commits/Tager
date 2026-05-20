import React from 'react'

export default function SectionTitle({title,subtitle}) {
  return (
    <div style={{marginBottom:24}}>
      <h2 style={{fontSize:34}}>{title}</h2>
      {subtitle && <p>{subtitle}</p>}
    </div>
  )
}
