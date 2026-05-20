import React from 'react'
import '../styles/dashboard.css'
import DashboardSidebar from '../components/dashboard/DashboardSidebar.jsx'
import StatCard from '../components/common/StatCard.jsx'
import DataTable from '../components/dashboard/DataTable.jsx'

export default function AdminDashboard() {
  const supplierRows = [{'اسم المورد':'شركة النيل','اسم الشركة':'النيل للزيوت','رقم الهاتف':'01000000000','المحافظة':'القاهرة','عدد المنتجات':'12','حالة التحقق':'بانتظار المراجعة','تاريخ التسجيل':'2026-05-20','إجراء':'اعتماد'}]
  const productRows = [{'صورة المنتج':'-','اسم المنتج':'زيت طعام','المورد':'شركة النيل','التصنيف':'الزيوت','سعر الجملة':'720','الحالة':'بانتظار المراجعة','إجراء':'نشر'}]
  return (
    <section className="section"><div className="container dashboard-layout">
      <DashboardSidebar items={['نظرة عامة','إدارة الموردين','إدارة المشترين','مراجعة المنتجات','إدارة الطلبات','الفئات','العمولات','المدفوعات','الشكاوى','التقارير','الإعدادات']} />
      <div className="dashboard-content">
        <div className="card"><h1>لوحة الإدارة</h1><p>صفحة داخلية لفريق Tager لإدارة المنصة والموردين والمنتجات والطلبات.</p><span className="badge">وصول إداري فقط</span></div>
        <div className="stats"><StatCard label="إجمالي الموردين" value="0" desc="موردين مسجلين" /><StatCard label="بانتظار الاعتماد" value="0" desc="تحتاج مراجعة" /><StatCard label="إجمالي المنتجات" value="0" desc="كل المنتجات" /><StatCard label="إجمالي العمولات" value="0 جنيه" desc="مستحقة" /></div>
        <div className="card"><h2>إدارة الموردين</h2><DataTable columns={Object.keys(supplierRows[0])} rows={supplierRows} /></div>
        <div className="card"><h2>مراجعة المنتجات</h2><DataTable columns={Object.keys(productRows[0])} rows={productRows} /></div>
      </div>
    </div></section>
  )
}
