import { useEffect } from 'react'
import type { Session } from '@supabase/supabase-js'
import { Routes, Route, Navigate } from 'react-router-dom'
import { supabase } from '@/lib/supabase'
import { useAuthStore } from '@/store/authStore'
import AppLayout from '@/components/layout/AppLayout'
import LoginPage from '@/pages/auth/LoginPage'
import RegisterPage from '@/pages/auth/RegisterPage'
import DashboardPage from '@/pages/dashboard/DashboardPage'
import QBankPage from '@/pages/qbank/QBankPage'
import ProgressPage from '@/pages/progress/ProgressPage'
import FlashcardsPage from '@/pages/flashcards/FlashcardsPage'
import WorkspacePage from '@/pages/workspace/WorkspacePage'
import AdminPage from '@/pages/admin/AdminPage'
import ProtectedRoute from '@/components/shared/ProtectedRoute'
import AdminRoute from '@/components/shared/AdminRoute'

export default function App() {
  const { setSession, setProfile, setLoading } = useAuthStore()

  useEffect(() => {
    async function loadProfile(session: Session | null) {
      if (!session?.user) {
        setProfile(null)
        return
      }
      const { data } = await supabase
        .from('user_profiles')
        .select('*')
        .eq('id', session.user.id)
        .maybeSingle()

      let row = data
      // Si no existe la fila de perfil, la creamos con los datos del registro (auto-reparación).
      if (!row) {
        const meta = (session.user.user_metadata ?? {}) as Record<string, string>
        const { data: created } = await supabase
          .from('user_profiles')
          .upsert({
            id: session.user.id,
            full_name: meta.full_name || session.user.email,
            role: meta.role || 'student',
            university: meta.university || null,
          })
          .select('*')
          .single()
        row = created
      }

      if (!row) {
        setProfile(null)
        return
      }
      setProfile({
        id: row.id,
        fullName: row.full_name ?? '',
        role: row.role,
        university: row.university ?? undefined,
        createdAt: row.created_at,
      })
    }

    supabase.auth.getSession().then(async ({ data }) => {
      setSession(data.session)
      await loadProfile(data.session)
      setLoading(false)
    })

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session)
      void loadProfile(session)
    })

    return () => subscription.unsubscribe()
  }, [setSession, setProfile, setLoading])

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/banco-preguntas" element={<QBankPage />} />
          <Route path="/progreso" element={<ProgressPage />} />
          <Route path="/flashcards" element={<FlashcardsPage />} />
          <Route path="/workspace" element={<WorkspacePage />} />
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminPage />} />
          </Route>
        </Route>
      </Route>
    </Routes>
  )
}
