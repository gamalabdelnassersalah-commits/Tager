import React from 'react'

export default function DataTable({columns,rows}) {
  return (
    <table className="table">
      <thead><tr>{columns.map(c=><th key={c}>{c}</th>)}</tr></thead>
      <tbody>
        {rows.map((row,i)=><tr key={i}>{columns.map(c=><td key={c}>{row[c] || '-'}</td>)}</tr>)}
      </tbody>
    </table>
  )
}
