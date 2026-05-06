import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { message } from 'antd'
import { getAgentDetail } from '@/api/agent'
import type { Agent } from '@/types'
import './AgentDetail.css'

type TabKey = 'metrics' | 'logs' | 'charts' | 'config'

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

export default function AgentDetail() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  const [agent, setAgent] = useState<Agent | null>(null)
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState<TabKey>('metrics')

  const TABS: { key: TabKey; label: string }[] = [
    { key: 'metrics', label: t('agentDetail.metrics') },
    { key: 'logs', label: t('agentDetail.logs') },
    { key: 'charts', label: t('agentDetail.charts') },
    { key: 'config', label: t('agentDetail.config') },
  ]

  const METRICS = [
    { value: '99.2%', label: t('agentDetail.successRate'), color: '#00d4aa' },
    { value: '245ms', label: t('agentDetail.avgLatency'), color: '#e2e8f0' },
    { value: '1,247', label: t('agentDetail.totalRuns'), color: '#ff9f43' },
  ]

  const RESOURCES = [
    { value: '2.4M', label: t('agentDetail.tokens'), color: '#00d4aa' },
    { value: '$12.4', label: t('agentDetail.cost'), color: '#e2e8f0' },
    { value: '86%', label: t('agentDetail.cacheHit'), color: '#ff9f43' },
  ]

  useEffect(() => {
    if (id) fetchAgent()
  }, [id])

  const fetchAgent = async () => {
    setLoading(true)
    try {
      const data = await getAgentDetail(id!)
      setAgent(data)
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('common.error')
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const statusMap: Record<string, string> = {
    active: t('agentDetail.running'),
    inactive: t('agentDetail.stopped'),
    draft: t('agentDetail.draft'),
  }

  return (
    <div className="agent-detail-page">
      {/* Identity Card */}
      <div className="agent-detail-identity">
        <div className="agent-detail-avatar">
          🤖
        </div>
        <div className="agent-detail-info">
          <div className="agent-detail-name">
            {loading ? '—' : agent?.name || t('agentDetail.unknownAgent')}
          </div>
          <div className="agent-detail-meta">
            ID: {agent?.id || '—'} · {t('agentDetail.version')}: 2.3.1 · {t('agentDetail.status')}:{" "}
            <span
              className={`agent-detail-status agent-detail-status--${agent?.status || 'inactive'}`}
            >
              {agent ? statusMap[agent.status] : '—'}
            </span>
          </div>
        </div>
        <div className="agent-detail-badges">
          <span className="agent-detail-badge agent-detail-badge--llm">
            LLM: {agent?.modelConfig?.model || 'GPT-4'}
          </span>
          <span className="agent-detail-badge agent-detail-badge--feature">
            {t('agentDetail.autoScale')}
          </span>
        </div>
      </div>

      {/* PillNav */}
      <div className="agent-detail-nav">
        <div className="agent-detail-pillnav">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`agent-detail-pillnav-btn ${activeTab === tab.key ? 'agent-detail-pillnav-btn--active' : ''}`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'metrics' && <MetricsTab metrics={METRICS} resources={RESOURCES} t={t} />}
      {activeTab === 'logs' && <LogsTab t={t} />}
      {activeTab === 'charts' && <ChartsTab t={t} />}
      {activeTab === 'config' && <ConfigTab agent={agent} t={t} />}
    </div>
  )
}

function MetricsTab({
  metrics,
  resources,
  t,
}: {
  metrics: { value: string; label: string; color: string }[]
  resources: { value: string; label: string; color: string }[]
  t: (key: string) => string
}) {
  return (
    <>
      <div className="agent-detail-metrics-row">
        <MetricCard title={t('agentDetail.executionMetrics')} items={metrics} />
        <MetricCard title={t('agentDetail.resourceUsage')} items={resources} />
      </div>
      <LogsPanel t={t} />
    </>
  )
}

const METRIC_COLOR_CLASS: Record<string, string> = {
  '#00d4aa': 'agent-detail-metric-value--cyan',
  '#e2e8f0': 'agent-detail-metric-value--primary',
  '#ff9f43': 'agent-detail-metric-value--amber',
}

function MetricCard({
  title,
  items,
}: {
  title: string
  items: { value: string; label: string; color: string }[]
}) {
  return (
    <div className="agent-detail-metric-card">
      <div className="agent-detail-metric-title">{title}</div>
      <div className="agent-detail-metric-items">
        {items.map((item, i) => (
          <div key={i} className="agent-detail-metric-item">
            <div className={`agent-detail-metric-value ${METRIC_COLOR_CLASS[item.color] || ''}`}>
              {item.value}
            </div>
            <div className="agent-detail-metric-label">
              {item.label}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function LogsPanel({ t }: { t: (key: string) => string }) {
  return (
    <div className="agent-detail-logs-panel">
      <div className="agent-detail-logs-header">
        <span className="agent-detail-logs-title">
          {t('agentDetail.executionLogs')}
        </span>
        <span className="agent-detail-logs-hint">{t('agentDetail.autoScroll')}</span>
      </div>
      <TerminalLog />
    </div>
  )
}

function TerminalLog() {
  return (
    <div className="agent-detail-terminal">
      {LOGS.map((log, i) => (
        <div key={i} className="agent-detail-log-line">
          <span className="agent-detail-log-time">{log.time}</span>
          <span className={`agent-detail-log-level--${log.level}`}>[{log.level.toUpperCase()}]</span>
          <span className="agent-detail-log-text">{log.text}</span>
        </div>
      ))}
      <div className="agent-detail-log-line">
        <span className="agent-detail-log-time">10:42:23</span>
        <span className="agent-detail-cursor">...</span>
      </div>
    </div>
  )
}

function LogsTab({ t }: { t: (key: string) => string }) {
  return <LogsPanel t={t} />
}

function ChartsTab({ t }: { t: (key: string) => string }) {
  return (
    <div className="agent-detail-charts-placeholder">
      {t('agentDetail.chartsComingSoon')}
    </div>
  )
}

function ConfigTab({ agent, t }: { agent: Agent | null; t: (key: string) => string }) {
  return (
    <div className="agent-detail-config-card">
      <div className="agent-detail-config-title">
        {t('agentDetail.configuration')}
      </div>
      <pre className="agent-detail-config-pre">
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
          : t('agentDetail.noAgentSelected')}
      </pre>
    </div>
  )
}
