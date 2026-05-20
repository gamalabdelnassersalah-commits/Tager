# Deploy to Vercel

## مهم جدًا
ارفع محتويات المشروع من الجذر مباشرة، وليس فولدر داخلي فارغ.

لازم تكون الملفات التالية موجودة في نفس المستوى:
- index.html
- package.json
- vite.config.js
- vercel.json
- src/main.jsx

## إعدادات Vercel
Framework Preset: Vite  
Build Command: npm run build  
Output Directory: dist  
Install Command: npm install  

## لو ظهر خطأ Failed to resolve /src/main.jsx
معناه أن فولدر src لم يتم رفعه أو أن Root Directory في Vercel غير صحيح.

الحل:
1. تأكد أن src موجود بجانب index.html.
2. في Vercel > Project Settings > General > Root Directory اتركها فاضية.
3. أعد رفع المشروع كاملًا.
