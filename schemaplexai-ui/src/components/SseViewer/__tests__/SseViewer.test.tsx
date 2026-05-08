import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import SseViewer from '../index'
import { useSseStore } from '@/stores/sseStore'

vi.mock('@/stores/sseStore', () => ({
  useSseStore: vi.fn(),
}))

describe('SseViewer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mockStore = (events: any[], connected: boolean, connecting: boolean) => {
    vi.mocked(useSseStore).mockReturnValue({ events, connected, connecting } as ReturnType<typeof useSseStore>)
  }

  it('renders empty state when no events', () => {
    mockStore([], false, false)
    render(<SseViewer />)
    expect(screen.getByText('暂无事件')).toBeInTheDocument()
  })

  it('shows connection status', () => {
    mockStore([], true, false)
    render(<SseViewer />)
    expect(screen.getByText('已连接')).toBeInTheDocument()
  })

  it('shows connecting spinner', () => {
    mockStore([], false, true)
    render(<SseViewer />)
    expect(screen.getByText('执行状态:')).toBeInTheDocument()
  })

  it('renders thinking events with correct styling', () => {
    mockStore(
      [{ id: '1', type: 'thought', content: 'Analyzing request...', timestamp: Date.now() }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('思考')).toBeInTheDocument()
    expect(screen.getByText('Analyzing request...')).toBeInTheDocument()
  })

  it('renders tool_call events with tool name', () => {
    mockStore(
      [{
        id: '2',
        type: 'tool_call',
        content: 'Calling search tool',
        timestamp: Date.now(),
        metadata: { toolName: 'search' },
      }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('工具调用')).toBeInTheDocument()
    expect(screen.getByText('search')).toBeInTheDocument()
  })

  it('renders tool_result events', () => {
    mockStore(
      [{ id: '3', type: 'tool_result', content: 'Found 5 results', timestamp: Date.now() }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('工具结果')).toBeInTheDocument()
    expect(screen.getByText('Found 5 results')).toBeInTheDocument()
  })

  it('renders error events with red styling', () => {
    mockStore(
      [{ id: '4', type: 'error', content: 'Failed to connect to LLM', timestamp: Date.now() }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('错误')).toBeInTheDocument()
    expect(screen.getByText('Failed to connect to LLM')).toBeInTheDocument()
  })

  it('renders plan events', () => {
    mockStore(
      [{ id: '5', type: 'plan', content: 'Step 1: Analyze\nStep 2: Execute', timestamp: Date.now() }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('计划')).toBeInTheDocument()
  })

  it('renders output events', () => {
    mockStore(
      [{ id: '6', type: 'output', content: 'Final result: Success', timestamp: Date.now() }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('输出')).toBeInTheDocument()
    expect(screen.getByText('Final result: Success')).toBeInTheDocument()
  })

  it('renders state-transition events', () => {
    mockStore(
      [{ id: '7', type: 'state-transition', content: 'THINKING -> TOOL_CALLING', timestamp: Date.now() }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('状态变更')).toBeInTheDocument()
  })

  it('shows event count', () => {
    mockStore(
      [
        { id: '1', type: 'thought', content: 'a', timestamp: Date.now() },
        { id: '2', type: 'thought', content: 'b', timestamp: Date.now() },
        { id: '3', type: 'output', content: 'c', timestamp: Date.now() },
      ],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText(/共 3 条事件/)).toBeInTheDocument()
  })

  it('renders completed events', () => {
    mockStore(
      [{ id: '8', type: 'completed', content: 'Execution finished', timestamp: Date.now() }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('完成')).toBeInTheDocument()
  })

  it('renders execution-completed events', () => {
    mockStore(
      [{ id: '9', type: 'execution-completed', content: 'All tasks done', timestamp: Date.now() }],
      false, false
    )
    render(<SseViewer />)
    expect(screen.getByText('执行完成')).toBeInTheDocument()
  })
})
