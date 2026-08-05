import api from '@/lib/axios'
import type { ApiResponse, Specialty } from '@/types'

export const specialtyService = {
  list: async () => {
    const { data } = await api.get<ApiResponse<Specialty[]>>('/specialties')
    return data.data
  },
}
