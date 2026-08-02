import api from '@/lib/axios'
import type { ApiResponse, ProgressResponse } from '@/types'

export const progressService = {
  getAll: async () => {
    const { data } = await api.get<ApiResponse<ProgressResponse[]>>('/progress')
    return data.data
  },

  getAlerts: async () => {
    const { data } = await api.get<ApiResponse<ProgressResponse[]>>('/progress/alerts')
    return data.data
  },
}
