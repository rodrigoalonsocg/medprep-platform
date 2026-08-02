import { useState, useEffect, useCallback } from 'react'
import { Play, Pause, RotateCcw, Timer } from 'lucide-react'
import { cn } from '@/lib/utils'

const POMODORO_MINUTES = 50
const BREAK_MINUTES = 10

export default function WorkspacePage() {
  const [seconds, setSeconds] = useState(POMODORO_MINUTES * 60)
  const [isRunning, setIsRunning] = useState(false)
  const [isBreak, setIsBreak] = useState(false)

  const reset = useCallback(() => {
    setIsRunning(false)
    setSeconds(isBreak ? BREAK_MINUTES * 60 : POMODORO_MINUTES * 60)
  }, [isBreak])

  useEffect(() => {
    if (!isRunning) return
    const id = setInterval(() => {
      setSeconds((s) => {
        if (s <= 1) {
          setIsRunning(false)
          setIsBreak((b) => !b)
          return isBreak ? POMODORO_MINUTES * 60 : BREAK_MINUTES * 60
        }
        return s - 1
      })
    }, 1000)
    return () => clearInterval(id)
  }, [isRunning, isBreak])

  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  const progress = isBreak
    ? ((BREAK_MINUTES * 60 - seconds) / (BREAK_MINUTES * 60)) * 100
    : ((POMODORO_MINUTES * 60 - seconds) / (POMODORO_MINUTES * 60)) * 100

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Workspace</h1>
        <p className="text-muted-foreground">Organiza tu sesión de estudio</p>
      </div>

      {/* Pomodoro */}
      <div className="flex flex-col items-center rounded-xl border bg-card p-10 shadow-sm">
        <div className="mb-2 flex items-center gap-2 text-sm font-medium text-muted-foreground">
          <Timer className="h-4 w-4" />
          {isBreak ? 'Descanso' : 'Estudio'}
        </div>

        {/* Ring */}
        <div className="relative my-6 h-48 w-48">
          <svg className="h-full w-full -rotate-90" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="44" fill="none" stroke="hsl(var(--muted))" strokeWidth="8" />
            <circle
              cx="50"
              cy="50"
              r="44"
              fill="none"
              stroke={isBreak ? '#16a34a' : 'hsl(var(--primary))'}
              strokeWidth="8"
              strokeLinecap="round"
              strokeDasharray={`${2 * Math.PI * 44}`}
              strokeDashoffset={`${2 * Math.PI * 44 * (1 - progress / 100)}`}
              className="transition-all duration-1000"
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-4xl font-bold tabular-nums">
              {String(minutes).padStart(2, '0')}:{String(secs).padStart(2, '0')}
            </span>
          </div>
        </div>

        <div className="flex gap-3">
          <button
            onClick={() => setIsRunning((r) => !r)}
            className={cn(
              'flex items-center gap-2 rounded-lg px-6 py-2.5 font-medium',
              isBreak ? 'bg-green-600 text-white' : 'bg-primary text-primary-foreground',
            )}
          >
            {isRunning ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
            {isRunning ? 'Pausar' : 'Iniciar'}
          </button>
          <button
            onClick={reset}
            className="flex items-center gap-2 rounded-lg border px-4 py-2.5 font-medium text-muted-foreground hover:bg-accent"
          >
            <RotateCcw className="h-4 w-4" />
          </button>
        </div>

        <p className="mt-4 text-xs text-muted-foreground">
          {isBreak ? 'Descansa 10 minutos antes del próximo bloque' : '50 min de estudio concentrado'}
        </p>
      </div>

      <div className="rounded-xl border bg-card p-6">
        <h2 className="mb-4 font-semibold">Calendario de bloques</h2>
        <p className="text-sm text-muted-foreground">
          El calendario de planificación de los 18 meses estará disponible en la próxima versión.
        </p>
      </div>
    </div>
  )
}
