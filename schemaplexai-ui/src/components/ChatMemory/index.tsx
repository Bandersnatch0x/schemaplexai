import { List, Typography } from 'antd'

const { Text } = Typography

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

interface ChatMemoryProps {
  messages: ChatMessage[]
}

export default function ChatMemory({ messages }: ChatMemoryProps) {
  return (
    <List
      dataSource={messages}
      renderItem={(msg) => (
        <List.Item style={{ justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start' }}>
          <div
            style={{
              maxWidth: '80%',
              padding: '8px 12px',
              borderRadius: 8,
              background: msg.role === 'user' ? '#1677ff' : '#f6f8fa',
              color: msg.role === 'user' ? '#fff' : 'inherit',
            }}
          >
            <Text style={{ color: 'inherit' }}>{msg.content}</Text>
            <div style={{ fontSize: 12, opacity: 0.7, marginTop: 4, textAlign: 'right' }}>
              {new Date(msg.timestamp).toLocaleTimeString()}
            </div>
          </div>
        </List.Item>
      )}
      locale={{ emptyText: '暂无对话记录' }}
    />
  )
}
