import { useState, useEffect, useCallback, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Play, Pause, RotateCcw, Timer, Clock } from 'lucide-react'
import { specialtyService } from '@/services/specialty.service'
import { studyService } from '@/services/study.service'
import { cn } from '@/lib/utils'

const POMODORO_MINUTES = 50
const BREAK_MINUTES = 10

export default function WorkspacePage() {
  const [seconds, setSeconds] = useState(POMODORO_MINUTES * 60)
  const [isRunning, setIsRunning] = useState(false)
  const [isBreak, setIsBreak] = useState(false)
  const [specialtyId, setSpecialtyId] = useState<string>('')
  const startedAtRef = useRef<string | null>(null)
  const queryClient = useQueryClient()

  const { data: specialties } = useQuery({ queryKey: ['specialties'], queryFn: specialtyService.list })
  const { data: stats } = useQuery({ queryKey: ['study-stats'], queryFn: studyService.stats })
  const { data: sessions } = useQuery({ queryKey: ['study-sessions'], queryFn: studyService.list })

  const logSession = useMutation({
    mutationFn: studyService.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['study-stats'] })
      queryClient.invalidateQueries({ queryKey: ['study-sessions'] })
    },
  })

  const reset = useCallback(() => {
    setIsRunning(false)
    setSeconds(isBreak ? BREAK_MINUTES * 60 : POMODORO_MINUTES * 60)
  }, [isBreak])

  useEffect(() => {
    if (!isRunning) return
    if (!startedAtRef.current && !isBreak) startedAtRef.current = new Date().toISOString()

    const id = setInterval(() => {
      setSeconds((s) => {
        if (s <= 1) {
          setIsRunning(false)
          // Un bloque de estudio completado → registrar sesión
          if (!isBreak) {
            logSession.mutate({
              specialtyId: specialtyId || undefined,
              durationMinutes: POMODORO_MINUTES,
              sessionType: 'POMODORO',
              startedAt: startedAtRef.current ?? undefined,
              endedAt: new Date().toISOString(),
            })
            startedAtRef.current = null
          }
          setIsBreak((b) => !b)
          return isBreak ? POMODORO_MINUTES * 60 : BREAK_MINUTES * 60
        }
        return s - 1
      })
    }, 1000)
    return () => clearInterval(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isRunning, isBreak, specialtyId])

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

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        {/* Pomodoro */}
        <div className="flex flex-col items-center rounded-xl border bg-card p-10 shadow-sm">
          <div className="mb-2 flex items-center gap-2 text-sm font-medium text-muted-foreground">
            <Timer className="h-4 w-4" />
            {isBreak ? 'Descanso' : 'Estudio'}
          </div>

          <div className="relative my-6 h-48 w-48">
            <svg className="h-full w-full -rotate-90" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="44" fill="none" stroke="hsl(var(--muted))" strokeWidth="8" />
              <circle
                cx="50" cy="50" r="44" fill="none"
                stroke={isBreak ? '#16a34a' : 'hsl(var(--primary))'}
                strokeWidth="8" strokeLinecap="round"
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

          <select
            value={specialtyId}
            onChange={(e) => setSpecialtyId(e.target.value)}
            className="mb-4 rounded-lg border px-3 py-1.5 text-sm"
          >
            <option value="">Sin especialidad</option>
            {specialties?.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>

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

        {/* Panel lateral */}
        <div className="space-y-6">
          <div className="rounded-xl border bg-card p-6 shadow-sm">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <Clock className="h-4 w-4" /> Esta semana
            </div>
            <p className="mt-2 text-3xl font-bold">
              {Math.floor((stats?.minutesThisWeek ?? 0) / 60)}h {(stats?.minutesThisWeek ?? 0) % 60}m
            </p>
            <p className="text-xs text-muted-foreground">tiempo de estudio registrado</p>
          </div>

          <div className="rounded-xl border bg-card p-6 shadow-sm">
            <h2 className="mb-3 font-semibold">Sesiones recientes</h2>
            {(sessions?.length ?? 0) === 0 ? (
              <p className="text-sm text-muted-foreground">
                Aún no registras sesiones. Completa un bloque Pomodoro para empezar.
              </p>
            ) : (
              <ul className="space-y-2">
                {sessions!.slice(0, 6).map((s) => (
                  <li key={s.id} className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">
                      {new Date(s.startedAt).toLocaleDateString('es-PE', { day: '2-digit', month: 'short' })}
                      {s.specialtyName ? ` · ${s.specialtyName}` : ''}
                    </span>
                    <span className="font-medium">{s.durationMinutes} min</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
