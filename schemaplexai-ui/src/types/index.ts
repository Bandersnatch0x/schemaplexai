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
