import request from './request'
import type { Notification, NotificationPageResult } from '@/types/notification'
import type { ApiResponse } from '@/types'

export interface NotificationQuery {
  page?: number
  size?: number
  read?: boolean
}

export function getNotificationPage(params: NotificationQuery) {
  return request.get<ApiResponse<NotificationPageResult>>('/web/notification/page', { params })
}

export function markAsRead(id: string) {
  return request.put<ApiResponse<boolean>>(`/web/notification/${id}/read`)
}

export function markAllAsRead() {
  return request.put<ApiResponse<number>>('/web/notification/read-all')
}
