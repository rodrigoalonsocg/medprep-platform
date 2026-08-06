import { useQuery } from '@tanstack/react-query'
import { progressService } from '@/services/progress.service'
import { trafficLightBg, formatPercent } from '@/lib/utils'
import { AlertTriangle, CheckCircle2, TrendingUp, BookOpen } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'

export default function DashboardPage() {
  const { profile } = useAuthStore()

  const { data: progress = [], isLoading } = useQuery({
    queryKey: ['progress'],
    queryFn: progressService.getAll,
  })

  const { data: alerts = [] } = useQuery({
    queryKey: ['progress-alerts'],
    queryFn: progressService.getAlerts,
  })

  const totalAttempts = progress.reduce((sum, p) => sum + p.totalAttempts, 0)
  const avgAccuracy = progress.length > 0
    ? progress.reduce((sum, p) => sum + p.accuracyPercentage, 0) / progress.length
    : 0

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Hola, {profile?.fullName?.split(' ')[0]} 👋</h1>
        <p className="text-muted-foreground">Resumen de tu preparación para el internado</p>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard
          icon={<BookOpen className="h-5 w-5 text-blue-600" />}
          label="Preguntas respondidas"
          value={totalAttempts.toString()}
          bg="bg-blue-50"
        />
        <StatCard
          icon={<TrendingUp className="h-5 w-5 text-green-600" />}
          label="Precisión global"
          value={formatPercent(avgAccuracy)}
          bg="bg-green-50"
        />
        <StatCard
          icon={<AlertTriangle className="h-5 w-5 text-red-600" />}
          label="Especialidades en rojo"
          value={alerts.length.toString()}
          bg="bg-red-50"
        />
      </div>

      {/* Alertas de sprint */}
      {alerts.length > 0 && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4">
          <h2 className="mb-3 flex items-center gap-2 font-semibold text-red-700">
            <AlertTriangle className="h-4 w-4" />
            Sprint de Recuperación requerido
          </h2>
          <div className="space-y-2">
            {alerts.map((alert) => (
              <div key={alert.specialtyId} className="flex items-center justify-between rounded bg-white p-3 shadow-sm">
                <span className="font-medium">{alert.specialtyName}</span>
                <span className="text-sm text-red-600">{formatPercent(alert.accuracyPercentage)} de aciertos</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Progreso por especialidad */}
      <div>
        <h2 className="mb-4 text-lg font-semibold">Mi Progreso</h2>
        {isLoading ? (
          <div className="space-y-2">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-16 animate-pulse rounded-lg bg-muted" />
            ))}
          </div>
        ) : progress.length === 0 ? (
          <EmptyState />
        ) : (
          <div className="space-y-3">
            {progress.map((p) => (
              <div key={p.specialtyId} className="flex items-center gap-4 rounded-lg border bg-card p-4">
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="font-medium">{p.specialtyName}</span>
                    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${trafficLightBg(p.trafficLight)}`}>
                      {p.trafficLight}
                    </span>
                  </div>
                  <div className="mt-2 h-2 overflow-hidden rounded-full bg-muted">
                    <div
                      className={`h-full rounded-full transition-all ${
                        p.trafficLight === 'VERDE'
                          ? 'bg-green-500'
                          : p.trafficLight === 'AMARILLO'
                          ? 'bg-yellow-500'
                          : 'bg-red-500'
                      }`}
                      style={{ width: `${Math.min(p.accuracyPercentage, 100)}%` }}
                    />
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-lg font-bold">{formatPercent(p.accuracyPercentage)}</p>
                  <p className="text-xs text-muted-foreground">{p.totalAttempts} intentos</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, bg }: {
  icon: React.ReactNode
  label: string
  value: string
  bg: string
}) {
  return (
    <div className="flex items-center gap-4 rounded-lg border bg-card p-5">
      <div className={`flex h-10 w-10 items-center justify-center rounded-full ${bg}`}>
        {icon}
      </div>
      <div>
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="text-2xl font-bold">{value}</p>
      </div>
    </div>
  )
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed p-10 text-center">
      <CheckCircle2 className="h-10 w-10 text-muted-foreground" />
      <p className="font-medium">Aún no tienes intentos registrados</p>
      <p className="text-sm text-muted-foreground">
        Ve al Banco de Preguntas y comienza a practicar para ver tu progreso aquí.
      </p>
    </div>
  )
}
