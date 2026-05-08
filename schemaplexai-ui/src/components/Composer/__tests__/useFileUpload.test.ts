import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useFileUpload } from '../useFileUpload'
import * as uploadApi from '@/api/upload'

vi.mock('@/api/upload', async () => {
  const actual = await vi.importActual<typeof import('@/api/upload')>('@/api/upload')
  return {
    ...actual,
    uploadFile: vi.fn(),
    getScanStatus: vi.fn(),
  }
})

describe('useFileUpload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('initial state has no attachments', () => {
    const { result } = renderHook(() => useFileUpload())
    expect(result.current.attachments).toEqual([])
    expect(result.current.uploading).toBe(false)
  })

  it('adds file after successful upload', async () => {
    vi.mocked(uploadApi.uploadFile).mockResolvedValue({
      id: '1',
      name: 'test.txt',
      url: 'http://minio/test.txt',
      mimeType: 'text/plain',
      size: 12,
    })

    const { result } = renderHook(() => useFileUpload())
    const file = new File(['hello world'], 'test.txt', { type: 'text/plain' })
    const dt = new DataTransfer()
    dt.items.add(file)

    await act(async () => {
      await result.current.addFiles(dt.files)
    })

    expect(result.current.attachments).toHaveLength(1)
    expect(result.current.attachments[0].name).toBe('test.txt')
  })

  it('removes attachment by id', async () => {
    vi.mocked(uploadApi.uploadFile).mockResolvedValue({
      id: '1',
      name: 'test.txt',
      url: 'http://minio/test.txt',
      mimeType: 'text/plain',
      size: 12,
    })

    const { result } = renderHook(() => useFileUpload())
    const file = new File(['hello'], 'test.txt', { type: 'text/plain' })
    const dt = new DataTransfer()
    dt.items.add(file)

    await act(async () => {
      await result.current.addFiles(dt.files)
    })

    act(() => result.current.removeAttachment('1'))
    expect(result.current.attachments).toHaveLength(0)
  })

  it('blocks upload when scan is unhealthy', async () => {
    vi.mocked(uploadApi.getScanStatus).mockResolvedValue({ healthy: false })
    const { result } = renderHook(() => useFileUpload())

    await act(async () => {
      await result.current.checkScanStatus()
    })

    const file = new File(['hello'], 'test.txt', { type: 'text/plain' })
    const dt = new DataTransfer()
    dt.items.add(file)

    await act(async () => {
      await result.current.addFiles(dt.files)
    })

    expect(result.current.attachments).toHaveLength(0)
  })
})
