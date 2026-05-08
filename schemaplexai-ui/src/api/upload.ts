import request from './request'

export interface UploadResult {
  id: string
  name: string
  url: string
  mimeType: string
  size: number
}

export interface ScanStatus {
  healthy: boolean
  message?: string
}

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<UploadResult>('/context/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getScanStatus() {
  return request.get<ScanStatus>('/context/files/scan-status')
}
