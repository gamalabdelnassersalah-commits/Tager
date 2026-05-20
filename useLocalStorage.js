import { useEffect, useState } from 'react'
export function useLocalStorage(key, initial){
  const [value,setValue] = useState(()=> JSON.parse(localStorage.getItem(key) || 'null') ?? initial)
  useEffect(()=>localStorage.setItem(key, JSON.stringify(value)),[key,value])
  return [value,setValue]
}
