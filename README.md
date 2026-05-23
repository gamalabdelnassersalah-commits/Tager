# Tager Full Ready Project

مشروع كامل جاهز كبداية قوية لمنصة **Tager** لتجارة الجملة وجملة الجملة للمواد الغذائية في مصر.

## يحتوي على
- React + Vite
- RTL عربي كامل
- Supabase Backend جاهز
- Demo Mode يعمل بدون Supabase
- صفحات: الرئيسية، المنتجات، الفئات، المورد، المشتري، لوحات التحكم، تواصل معنا
- تسجيل مورد ومشتري
- إضافة منتج
- منطق سعر الجملة وجملة الجملة
- SQL كامل للجداول والحماية RLS
- جاهز للنشر على Vercel

## التشغيل
```bash
npm install
npm run dev
```

## البناء
```bash
npm run build
```

## Supabase
1. أنشئ مشروع Supabase.
2. شغّل `supabase/schema.sql`.
3. شغّل `supabase/seed.sql`.
4. انسخ `.env.example` إلى `.env`.
5. ضع URL و ANON KEY.

## Vercel
- Framework: Vite
- Build Command: npm run build
- Output Directory: dist
- Root Directory: empty
