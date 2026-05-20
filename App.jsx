import React, { useState } from 'react'
import Header from './components/layout/Header.jsx'
import Footer from './components/layout/Footer.jsx'
import Home from './pages/Home.jsx'
import Products from './pages/Products.jsx'
import Categories from './pages/Categories.jsx'
import SupplierJoin from './pages/SupplierJoin.jsx'
import CustomerJoin from './pages/CustomerJoin.jsx'
import SupplierDashboard from './pages/SupplierDashboard.jsx'
import CustomerDashboard from './pages/CustomerDashboard.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import Contact from './pages/Contact.jsx'

const pages = {
  home: Home,
  products: Products,
  categories: Categories,
  supplier: SupplierJoin,
  customer: CustomerJoin,
  supplierDashboard: SupplierDashboard,
  customerDashboard: CustomerDashboard,
  admin: AdminDashboard,
  contact: Contact
}

export default function App() {
  const [page, setPage] = useState('home')
  const Page = pages[page] || Home

  return (
    <div className="app-shell">
      <Header currentPage={page} onNavigate={setPage} />
      <main>
        <Page onNavigate={setPage} />
      </main>
      <Footer onNavigate={setPage} />
    </div>
  )
}
