# AI Projects Platform - Local Smart Engine

منصة إدارة مشاريع عربية RTL تعمل بدون أي API.

## ماذا يعني بدون API؟
التحليل داخل المنصة لا يستخدم OpenAI ولا أي خدمة خارجية. التحليل يعمل من خلال JavaScript داخل المتصفح باستخدام:

- Risk Scoring
- Delay Probability
- Budget Variance
- Manpower Gap
- Consumption Analysis
- Rule-Based Recommendations
- Local Report Generator
- Simple Text Document Analyzer

## التشغيل

```bash
npm install
npm run dev
```

## البناء للرفع على Vercel

```bash
npm run build
```

Output Directory:

```text
dist
```

## ملاحظة مهمة
هذا ليس ذكاء اصطناعي حقيقي مثل ChatGPT، لكنه محرك تحليل ذكي داخلي بدون إنترنت وبدون مفاتيح API.
يمكن تطويره لاحقاً بإضافة قواعد أكثر أو قاعدة بيانات محلية.
