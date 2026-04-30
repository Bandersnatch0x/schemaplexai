import { useRef, useEffect } from 'react'
import { List, Tag, Spin } from 'antd'
import { useSseStore } from '@/stores/sseStore'
import type { SseEvent } from '@/types'

const typeColors: Record<string, string> = {
  thinking: 'blue',
  tool_calling: 'orange',
  observation: 'purple',
  completed: 'green',
  error: 'red',
}

const typeLabels: Record<string, string> = {
  thinking: '思考中',
  tool_calling: '调用工具',
  observation: '观察结果',
  completed: '完成',
  error: '错误',
}

export default function SseViewer() {
  const { events, connected, connecting } = useSseStore()
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight
    }
  }, [events])

  const renderItem = (item: SseEvent) => (
    <List.Item>
      <div style={{ width: '100%' }}>
        <div style={{ marginBottom: 4 }}>
          <Tag color={typeColors[item.type] || 'default'}>{typeLabels[item.type] || item.type}</Tag>
          <span style={{ marginLeft: 8, fontSize: 12, color: '#999' }}>
            {new Date(item.timestamp).toLocaleTimeString()}
          </span>
        </div>
        <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', background: '#f6f8fa', padding: 8, borderRadius: 4 }}>
          {item.content}
        </div>
      </div>
    </List.Item>
  )

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
        <span>执行状态:</span>
        {connecting ? <Spin size="small" /> : null}
        <Tag color={connected ? 'green' : 'default'}>{connected ? '已连接' : '未连接'}</Tag>
      </div>
      <div ref={listRef} style={{ flex: 1, overflow: 'auto', border: '1px solid #f0f0f0', borderRadius: 4, padding: '0 12px' }}>
        <List
          dataSource={events}
          renderItem={renderItem}
          locale={{ emptyText: '暂无事件' }}
        />
      </div>
    </div>
  )
}
