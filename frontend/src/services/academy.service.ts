import api from '@/lib/axios'
import type { AcademyDTO, ApiResponse, DocumentDTO } from '@/types'

export const academyService = {
  list: async () => {
    const { data } = await api.get<ApiResponse<AcademyDTO[]>>('/academies')
    return data.data
  },

  create: async (payload: { name: string; description?: string }) => {
    const { data } = await api.post<ApiResponse<AcademyDTO>>('/academies', payload)
    return data.data
  },

  listDocuments: async (academyId: string) => {
    const { data } = await api.get<ApiResponse<DocumentDTO[]>>(`/academies/${academyId}/documents`)
    return data.data
  },

  uploadDocument: async (academyId: string, file: File, isPublic = false) => {
    const form = new FormData()
    form.append('file', file)
    form.append('isPublic', String(isPublic))
    const { data } = await api.post<ApiResponse<DocumentDTO>>(
      `/academies/${academyId}/documents`,
      form,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
    return data.data
  },

  getDownloadUrl: async (documentId: string) => {
    const { data } = await api.get<ApiResponse<{ url: string }>>(
      `/academies/documents/${documentId}/download`,
    )
    return data.data.url
  },
}
