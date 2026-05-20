export function whatsappLink(phone='201000000000', message='مرحبًا، أريد الاستفسار عن منصة Tager لتجارة الجملة للمواد الغذائية في مصر.'){
  return `https://wa.me/${phone}?text=${encodeURIComponent(message)}`
}
