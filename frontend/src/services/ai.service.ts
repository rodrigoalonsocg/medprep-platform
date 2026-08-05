import api from '@/lib/axios'
import type { ApiResponse, FlashcardDTO, PatternDTO } from '@/types'

export const aiService = {
  analyzePattern: async (questionId: string) => {
    const { data } = await api.post<ApiResponse<PatternDTO>>(`/ai/analyze/${questionId}`)
    return data.data
  },

  generateFlashcards: async (questionId: string) => {
    const { data } = await api.post<ApiResponse<FlashcardDTO[]>>(`/ai/flashcards/${questionId}`)
    return data.data
  },

  getFlashcards: async () => {
    const { data } = await api.get<ApiResponse<FlashcardDTO[]>>('/ai/flashcards')
    return data.data
  },

  // Descarga el .txt de Anki (marca las flashcards como exportadas en el backend)
  exportToAnki: async () => {
    const { data } = await api.get('/ai/flashcards/export', { responseType: 'blob' })
    return data as Blob
  },
}
