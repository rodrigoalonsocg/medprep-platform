import api from '@/lib/axios'
import type { ApiResponse, SessionType, StudySessionDTO } from '@/types'

export interface CreateStudySessionPayload {
  specialtyId?: string
  durationMinutes: number
  sessionType: SessionType
  startedAt?: string
  endedAt?: string
}

export const studyService = {
  create: async (payload: CreateStudySessionPayload) => {
    const { data } = await api.post<ApiResponse<StudySessionDTO>>('/study-sessions', payload)
    return data.data
  },

  list: async () => {
    const { data } = await api.get<ApiResponse<StudySessionDTO[]>>('/study-sessions')
    return data.data
  },

  stats: async () => {
    const { data } = await api.get<ApiResponse<{ minutesThisWeek: number }>>('/study-sessions/stats')
    return data.data
  },
}
