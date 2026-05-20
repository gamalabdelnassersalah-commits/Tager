import React from 'react'
export default function StatusBadge({status}){
  const cls = status.includes('مرفوض') || status.includes('ملغي') ? 'status-bad' : status.includes('بانتظار') || status.includes('جاري') ? 'status-warn' : 'status-ok'
  return <span className={'status '+cls}>{status}</span>
}
