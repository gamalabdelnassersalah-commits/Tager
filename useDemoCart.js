import { useState } from 'react'
export function useDemoCart(){
  const [items,setItems] = useState([])
  const addItem = item => setItems(prev => [...prev,item])
  const clear = () => setItems([])
  return {items,addItem,clear,count:items.length}
}
