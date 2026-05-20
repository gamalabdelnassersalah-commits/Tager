import React from 'react'

export default function StatCard({label,value,desc}) {
  return (
    <div className="card">
      <div className="stat-number">{value}</div>
      <h3>{label}</h3>
      <p>{desc}</p>
    </div>
  )
}
