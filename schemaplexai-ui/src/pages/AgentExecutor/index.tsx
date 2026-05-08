import { useState, useRef, useCallback, useEffect } from 'react'
import { Card, Select, Row, Col, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { useAgentStore } from '@/stores/agentStore'
import { useSseStore } from '@/stores/sseStore'
import SseViewer from '@/components/SseViewer'
import ChatMemory, { type ChatMessage } from '@/components/ChatMemory'
import Composer from '@/components/Composer'
import type { ComposerValue } from '@/components/Composer/types'
import { sseRequest } from '@/api/request'
import { getAgentList } from '@/api/agent-config'
import type { SseEvent } from '@/types'
import './AgentExecutor.css'

const { Option } = Select

export default function AgentExecutor() {
  const { t } = useTranslation()
  const { agents, setAgents } = useAgentStore()
  const addEvent = useSseStore((state) => state.addEvent)
  const clearEvents = useSseStore((state) => state.clearEvents)
  const setConnected = useSseStore((state) => state.setConnected)
  const setConnecting = useSseStore((state) => state.setConnecting)
  const [selectedAgent, setSelectedAgent] = useState<string>()
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
        const msg = err instanceof Error ? err.message : t('agentExecutor.fetchError')
        message.error(msg)
      })
      .finally(() => {
        setAgentsLoading(false)
      })
  }, [setAgents, t])

  useEffect(() => {
    return () => {
      if (esRef.current) {
        esRef.current.close()
        esRef.current = null
      }
    }
  }, [])

  const handleExecute = useCallback((value: ComposerValue) => {
    if (!selectedAgent) {
      message.warning(t('agentExecutor.selectAgentFirst'))
      return
    }
    if (!value.text.trim() && value.attachments.length === 0) {
      message.warning(t('agentExecutor.inputPrompt'))
      return
    }

    if (esRef.current) {
      esRef.current.close()
      esRef.current = null
    }

    clearEvents()
    setExecuting(true)
    setConnecting(true)

    const attachmentNames = value.attachments.map((a) => a.name).join(', ')
    const displayContent = value.attachments.length > 0
      ? `${value.text}\n[附件: ${attachmentNames}]`
      : value.text

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: displayContent,
      timestamp: Date.now(),
    }
    setMessages((prev) => [...prev, userMsg])

    const payload: Record<string, unknown> = {
      prompt: value.text,
      attachments: value.attachments,
    }
    const es = sseRequest(`/agents/${selectedAgent}/executions/events`, payload)
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
        message.error(t('agentExecutor.sseError'))
      }
    }
  }, [selectedAgent, clearEvents, setConnecting, setConnected, addEvent, t])

  return (
    <div className="agent-exec-container">
      <Row gutter={[16, 16]} className="agent-exec-toolbar">
        <Col span={8}>
          <Select
            placeholder={t('agentExecutor.selectAgent')}
            className="agent-exec-select"
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
          <Composer
            placeholder={t('agentExecutor.promptPlaceholder')}
            disabled={!selectedAgent}
            loading={executing}
            onSend={handleExecute}
          />
        </Col>
      </Row>
      <Row gutter={[16, 16]} className="agent-exec-main">
        <Col span={12} style={{ height: '100%' }}>
          <Card title={t('agentExecutor.chatHistory')} className="agent-exec-panel agent-exec-chat-panel" style={{ height: '100%', overflow: 'auto' }}>
            <ChatMemory messages={messages} />
          </Card>
        </Col>
        <Col span={12} style={{ height: '100%' }}>
          <Card title={t('agentExecutor.executionStatus')} className="agent-exec-panel agent-exec-sse-panel" style={{ height: '100%' }}>
            <SseViewer />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
