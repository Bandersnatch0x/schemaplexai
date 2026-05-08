export interface UserInfo {
  id: string
  username: string
  nickname?: string
  avatar?: string
  email?: string
  roles: string[]
}

export interface Tenant {
  id: string
  name: string
  code: string
}

export interface Agent {
  id: string
  name: string
  description?: string
  type: string
  status: 'active' | 'inactive' | 'draft'
  modelConfig?: ModelConfig
  tools?: string[]
  createdAt: string
  updatedAt: string
}

export interface ModelConfig {
  model: string
  temperature: number
  maxTokens: number
  topP: number
}

export interface ExecutionRecord {
  id: string
  agentId: string
  agentName: string
  status: 'running' | 'success' | 'failed'
  prompt: string
  result?: string
  tokenUsed: number
  duration: number
  createdAt: string
}

export interface SseEvent {
  id: string
  type: 'thinking' | 'tool_calling' | 'observation' | 'completed' | 'error'
  content: string
  timestamp: number
  metadata?: Record<string, unknown>
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export type TaskStatus =
  | 'BACKLOG'
  | 'QUEUED'
  | 'IN_PROGRESS'
  | 'AWAITING_REVIEW'
  | 'REVISING'
  | 'BLOCKED'
  | 'DONE'

export type TaskPriority = 'P0' | 'P1' | 'P2' | 'P3'

export type AssignmentType = 'MANUAL' | 'AUTO' | 'MIXED'

export interface SfTask {
  id: string
  tenantId: string
  title: string
  description?: string
  skillTags?: string[]
  priority: TaskPriority
  status: TaskStatus
  assignedRuntimeId?: string
  assignedAgentId?: string
  assignmentType: AssignmentType
  specId?: string
  blockerReason?: string
  createdAt: string
  updatedAt: string
}
