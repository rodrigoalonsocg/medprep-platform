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
import AcademiesPage from '@/pages/academies/AcademiesPage'
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
      const { data, error } = await supabase
        .from('user_profiles')
        .select('*')
        .eq('id', session.user.id)
        .single()

      if (error || !data) {
        setProfile(null)
        return
      }
      setProfile({
        id: data.id,
        fullName: data.full_name ?? '',
        role: data.role,
        university: data.university ?? undefined,
        createdAt: data.created_at,
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
          <Route path="/academias" element={<AcademiesPage />} />
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminPage />} />
          </Route>
        </Route>
      </Route>
    </Routes>
  )
}
