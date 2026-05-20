import React from 'react'

export default function Footer({onNavigate}) {
  return (
    <footer className="section" style={{background:'var(--green)',color:'white'}}>
      <div className="container grid grid-3">
        <div>
          <h2 style={{color:'white'}}>Tager</h2>
          <p style={{color:'#e5e7eb'}}>منصة تجارة الجملة وجملة الجملة للمواد الغذائية في مصر.</p>
        </div>
        <div>
          <h3 style={{color:'white'}}>روابط سريعة</h3>
          <p><button className="btn btn-secondary" onClick={()=>onNavigate('supplier')}>انضم كمورد</button></p>
          <p><button className="btn btn-secondary" onClick={()=>onNavigate('customer')}>سجل كمشتري</button></p>
        </div>
        <div>
          <h3 style={{color:'white'}}>تواصل</h3>
          <p style={{color:'#e5e7eb'}}>واتساب: ضع رقمك الرسمي هنا</p>
          <p style={{color:'#e5e7eb'}}>© 2026 Tager. جميع الحقوق محفوظة.</p>
        </div>
      </div>
    </footer>
  )
}
