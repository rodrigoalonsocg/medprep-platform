import { describe, it, expect } from 'vitest'
import { cn, formatPercent, trafficLightBg, trafficLightColor } from './utils'

describe('utils', () => {
  it('formatPercent redondea a un decimal', () => {
    expect(formatPercent(76.345)).toBe('76.3%')
    expect(formatPercent(0)).toBe('0.0%')
    expect(formatPercent(100)).toBe('100.0%')
  })

  it('trafficLightColor devuelve la clase por color', () => {
    expect(trafficLightColor('VERDE')).toContain('verde')
    expect(trafficLightColor('AMARILLO')).toContain('amarillo')
    expect(trafficLightColor('ROJO')).toContain('rojo')
  })

  it('trafficLightBg devuelve fondo y texto', () => {
    expect(trafficLightBg('VERDE')).toContain('green')
    expect(trafficLightBg('ROJO')).toContain('red')
  })

  it('cn combina y deduplica clases de tailwind', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4')
    expect(cn('text-sm', false && 'hidden', 'font-bold')).toBe('text-sm font-bold')
  })
})
