import request from './request'

export interface ContextItem {
  id: string
  name: string
  type: string
  workspaceId?: string
  content?: string
  metadata?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export function getContextList(params?: { page?: number; pageSize?: number; keyword?: string; type?: string }) {
  return request.get<{ list: ContextItem[]; total: number }>('/context/contexts/page', { params })
}

export function getContextDetail(id: string) {
  return request.get<ContextItem>(`/context/contexts/${id}`)
}

export function createContext(data: Omit<ContextItem, 'id' | 'createdAt' | 'updatedAt'>) {
  return request.post<ContextItem>('/context/contexts', data)
}

export function updateContext(id: string, data: Partial<ContextItem>) {
  return request.put<ContextItem>(`/context/contexts/${id}`, data)
}

export function deleteContext(id: string) {
  return request.delete<void>(`/context/contexts/${id}`)
}

export function searchKnowledge(query: string, topK = 5) {
  return request.post<{ results: { id: string; score: number; content: string }[] }>('/context/rag/search', {
    query,
    topK,
  })
}
