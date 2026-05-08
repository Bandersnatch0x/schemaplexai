import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import Composer from '../index'
import * as uploadApi from '@/api/upload'
import type { ComposerValue } from '../types'

vi.mock('@/api/upload', async () => {
  const actual = await vi.importActual<typeof import('@/api/upload')>('@/api/upload')
  return {
    ...actual,
    uploadFile: vi.fn(),
    getScanStatus: vi.fn(),
  }
})

describe('Composer', () => {
  const mockSend = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(uploadApi.getScanStatus).mockResolvedValue({ healthy: true })
  })

  it('renders textarea and send button', async () => {
    render(<Composer onSend={mockSend} />)
    expect(screen.getByRole('textbox')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /agentExecutor\.execute/i })).toBeInTheDocument()
    )
  })

  it('sends value on Enter without Shift', async () => {
    render(<Composer onSend={mockSend} />)
    const textarea = screen.getByRole('textbox')
    fireEvent.change(textarea, { target: { value: 'hello' } })
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: false })
    await waitFor(() =>
      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({ text: 'hello', attachments: [], mentions: [] })
      )
    )
  })

  it('does not send on Shift+Enter', () => {
    render(<Composer onSend={mockSend} />)
    const textarea = screen.getByRole('textbox')
    fireEvent.change(textarea, { target: { value: 'hello' } })
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: true })
    expect(mockSend).not.toHaveBeenCalled()
  })

  it('shows mention candidates when typing @', () => {
    render(<Composer onSend={mockSend} />)
    const textarea = screen.getByRole('textbox')
    fireEvent.change(textarea, { target: { value: '@co' } })
    expect(screen.getByText('code-review')).toBeInTheDocument()
  })

  it('inserts mention on click', () => {
    render(<Composer onSend={mockSend} />)
    const textarea = screen.getByRole('textbox') as HTMLTextAreaElement
    fireEvent.change(textarea, { target: { value: '@co' } })
    fireEvent.click(screen.getByText('code-review'))
    expect(textarea.value).toContain('[@code-review]')
  })

  it('disables upload when scan is unhealthy', async () => {
    vi.mocked(uploadApi.getScanStatus).mockResolvedValue({ healthy: false, message: 'down' })
    render(<Composer onSend={mockSend} />)
    await waitFor(() => expect(screen.getByText('扫描不可用')).toBeInTheDocument())
  })

  it('shows markdown preview toggle button', async () => {
    render(<Composer onSend={mockSend} />)
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /markdown 预览/i })).toBeInTheDocument()
    )
  })

  it('shows preview content when markdown mode is active', async () => {
    render(<Composer onSend={mockSend} />)
    const textarea = screen.getByRole('textbox')
    fireEvent.change(textarea, { target: { value: '# Hello **world**' } })

    const previewButton = await waitFor(() =>
      screen.getByRole('button', { name: /markdown 预览/i })
    )
    fireEvent.click(previewButton)

    // After switching to preview mode, textarea is hidden and markdown is rendered
    await waitFor(() => {
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    })
  })

  it('shows empty preview when no text in preview mode', async () => {
    render(<Composer onSend={mockSend} />)

    const previewButton = await waitFor(() =>
      screen.getByRole('button', { name: /markdown 预览/i })
    )
    fireEvent.click(previewButton)

    await waitFor(() =>
      expect(screen.getByText('无内容可预览')).toBeInTheDocument()
    )
  })

  it('switches back to edit mode from preview', async () => {
    render(<Composer onSend={mockSend} />)
    const textarea = screen.getByRole('textbox')
    fireEvent.change(textarea, { target: { value: 'some text' } })

    // Switch to preview
    const previewButton = await waitFor(() =>
      screen.getByRole('button', { name: /markdown 预览/i })
    )
    fireEvent.click(previewButton)

    // Now the toggle should say 'edit mode'
    const editButton = await waitFor(() =>
      screen.getByRole('button', { name: /编辑模式/i })
    )
    fireEvent.click(editButton)

    // Back to edit mode
    await waitFor(() =>
      expect(screen.getByRole('textbox')).toBeInTheDocument()
    )
  })
})
