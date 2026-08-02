export type UserRole = 'admin' | 'student'

export interface UserProfile {
  id: string
  fullName: string
  role: UserRole
  university?: string
  createdAt: string
}

export type Difficulty = 'BAJA' | 'MEDIA' | 'ALTA'
export type TrafficLight = 'VERDE' | 'AMARILLO' | 'ROJO'
export type AttemptStatus = 'CORRECTA' | 'INCORRECTA' | 'DUDOSA'

export interface Specialty {
  id: string
  name: string
  code: string
  subspecialties?: Subspecialty[]
}

export interface Subspecialty {
  id: string
  specialtyId: string
  name: string
  code: string
}

export interface Question {
  id: string
  specialtyId: string
  specialtyName: string
  subspecialtyId?: string
  subspecialtyName?: string
  stem: string
  optionA: string
  optionB: string
  optionC?: string
  optionD?: string
  optionE?: string
  explanation?: string
  difficulty: Difficulty
  source?: string
  year?: number
  keywords?: string[]
  createdAt: string
}

export interface AttemptResponse {
  id: string
  questionId: string
  selectedOption: string
  correctOption: string
  correct: boolean
  status: AttemptStatus
  explanation?: string
  attemptedAt: string
}

export interface ProgressResponse {
  specialtyId: string
  specialtyName: string
  totalAttempts: number
  correctAttempts: number
  accuracyPercentage: number
  trafficLight: TrafficLight
  lastUpdated: string
  sprintRequired: boolean
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  errorCode?: string
  timestamp: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface Flashcard {
  id: string
  questionId?: string
  front: string
  back: string
  specialtyId?: string
  isExported: boolean
  createdAt: string
}
