import { create } from 'zustand'
import type { Session, User } from '@supabase/supabase-js'
import type { UserProfile } from '@/types'

interface AuthState {
  session: Session | null
  user: User | null
  profile: UserProfile | null
  isLoading: boolean
  setSession: (session: Session | null) => void
  setProfile: (profile: UserProfile | null) => void
  setLoading: (loading: boolean) => void
  isAdmin: () => boolean
}

export const useAuthStore = create<AuthState>((set, get) => ({
  session: null,
  user: null,
  profile: null,
  isLoading: true,
  setSession: (session) => set({ session, user: session?.user ?? null }),
  setProfile: (profile) => set({ profile }),
  setLoading: (isLoading) => set({ isLoading }),
  isAdmin: () => get().profile?.role === 'admin',
}))
