import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Play, Pause, RotateCcw } from 'lucide-react'
import { studyService } from '@/services/study.service'
import { cn } from '@/lib/utils'

type Mode = 'pomodoro' | 'short' | 'long'
const LABELS: Record<Mode, string> = { pomodoro: 'Pomodoro', short: 'Descanso corto', long: 'Descanso largo' }

interface Settings {
  durations: Record<Mode, number> // minutos
  autoStartBreaks: boolean
  autoStartPomodoros: boolean
  muteNotifications: boolean
}

const DEFAULTS: Settings = {
  durations: { pomodoro: 25, short: 5, long: 15 },
  autoStartBreaks: false,
  autoStartPomodoros: false,
  muteNotifications: false,
}

function loadSettings(): Settings {
  try {
    return { ...DEFAULTS, ...JSON.parse(localStorage.getItem('pomodoro') || '{}') }
  } catch {
    return DEFAULTS
  }
}

export default function WorkspacePage() {
  const [settings, setSettings] = useState<Settings>(loadSettings)
  const [mode, setMode] = useState<Mode>('pomodoro')
  const [secondsLeft, setSecondsLeft] = useState(settings.durations.pomodoro * 60)
  const [running, setRunning] = useState(false)
  const audioRef = useRef<HTMLAudioElement>(null)
  const pomodoroCount = useRef(0)

  const { data: stats, refetch: refetchStats } = useQuery({
    queryKey: ['study-stats'],
    queryFn: studyService.stats,
  })

  // Estrellas fijas (estilo pixel) para el fondo nocturno
  const stars = useMemo(
    () => Array.from({ length: 45 }, () => ({
      top: Math.random() * 90,
      left: Math.random() * 98,
      size: Math.random() < 0.3 ? 3 : 2,
    })),
    [],
  )

  useEffect(() => {
    localStorage.setItem('pomodoro', JSON.stringify(settings))
  }, [settings])

  // Si no está corriendo, el tiempo sigue a la duración del modo actual
  useEffect(() => {
    if (!running) setSecondsLeft(settings.durations[mode] * 60)
  }, [mode, settings.durations, running])

  // Tick
  useEffect(() => {
    if (!running) return
    const id = setInterval(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(id)
  }, [running])

  // Fin del temporizador
  useEffect(() => {
    if (secondsLeft !== 0 || !running) return
    audioRef.current?.play().catch(() => {})
    if (!settings.muteNotifications && 'Notification' in window && Notification.permission === 'granted') {
      new Notification('MedPrep', { body: mode === 'pomodoro' ? '¡Pomodoro terminado! Toma un descanso.' : '¡Descanso terminado! A estudiar.' })
    }
    if (mode === 'pomodoro') {
      studyService.create({ durationMinutes: settings.durations.pomodoro, sessionType: 'POMODORO' })
        .then(() => refetchStats()).catch(() => {})
      pomodoroCount.current += 1
      switchTo(pomodoroCount.current % 4 === 0 ? 'long' : 'short', settings.autoStartBreaks)
    } else {
      switchTo('pomodoro', settings.autoStartPomodoros)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [secondsLeft, running])

  const switchTo = (m: Mode, autostart: boolean) => {
    setMode(m)
    setSecondsLeft(settings.durations[m] * 60)
    setRunning(autostart)
  }

  const start = () => {
    if (!settings.muteNotifications && 'Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
    setRunning(true)
  }

  const reset = () => {
    setRunning(false)
    setSecondsLeft(settings.durations[mode] * 60)
  }

  // Modo noche en TODA la página (sidebar incluido) mientras corre
  useEffect(() => {
    document.documentElement.classList.toggle('night', running)
    return () => document.documentElement.classList.remove('night')
  }, [running])

  const mm = String(Math.floor(secondsLeft / 60)).padStart(2, '0')
  const ss = String(secondsLeft % 60).padStart(2, '0')

  const setDuration = (m: Mode, v: number) =>
    setSettings((s) => ({ ...s, durations: { ...s.durations, [m]: Math.max(1, Math.min(180, v || 1)) } }))

  return (
    <div className="mx-auto max-w-xl space-y-6">
      {running && (
        <div className="pointer-events-none fixed inset-0 z-40">
          {stars.map((st, i) => (
            <span key={i} className="absolute rounded-[1px] bg-white/90"
              style={{ top: `${st.top}%`, left: `${st.left}%`, width: st.size, height: st.size }} />
          ))}
          <Dino />
          <div className="absolute bottom-0 right-0 h-8 bg-slate-800/70" style={{ left: SIDEBAR }} />
        </div>
      )}
      <div>
        <h1 className="text-2xl font-bold">Pomodoro</h1>
        <p className="text-muted-foreground">Enfócate con bloques de tiempo. Esta semana: {stats?.minutesThisWeek ?? 0} min.</p>
      </div>

      {/* Selector de modo */}
      <div className="flex justify-center gap-2">
        {(Object.keys(LABELS) as Mode[]).map((m) => (
          <button
            key={m}
            onClick={() => { setMode(m); setRunning(false) }}
            className={cn(
              'rounded-lg px-3 py-1.5 text-sm font-medium transition-colors',
              mode === m ? 'bg-primary text-primary-foreground' : 'bg-secondary text-secondary-foreground hover:bg-accent',
            )}
          >
            {LABELS[m]}
          </button>
        ))}
      </div>

      {/* Temporizador (fondo nocturno al correr) */}
      <div className="relative z-30 overflow-hidden rounded-2xl border bg-card p-10 text-center">
        <div className="font-mono text-7xl font-bold tabular-nums">{mm}:{ss}</div>
        <div className="mt-6 flex justify-center gap-3">
          <button onClick={running ? () => setRunning(false) : start}
            className="flex items-center gap-2 rounded-lg bg-primary px-6 py-2.5 font-medium text-primary-foreground">
            {running ? <><Pause className="h-4 w-4" /> Pausar</> : <><Play className="h-4 w-4" /> Iniciar</>}
          </button>
          <button onClick={reset}
            className="flex items-center gap-2 rounded-lg border px-4 py-2.5 text-sm">
            <RotateCcw className="h-4 w-4" /> Reiniciar
          </button>
        </div>
      </div>

      {/* Configuración */}
      <div className="space-y-4 rounded-xl border bg-card p-6 shadow-sm">
        <h3 className="font-semibold">Configuración</h3>
        <div className="grid grid-cols-3 gap-3">
          {(Object.keys(LABELS) as Mode[]).map((m) => (
            <label key={m} className="text-sm">
              <span className="mb-1 block text-muted-foreground">{LABELS[m]} (min)</span>
              <input type="number" min={1} max={180} value={settings.durations[m]}
                onChange={(e) => setDuration(m, Number(e.target.value))}
                className="w-full rounded-lg border px-3 py-2 text-sm" />
            </label>
          ))}
        </div>
        <Switch label="Auto-iniciar descansos" checked={settings.autoStartBreaks}
          onChange={(v) => setSettings((s) => ({ ...s, autoStartBreaks: v }))} />
        <Switch label="Auto-iniciar pomodoros" checked={settings.autoStartPomodoros}
          onChange={(v) => setSettings((s) => ({ ...s, autoStartPomodoros: v }))} />
        <Switch label="Silenciar notificaciones de escritorio" checked={settings.muteNotifications}
          onChange={(v) => setSettings((s) => ({ ...s, muteNotifications: v }))} />
      </div>

      <audio ref={audioRef} src="/bell.mp3" preload="auto" />
    </div>
  )
}

const DINO = [
  '.....#####',
  '.....#.###',
  '.....#####',
  '.....####.',
  '......#...',
  '#.....###.',
  '##...####.',
  '###.#####.',
  '#########.',
  '.########.',
  '..#######.',
  '...####...',
  '...#..#...',
  '..##..##..',
]
const PHRASES = ['¡Tú puedes!', 'Sigue así', 'Enfócate', 'Vas muy bien', 'Respira y sigue']
const SIDEBAR = 272 // no invadir la barra izquierda (w-64)

function Dino() {
  const [x, setX] = useState(SIDEBAR)
  const [greet, setGreet] = useState<string | null>(null)
  const greetRef = useRef<string | null>(null)

  useEffect(() => {
    let raf = 0
    let last = performance.now()
    const speed = 42 // px/s
    const loop = (now: number) => {
      const dt = Math.min(0.05, (now - last) / 1000)
      last = now
      if (!greetRef.current) {
        setX((p) => (p > window.innerWidth + 20 ? SIDEBAR : p + speed * dt))
        if (Math.random() < 0.0006) { // frases poco frecuentes (no distraer)
          const phrase = PHRASES[Math.floor(Math.random() * PHRASES.length)]
          greetRef.current = phrase
          setGreet(phrase)
          window.setTimeout(() => { greetRef.current = null; setGreet(null) }, 2200)
        }
      }
      raf = requestAnimationFrame(loop)
    }
    raf = requestAnimationFrame(loop)
    return () => cancelAnimationFrame(raf)
  }, [])

  return (
    <div className="absolute bottom-6" style={{ left: x }}>
      {greet && (
        <div className="absolute -top-9 left-2 flex flex-col items-center opacity-30">
          <div className="whitespace-nowrap rounded-md border border-slate-900 bg-white px-2 py-0.5 text-[10px] font-semibold text-slate-900">{greet}</div>
          <div className="-mt-1 h-2 w-2 rotate-45 border-b border-r border-slate-900 bg-white" />
        </div>
      )}
      <svg width={26} height={37} viewBox="0 0 10 14" shapeRendering="crispEdges" className={`opacity-30 ${greet ? '' : 'dino-walk'}`}>
        {DINO.flatMap((row, r) =>
          row.split('').map((c, col) => (c === '#'
            ? <rect key={`${r}-${col}`} x={col} y={r} width={1} height={1} fill="#e5e7eb" />
            : null)),
        )}
      </svg>
    </div>
  )
}

function Switch({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button onClick={() => onChange(!checked)} className="flex w-full items-center justify-between text-sm">
      <span>{label}</span>
      <span className={cn('relative h-6 w-11 rounded-full transition-colors', checked ? 'bg-primary' : 'bg-muted')}>
        <span className={cn('absolute top-0.5 h-5 w-5 rounded-full bg-white transition-transform', checked ? 'left-0.5 translate-x-5' : 'left-0.5')} />
      </span>
    </button>
  )
}
