export function calculateTier(product, qty){
  if(qty < product.minWholesaleQty){
    return {type:'none',price:0,total:0,message:'الكمية أقل من الحد الأدنى للطلب.'}
  }
  if(qty >= product.minSuperQty){
    return {type:'super',price:product.superPrice,total:qty*product.superPrice,message:'تم تطبيق سعر جملة الجملة.'}
  }
  return {type:'wholesale',price:product.wholesalePrice,total:qty*product.wholesalePrice,message:'تم تطبيق سعر الجملة.'}
}
