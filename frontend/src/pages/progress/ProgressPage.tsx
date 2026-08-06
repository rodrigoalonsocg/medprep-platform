import { useQuery } from '@tanstack/react-query'
import { progressService } from '@/services/progress.service'
import { formatPercent, trafficLightBg } from '@/lib/utils'
import { RadialBarChart, RadialBar, ResponsiveContainer, PolarAngleAxis } from 'recharts'
import { Activity } from 'lucide-react'

export default function ProgressPage() {
  const { data: progress = [], isLoading } = useQuery({
    queryKey: ['progress'],
    queryFn: progressService.getAll,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <Activity className="h-6 w-6" />
        </div>
        <div>
          <h1 className="text-2xl font-bold">Mi Progreso</h1>
          <p className="text-muted-foreground">Tu rendimiento por especialidad en tiempo real</p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        {isLoading
          ? [...Array(6)].map((_, i) => (
              <div key={i} className="h-48 animate-pulse rounded-xl bg-muted" />
            ))
          : progress.map((p) => (
              <SpecialtyCard key={p.specialtyId} {...p} />
            ))}
      </div>

      {progress.length === 0 && !isLoading && (
        <div className="rounded-lg border border-dashed p-10 text-center text-muted-foreground">
          Comienza a responder preguntas para ver tu progreso aquí.
        </div>
      )}
    </div>
  )
}

function SpecialtyCard({ specialtyName, accuracyPercentage, trafficLight, totalAttempts, correctAttempts, sprintRequired }: {
  specialtyName: string
  accuracyPercentage: number
  trafficLight: 'VERDE' | 'AMARILLO' | 'ROJO'
  totalAttempts: number
  correctAttempts: number
  sprintRequired: boolean
}) {
  const color = { VERDE: '#16a34a', AMARILLO: '#ca8a04', ROJO: '#dc2626' }[trafficLight]
  const chartData = [{ value: accuracyPercentage, fill: color }]

  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm">
      <div className="flex items-start justify-between">
        <h3 className="font-semibold">{specialtyName}</h3>
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${trafficLightBg(trafficLight)}`}>
          {trafficLight}
        </span>
      </div>

      <div className="my-4 flex h-28 items-center justify-center">
        <ResponsiveContainer width={110} height={110}>
          <RadialBarChart innerRadius={30} outerRadius={50} data={chartData} startAngle={90} endAngle={-270}>
            <PolarAngleAxis type="number" domain={[0, 100]} angleAxisId={0} tick={false} />
            <RadialBar dataKey="value" cornerRadius={6} background={{ fill: 'rgba(148,163,184,0.25)' }} />
          </RadialBarChart>
        </ResponsiveContainer>
        <div className="absolute text-center">
          <p className="text-xl font-bold" style={{ color }}>{formatPercent(accuracyPercentage)}</p>
        </div>
      </div>

      <div className="flex justify-between text-sm text-muted-foreground">
        <span>{correctAttempts} correctas</span>
        <span>{totalAttempts} total</span>
      </div>

      {sprintRequired && (
        <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-xs font-medium text-red-700 dark:bg-red-950/50 dark:text-red-300">
          ⚠️ Sprint de recuperación recomendado
        </div>
      )}
    </div>
  )
}
