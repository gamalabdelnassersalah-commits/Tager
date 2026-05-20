import React, { useState } from 'react'
import { calculateTier } from '../../utils/pricing.js'

export default function PriceCalculator({product}) {
  const [qty,setQty] = useState(product.minWholesaleQty)
  const result = calculateTier(product, Number(qty))
  return (
    <div className="card">
      <h3>حساب السعر حسب الكمية</h3>
      <input className="input" type="number" value={qty} onChange={e=>setQty(e.target.value)} />
      <p>{result.message}</p>
      <h3>الإجمالي: {result.total.toLocaleString('ar-EG')} جنيه</h3>
    </div>
  )
}
