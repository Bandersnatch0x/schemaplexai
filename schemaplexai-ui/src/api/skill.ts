import request from './request'

export interface Skill {
  id: string
  name: string
  code: string
  description?: string
  content?: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface SkillPayload {
  name: string
  code: string
  description?: string
  content?: string
  status?: number
}

export function getSkillList() {
  return request.get<Skill[]>('/integration/skills')
}

export function getSkillDetail(id: string) {
  return request.get<Skill>(`/integration/skills/${id}`)
}

export function createSkill(data: SkillPayload) {
  return request.post<string>('/integration/skills', data)
}

export function updateSkill(id: string, data: SkillPayload) {
  return request.put<boolean>(`/integration/skills/${id}`, data)
}

export function deleteSkill(id: string) {
  return request.delete<boolean>(`/integration/skills/${id}`)
}
