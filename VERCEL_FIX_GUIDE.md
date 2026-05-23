# حل خطأ Vercel: Could not resolve /src/main.jsx

السبب: Vercel يبني من Root Directory لا يحتوي على فولدر `src`.

## الحل الصحيح
بعد فك الضغط، ارفع محتويات المشروع نفسها بحيث تظهر الملفات مباشرة هكذا:

```
index.html
package.json
vite.config.js
vercel.json
src/
supabase/
docs/
```

لا ترفع فولدر داخلي فقط، ولا تضع Root Directory على قيمة مثل:
- src
- public
- tager_full_project_ready

## إعدادات Vercel
- Framework Preset: Vite
- Install Command: npm install
- Build Command: npm run build
- Output Directory: dist
- Root Directory: اتركها فاضية

## اختبار قبل الرفع
على جهازك:
```
npm install
npm run build
```
لو build اشتغل محليًا، ارفع نفس الفولدر إلى Vercel.
