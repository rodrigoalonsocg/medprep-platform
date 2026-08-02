import { useEffect } from 'react'
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

export default function App() {
  const { setSession, setLoading } = useAuthStore()

  useEffect(() => {
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session)
      setLoading(false)
    })

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session)
    })

    return () => subscription.unsubscribe()
  }, [setSession, setLoading])

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
          <Route path="/admin" element={<AdminPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
