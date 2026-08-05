import api from '@/lib/axios'
import type { ApiResponse, AttemptResponse, Difficulty, Page, Question } from '@/types'

export interface QuestionFilters {
  specialtyId?: string
  subspecialtyId?: string
  difficulty?: string
  page?: number
  size?: number
}

export interface CreateQuestionPayload {
  specialtyId: string
  subspecialtyId?: string
  stem: string
  optionA: string
  optionB: string
  optionC?: string
  optionD?: string
  optionE?: string
  correctOption: string
  explanation?: string
  difficulty: Difficulty
  source?: string
  year?: number
}

export const questionService = {
  list: async (filters: QuestionFilters = {}) => {
    const { data } = await api.get<ApiResponse<Page<Question>>>('/questions', { params: filters })
    return data.data
  },

  getErrorQuestions: async (page = 0) => {
    const { data } = await api.get<ApiResponse<Page<Question>>>('/questions/errors', { params: { page } })
    return data.data
  },

  generateSimulacro: async (specialtyId?: string, limit = 10) => {
    const { data } = await api.get<ApiResponse<Question[]>>('/questions/simulacro', {
      params: { specialtyId, limit },
    })
    return data.data
  },

  submitAttempt: async (questionId: string, selectedOption: string, timeSpentSeconds?: number) => {
    const { data } = await api.post<ApiResponse<AttemptResponse>>('/questions/attempt', {
      questionId,
      selectedOption,
      timeSpentSeconds,
    })
    return data.data
  },

  create: async (payload: CreateQuestionPayload) => {
    const { data } = await api.post<ApiResponse<Question>>('/questions', payload)
    return data.data
  },
}
