import { useState } from 'react'
import { useTranslation } from 'react-i18next'
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

const RUNS: WorkflowRun[] = [
  {
    id: '1',
    name: 'CI/CD Pipeline',
    status: 'running',
    startHour: 9.2,
    duration: 1.5,
    color: '#00d4aa',
  },
  {
    id: '2',
    name: 'Security Scan',
    status: 'completed',
    startHour: 9.5,
    duration: 0.8,
    color: '#64748b',
  },
  {
    id: '3',
    name: 'Data Ingestion',
    status: 'failed',
    startHour: 10.2,
    duration: 0.5,
    color: '#ff4757',
  },
  {
    id: '4',
    name: 'Model Training',
    status: 'running',
    startHour: 10.5,
    duration: 1.2,
    color: '#00d4aa',
  },
  {
    id: '5',
    name: 'Report Generation',
    status: 'completed',
    startHour: 11.0,
    duration: 0.6,
    color: '#64748b',
  },
]

const HOURS = ['09:00', '10:00', '11:00', '12:00']

export default function WorkflowMonitor() {
  const { t } = useTranslation()
  const [filter, setFilter] = useState<StatusFilter>('all')

  const FILTERS: { key: StatusFilter; label: string }[] = [
    { key: 'all', label: t('workflowMonitor.all') },
    { key: 'running', label: t('workflowMonitor.running') },
    { key: 'completed', label: t('workflowMonitor.completed') },
    { key: 'failed', label: t('workflowMonitor.failed') },
  ]

  const filteredRuns =
    filter === 'all' ? RUNS : RUNS.filter((r) => r.status === filter)

  const statusCount: Record<StatusFilter, number> = {
    all: RUNS.length,
    running: RUNS.filter((r) => r.status === 'running').length,
    completed: RUNS.filter((r) => r.status === 'completed').length,
    failed: RUNS.filter((r) => r.status === 'failed').length,
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
            {HOURS.map((h) => (
              <div key={h} className="workflow-monitor-axis-label">
                {h}
              </div>
            ))}
          </div>

          {/* Grid Lines */}
          <div className="workflow-monitor-grid">
            <div className="workflow-monitor-gridlines">
              {HOURS.map((_, i) => (
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
                      left: `${((run.startHour - 9) / 3) * 100}%`,
                      width: `${(run.duration / 3) * 100}%`,
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
