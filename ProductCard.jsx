import React from 'react'
import { calculateTier } from '../../utils/pricing.js'

export default function ProductCard({product}) {
  const tier = calculateTier(product, product.minWholesaleQty)
  return (
    <div className="card product-card">
      <img src={product.image} alt={product.name} />
      <h3>{product.name}</h3>
      <p>المورد: {product.supplier}</p>
      <p>المحافظة: {product.governorate}</p>
      <p>وحدة البيع: {product.unit}</p>
      <div className="price-box">
        <div><b>جملة</b><br />{product.minWholesaleQty} {product.unit}<br />{product.wholesalePrice} جنيه</div>
        <div><b>جملة الجملة</b><br />{product.minSuperQty} {product.unit}<br />{product.superPrice} جنيه</div>
      </div>
      <p className="badge">السعر الحالي عند الحد الأدنى: {tier.price} جنيه</p>
      <div style={{display:'flex',gap:8,marginTop:14}}>
        <button className="btn btn-secondary">عرض التفاصيل</button>
        <button className="btn btn-primary">إضافة للسلة</button>
      </div>
    </div>
  )
}
