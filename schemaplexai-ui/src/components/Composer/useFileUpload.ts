import { useState, useCallback } from 'react'
import { message } from 'antd'
import { uploadFile, getScanStatus } from '@/api/upload'
import type { ComposerAttachment } from './types'

const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
const ALLOWED_TYPES = [
  'image/',
  'text/',
  'application/pdf',
  'application/json',
  'application/yaml',
  'application/x-yaml',
  'application/vnd.openxmlformats-officedocument',
  'application/msword',
  'application/vnd.ms-excel',
]

function isAllowedType(mime: string): boolean {
  return ALLOWED_TYPES.some((prefix) => mime.startsWith(prefix))
}

interface UseFileUploadReturn {
  attachments: ComposerAttachment[]
  uploading: boolean
  scanHealthy: boolean | null
  checkScanStatus: () => Promise<void>
  addFiles: (files: FileList | null) => Promise<void>
  removeAttachment: (id: string) => void
  handlePaste: (event: React.ClipboardEvent) => Promise<void>
  reset: () => void
}

export function useFileUpload(): UseFileUploadReturn {
  const [attachments, setAttachments] = useState<ComposerAttachment[]>([])
  const [uploading, setUploading] = useState(false)
  const [scanHealthy, setScanHealthy] = useState<boolean | null>(null)

  const checkScanStatus = useCallback(async () => {
    try {
      const status = await getScanStatus()
      setScanHealthy(status.healthy)
    } catch {
      setScanHealthy(false)
    }
  }, [])

  const uploadSingle = useCallback(async (file: File): Promise<ComposerAttachment | null> => {
    if (file.size > MAX_FILE_SIZE) {
      message.warning(`文件过大: ${file.name} (最大 50MB)`)
      return null
    }
    if (!isAllowedType(file.type)) {
      message.warning(`不支持的文件类型: ${file.name}`)
      return null
    }
    try {
      const result = await uploadFile(file)
      return {
        id: result.id,
        name: result.name,
        url: result.url,
        mimeType: result.mimeType,
        size: result.size,
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : '上传失败'
      message.error(`${file.name}: ${msg}`)
      return null
    }
  }, [])

  const addFiles = useCallback(
    async (files: FileList | null) => {
      if (!files || files.length === 0) return
      if (scanHealthy === false) {
        message.error('文件扫描服务不可用，上传已禁用')
        return
      }
      setUploading(true)
      try {
        const results = await Promise.all(Array.from(files).map(uploadSingle))
        const valid = results.filter((r): r is ComposerAttachment => r !== null)
        if (valid.length > 0) {
          setAttachments((prev) => [...prev, ...valid])
        }
      } finally {
        setUploading(false)
      }
    },
    [scanHealthy, uploadSingle]
  )

  const removeAttachment = useCallback((id: string) => {
    setAttachments((prev) => prev.filter((a) => a.id !== id))
  }, [])

  const handlePaste = useCallback(
    async (event: React.ClipboardEvent) => {
      const items = event.clipboardData.items
      const imageFiles: File[] = []
      for (let i = 0; i < items.length; i++) {
        const item = items[i]
        if (item.kind === 'file' && item.type.startsWith('image/')) {
          const file = item.getAsFile()
          if (file) imageFiles.push(file)
        }
      }
      if (imageFiles.length > 0) {
        event.preventDefault()
        await addFiles(imageFiles as unknown as FileList)
      }
    },
    [addFiles]
  )

  const reset = useCallback(() => {
    setAttachments([])
    setUploading(false)
  }, [])

  return {
    attachments,
    uploading,
    scanHealthy,
    checkScanStatus,
    addFiles,
    removeAttachment,
    handlePaste,
    reset,
  }
}
