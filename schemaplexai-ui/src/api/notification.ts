import request from './request'
import type { NotificationPageResult } from '@/types/notification'

export interface NotificationQuery {
  page?: number
  size?: number
  read?: boolean
}

export function getNotificationPage(params: NotificationQuery) {
  return request.get<NotificationPageResult>('/web/notification/page', { params })
}

export function markAsRead(id: string) {
  return request.put<boolean>(`/web/notification/${id}/read`)
}

export function markAllAsRead() {
  return request.put<number>('/web/notification/read-all')
}
