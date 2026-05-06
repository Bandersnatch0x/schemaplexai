import { useState, useRef, useCallback, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  RobotOutlined,
  PlusOutlined,
  ClearOutlined,
  SaveOutlined,
  PlayCircleOutlined,
  BranchesOutlined,
  FlagOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons'
import { Button, message } from 'antd'
import './AgentCanvas.css'

interface CanvasNode {
  id: string
  type: 'start' | 'agent' | 'condition' | 'end'
  label: string
  x: number
  y: number
  status?: 'idle' | 'running' | 'completed' | 'error'
  conditionExpr?: string
}

interface CanvasConnection {
  id: string
  from: string
  to: string
  label?: string
}

const INITIAL_NODES: CanvasNode[] = [
  { id: 'n1', type: 'start', label: 'start', x: 80, y: 280 },
  { id: 'n2', type: 'agent', label: 'requirementAnalysis', x: 280, y: 280, status: 'completed' },
  { id: 'n3', type: 'condition', label: 'valid', x: 520, y: 280, conditionExpr: 'input.valid == true' },
  { id: 'n4', type: 'agent', label: 'codeGeneration', x: 760, y: 180, status: 'running' },
  { id: 'n5', type: 'agent', label: 'codeReview', x: 1000, y: 180, status: 'idle' },
  { id: 'n6', type: 'end', label: 'end', x: 1200, y: 180 },
  { id: 'n7', type: 'agent', label: 'refinement', x: 760, y: 420, status: 'idle' },
]

const INITIAL_CONNECTIONS: CanvasConnection[] = [
  { id: 'c1', from: 'n1', to: 'n2' },
  { id: 'c2', from: 'n2', to: 'n3' },
  { id: 'c3', from: 'n3', to: 'n4', label: 'yes' },
  { id: 'c4', from: 'n4', to: 'n5' },
  { id: 'c5', from: 'n5', to: 'n6' },
  { id: 'c6', from: 'n3', to: 'n7', label: 'no' },
  { id: 'c7', from: 'n7', to: 'n2' },
]

function getNodeCenter(node: CanvasNode): { cx: number; cy: number } {
  const width = node.type === 'condition' ? 120 : node.type === 'start' || node.type === 'end' ? 80 : 140
  const height = node.type === 'condition' ? 80 : node.type === 'start' || node.type === 'end' ? 80 : 64
  return { cx: node.x + width / 2, cy: node.y + height / 2 }
}

function getEdgePoint(node: CanvasNode, targetCx: number, targetCy: number): { x: number; y: number } {
  const width = node.type === 'condition' ? 120 : node.type === 'start' || node.type === 'end' ? 80 : 140
  const height = node.type === 'condition' ? 80 : node.type === 'start' || node.type === 'end' ? 80 : 64
  const cx = node.x + width / 2
  const cy = node.y + height / 2
  const dx = targetCx - cx
  const dy = targetCy - cy
  const angle = Math.atan2(dy, dx)

  let dist: number
  if (node.type === 'condition') {
    const absCos = Math.abs(Math.cos(angle))
    const absSin = Math.abs(Math.sin(angle))
    dist = Math.min((width / 2) / (absCos || 0.001), (height / 2) / (absSin || 0.001))
  } else if (node.type === 'start' || node.type === 'end') {
    dist = width / 2
  } else {
    const absCos = Math.abs(Math.cos(angle))
    const absSin = Math.abs(Math.sin(angle))
    dist = Math.min((width / 2) / (absCos || 0.001), (height / 2) / (absSin || 0.001))
  }

  return { x: cx + Math.cos(angle) * dist, y: cy + Math.sin(angle) * dist }
}

function buildBezierPath(from: CanvasNode, to: CanvasNode): string {
  const fromCenter = getNodeCenter(from)
  const toCenter = getNodeCenter(to)
  const start = getEdgePoint(from, toCenter.cx, toCenter.cy)
  const end = getEdgePoint(to, fromCenter.cx, fromCenter.cy)

  const dx = end.x - start.x
  const controlOffset = Math.abs(dx) * 0.4

  const cp1x = start.x + controlOffset
  const cp1y = start.y
  const cp2x = end.x - controlOffset
  const cp2y = end.y

  return `M ${start.x} ${start.y} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${end.x} ${end.y}`
}

function buildArrowPath(from: CanvasNode, to: CanvasNode): string {
  const fromCenter = getNodeCenter(from)
  const toCenter = getNodeCenter(to)
  const end = getEdgePoint(to, fromCenter.cx, fromCenter.cy)

  const angle = Math.atan2(toCenter.cy - fromCenter.cy, toCenter.cx - fromCenter.cx)
  const arrowLen = 10
  const arrowAngle = Math.PI / 6

  const x1 = end.x - arrowLen * Math.cos(angle - arrowAngle)
  const y1 = end.y - arrowLen * Math.sin(angle - arrowAngle)
  const x2 = end.x - arrowLen * Math.cos(angle + arrowAngle)
  const y2 = end.y - arrowLen * Math.sin(angle + arrowAngle)

  return `M ${end.x} ${end.y} L ${x1} ${y1} M ${end.x} ${end.y} L ${x2} ${y2}`
}

function getLabelPosition(from: CanvasNode, to: CanvasNode): { x: number; y: number } {
  const fromCenter = getNodeCenter(from)
  const toCenter = getNodeCenter(to)
  return { x: (fromCenter.cx + toCenter.cx) / 2, y: (fromCenter.cy + toCenter.cy) / 2 - 8 }
}

export default function AgentCanvas() {
  const { t } = useTranslation()
  const [nodes, setNodes] = useState<CanvasNode[]>(INITIAL_NODES)
  const [connections] = useState<CanvasConnection[]>(INITIAL_CONNECTIONS)
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [panelCollapsed, setPanelCollapsed] = useState(false)
  const canvasRef = useRef<HTMLDivElement>(null)

  const selectedNode = useMemo(
    () => nodes.find((n) => n.id === selectedNodeId) || null,
    [nodes, selectedNodeId],
  )

  const handleAddNode = useCallback(
    (type: CanvasNode['type']) => {
      const id = `n${Date.now()}`
      const baseX = 200 + Math.random() * 400
      const baseY = 200 + Math.random() * 200
      const newNode: CanvasNode = {
        id,
        type,
        label: type,
        x: baseX,
        y: baseY,
        status: type === 'agent' ? 'idle' : undefined,
      }
      setNodes((prev) => [...prev, newNode])
      setSelectedNodeId(id)
    },
    [],
  )

  const handleClear = useCallback(() => {
    setNodes([])
    setSelectedNodeId(null)
  }, [])

  const handleSave = useCallback(() => {
    message.success(t('common.success'))
  }, [t])

  const handleSelectNode = useCallback((id: string) => {
    setSelectedNodeId((prev) => (prev === id ? null : id))
  }, [])

  const statusIcon = (status: CanvasNode['status']) => {
    switch (status) {
      case 'running':
        return <span className="canvas-status-dot canvas-status-dot--running" />
      case 'completed':
        return <CheckCircleOutlined className="canvas-status-icon canvas-status-icon--completed" />
      case 'error':
        return <CloseCircleOutlined className="canvas-status-icon canvas-status-icon--error" />
      default:
        return <span className="canvas-status-dot canvas-status-dot--idle" />
    }
  }

  const nodeTypeLabel = (type: CanvasNode['type']) => {
    switch (type) {
      case 'start':
        return t('canvas.start')
      case 'end':
        return t('canvas.end')
      case 'agent':
        return t('canvas.agent')
      case 'condition':
        return t('canvas.condition')
    }
  }

  return (
    <div className="canvas-page" data-testid="agent-canvas-page">
      {/* Toolbar */}
      <div className="canvas-toolbar">
        <Button
          className="canvas-toolbar-btn canvas-toolbar-btn--cyan"
          icon={<RobotOutlined />}
          onClick={() => handleAddNode('agent')}
        >
          {t('canvas.addAgentNode')}
        </Button>
        <Button
          className="canvas-toolbar-btn canvas-toolbar-btn--amber"
          icon={<BranchesOutlined />}
          onClick={() => handleAddNode('condition')}
        >
          {t('canvas.addConditionNode')}
        </Button>
        <Button
          className="canvas-toolbar-btn canvas-toolbar-btn--green"
          icon={<PlayCircleOutlined />}
          onClick={() => handleAddNode('start')}
        >
          {t('canvas.addStartNode')}
        </Button>
        <Button
          className="canvas-toolbar-btn canvas-toolbar-btn--red"
          icon={<FlagOutlined />}
          onClick={() => handleAddNode('end')}
        >
          {t('canvas.addEndNode')}
        </Button>
        <div className="canvas-toolbar-divider" />
        <Button
          className="canvas-toolbar-btn canvas-toolbar-btn--ghost"
          icon={<ClearOutlined />}
          onClick={handleClear}
        >
          {t('canvas.clearCanvas')}
        </Button>
        <Button
          className="canvas-toolbar-btn canvas-toolbar-btn--primary"
          icon={<SaveOutlined />}
          onClick={handleSave}
        >
          {t('canvas.saveWorkflow')}
        </Button>
      </div>

      {/* Canvas + Properties Panel */}
      <div className="canvas-workspace">
        <div className="canvas-area" ref={canvasRef}>
          {/* Grid background */}
          <div className="canvas-grid" />

          {/* SVG Connections */}
          <svg className="canvas-svg">
            <defs>
              <marker
                id="canvas-arrowhead"
                markerWidth="10"
                markerHeight="7"
                refX="9"
                refY="3.5"
                orient="auto"
              >
                <polygon points="0 0, 10 3.5, 0 7" fill="#1e2a33" />
              </marker>
            </defs>
            {connections.map((conn) => {
              const fromNode = nodes.find((n) => n.id === conn.from)
              const toNode = nodes.find((n) => n.id === conn.to)
              if (!fromNode || !toNode) return null
              const pathD = buildBezierPath(fromNode, toNode)
              const arrowD = buildArrowPath(fromNode, toNode)
              const labelPos = getLabelPosition(fromNode, toNode)
              return (
                <g key={conn.id}>
                  <path d={pathD} className="canvas-connection" fill="none" />
                  <path d={arrowD} className="canvas-connection-arrow" fill="none" />
                  {conn.label && (
                    <text
                      x={labelPos.x}
                      y={labelPos.y}
                      className="canvas-connection-label"
                      textAnchor="middle"
                    >
                      {t(`canvas.${conn.label}`)}
                    </text>
                  )}
                </g>
              )
            })}
          </svg>

          {/* Nodes */}
          {nodes.map((node) => {
            const isSelected = selectedNodeId === node.id
            const nodeClass = `canvas-node canvas-node--${node.type}${isSelected ? ' canvas-node--selected' : ''}`

            if (node.type === 'start' || node.type === 'end') {
              return (
                <div
                  key={node.id}
                  className={nodeClass}
                  style={{ left: node.x, top: node.y }}
                  onClick={() => handleSelectNode(node.id)}
                  data-testid={`canvas-node-${node.id}`}
                >
                  <div className="canvas-node-circle" />
                  <span className="canvas-node-label">{t(`canvas.${node.label}`)}</span>
                </div>
              )
            }

            if (node.type === 'condition') {
              return (
                <div
                  key={node.id}
                  className={nodeClass}
                  style={{ left: node.x, top: node.y }}
                  onClick={() => handleSelectNode(node.id)}
                  data-testid={`canvas-node-${node.id}`}
                >
                  <div className="canvas-node-diamond" />
                  <span className="canvas-node-label">{t(`canvas.${node.label}`)}</span>
                  {node.conditionExpr && (
                    <span className="canvas-node-sublabel">{node.conditionExpr}</span>
                  )}
                </div>
              )
            }

            return (
              <div
                key={node.id}
                className={nodeClass}
                style={{ left: node.x, top: node.y }}
                onClick={() => handleSelectNode(node.id)}
                data-testid={`canvas-node-${node.id}`}
              >
                <div className="canvas-node-header">
                  <RobotOutlined className="canvas-node-icon" />
                  <span className="canvas-node-title">{t(`canvas.${node.label}`)}</span>
                  {node.status && statusIcon(node.status)}
                </div>
              </div>
            )
          })}
        </div>

        {/* Properties Panel */}
        <div className={`canvas-panel${panelCollapsed ? ' canvas-panel--collapsed' : ''}`}>
          <div className="canvas-panel-header">
            <span className="canvas-panel-title">{t('canvas.properties')}</span>
            <button
              className="canvas-panel-toggle"
              onClick={() => setPanelCollapsed((p) => !p)}
              aria-label={panelCollapsed ? 'Expand panel' : 'Collapse panel'}
            >
              {panelCollapsed ? <PlusOutlined /> : '—'}
            </button>
          </div>
          {!panelCollapsed && (
            <div className="canvas-panel-body">
              {selectedNode ? (
                <div className="canvas-panel-content">
                  <div className="canvas-panel-field">
                    <span className="canvas-panel-field-label">{t('canvas.nodeType')}</span>
                    <span className="canvas-panel-field-value">{nodeTypeLabel(selectedNode.type)}</span>
                  </div>
                  <div className="canvas-panel-field">
                    <span className="canvas-panel-field-label">{t('canvas.nodeName')}</span>
                    <span className="canvas-panel-field-value">
                      {t(`canvas.${selectedNode.label}`)}
                    </span>
                  </div>
                  {selectedNode.status && (
                    <div className="canvas-panel-field">
                      <span className="canvas-panel-field-label">{t('canvas.nodeStatus')}</span>
                      <span className="canvas-panel-field-value">{selectedNode.status}</span>
                    </div>
                  )}
                  {selectedNode.conditionExpr && (
                    <div className="canvas-panel-field">
                      <span className="canvas-panel-field-label">Expression</span>
                      <span className="canvas-panel-field-value canvas-panel-field-value--mono">
                        {selectedNode.conditionExpr}
                      </span>
                    </div>
                  )}
                </div>
              ) : (
                <div className="canvas-panel-empty">{t('canvas.selectNode')}</div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Mini-map */}
      <div className="canvas-minimap">
        <div className="canvas-minimap-title">{t('canvas.title')}</div>
        <div className="canvas-minimap-viewport">
          {nodes.map((node) => {
            const scaleX = 140 / 1400
            const scaleY = 90 / 600
            const width = node.type === 'condition' ? 12 : node.type === 'start' || node.type === 'end' ? 8 : 14
            const height = node.type === 'condition' ? 8 : node.type === 'start' || node.type === 'end' ? 8 : 6
            return (
              <div
                key={node.id}
                className={`canvas-minimap-node canvas-minimap-node--${node.type}`}
                style={{
                  left: node.x * scaleX,
                  top: node.y * scaleY,
                  width,
                  height,
                }}
              />
            )
          })}
        </div>
      </div>
    </div>
  )
}
