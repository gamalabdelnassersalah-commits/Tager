import React from 'react'
import '../styles/dashboard.css'
import DashboardSidebar from '../components/dashboard/DashboardSidebar.jsx'
import StatCard from '../components/common/StatCard.jsx'
import DataTable from '../components/dashboard/DataTable.jsx'

export default function CustomerDashboard() {
  const rows = [{'رقم الطلب':'ORD-2001','المورد':'شركة النيل','عدد المنتجات':'4','قيمة الطلب':'12,000 جنيه','طريقة الدفع':'دفع عند الاستلام','حالة الطلب':'جاري التجهيز','تاريخ الطلب':'2026-05-20','إجراء':'تتبع'}]
  const columns = Object.keys(rows[0])
  return (
    <section className="section"><div className="container dashboard-layout">
      <DashboardSidebar items={['نظرة عامة','طلباتي','المنتجات المحفوظة','بيانات النشاط','العناوين','الدعم']} />
      <div className="dashboard-content">
        <div className="card"><h1>لوحة المشتري</h1><p>متابعة طلباتك، إدارة بيانات نشاطك التجاري، حفظ المنتجات، والتواصل مع الدعم.</p></div>
        <div className="stats"><StatCard label="عدد الطلبات" value="0" desc="كل الطلبات" /><StatCard label="الطلبات الجارية" value="0" desc="قيد التنفيذ" /><StatCard label="المنتجات المحفوظة" value="0" desc="للرجوع إليها" /><StatCard label="إجمالي الطلبات" value="0 جنيه" desc="مكتملة" /></div>
        <div className="card"><h2>طلباتي</h2><DataTable columns={columns} rows={rows} /></div>
      </div>
    </div></section>
  )
}
