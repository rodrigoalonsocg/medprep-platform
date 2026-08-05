import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  BookOpen,
  TrendingUp,
  Layers,
  Timer,
  GraduationCap,
  ShieldCheck,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/banco-preguntas', label: 'Banco de Preguntas', icon: BookOpen },
  { to: '/progreso', label: 'Semáforo Clínico', icon: TrendingUp },
  { to: '/flashcards', label: 'Flashcards', icon: Layers },
  { to: '/workspace', label: 'Workspace', icon: Timer },
  { to: '/academias', label: 'Academias', icon: GraduationCap },
]

export default function Sidebar() {
  const { isAdmin, profile } = useAuthStore()

  return (
    <aside className="flex w-64 flex-col border-r bg-card">
      <div className="flex h-16 items-center border-b px-6">
        <span className="text-xl font-bold text-primary">MedPrep</span>
      </div>

      <nav className="flex-1 space-y-1 p-4">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
              )
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}

        {isAdmin() && (
          <NavLink
            to="/admin"
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
              )
            }
          >
            <ShieldCheck className="h-4 w-4" />
            Administración
          </NavLink>
        )}
      </nav>

      <div className="border-t p-4">
        <p className="truncate text-xs text-muted-foreground">{profile?.fullName}</p>
        <p className="text-xs text-muted-foreground capitalize">{profile?.role}</p>
      </div>
    </aside>
  )
}
