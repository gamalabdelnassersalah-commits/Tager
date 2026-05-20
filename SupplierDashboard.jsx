import React from 'react'
import '../styles/dashboard.css'
import DashboardSidebar from '../components/dashboard/DashboardSidebar.jsx'
import StatCard from '../components/common/StatCard.jsx'
import DataTable from '../components/dashboard/DataTable.jsx'
import { orders } from '../data/orders.js'

export default function SupplierDashboard() {
  const productColumns = ['صورة المنتج','اسم المنتج','التصنيف','الكمية المتاحة','أقل كمية جملة','سعر الجملة (جنيه)','حالة المنتج','إجراء']
  const productRows = [{'اسم المنتج':'زيت طعام','التصنيف':'الزيوت','الكمية المتاحة':'500','أقل كمية جملة':'10','سعر الجملة (جنيه)':'720','حالة المنتج':'منشور','إجراء':'تعديل'}]
  return (
    <section className="section"><div className="container dashboard-layout">
      <DashboardSidebar items={['نظرة عامة','منتجاتي','إضافة منتج','الطلبات الجديدة','المخزون','بيانات الشركة','الدعم']} />
      <div className="dashboard-content">
        <div className="card"><h1>لوحة المورد</h1><p>إدارة المنتجات، متابعة الطلبات، تحديث الأسعار، والتحكم في بيانات نشاطك التجاري.</p></div>
        <div className="stats"><StatCard label="عدد المنتجات" value="0" desc="إجمالي المنتجات" /><StatCard label="الطلبات الجديدة" value="0" desc="تحتاج مراجعة" /><StatCard label="إجمالي المبيعات" value="0 جنيه" desc="مبيعات مؤكدة" /><StatCard label="منخفض المخزون" value="0" desc="منتجات تحتاج تحديث" /></div>
        <div className="card"><h2>منتجاتي</h2><DataTable columns={productColumns} rows={productRows} /></div>
        <div className="card"><h2>الطلبات الجديدة</h2><DataTable columns={Object.keys(orders[0])} rows={orders} /></div>
      </div>
    </div></section>
  )
}
