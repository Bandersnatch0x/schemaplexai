import { useRef, useEffect, useState, useCallback } from 'react'
import { List, Tag, Spin, Button, Tooltip } from 'antd'
import {
  CopyOutlined,
  DownOutlined,
  RightOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  BulbOutlined,
  ToolOutlined,
  FileTextOutlined,
  CodeOutlined,
  SyncOutlined,
  ApartmentOutlined,
} from '@ant-design/icons'
import { useSseStore } from '@/stores/sseStore'
import type { SseEvent } from '@/types'

interface TypeConfig {
  color: string
  label: string
  icon: React.ReactNode
  bgColor: string
  borderColor: string
}

const typeConfig: Record<string, TypeConfig> = {
  thinking: {
    color: 'blue',
    label: '思考中',
    icon: <BulbOutlined />,
    bgColor: '#e6f4ff',
    borderColor: '#91caff',
  },
  thought: {
    color: 'blue',
    label: '思考',
    icon: <BulbOutlined />,
    bgColor: '#e6f4ff',
    borderColor: '#91caff',
  },
  tool_calling: {
    color: 'green',
    label: '调用工具',
    icon: <ToolOutlined />,
    bgColor: '#f6ffed',
    borderColor: '#b7eb8f',
  },
  tool_call: {
    color: 'green',
    label: '工具调用',
    icon: <ToolOutlined />,
    bgColor: '#f6ffed',
    borderColor: '#b7eb8f',
  },
  tool_result: {
    color: 'cyan',
    label: '工具结果',
    icon: <CheckCircleOutlined />,
    bgColor: '#e6fffb',
    borderColor: '#87e8de',
  },
  observation: {
    color: 'purple',
    label: '观察结果',
    icon: <EyeIcon />,
    bgColor: '#f9f0ff',
    borderColor: '#d3adf7',
  },
  plan: {
    color: 'geekblue',
    label: '计划',
    icon: <ApartmentOutlined />,
    bgColor: '#f0f5ff',
    borderColor: '#adc6ff',
  },
  file_diff: {
    color: 'default',
    label: '文件变更',
    icon: <FileTextOutlined />,
    bgColor: '#fafafa',
    borderColor: '#d9d9d9',
  },
  output: {
    color: 'green',
    label: '输出',
    icon: <FileTextOutlined />,
    bgColor: '#f6ffed',
    borderColor: '#b7eb8f',
  },
  completed: {
    color: 'green',
    label: '完成',
    icon: <CheckCircleOutlined />,
    bgColor: '#f6ffed',
    borderColor: '#b7eb8f',
  },
  'execution-completed': {
    color: 'green',
    label: '执行完成',
    icon: <CheckCircleOutlined />,
    bgColor: '#f6ffed',
    borderColor: '#b7eb8f',
  },
  approval_req: {
    color: 'default',
    label: '审批请求',
    icon: <SyncOutlined />,
    bgColor: '#fafafa',
    borderColor: '#d9d9d9',
  },
  approval_resp: {
    color: 'default',
    label: '审批响应',
    icon: <SyncOutlined />,
    bgColor: '#fafafa',
    borderColor: '#d9d9d9',
  },
  error: {
    color: 'red',
    label: '错误',
    icon: <CloseCircleOutlined />,
    bgColor: '#fff2f0',
    borderColor: '#ffccc7',
  },
  'state-transition': {
    color: 'default',
    label: '状态变更',
    icon: <SyncOutlined />,
    bgColor: '#fafafa',
    borderColor: '#d9d9d9',
  },
}

function EyeIcon() {
  return (
    <svg viewBox="0 0 1024 1024" width="1em" height="1em" fill="currentColor">
      <path d="M942.2 486.2C847.4 286.5 704.1 186 512 186c-192.2 0-335.4 100.5-430.2 300.3a7.85 7.85 0 0 0 0 6.5C206.5 851.2 281.5 949.1 512 949.1c192.2 0 335.4-100.5 430.2-300.3 7.7-16.2 7.7-36 0-52.6zM512 832c-142.1 0-256-113.9-256-256s113.9-256 256-256 256 113.9 256 256-113.9 256-256 256z" />
      <path d="M512 448c-35.3 0-64 28.7-64 64s28.7 64 64 64 64-28.7 64-64-28.7-64-64-64z" />
    </svg>
  )
}

function formatTime(ts: number): string {
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0')
}

function useCopyButton() {
  const [copiedId, setCopiedId] = useState<string | null>(null)

  const copy = useCallback((id: string, text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopiedId(id)
      setTimeout(() => setCopiedId(null), 1500)
    })
  }, [])

  return { copiedId, copy }
}

export default function SseViewer() {
  const { events, connected, connecting } = useSseStore()
  const listRef = useRef<HTMLDivElement>(null)
  const { copiedId, copy } = useCopyButton()
  const [expandedMap, setExpandedMap] = useState<Record<string, boolean>>({})

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight
    }
  }, [events])

  const toggleExpand = useCallback((id: string) => {
    setExpandedMap((prev) => ({ ...prev, [id]: !prev[id] }))
  }, [])

  const isExpanded = useCallback(
    (item: SseEvent) => {
      if (item.expanded !== undefined) return item.expanded
      return expandedMap[item.id] ?? true
    },
    [expandedMap]
  )

  const renderItem = (item: SseEvent) => {
    const config = typeConfig[item.type] || typeConfig['state-transition']
    const expanded = isExpanded(item)
    const content = item.content || (item.metadata?.content as string) || ''
    const shouldTruncate = content.length > 300
    const displayContent = expanded || !shouldTruncate ? content : content.slice(0, 300) + '...'

    return (
      <List.Item style={{ padding: '8px 0', borderBottom: 'none' }}>
        <div
          style={{
            width: '100%',
            background: config.bgColor,
            border: `1px solid ${config.borderColor}`,
            borderRadius: 8,
            padding: '10px 14px',
            transition: 'all 0.2s',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Tag color={config.color} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                {config.icon}
                {config.label}
              </Tag>
              <span style={{ fontSize: 12, color: '#999', fontFamily: 'monospace' }}>
                {formatTime(item.timestamp)}
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              {shouldTruncate && (
                <Button
                  type="text"
                  size="small"
                  icon={expanded ? <DownOutlined /> : <RightOutlined />}
                  onClick={() => toggleExpand(item.id)}
                  style={{ color: '#999' }}
                >
                  {expanded ? '收起' : '展开'}
                </Button>
              )}
              <Tooltip title={copiedId === item.id ? '已复制' : '复制内容'}>
                <Button
                  type="text"
                  size="small"
                  icon={copiedId === item.id ? <CheckCircleOutlined style={{ color: '#52c41a' }} /> : <CopyOutlined />}
                  onClick={() => copy(item.id, content)}
                  style={{ color: '#999' }}
                />
              </Tooltip>
            </div>
          </div>
          <div
            style={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              fontSize: 13,
              lineHeight: 1.6,
              color: '#262626',
              fontFamily: "'SF Mono', Monaco, 'Cascadia Code', monospace",
              maxHeight: expanded ? undefined : 120,
              overflow: expanded ? undefined : 'hidden',
            }}
          >
            {displayContent}
          </div>
          {item.metadata && typeof item.metadata.toolName === 'string' && (
            <div style={{ marginTop: 6, fontSize: 12, color: '#8c8c8c' }}>
              Tool: <span><CodeOutlined /></span> <span>{item.metadata.toolName}</span>
            </div>
          )}
        </div>
      </List.Item>
    )
  }

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
        <span>执行状态:</span>
        {connecting ? <Spin size="small" /> : null}
        <Tag color={connected ? 'green' : 'default'}>{connected ? '已连接' : '未连接'}</Tag>
        <span style={{ marginLeft: 'auto', fontSize: 12, color: '#999' }}>
          共 {events.length} 条事件
        </span>
      </div>
      <div
        ref={listRef}
        style={{
          flex: 1,
          overflow: 'auto',
          border: '1px solid #f0f0f0',
          borderRadius: 4,
          padding: '0 12px',
          background: '#fafafa',
        }}
      >
        <List dataSource={events} renderItem={renderItem} locale={{ emptyText: '暂无事件' }} />
      </div>
    </div>
  )
}
