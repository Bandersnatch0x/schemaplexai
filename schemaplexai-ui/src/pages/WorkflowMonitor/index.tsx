import { useEffect, useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { message } from 'antd'
import { getWorkflowList, getWorkflowInstances, type Workflow, type WorkflowInstance } from '@/api/workflow'
import './WorkflowMonitor.css'

type StatusFilter = 'all' | 'running' | 'completed' | 'failed'

interface WorkflowRun {
  id: string
  name: string
  status: StatusFilter
  startHour: number
  duration: number
  color: string
}

function mapInstanceToRun(instance: WorkflowInstance, templateMap: Map<string, Workflow>): WorkflowRun {
  const created = new Date(instance.createdAt)
  const updated = new Date(instance.updatedAt)
  const startHour = created.getHours() + created.getMinutes() / 60
  const durationMs = updated.getTime() - created.getTime()
  const duration = Math.max(0.1, durationMs / 1000 / 60 / 60)

  const status: StatusFilter =
    instance.status === 'running' ? 'running' :
    instance.status === 'completed' || instance.status === 'success' ? 'completed' :
    instance.status === 'failed' || instance.status === 'error' ? 'failed' : 'completed'

  const color = status === 'running' ? '#00d4aa' : status === 'failed' ? '#ff4757' : '#64748b'
  const template = templateMap.get(instance.templateId)

  return {
    id: instance.id,
    name: template?.name || `Workflow #${instance.id}`,
    status,
    startHour,
    duration,
    color,
  }
}

interface TimelineMeta {
  start: number
  duration: number
  hours: string[]
}

function buildTimelineMeta(runs: WorkflowRun[]): TimelineMeta {
  if (runs.length === 0) {
    return { start: 9, duration: 3, hours: ['09:00', '10:00', '11:00', '12:00'] }
  }
  const allStartHours = runs.map((r) => r.startHour)
  const allEndHours = runs.map((r) => r.startHour + r.duration)
  const minHour = Math.floor(Math.min(...allStartHours, ...allEndHours))
  const maxHour = Math.ceil(Math.max(...allStartHours, ...allEndHours))
  const start = Math.max(0, minHour)
  const duration = Math.max(3, maxHour - start)
  const hours: string[] = []
  for (let h = start; h <= start + duration; h++) {
    hours.push(`${String(h).padStart(2, '0')}:00`)
  }
  return { start, duration, hours }
}

export default function WorkflowMonitor() {
  const { t } = useTranslation()
  const [runs, setRuns] = useState<WorkflowRun[]>([])
  const [filter, setFilter] = useState<StatusFilter>('all')
  const [loading, setLoading] = useState(false)
  const [apiStatus, setApiStatus] = useState<'connected' | 'disconnected'>('connected')

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const [templatesRes, instancesRes] = await Promise.all([
        getWorkflowList({ page: 1, pageSize: 100 }),
        getWorkflowInstances({ current: 1, size: 50 }),
      ])

      const templateMap = new Map<string, Workflow>()
      templatesRes.list.forEach((w) => templateMap.set(w.id, w))

      if (instancesRes.list.length > 0) {
        const mapped = instancesRes.list.map((i) => mapInstanceToRun(i, templateMap))
        setRuns(mapped)
      } else {
        setRuns([])
      }
      setApiStatus('connected')
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('common.error')
      message.error(msg)
      setApiStatus('disconnected')
    } finally {
      setLoading(false)
    }
  }

  const timelineMeta = useMemo(() => buildTimelineMeta(runs), [runs])

  const FILTERS: { key: StatusFilter; label: string }[] = [
    { key: 'all', label: t('workflowMonitor.all') },
    { key: 'running', label: t('workflowMonitor.running') },
    { key: 'completed', label: t('workflowMonitor.completed') },
    { key: 'failed', label: t('workflowMonitor.failed') },
  ]

  const filteredRuns =
    filter === 'all' ? runs : runs.filter((r) => r.status === filter)

  const statusCount: Record<StatusFilter, number> = {
    all: runs.length,
    running: runs.filter((r) => r.status === 'running').length,
    completed: runs.filter((r) => r.status === 'completed').length,
    failed: runs.filter((r) => r.status === 'failed').length,
  }

  const BAR_COLOR_CLASS: Record<string, string> = {
    '#00d4aa': 'workflow-monitor-bar--cyan',
    '#64748b': 'workflow-monitor-bar--secondary',
    '#ff4757': 'workflow-monitor-bar--red',
  }

  const STAT_VALUE_CLASS: Record<string, string> = {
    '#e2e8f0': 'workflow-monitor-stat-value--primary',
    '#00d4aa': 'workflow-monitor-stat-value--cyan',
    '#64748b': 'workflow-monitor-stat-value--secondary',
    '#ff4757': 'workflow-monitor-stat-value--red',
  }

  return (
    <div className="workflow-monitor-page">
      {loading && <div className="workflow-monitor-loading">{t('common.loading')}</div>}
      {apiStatus === 'disconnected' && (
        <div className="workflow-monitor-offline-notice">
          {t('cockpit.backendUnavailable')}
        </div>
      )}

      {/* Stats Row */}
      <div className="workflow-monitor-stats">
        <StatBadge label={t('workflowMonitor.total')} value={statusCount.all} colorClass={STAT_VALUE_CLASS['#e2e8f0']} />
        <StatBadge label={t('workflowMonitor.running')} value={statusCount.running} colorClass={STAT_VALUE_CLASS['#00d4aa']} />
        <StatBadge label={t('workflowMonitor.completed')} value={statusCount.completed} colorClass={STAT_VALUE_CLASS['#64748b']} />
        <StatBadge label={t('workflowMonitor.failed')} value={statusCount.failed} colorClass={STAT_VALUE_CLASS['#ff4757']} />
      </div>

      {/* Timeline Card */}
      <div className="workflow-monitor-timeline">
        <div className="workflow-monitor-timeline-header">
          <div className="workflow-monitor-timeline-title">
            {t('workflowMonitor.timeline')}
          </div>

          {/* PillNav */}
          <div className="workflow-monitor-pillnav">
            {FILTERS.map((f) => (
              <button
                key={f.key}
                onClick={() => setFilter(f.key)}
                className={`workflow-monitor-pillnav-btn ${filter === f.key ? 'workflow-monitor-pillnav-btn--active' : ''}`}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        <div className="workflow-monitor-timeline-body">
          {/* Timeline Header */}
          <div className="workflow-monitor-axis-header">
            {timelineMeta.hours.map((h) => (
              <div key={h} className="workflow-monitor-axis-label">
                {h}
              </div>
            ))}
          </div>

          {/* Grid Lines */}
          <div className="workflow-monitor-grid">
            <div className="workflow-monitor-gridlines">
              {timelineMeta.hours.map((_, i) => (
                <div
                  key={i}
                  className={`workflow-monitor-gridline ${i > 0 ? 'workflow-monitor-gridline--dashed' : ''}`}
                />
              ))}
            </div>

            {/* Workflow Rows */}
            {filteredRuns.map((run) => (
              <div key={run.id} className="workflow-monitor-row">
                <div className="workflow-monitor-row-label">
                  {run.name}
                </div>
                <div className="workflow-monitor-row-track">
                  <div
                    className={`workflow-monitor-bar ${BAR_COLOR_CLASS[run.color] || ''}`}
                    style={{
                      left: `${((run.startHour - timelineMeta.start) / timelineMeta.duration) * 100}%`,
                      width: `${(run.duration / timelineMeta.duration) * 100}%`,
                    }}
                  />
                </div>
              </div>
            ))}

            {filteredRuns.length === 0 && (
              <div className="workflow-monitor-empty">
                {t('workflowMonitor.noWorkflows')}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Recent Runs Table */}
      <div className="workflow-monitor-recent">
        <div className="workflow-monitor-recent-header">
          {t('workflowMonitor.recentRuns')}
        </div>
        <div className="workflow-monitor-table-wrap">
          <table className="workflow-monitor-table">
            <thead>
              <tr>
                <th>{t('workflowMonitor.workflow')}</th>
                <th>{t('workflowMonitor.running')}</th>
                <th>{t('workflowMonitor.started')}</th>
                <th>{t('workflowMonitor.duration')}</th>
              </tr>
            </thead>
            <tbody>
              {filteredRuns.map((run) => (
                <tr key={run.id}>
                  <td>{run.name}</td>
                  <td>
                    <StatusBadge status={run.status} />
                  </td>
                  <td>
                    {String(Math.floor(run.startHour)).padStart(2, '0')}:
                    {String(
                      Math.floor((run.startHour % 1) * 60)
                    ).padStart(2, '0')}
                  </td>
                  <td>
                    {Math.round(run.duration * 60)}m
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function StatBadge({
  label,
  value,
  colorClass,
}: {
  label: string
  value: number
  colorClass: string
}) {
  return (
    <div className="workflow-monitor-stat">
      <span className="workflow-monitor-stat-label">{label}</span>
      <span className={`workflow-monitor-stat-value ${colorClass}`}>{value}</span>
    </div>
  )
}

function StatusBadge({ status }: { status: StatusFilter }) {
  const { t } = useTranslation()
  const labelMap: Record<StatusFilter, string> = {
    running: t('workflowMonitor.running'),
    completed: t('workflowMonitor.completed'),
    failed: t('workflowMonitor.failed'),
    all: t('workflowMonitor.all'),
  }

  return (
    <span className={`workflow-monitor-status workflow-monitor-status--${status}`}>
      {labelMap[status]}
    </span>
  )
}
