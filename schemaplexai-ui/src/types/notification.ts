export type NotificationType = 'SYSTEM' | 'TASK' | 'WORKFLOW'

export interface Notification {
  id: string
  title: string
  content: string
  type: NotificationType
  read: boolean
  createdAt: string
}

export interface NotificationPageResult {
  records: Notification[]
  total: number
  size: number
  current: number
  pages: number
}
