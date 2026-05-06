import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import AgentCanvas from '@/pages/AgentCanvas'

const mockMessage = vi.hoisted(() => ({
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
  info: vi.fn(),
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'canvas.title': 'Canvas',
        'canvas.subtitle': 'Visual agent workflow orchestration',
        'canvas.addAgentNode': 'Add Agent Node',
        'canvas.addConditionNode': 'Add Condition Node',
        'canvas.addStartNode': 'Add Start Node',
        'canvas.addEndNode': 'Add End Node',
        'canvas.clearCanvas': 'Clear Canvas',
        'canvas.saveWorkflow': 'Save Workflow',
        'canvas.properties': 'Properties',
        'canvas.nodeType': 'Node Type',
        'canvas.nodeName': 'Node Name',
        'canvas.nodeStatus': 'Status',
        'canvas.selectNode': 'Select a node to view details',
        'canvas.start': 'Start',
        'canvas.end': 'End',
        'canvas.agent': 'Agent',
        'canvas.condition': 'Condition',
        'canvas.yes': 'Yes',
        'canvas.no': 'No',
        'canvas.valid': 'Valid',
        'canvas.requirementAnalysis': 'Requirement Analysis',
        'canvas.codeGeneration': 'Code Generation',
        'canvas.codeReview': 'Code Review',
        'canvas.refinement': 'Refinement',
        'common.success': 'Success',
        'common.error': 'Error',
      }
      return translations[key] || key
    },
    i18n: { language: 'en' },
  }),
  Trans: ({ children }: { children: React.ReactNode }) => children,
}))

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    message: mockMessage,
  }
})

function renderAgentCanvas() {
  return render(<AgentCanvas />)
}

describe('AgentCanvas component', () => {
  it('renders the canvas page with toolbar buttons', () => {
    renderAgentCanvas()

    expect(screen.getByTestId('agent-canvas-page')).toBeInTheDocument()
    expect(screen.getByText('Add Agent Node')).toBeInTheDocument()
    expect(screen.getByText('Add Condition Node')).toBeInTheDocument()
    expect(screen.getByText('Add Start Node')).toBeInTheDocument()
    expect(screen.getByText('Add End Node')).toBeInTheDocument()
    expect(screen.getByText('Clear Canvas')).toBeInTheDocument()
    expect(screen.getByText('Save Workflow')).toBeInTheDocument()
  })

  it('renders demo nodes on initial load', () => {
    renderAgentCanvas()

    expect(screen.getByTestId('canvas-node-n1')).toBeInTheDocument()
    expect(screen.getByTestId('canvas-node-n2')).toBeInTheDocument()
    expect(screen.getByTestId('canvas-node-n3')).toBeInTheDocument()
    expect(screen.getByTestId('canvas-node-n4')).toBeInTheDocument()
    expect(screen.getByTestId('canvas-node-n5')).toBeInTheDocument()
    expect(screen.getByTestId('canvas-node-n6')).toBeInTheDocument()
    expect(screen.getByTestId('canvas-node-n7')).toBeInTheDocument()
  })

  it('renders the properties panel', () => {
    renderAgentCanvas()

    expect(screen.getByText('Properties')).toBeInTheDocument()
    expect(screen.getByText('Select a node to view details')).toBeInTheDocument()
  })

  it('renders the mini-map', () => {
    renderAgentCanvas()

    expect(screen.getByText('Canvas')).toBeInTheDocument()
  })

  it('clears all nodes when clear canvas is clicked', async () => {
    renderAgentCanvas()

    expect(screen.getByTestId('canvas-node-n1')).toBeInTheDocument()

    const clearBtn = screen.getByText('Clear Canvas')
    clearBtn.click()

    await waitFor(() => {
      expect(screen.queryByTestId('canvas-node-n1')).not.toBeInTheDocument()
      expect(screen.queryByTestId('canvas-node-n2')).not.toBeInTheDocument()
    })
  })

  it('shows node details when a node is clicked', async () => {
    renderAgentCanvas()

    const node = screen.getByTestId('canvas-node-n2')
    node.click()

    await waitFor(() => {
      expect(screen.getByText('Node Type')).toBeInTheDocument()
      expect(screen.getByText('Node Name')).toBeInTheDocument()
      expect(screen.getByText('Status')).toBeInTheDocument()
    })
  })

  it('clickingAddAgentNodeButtonAddsAgentNodeToCanvas', async () => {
    renderAgentCanvas()

    const before = screen.getAllByTestId(/canvas-node-n/).length

    const addBtn = screen.getByText('Add Agent Node')
    addBtn.click()

    await waitFor(() => {
      const after = screen.getAllByTestId(/canvas-node-n/).length
      expect(after).toBe(before + 1)
    })

    const addedNode = screen.getAllByTestId(/canvas-node-n/).pop()
    expect(addedNode).toHaveClass('canvas-node--agent')
  })

  it('clickingSaveButtonShowsSuccessMessage', () => {
    renderAgentCanvas()

    const saveBtn = screen.getByText('Save Workflow')
    saveBtn.click()

    expect(mockMessage.success).toHaveBeenCalledWith('Success')
  })

  it('clickingPropertiesToggleCollapsesAndExpandsPanel', async () => {
    renderAgentCanvas()

    const panel = screen.getByText('Properties').closest('.canvas-panel') as HTMLElement
    expect(panel).not.toHaveClass('canvas-panel--collapsed')

    const toggleBtn = screen.getByLabelText('Collapse panel')
    toggleBtn.click()

    await waitFor(() => {
      expect(panel).toHaveClass('canvas-panel--collapsed')
    })

    const expandBtn = screen.getByLabelText('Expand panel')
    expandBtn.click()

    await waitFor(() => {
      expect(panel).not.toHaveClass('canvas-panel--collapsed')
    })
  })

  it('clickingSameNodeTwiceDeselectsIt', async () => {
    renderAgentCanvas()

    const node = screen.getByTestId('canvas-node-n2')

    node.click()
    await waitFor(() => {
      expect(node).toHaveClass('canvas-node--selected')
    })

    node.click()
    await waitFor(() => {
      expect(node).not.toHaveClass('canvas-node--selected')
    })
  })
})
