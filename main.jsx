import React, { useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line, AreaChart, Area } from 'recharts';
import { LayoutDashboard, FolderKanban, ClipboardList, Bot, FileText, Upload, Bell, Search, Plus, Users, Settings, WalletCards, AlertTriangle, CheckCircle2, Clock3, Menu, X, BrainCircuit, ShieldAlert, TrendingUp, FileSpreadsheet } from 'lucide-react';
import './style.css';

const today = new Date('2026-05-23T12:00:00+03:00');

const projectsSeed = [
  { id: 1, name: 'مشروع مستشفى الدمام', client: 'Hospital Group', manager: 'أحمد عمر', status: 'متأخر', progress: 62, planned: 76, budget: 1250000, spent: 910000, manpower: 42, requiredManpower: 55, end: '2026-07-30', category: 'تشغيل ونظافة' },
  { id: 2, name: 'تشغيل ونظافة مبنى الإدارة', client: 'شركة الغريري', manager: 'خالد سالم', status: 'نشط', progress: 78, planned: 80, budget: 520000, spent: 311000, manpower: 23, requiredManpower: 24, end: '2026-06-18', category: 'تشغيل' },
  { id: 3, name: 'توريد مواد ومعدات', client: 'Procurement', manager: 'محمد علي', status: 'نشط', progress: 45, planned: 52, budget: 740000, spent: 256000, manpower: 12, requiredManpower: 15, end: '2026-08-10', category: 'مشتريات' },
  { id: 4, name: 'مشروع صيانة المرافق', client: 'Facilities', manager: 'سعيد حسن', status: 'مكتمل', progress: 100, planned: 100, budget: 380000, spent: 352000, manpower: 18, requiredManpower: 18, end: '2026-05-05', category: 'صيانة' },
];

const tasksSeed = [
  { id: 1, projectId: 1, project: 'مشروع مستشفى الدمام', title: 'اعتماد خطة العمالة', owner: 'أحمد', due: '2026-05-20', priority: 'عالية', status: 'متأخرة', weight: 25 },
  { id: 2, projectId: 2, project: 'تشغيل ونظافة مبنى الإدارة', title: 'تحديث تقرير الاستهلاك', owner: 'خالد', due: '2026-05-26', priority: 'متوسطة', status: 'قيد التنفيذ', weight: 12 },
  { id: 3, projectId: 3, project: 'توريد مواد ومعدات', title: 'مقارنة عروض Reza / SIDCO / NAPCO', owner: 'جمال', due: '2026-05-29', priority: 'عالية', status: 'لم تبدأ', weight: 20 },
  { id: 4, projectId: 4, project: 'مشروع صيانة المرافق', title: 'إغلاق التقرير النهائي', owner: 'سعيد', due: '2026-05-20', priority: 'منخفضة', status: 'مكتملة', weight: 5 },
  { id: 5, projectId: 1, project: 'مشروع مستشفى الدمام', title: 'تثبيت جدول الورديات', owner: 'أحمد', due: '2026-05-22', priority: 'عالية', status: 'متأخرة', weight: 18 },
  { id: 6, projectId: 1, project: 'مشروع مستشفى الدمام', title: 'اعتماد مواد التنظيف', owner: 'جمال', due: '2026-05-27', priority: 'متوسطة', status: 'قيد التنفيذ', weight: 10 },
];

const consumptionsSeed = [
  { projectId: 1, item: 'مطهرات', plannedQty: 100, actualQty: 138, unitCost: 18 },
  { projectId: 1, item: 'أكياس نفايات', plannedQty: 600, actualQty: 710, unitCost: 1.8 },
  { projectId: 2, item: 'منظف أرضيات', plannedQty: 80, actualQty: 74, unitCost: 22 },
  { projectId: 3, item: 'قطع غيار', plannedQty: 50, actualQty: 44, unitCost: 115 },
];

const chartData = [
  { name: 'يناير', planned: 40, actual: 35 },
  { name: 'فبراير', planned: 55, actual: 49 },
  { name: 'مارس', planned: 70, actual: 58 },
  { name: 'أبريل', planned: 82, actual: 74 },
  { name: 'مايو', planned: 90, actual: 81 },
];

const menu = [
  { label: 'لوحة التحكم', icon: LayoutDashboard, key: 'dashboard' },
  { label: 'المشاريع', icon: FolderKanban, key: 'projects' },
  { label: 'المهام', icon: ClipboardList, key: 'tasks' },
  { label: 'تحليل محلي ذكي', icon: BrainCircuit, key: 'ai' },
  { label: 'تحليل الملفات', icon: FileSpreadsheet, key: 'files' },
  { label: 'التقارير', icon: FileText, key: 'reports' },
  { label: 'الميزانية', icon: WalletCards, key: 'budget' },
  { label: 'الموظفين', icon: Users, key: 'employees' },
  { label: 'الإعدادات', icon: Settings, key: 'settings' },
];

function daysBetween(dateA, dateB) {
  const a = new Date(dateA);
  const b = new Date(dateB);
  return Math.ceil((a - b) / (1000 * 60 * 60 * 24));
}

function money(n) {
  return new Intl.NumberFormat('ar-SA', { style: 'currency', currency: 'SAR', maximumFractionDigits: 0 }).format(n);
}

function priorityScore(priority) {
  if (priority === 'عالية') return 25;
  if (priority === 'متوسطة') return 12;
  return 5;
}

function analyzeProject(project, tasks, consumptions) {
  const projectTasks = tasks.filter(t => t.projectId === project.id);
  const delayedTasks = projectTasks.filter(t => t.status === 'متأخرة');
  const openHighTasks = projectTasks.filter(t => t.priority === 'عالية' && t.status !== 'مكتملة');
  const budgetUse = project.budget ? (project.spent / project.budget) * 100 : 0;
  const progressGap = Math.max(project.planned - project.progress, 0);
  const manpowerGap = Math.max(project.requiredManpower - project.manpower, 0);
  const daysToEnd = daysBetween(project.end, today);
  const projectConsumptions = consumptions.filter(c => c.projectId === project.id);
  const overConsumption = projectConsumptions.reduce((sum, c) => sum + Math.max(c.actualQty - c.plannedQty, 0) * c.unitCost, 0);
  const consumptionRate = projectConsumptions.length
    ? Math.round(projectConsumptions.reduce((sum, c) => sum + (c.actualQty / c.plannedQty) * 100, 0) / projectConsumptions.length)
    : 100;

  let risk = 0;
  risk += progressGap * 1.7;
  risk += delayedTasks.length * 15;
  risk += openHighTasks.length * 12;
  risk += Math.max(budgetUse - project.progress, 0) * 0.9;
  risk += manpowerGap * 2.5;
  risk += consumptionRate > 110 ? (consumptionRate - 100) * 0.6 : 0;
  risk += daysToEnd < 30 && project.progress < 85 ? 12 : 0;
  risk = Math.min(Math.round(risk), 100);

  const level = risk >= 70 ? 'خطر عالي' : risk >= 40 ? 'خطر متوسط' : 'مستقر';
  const delayProbability = Math.min(95, Math.round(risk * 0.82 + progressGap * 0.7));
  const expectedOverrun = Math.max(0, Math.round(project.spent - (project.progress / 100) * project.budget + overConsumption));

  const recommendations = [];
  if (delayedTasks.length) recommendations.push(`إغلاق ${delayedTasks.length} مهمة متأخرة خلال 48 ساعة.`);
  if (openHighTasks.length) recommendations.push(`توجيه المهام عالية الأولوية إلى مدير المشروع يومياً.`);
  if (manpowerGap) recommendations.push(`زيادة العمالة المقترحة: ${manpowerGap} فرد/أفراد حتى يعود الإنجاز للمخطط.`);
  if (budgetUse > project.progress + 10) recommendations.push('مراجعة المصروفات لأنها أعلى من نسبة الإنجاز الفعلية.');
  if (consumptionRate > 110) recommendations.push(`مراجعة استهلاك المواد لأنه وصل إلى ${consumptionRate}% من المخطط.`);
  if (!recommendations.length) recommendations.push('المشروع مستقر، استمر في التحديث الأسبوعي والتوثيق.');

  return { risk, level, delayedTasks, openHighTasks, budgetUse, progressGap, manpowerGap, daysToEnd, consumptionRate, delayProbability, expectedOverrun, recommendations };
}

function analyzeAll(projects, tasks, consumptions) {
  const projectAnalyses = projects.map(p => ({ ...p, ai: analyzeProject(p, tasks, consumptions) }));
  const delayed = tasks.filter(t => t.status === 'متأخرة');
  const highOpen = tasks.filter(t => t.priority === 'عالية' && t.status !== 'مكتملة');
  const avgRisk = Math.round(projectAnalyses.reduce((a, p) => a + p.ai.risk, 0) / projectAnalyses.length);
  const topRisk = [...projectAnalyses].sort((a, b) => b.ai.risk - a.ai.risk)[0];
  return { projectAnalyses, delayed, highOpen, avgRisk, topRisk };
}

function generateAnswer(question, analysis, stats) {
  const q = question.trim();
  const top = analysis.topRisk;
  if (!q) return 'اكتب سؤالك أولاً، مثال: حلل التأخير أو اعمل تقرير للإدارة.';

  if (q.includes('تقرير') || q.includes('report')) {
    return `تقرير تنفيذي محلي بدون API:\n\nإجمالي المشاريع: ${stats.total}\nمتوسط الإنجاز: ${stats.avg}%\nإجمالي الميزانية: ${money(stats.budget)}\nإجمالي المصروف: ${money(stats.spent)}\nمتوسط المخاطر: ${analysis.avgRisk}%\nأعلى مشروع خطورة: ${top.name} - ${top.ai.level} (${top.ai.risk}%)\nاحتمال التأخير: ${top.ai.delayProbability}%\n\nالتوصيات:\n- ${top.ai.recommendations.join('\n- ')}\n- متابعة المهام المتأخرة وعددها ${analysis.delayed.length}.\n- مراجعة المهام عالية الأولوية وعددها ${analysis.highOpen.length}.`;
  }

  if (q.includes('تكلفة') || q.includes('ميزانية') || q.includes('مصروف')) {
    const over = analysis.projectAnalyses.filter(p => p.ai.expectedOverrun > 0).sort((a,b)=>b.ai.expectedOverrun-a.ai.expectedOverrun);
    return `تحليل الميزانية المحلي:\n\nإجمالي المصروفات: ${money(stats.spent)} من ${money(stats.budget)} بنسبة ${Math.round(stats.spent / stats.budget * 100)}%.\nأعلى مشروع يحتاج مراجعة: ${over[0]?.name || top.name}.\nانحراف متوقع: ${money(over[0]?.ai.expectedOverrun || top.ai.expectedOverrun)}.\n\nقرار مقترح:\n- إيقاف الشراء غير الضروري مؤقتاً.\n- مقارنة سعر الموردين قبل الاعتماد.\n- مراجعة استهلاك المواد الذي تجاوز المخطط.\n- ربط الصرف بنسبة الإنجاز وليس بمجرد الطلب.`;
  }

  if (q.includes('تأخير') || q.includes('متأخر') || q.includes('risk') || q.includes('خطر')) {
    return `تحليل التأخير والمخاطر:\n\nأعلى مشروع معرض للتأخير: ${top.name}.\nدرجة الخطر: ${top.ai.risk}%.\nاحتمال التأخير: ${top.ai.delayProbability}%.\nفجوة الإنجاز عن المخطط: ${top.ai.progressGap}%.\nالمهام المتأخرة داخل المشروع: ${top.ai.delayedTasks.length}.\nنقص العمالة: ${top.ai.manpowerGap}.\n\nالإجراء السريع:\n- ${top.ai.recommendations.join('\n- ')}`;
  }

  return `تحليل محلي ذكي حسب البيانات الحالية:\n\nيوجد ${stats.total} مشاريع، متوسط الإنجاز ${stats.avg}%، ومتوسط المخاطر ${analysis.avgRisk}%.\nأعلى مشروع يحتاج تدخل هو ${top.name} بدرجة خطر ${top.ai.risk}% واحتمال تأخير ${top.ai.delayProbability}%.\n\nالتوصيات المختصرة:\n- ${top.ai.recommendations.join('\n- ')}\n- تحديث المهام يومياً.\n- مراجعة الميزانية مقابل الإنجاز.\n- إصدار تقرير أسبوعي للإدارة.`;
}

function Badge({ children, type = 'default' }) { return <span className={`badge ${type}`}>{children}</span>; }
function Card({ title, value, icon: Icon, note, danger }) { return <div className="card stat-card"><div><p>{title}</p><h2>{value}</h2><span>{note}</span></div><div className={`icon-box ${danger ? 'danger-bg' : ''}`}><Icon size={24}/></div></div>; }

function App(){
  const [active, setActive] = useState('dashboard');
  const [sidebar, setSidebar] = useState(false);
  const [projects] = useState(projectsSeed);
  const [tasks] = useState(tasksSeed);
  const [consumptions] = useState(consumptionsSeed);
  const [aiInput, setAiInput] = useState('حلل التأخير والمخاطر في المشاريع');
  const [aiAnswer, setAiAnswer] = useState('');
  const [fileText, setFileText] = useState('فاتورة مواد تنظيف لمشروع مستشفى الدمام بقيمة 48500 ريال. يوجد زيادة استهلاك مطهرات وأكياس نفايات.');
  const [fileResult, setFileResult] = useState('');

  const stats = useMemo(() => ({
    total: projects.length,
    active: projects.filter(p=>p.status==='نشط').length,
    delayed: projects.filter(p=>p.status==='متأخر').length,
    completed: projects.filter(p=>p.status==='مكتمل').length,
    avg: Math.round(projects.reduce((a,p)=>a+p.progress,0)/projects.length),
    budget: projects.reduce((a,p)=>a+p.budget,0),
    spent: projects.reduce((a,p)=>a+p.spent,0),
  }), [projects]);

  const analysis = useMemo(() => analyzeAll(projects, tasks, consumptions), [projects, tasks, consumptions]);
  const budgetData = [{ name: 'مستخدم', value: stats.spent }, { name: 'متبقي', value: stats.budget - stats.spent }];
  const riskChart = analysis.projectAnalyses.map(p => ({ name: p.name.replace('مشروع ', ''), risk: p.ai.risk, delay: p.ai.delayProbability }));

  const runLocalAI = () => setAiAnswer(generateAnswer(aiInput, analysis, stats));

  const analyzeTextFile = () => {
    const text = fileText.toLowerCase();
    const foundMoney = [...fileText.matchAll(/\d+[,.]?\d*/g)].map(m => Number(m[0].replace(',', ''))).filter(n => n > 100);
    const amount = foundMoney.length ? Math.max(...foundMoney) : 0;
    const type = text.includes('فاتورة') ? 'فاتورة' : text.includes('عقد') ? 'عقد' : text.includes('تقرير') ? 'تقرير' : 'مستند عام';
    const riskWords = ['زيادة', 'متأخر', 'غرامة', 'نقص', 'مشكلة', 'استهلاك'];
    const hits = riskWords.filter(w => fileText.includes(w));
    const risk = Math.min(100, hits.length * 20 + (amount > 30000 ? 20 : 0));
    setFileResult(`نوع المستند المتوقع: ${type}\nالقيمة المستخرجة تقريبياً: ${amount ? money(amount) : 'لم يتم العثور على مبلغ'}\nكلمات الخطر: ${hits.length ? hits.join('، ') : 'لا يوجد'}\nدرجة المخاطر: ${risk}%\nالتوصية: ${risk > 50 ? 'مراجعة المستند واعتماده من المدير قبل الصرف.' : 'المستند منخفض المخاطر ويمكن أرشفته.'}`);
  };

  return <div className="app">
    <aside className={`sidebar ${sidebar ? 'open' : ''}`}>
      <div className="brand"><div className="logo">AI</div><div><h1>منصة المشاريع</h1><p>Local Smart Engine</p></div><button className="close" onClick={()=>setSidebar(false)}><X size={20}/></button></div>
      <nav>{menu.map(item => { const Icon = item.icon; return <button key={item.key} onClick={()=>{setActive(item.key); setSidebar(false)}} className={active===item.key?'active':''}><Icon size={20}/>{item.label}</button> })}</nav>
    </aside>

    <main>
      <header className="topbar"><button className="hamb" onClick={()=>setSidebar(true)}><Menu/></button><div className="search"><Search size={18}/><input placeholder="بحث عن مشروع، مهمة، ملف..."/></div><button className="notify"><Bell size={20}/><span></span></button><button className="add"><Plus size={18}/> إضافة مشروع</button></header>

      {active==='dashboard' && <section className="page"><div className="title"><h2>لوحة التحكم الذكية بدون API</h2><p>التحليل يعمل من داخل الكود فقط باستخدام قواعد حسابية وRisk Scoring.</p></div>
        <div className="stats"><Card title="إجمالي المشاريع" value={stats.total} icon={FolderKanban} note={`${stats.active} نشط`} /><Card title="متوسط الإنجاز" value={`${stats.avg}%`} icon={CheckCircle2} note="حسب آخر تحديث" /><Card title="متوسط المخاطر" value={`${analysis.avgRisk}%`} icon={ShieldAlert} note={analysis.topRisk.name} danger /><Card title="المصروفات" value={money(stats.spent)} icon={WalletCards} note={`من ${money(stats.budget)}`} /></div>
        <div className="grid two"><div className="card"><h3>المخطط مقابل الفعلي</h3><ResponsiveContainer width="100%" height={280}><BarChart data={chartData}><XAxis dataKey="name"/><YAxis/><Tooltip/><Bar dataKey="planned" name="المخطط"/><Bar dataKey="actual" name="الفعلي"/></BarChart></ResponsiveContainer></div><div className="card"><h3>نسبة المخاطر والتأخير</h3><ResponsiveContainer width="100%" height={280}><AreaChart data={riskChart}><XAxis dataKey="name"/><YAxis/><Tooltip/><Area dataKey="risk" name="الخطر"/><Area dataKey="delay" name="احتمال التأخير"/></AreaChart></ResponsiveContainer></div></div>
        <div className="card alerts"><h3>تنبيهات المحرك المحلي</h3>{analysis.projectAnalyses.filter(p=>p.ai.risk>35).map(p=><div key={p.id}><AlertTriangle/> {p.name}: {p.ai.level} - خطر {p.ai.risk}% - {p.ai.recommendations[0]}</div>)}</div>
      </section>}

      {active==='projects' && <section className="page"><div className="title"><h2>إدارة المشاريع</h2><p>كل مشروع يتم تحليله محلياً بدون أي اتصال خارجي.</p></div><div className="card table-wrap"><table><thead><tr><th>المشروع</th><th>الحالة</th><th>الإنجاز/المخطط</th><th>الخطر</th><th>احتمال التأخير</th><th>التوصية</th></tr></thead><tbody>{analysis.projectAnalyses.map(p=><tr key={p.id}><td>{p.name}<br/><small>{p.client} - {p.manager}</small></td><td><Badge type={p.status==='متأخر'?'danger':p.status==='مكتمل'?'success':'default'}>{p.status}</Badge></td><td>{p.progress}% / {p.planned}%<div className="progress"><span style={{width:p.progress+'%'}}></span></div></td><td><Badge type={p.ai.risk>=70?'danger':p.ai.risk>=40?'warning':'success'}>{p.ai.risk}%</Badge></td><td>{p.ai.delayProbability}%</td><td>{p.ai.recommendations[0]}</td></tr>)}</tbody></table></div></section>}

      {active==='tasks' && <section className="page"><div className="title"><h2>المهام</h2><p>تحليل وزن المهمة والأولوية والتأخير.</p></div><div className="card table-wrap"><table><thead><tr><th>المهمة</th><th>المشروع</th><th>المسؤول</th><th>الموعد</th><th>الأولوية</th><th>الحالة</th><th>النقاط</th></tr></thead><tbody>{tasks.map(t=><tr key={t.id}><td>{t.title}</td><td>{t.project}</td><td>{t.owner}</td><td>{t.due}</td><td><Badge type={t.priority==='عالية'?'danger':'default'}>{t.priority}</Badge></td><td>{t.status}</td><td>{t.weight + priorityScore(t.priority)}</td></tr>)}</tbody></table></div></section>}

      {active==='ai' && <section className="page"><div className="title"><h2>محرك تحليل محلي بدون API</h2><p>اكتب سؤالاً، وسيتم توليد الرد من بيانات المشروع والقواعد الداخلية فقط.</p></div><div className="card ai-box"><textarea value={aiInput} onChange={e=>setAiInput(e.target.value)} /><button onClick={runLocalAI}><Bot size={18}/> تحليل محلي الآن</button>{aiAnswer && <pre className="ai-answer">{aiAnswer}</pre>}<small>لا يوجد OpenAI API ولا Backend. كل التحليل يعمل داخل المتصفح.</small></div></section>}

      {active==='files' && <section className="page"><div className="title"><h2>تحليل نصوص الملفات بدون API</h2><p>ضع نص فاتورة أو عقد أو تقرير، وسيتم استخراج النوع والمبلغ والمخاطر بقواعد محلية.</p></div><div className="card ai-box"><textarea value={fileText} onChange={e=>setFileText(e.target.value)} /><button onClick={analyzeTextFile}><FileSpreadsheet size={18}/> تحليل المستند</button>{fileResult && <pre className="ai-answer">{fileResult}</pre>}<small>بدون OCR وبدون API. التحليل يعمل على النص المدخل فقط.</small></div></section>}

      {active==='reports' && <section className="page"><div className="title"><h2>التقارير</h2><p>تقارير محلية يتم توليدها من البيانات والتحليل الداخلي.</p></div><div className="grid two"><div className="card"><h3>تقرير الإدارة</h3><p>أعلى خطر: {analysis.topRisk.name} - {analysis.topRisk.ai.risk}%</p><button onClick={()=>{setActive('ai'); setAiInput('اعمل تقرير للإدارة');}}>إنشاء تقرير</button></div><div className="card"><h3>تقرير الميزانية</h3><p>المصروفات الحالية {money(stats.spent)} من {money(stats.budget)}</p><button onClick={()=>{setActive('ai'); setAiInput('حلل الميزانية والمصروفات');}}>إنشاء تقرير</button></div></div></section>}

      {active==='budget' && <section className="page"><div className="title"><h2>الميزانية</h2><p>مقارنة المصروفات بالميزانية ونسبة الإنجاز.</p></div><div className="grid two"><div className="card"><ResponsiveContainer width="100%" height={280}><PieChart><Pie data={budgetData} dataKey="value" nameKey="name" outerRadius={95} label>{budgetData.map((_,i)=><Cell key={i}/>)}</Pie><Tooltip/></PieChart></ResponsiveContainer></div><div className="card"><h3>ملخص</h3><p>إجمالي الميزانية: {money(stats.budget)}</p><p>إجمالي المصروف: {money(stats.spent)}</p><p>النسبة المستخدمة: {Math.round(stats.spent/stats.budget*100)}%</p></div></div></section>}

      {['employees','settings'].includes(active) && <section className="page"><div className="title"><h2>{menu.find(m=>m.key===active)?.label}</h2><p>هذه الصفحة جاهزة للتطوير حسب متطلبات شركتك.</p></div><div className="card"><ResponsiveContainer width="100%" height={280}><LineChart data={chartData}><XAxis dataKey="name"/><YAxis/><Tooltip/><Line dataKey="actual" name="المؤشر"/></LineChart></ResponsiveContainer></div></section>}
    </main>
  </div>
}

createRoot(document.getElementById('root')).render(<App/>);
