import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { message } from 'antd'
import { getAgentDetail } from '@/api/agent'
import type { Agent } from '@/types'

type TabKey = 'metrics' | 'logs' | 'charts' | 'config'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'metrics', label: 'Metrics' },
  { key: 'logs', label: 'Logs' },
  { key: 'charts', label: 'Charts' },
  { key: 'config', label: 'Config' },
]

const LOGS = [
  {
    time: '10:42:15',
    level: 'info' as const,
    text: 'Agent initialized, config loaded from /agents/cr-v2.yaml',
  },
  {
    time: '10:42:16',
    level: 'info' as const,
    text: 'Connected to LLM provider: openai/gpt-4',
  },
  {
    time: '10:42:18',
    level: 'warn' as const,
    text: 'Context window at 78%, triggering summarization',
  },
  {
    time: '10:42:20',
    level: 'info' as const,
    text: 'Task completed: PR #1247 reviewed, 3 issues found',
  },
  {
    time: '10:42:21',
    level: 'error' as const,
    text: 'Retry failed for node "security-scan", max attempts exceeded',
  },
  {
    time: '10:42:22',
    level: 'info' as const,
    text: 'Fallback to backup agent triggered',
  },
]

const METRICS = [
  { value: '99.2%', label: 'Success Rate', color: '#00d4aa' },
  { value: '245ms', label: 'Avg Latency', color: '#e2e8f0' },
  { value: '1,247', label: 'Total Runs', color: '#ff9f43' },
]

const RESOURCES = [
  { value: '2.4M', label: 'Tokens', color: '#00d4aa' },
  { value: '$12.4', label: 'Cost', color: '#e2e8f0' },
  { value: '86%', label: 'Cache Hit', color: '#ff9f43' },
]

export default function AgentDetail() {
  const { id } = useParams<{ id: string }>()
  const [agent, setAgent] = useState<Agent | null>(null)
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState<TabKey>('metrics')

  useEffect(() => {
    if (id) fetchAgent()
  }, [id])

  const fetchAgent = async () => {
    setLoading(true)
    try {
      const data = await getAgentDetail(id!)
      setAgent(data)
    } catch (err) {
      const msg = err instanceof Error ? err.message : '获取 Agent 详情失败'
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const statusMap: Record<string, string> = {
    active: 'Running',
    inactive: 'Stopped',
    draft: 'Draft',
  }

  const statusColor: Record<string, string> = {
    active: '#00d4aa',
    inactive: '#64748b',
    draft: '#ff9f43',
  }

  return (
    <div>
      {/* Identity Card */}
      <div
        style={{
          display: 'flex',
          gap: 16,
          alignItems: 'center',
          padding: 20,
          background: '#111827',
          borderRadius: 8,
          border: '1px solid #1e2a33',
          marginBottom: 24,
        }}
      >
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 24,
            background: '#0d1117',
            border: '1px solid #1e2a33',
          }}
        >
          🤖
        </div>
        <div style={{ flex: 1 }}>
          <div
            style={{ fontSize: 16, fontWeight: 600, color: '#e2e8f0' }}
          >
            {loading ? '—' : agent?.name || 'Unknown Agent'}
          </div>
          <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
            ID: {agent?.id || '—'} · Version: 2.3.1 · Status:{" "}
            <span
              style={{
                color: agent ? statusColor[agent.status] : '#64748b',
              }}
            >
              {agent ? statusMap[agent.status] : '—'}
            </span>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              padding: '2px 8px',
              borderRadius: 4,
              fontSize: 11,
              fontWeight: 500,
              background: 'rgba(0,212,170,0.1)',
              color: '#00d4aa',
            }}
          >
            LLM: {agent?.modelConfig?.model || 'GPT-4'}
          </span>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              padding: '2px 8px',
              borderRadius: 4,
              fontSize: 11,
              fontWeight: 500,
              background: 'rgba(255,159,67,0.1)',
              color: '#ff9f43',
            }}
          >
            Auto-scale
          </span>
        </div>
      </div>

      {/* PillNav */}
      <div style={{ marginBottom: 24 }}>
        <div
          style={{
            display: 'inline-flex',
            background: '#0d1117',
            borderRadius: 20,
            padding: 4,
            gap: 4,
          }}
        >
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              style={{
                background: activeTab === tab.key ? '#00d4aa' : 'transparent',
                color: activeTab === tab.key ? '#0a0e1a' : '#64748b',
                border: 'none',
                padding: '6px 16px',
                borderRadius: 16,
                fontSize: 13,
                cursor: 'pointer',
                transition: 'all 0.2s',
                fontWeight: activeTab === tab.key ? 600 : 400,
                fontFamily: 'inherit',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'metrics' && <MetricsTab />}
      {activeTab === 'logs' && <LogsTab />}
      {activeTab === 'charts' && <ChartsTab />}
      {activeTab === 'config' && <ConfigTab agent={agent} />}
    </div>
  )
}

function MetricsTab() {
  return (
    <>
      <div
        style={{
          display: 'flex',
          gap: 16,
          marginBottom: 24,
        }}
      >
        <MetricCard title="Execution Metrics" items={METRICS} />
        <MetricCard title="Resource Usage" items={RESOURCES} />
      </div>
      <LogsPanel />
    </>
  )
}

function MetricCard({
  title,
  items,
}: {
  title: string
  items: { value: string; label: string; color: string }[]
}) {
  return (
    <div
      style={{
        flex: 1,
        background: '#111827',
        borderRadius: 8,
        border: '1px solid #1e2a33',
        padding: 20,
      }}
    >
      <div
        style={{
          fontSize: 14,
          fontWeight: 600,
          color: '#e2e8f0',
          marginBottom: 16,
        }}
      >
        {title}
      </div>
      <div style={{ display: 'flex', gap: 16 }}>
        {items.map((item, i) => (
          <div key={i} style={{ flex: 1, textAlign: 'center' }}>
            <div
              style={{
                fontSize: 24,
                fontWeight: 700,
                color: item.color,
              }}
            >
              {item.value}
            </div>
            <div
              style={{
                fontSize: 11,
                color: '#64748b',
                marginTop: 4,
              }}
            >
              {item.label}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function LogsPanel() {
  return (
    <div
      style={{
        background: '#111827',
        borderRadius: 8,
        border: '1px solid #1e2a33',
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          padding: '12px 16px',
          borderBottom: '1px solid #1e2a33',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <span style={{ fontSize: 13, fontWeight: 500, color: '#e2e8f0' }}>
          Execution Logs
        </span>
        <span style={{ fontSize: 11, color: '#64748b' }}>Auto-scroll on</span>
      </div>
      <TerminalLog />
    </div>
  )
}

function TerminalLog() {
  const levelColor: Record<string, string> = {
    info: '#00d4aa',
    warn: '#ff9f43',
    error: '#ff4757',
  }

  return (
    <div
      style={{
        background: '#0d1117',
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: 12,
        lineHeight: 1.6,
        padding: 16,
        maxHeight: 300,
        overflowY: 'auto',
      }}
    >
      {LOGS.map((log, i) => (
        <div key={i} style={{ display: 'flex', gap: 8 }}>
          <span style={{ color: '#64748b' }}>{log.time}</span>
          <span style={{ color: levelColor[log.level] }}>[{log.level.toUpperCase()}]</span>
          <span style={{ color: '#e2e8f0' }}>{log.text}</span>
        </div>
      ))}
      <div style={{ display: 'flex', gap: 8 }}>
        <span style={{ color: '#64748b' }}>10:42:23</span>
        <span className="cursor" style={{ color: '#00d4aa' }}>...</span>
      </div>
    </div>
  )
}

function LogsTab() {
  return <LogsPanel />
}

function ChartsTab() {
  return (
    <div
      style={{
        background: '#111827',
        borderRadius: 8,
        border: '1px solid #1e2a33',
        padding: 40,
        textAlign: 'center',
        color: '#64748b',
      }}
    >
      Charts view coming soon...
    </div>
  )
}

function ConfigTab({ agent }: { agent: Agent | null }) {
  return (
    <div
      style={{
        background: '#111827',
        borderRadius: 8,
        border: '1px solid #1e2a33',
        padding: 20,
      }}
    >
      <div
        style={{
          fontSize: 14,
          fontWeight: 600,
          color: '#e2e8f0',
          marginBottom: 16,
        }}
      >
        Configuration
      </div>
      <pre
        style={{
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: 12,
          color: '#e2e8f0',
          lineHeight: 1.6,
          overflow: 'auto',
        }}
      >
        {agent
          ? JSON.stringify(
              {
                id: agent.id,
                name: agent.name,
                type: agent.type,
                status: agent.status,
                model: agent.modelConfig?.model,
                temperature: agent.modelConfig?.temperature,
                maxTokens: agent.modelConfig?.maxTokens,
                tools: agent.tools,
              },
              null,
              2
            )
          : 'No agent selected'}
      </pre>
    </div>
  )
}
