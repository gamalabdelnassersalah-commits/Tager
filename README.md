# Tager No-SRC Vercel Version

هذه النسخة مخصصة لحل خطأ Vercel الخاص بـ `/src/main.jsx`.

## لماذا؟
بعض الرفع على Vercel لا يرفع فولدر `src` بشكل صحيح.  
هذه النسخة تستخدم `main.jsx` في الجذر مباشرة.

## التشغيل
```bash
npm install
npm run dev
```

## النشر على Vercel
تأكد أن الملفات التالية في Root مباشرة:
- index.html
- main.jsx
- style.css
- package.json
- vite.config.js
- vercel.json

الإعدادات:
- Framework: Vite
- Build Command: npm run build
- Output Directory: dist
- Root Directory: فارغ
