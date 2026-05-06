import { useState, useRef, useCallback, useEffect } from 'react'
import { Card, Select, Input, Button, Row, Col, message } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { useAgentStore } from '@/stores/agentStore'
import { useSseStore } from '@/stores/sseStore'
import SseViewer from '@/components/SseViewer'
import ChatMemory, { type ChatMessage } from '@/components/ChatMemory'
import { sseRequest } from '@/api/request'
import { getAgentList } from '@/api/agent'
import type { SseEvent } from '@/types'

const { TextArea } = Input
const { Option } = Select

export default function AgentExecutor() {
  const { agents, setAgents } = useAgentStore()
  const addEvent = useSseStore((state) => state.addEvent)
  const clearEvents = useSseStore((state) => state.clearEvents)
  const setConnected = useSseStore((state) => state.setConnected)
  const setConnecting = useSseStore((state) => state.setConnecting)
  const [selectedAgent, setSelectedAgent] = useState<string>()
  const [prompt, setPrompt] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [executing, setExecuting] = useState(false)
  const [agentsLoading, setAgentsLoading] = useState(false)
  const esRef = useRef<EventSource | null>(null)
  const retryCountRef = useRef(0)
  const MAX_RETRIES = 3

  useEffect(() => {
    setAgentsLoading(true)
    getAgentList({ page: 1, pageSize: 100 })
      .then((data) => {
        setAgents(data.list)
      })
      .catch((err) => {
        const msg = err instanceof Error ? err.message : '获取 Agent 列表失败'
        message.error(msg)
      })
      .finally(() => {
        setAgentsLoading(false)
      })
  }, [setAgents])

  useEffect(() => {
    return () => {
      if (esRef.current) {
        esRef.current.close()
        esRef.current = null
      }
    }
  }, [])

  const handleExecute = useCallback(() => {
    if (!selectedAgent) {
      message.warning('请先选择 Agent')
      return
    }
    if (!prompt.trim()) {
      message.warning('请输入 Prompt')
      return
    }

    // Close old connection to prevent duplicate EventSources
    if (esRef.current) {
      esRef.current.close()
      esRef.current = null
    }

    clearEvents()
    setExecuting(true)
    setConnecting(true)

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: prompt,
      timestamp: Date.now(),
    }
    setMessages((prev) => [...prev, userMsg])

    const es = sseRequest(`/agents/${selectedAgent}/executions/events`, { prompt })
    esRef.current = es

    es.onopen = () => {
      setConnecting(false)
      setConnected(true)
      retryCountRef.current = 0
    }

    es.onmessage = (event) => {
      try {
        const data: SseEvent = JSON.parse(event.data)
        addEvent(data)

        if (data.type === 'completed' || data.type === 'error') {
          const assistantMsg: ChatMessage = {
            id: data.id + '_reply',
            role: 'assistant',
            content: data.content,
            timestamp: Date.now(),
          }
          setMessages((prev) => [...prev, assistantMsg])
          setExecuting(false)
          setConnected(false)
          es.close()
          esRef.current = null
        }
      } catch {
        addEvent({ id: Date.now().toString(), type: 'observation', content: event.data, timestamp: Date.now() })
      }
    }

    es.onerror = () => {
      setExecuting(false)
      setConnected(false)
      setConnecting(false)
      retryCountRef.current += 1
      if (retryCountRef.current >= MAX_RETRIES) {
        es.close()
        esRef.current = null
        retryCountRef.current = 0
        message.error('SSE 连接失败，已达到最大重试次数')
      }
    }
  }, [selectedAgent, prompt, clearEvents, setConnecting, setConnected, addEvent])

  return (
    <div style={{ height: 'calc(100vh - 160px)', display: 'flex', flexDirection: 'column' }}>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Select
            placeholder="选择 Agent"
            style={{ width: '100%' }}
            value={selectedAgent}
            onChange={setSelectedAgent}
            loading={agentsLoading}
          >
            {agents.map((a) => (
              <Option key={a.id} value={a.id}>
                {a.name}
              </Option>
            ))}
          </Select>
        </Col>
        <Col span={16}>
          <div style={{ display: 'flex', gap: 8 }}>
            <TextArea
              rows={1}
              placeholder="输入 Prompt..."
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault()
                  handleExecute()
                }
              }}
            />
            <Button type="primary" icon={<SendOutlined />} loading={executing} onClick={handleExecute}>
              执行
            </Button>
          </div>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ flex: 1, overflow: 'hidden' }}>
        <Col span={12} style={{ height: '100%' }}>
          <Card title="对话历史" style={{ height: '100%', overflow: 'auto' }}>
            <ChatMemory messages={messages} />
          </Card>
        </Col>
        <Col span={12} style={{ height: '100%' }}>
          <Card title="执行状态" style={{ height: '100%' }}>
            <SseViewer />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
