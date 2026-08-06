import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  BookOpen,
  Activity,
  Layers,
  Timer,
  ShieldCheck,
  Stethoscope,
  X,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/progreso', label: 'Mi Progreso', icon: Activity },
  { to: '/banco-preguntas', label: 'Banco de Preguntas', icon: BookOpen },
  { to: '/flashcards', label: 'Flashcards', icon: Layers },
  { to: '/workspace', label: 'Pomodoro', icon: Timer },
]

const linkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
    isActive
      ? 'bg-primary text-primary-foreground shadow-sm'
      : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
  )

export default function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { isAdmin, profile } = useAuthStore()
  const initial = (profile?.fullName ?? '?').charAt(0).toUpperCase()

  return (
    <>
      {/* Backdrop (solo móvil, cuando el drawer está abierto) */}
      {open && (
        <div className="fixed inset-0 z-40 bg-black/50 lg:hidden" onClick={onClose} aria-hidden />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r bg-card transition-transform duration-200',
          'lg:static lg:z-auto lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        {/* Marca */}
        <div className="flex h-16 items-center gap-3 bg-primary px-5 text-primary-foreground">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/15 ring-1 ring-white/25">
            <Stethoscope className="h-5 w-5" />
          </div>
          <p className="text-xl font-bold tracking-tight">MedPrep</p>
          {/* Cerrar (solo móvil) */}
          <button onClick={onClose} className="ml-auto rounded-lg p-1 hover:bg-white/15 lg:hidden" aria-label="Cerrar menú">
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={linkClass} onClick={onClose}>
              <Icon className="h-[18px] w-[18px]" />
              {label}
            </NavLink>
          ))}

          {isAdmin() && (
            <>
              <div className="my-2 border-t" />
              <NavLink to="/admin" className={linkClass} onClick={onClose}>
                <ShieldCheck className="h-[18px] w-[18px]" />
                Administración
              </NavLink>
            </>
          )}
        </nav>

        <div className="border-t p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary text-sm font-semibold text-secondary-foreground">
              {initial}
            </div>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{profile?.fullName ?? 'Cargando...'}</p>
              <p className="text-xs capitalize text-muted-foreground">{profile?.role}</p>
            </div>
          </div>
        </div>
      </aside>
    </>
  )
}
