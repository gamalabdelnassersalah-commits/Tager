import React, { useState } from 'react'
import { createRoot } from 'react-dom/client'
import { ShoppingCart } from 'lucide-react'
import { createClient } from '@supabase/supabase-js'
import './style.css'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseAnon = import.meta.env.VITE_SUPABASE_ANON_KEY
const supabase = supabaseUrl && supabaseAnon ? createClient(supabaseUrl, supabaseAnon) : null
const whatsappNumber = import.meta.env.VITE_WHATSAPP_NUMBER || '201000000000'
const wa = () => `https://wa.me/${whatsappNumber}?text=${encodeURIComponent('مرحبًا، أريد الاستفسار عن منصة Tager لتجارة الجملة للمواد الغذائية في مصر.')}`

const categories = [
  { id: 1, name: 'المواد الغذائية الجافة', desc: 'مكرونة، أرز، بقوليات، دقيق، سكر ومنتجات أساسية.', icon: '🌾' },
  { id: 2, name: 'الزيوت والسمن', desc: 'زيوت طعام، سمن نباتي وحيواني وعبوات جملة.', icon: '🛢️' },
  { id: 3, name: 'الألبان ومنتجاتها', desc: 'أجبان، لبن، زبادي، قشطة ومنتجات مبردة.', icon: '🧀' },
  { id: 4, name: 'المعلبات', desc: 'تونة، فول، صلصة وخضروات معلبة.', icon: '🥫' }
]

const products = [
  { id: 1, name: 'زيت طعام 1 لتر - كرتونة 12 عبوة', supplier: 'شركة النيل للزيوت', city: 'القاهرة', unit: 'كرتونة', min1: 10, price1: 720, min2: 50, price2: 680, icon: '🛢️' },
  { id: 2, name: 'أرز مصري فاخر 25 كيلو', supplier: 'مورد الدلتا للحبوب', city: 'الدقهلية', unit: 'كيس', min1: 20, price1: 950, min2: 100, price2: 900, icon: '🍚' },
  { id: 3, name: 'مكرونة 400 جرام - كرتونة', supplier: 'المصرية للمكرونة', city: 'الإسكندرية', unit: 'كرتونة', min1: 15, price1: 420, min2: 80, price2: 390, icon: '🍝' },
  { id: 4, name: 'تونة قطع - كرتونة 48 علبة', supplier: 'أغذية البحر', city: 'بورسعيد', unit: 'كرتونة', min1: 8, price1: 1650, min2: 30, price2: 1550, icon: '🥫' }
]

function money(v) {
  return `${Number(v || 0).toLocaleString('ar-EG')} جنيه`
}

function calc(p, qty) {
  const q = Number(qty || 0)
  if (q < p.min1) return { ok: false, price: 0, total: 0, msg: 'الكمية أقل من الحد الأدنى للطلب.' }
  if (q >= p.min2) return { ok: true, price: p.price2, total: q * p.price2, msg: 'تم تطبيق سعر جملة الجملة.' }
  return { ok: true, price: p.price1, total: q * p.price1, msg: 'تم تطبيق سعر الجملة.' }
}

function Header({ page, setPage }) {
  const items = [['home','الرئيسية'], ['products','المنتجات'], ['categories','الفئات'], ['supplier','انضم كمورد'], ['customer','سجل كمشتري'], ['contact','تواصل معنا']]
  return <header className="top"><div className="container nav">
    <button className="logo" onClick={() => setPage('home')}>Tager</button>
    <div className="menu">{items.map(([k,l]) => <button key={k} onClick={() => setPage(k)} className={page===k?'active':''}>{l}</button>)}</div>
    <button className="cart" onClick={() => setPage('products')}><ShoppingCart size={18}/> السلة</button>
  </div></header>
}

function Hero({ title, sub, badge, p1, p2, on1, on2, icon='🥫' }) {
  return <section className="hero"><div className="container heroGrid">
    <div><span className="badge">{badge}</span><h1>{title}</h1><p>{sub}</p><div className="actions">{p1 && <button className="primary" onClick={on1}>{p1}</button>}{p2 && <button className="secondary" onClick={on2}>{p2}</button>}</div></div>
    <div className="art">{icon}</div>
  </div></section>
}

function Card({ icon='✅', title, desc, button, onClick }) {
  return <div className="card"><div className="big">{icon}</div><h3>{title}</h3><p>{desc}</p>{button && <button className="secondary" onClick={onClick}>{button}</button>}</div>
}

function ProductCard({ p }) {
  const [qty, setQty] = useState(p.min1)
  const r = calc(p, qty)
  return <div className="card">
    <div className="image">{p.icon}</div><h3>{p.name}</h3><p>المورد: {p.supplier}</p><p>المحافظة: {p.city} | وحدة البيع: {p.unit}</p>
    <div className="prices"><div><b>جملة</b><br/>من {p.min1} {p.unit}<h3>{money(p.price1)}</h3></div><div><b>جملة الجملة</b><br/>من {p.min2} {p.unit}<h3>{money(p.price2)}</h3></div></div>
    <input className="input" type="number" value={qty} onChange={e=>setQty(e.target.value)} />
    <p className={r.ok?'ok msg':'warn msg'}>{r.msg}</p><h3>الإجمالي: {money(r.total)}</h3>
    <button className="primary" disabled={!r.ok}>إضافة للسلة</button>
  </div>
}

function Form({ type }) {
  const [done, setDone] = useState('')
  const isSupplier = type === 'supplier'
  return <form className="card form" onSubmit={e => { e.preventDefault(); setDone(isSupplier ? 'تم إرسال طلب التسجيل كمورد.' : 'تم إرسال طلب التسجيل كمشتري.') }}>
    <h2>{isSupplier ? 'نموذج تسجيل المورد' : 'نموذج تسجيل المشتري'}</h2>
    <input className="input" placeholder={isSupplier ? 'اسم الشركة أو النشاط التجاري' : 'اسم النشاط التجاري'} required />
    <input className="input" placeholder="اسم المسؤول" required />
    <input className="input" placeholder="رقم الهاتف" required />
    <input className="input" placeholder="رقم واتساب" />
    <input className="input" placeholder="البريد الإلكتروني" type="email" required />
    <input className="input" placeholder="كلمة المرور" type="password" required />
    <input className="input" placeholder="المحافظة" />
    <textarea className="input" placeholder={isSupplier ? 'نوع المنتجات ومناطق التوصيل' : 'العنوان والمنتجات التي تهتم بها'} />
    <button className="primary">{isSupplier ? 'إرسال طلب التسجيل كمورد' : 'إرسال طلب التسجيل كمشتري'}</button>
    {done && <div className="notice">{done}</div>}
  </form>
}

function Home({ setPage }) {
  return <>
    <Hero title="منصة Tager لتجارة الجملة للمواد الغذائية في مصر" sub="Tager يربط بين موردي المواد الغذائية المعتمدين وبين التجار، السوبرماركت، المطاعم، الكافيهات، الفنادق والشركات في جميع محافظات مصر." badge="أسعار جملة وجملة الجملة – كميات كبيرة – توصيل للمحافظات" p1="تسجيل كمورد" p2="تسجيل مشتري" on1={()=>setPage('supplier')} on2={()=>setPage('customer')} />
    <section className="section"><div className="container"><h2>ما هي منصة Tager؟</h2><p>منصة إلكترونية متخصصة في تجارة الجملة وجملة الجملة للمواد الغذائية في مصر، تساعد الموردين على عرض منتجاتهم وتساعد المشترين على المقارنة والطلب بسهولة.</p></div></section>
    <section className="section"><div className="container"><h2>الفئات الرئيسية</h2><div className="grid">{categories.map(c => <Card key={c.id} icon={c.icon} title={c.name} desc={c.desc} button="عرض المنتجات" onClick={()=>setPage('products')} />)}</div></div></section>
    <section className="section"><div className="container grid two"><Card icon="🏭" title="مميزات الموردين" desc="اعرض منتجاتك، حدد أسعار الجملة، واستقبل طلبات جديدة." button="انضم كمورد" onClick={()=>setPage('supplier')} /><Card icon="🛒" title="مميزات المشترين" desc="قارن الأسعار واحصل على سعر الجملة وجملة الجملة حسب الكمية." button="سجل كمشتري" onClick={()=>setPage('customer')} /></div></section>
  </>
}

function Products() {
  return <><Hero title="تصفح منتجات الجملة على Tager" sub="اكتشف مواد غذائية بأسعار جملة وجملة الجملة من موردين معتمدين." badge="حدد الكمية وشاهد السعر المناسب تلقائياً" icon="🛍️" /><section className="section"><div className="container grid">{products.map(p => <ProductCard key={p.id} p={p} />)}</div></section></>
}

function Categories({ setPage }) {
  return <><Hero title="فئات المواد الغذائية على Tager" sub="اختر الفئة المناسبة وابدأ تصفح منتجات الجملة." badge="فئات واضحة للوصول للمنتجات بسرعة" icon="🧺" /><section className="section"><div className="container grid">{categories.map(c => <Card key={c.id} icon={c.icon} title={c.name} desc={c.desc} button="عرض المنتجات" onClick={()=>setPage('products')} />)}</div></section></>
}

function Supplier({ setPage }) {
  return <><Hero title="انضم إلى Tager كمورد مواد غذائية" sub="اعرض منتجاتك بالجملة وجملة الجملة ووصل للتجار والسوبرماركت والمطاعم في مصر." badge="عملاء جملة حقيقيين – أسعار واضحة – حد أدنى للطلب" p1="تسجيل كمورد الآن" p2="تواصل واتساب" on2={()=>window.open(wa(), '_blank')} icon="🏭" /><section className="section"><div className="container"><Form type="supplier" /></div></section></>
}

function Customer({ setPage }) {
  return <><Hero title="اشترِ مواد غذائية بالجملة من موردين معتمدين" sub="تصفح المنتجات، قارن الأسعار، اختر الكمية، واحصل على السعر تلقائياً حسب كمية الطلب." badge="منصة واحدة تربطك بموردي الجملة" p1="سجل كمشتري الآن" p2="تصفح المنتجات" on2={()=>setPage('products')} icon="🛒" /><section className="section"><div className="container"><Form type="customer" /></div></section></>
}

function Contact({ setPage }) {
  return <><Hero title="تواصل مع فريق Tager" sub="نحن هنا لمساعدة الموردين والمشترين في التسجيل والمنتجات والطلبات." badge="اختر طريقة التواصل المناسبة وسيرد عليك فريقنا قريباً" icon="☎️" /><section className="section"><div className="container grid"><Card icon="💬" title="واتساب" desc="للاستفسارات السريعة." button="تواصل واتساب" onClick={()=>window.open(wa(), '_blank')} /><Card icon="📧" title="البريد الإلكتروني" desc="للمراسلات الرسمية." /><Card icon="📝" title="نموذج التواصل" desc="املأ النموذج وسنرد عليك." /></div></section></>
}

function Dashboard({ title }) {
  return <section className="section"><div className="container"><div className="card"><h1>{title}</h1><p>صفحة داخلية للتجربة والتطوير.</p><div className="stats"><Card title="الطلبات" desc="0" /><Card title="المنتجات" desc="0" /><Card title="المبيعات" desc="0 جنيه" /></div></div></div></section>
}

function App() {
  const [page, setPage] = useState('home')
  const pages = {
    home: <Home setPage={setPage}/>,
    products: <Products/>,
    categories: <Categories setPage={setPage}/>,
    supplier: <Supplier setPage={setPage}/>,
    customer: <Customer setPage={setPage}/>,
    contact: <Contact setPage={setPage}/>,
    supplierDashboard: <Dashboard title="لوحة المورد"/>,
    customerDashboard: <Dashboard title="لوحة المشتري"/>,
    admin: <Dashboard title="لوحة الإدارة"/>
  }
  return <><Header page={page} setPage={setPage}/>{pages[page] || pages.home}<section className="section"><div className="container card"><h2>روابط داخلية للاختبار</h2><div className="actions"><button className="secondary" onClick={()=>setPage('supplierDashboard')}>لوحة المورد</button><button className="secondary" onClick={()=>setPage('customerDashboard')}>لوحة المشتري</button><button className="secondary" onClick={()=>setPage('admin')}>لوحة الإدارة</button></div></div></section><footer><div className="container"><h2>Tager</h2><p>© 2026 Tager. جميع الحقوق محفوظة.</p></div></footer></>
}

createRoot(document.getElementById('root')).render(<App />)
