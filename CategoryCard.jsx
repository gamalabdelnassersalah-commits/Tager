import React from 'react'

export default function CategoryCard({category,onNavigate}) {
  return (
    <div className="card category-card">
      <img src={category.image} alt={category.name} />
      <h3>{category.name}</h3>
      <p>{category.description}</p>
      <button className="btn btn-secondary" onClick={()=>onNavigate('products')}>عرض المنتجات</button>
    </div>
  )
}
