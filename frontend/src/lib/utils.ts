import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatPercent(value: number): string {
  return `${value.toFixed(1)}%`
}

export function trafficLightColor(light: 'VERDE' | 'AMARILLO' | 'ROJO'): string {
  return {
    VERDE: 'text-semaforo-verde',
    AMARILLO: 'text-semaforo-amarillo',
    ROJO: 'text-semaforo-rojo',
  }[light]
}

export function trafficLightBg(light: 'VERDE' | 'AMARILLO' | 'ROJO'): string {
  return {
    VERDE: 'bg-green-100 text-green-800',
    AMARILLO: 'bg-yellow-100 text-yellow-800',
    ROJO: 'bg-red-100 text-red-800',
  }[light]
}
