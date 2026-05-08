import request from './request'
import type { SfTask } from '@/types'

export interface TaskQuery {
  page?: number
  pageSize?: number
  status?: string
  priority?: string
  keyword?: string
}

export interface CreateTaskPayload {
  title: string
  description?: string
  skillTags?: string[]
  priority?: 'P0' | 'P1' | 'P2' | 'P3'
  assignmentType?: 'MANUAL' | 'AUTO' | 'MIXED'
  specId?: string
}

export function getTaskList(params?: TaskQuery) {
  return request.get<{ list: SfTask[]; total: number }>('/task/tasks', { params })
}

export function getTaskDetail(id: string) {
  return request.get<SfTask>(`/task/tasks/${id}`)
}

export function createTask(data: CreateTaskPayload) {
  return request.post<SfTask>('/task/tasks', data)
}

export function updateTask(id: string, data: Partial<CreateTaskPayload>) {
  return request.put<SfTask>(`/task/tasks/${id}`, data)
}

export function updateTaskStatus(id: string, status: string, blockerReason?: string) {
  return request.put<void>(`/task/tasks/${id}/status`, { status, blockerReason })
}

export function deleteTask(id: string) {
  return request.delete<void>(`/task/tasks/${id}`)
}

export interface TaskComment {
  id: string
  taskId: string
  content: string
  authorId: string
  authorName?: string
  createdAt: string
}

export function getTaskComments(taskId: string) {
  return request.get<TaskComment[]>(`/task/tasks/${taskId}/comments`)
}

export function addTaskComment(taskId: string, content: string) {
  return request.post<TaskComment>(`/task/tasks/${taskId}/comments`, { content })
}

export interface JobRecord {
  id: string
  name: string
  queue: string
  status: string
  retryCount: number
  maxRetries: number
  createdAt: string
  updatedAt: string
}

export function getJobList(params?: { page?: number; pageSize?: number; queue?: string }) {
  return request.get<{ list: JobRecord[]; total: number }>('/task/jobs', { params })
}

export function retryJob(id: string) {
  return request.post<void>(`/task/jobs/${id}/retry`)
}

export function cancelJob(id: string) {
  return request.post<void>(`/task/jobs/${id}/cancel`)
}
