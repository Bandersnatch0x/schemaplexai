import { useState } from 'react'

type StatusFilter = 'all' | 'running' | 'completed' | 'failed'

const FILTERS: { key: StatusFilter; label: string }[] = [
  { key: 'all', label: 'All' },
  { key: 'running', label: 'Running' },
  { key: 'completed', label: 'Completed' },
  { key: 'failed', label: 'Failed' },
]

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
  const [filter, setFilter] = useState<StatusFilter>('all')

  const filteredRuns =
    filter === 'all' ? RUNS : RUNS.filter((r) => r.status === filter)

  const statusCount: Record<StatusFilter, number> = {
    all: RUNS.length,
    running: RUNS.filter((r) => r.status === 'running').length,
    completed: RUNS.filter((r) => r.status === 'completed').length,
    failed: RUNS.filter((r) => r.status === 'failed').length,
  }

  return (
    <div>
      {/* Stats Row */}
      <div
        style={{
          display: 'flex',
          gap: 16,
          marginBottom: 24,
        }}
      >
        <StatBadge label="Total" value={statusCount.all} color="#e2e8f0" />
        <StatBadge
          label="Running"
          value={statusCount.running}
          color="#00d4aa"
        />
        <StatBadge
          label="Completed"
          value={statusCount.completed}
          color="#64748b"
        />
        <StatBadge label="Failed" value={statusCount.failed} color="#ff4757" />
      </div>

      {/* Timeline Card */}
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
            padding: '16px 20px',
            borderBottom: '1px solid #1e2a33',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <div
            style={{
              fontSize: 14,
              fontWeight: 600,
              color: '#e2e8f0',
            }}
          >
            Timeline View
          </div>

          {/* PillNav */}
          <div
            style={{
              display: 'inline-flex',
              background: '#0d1117',
              borderRadius: 20,
              padding: 4,
              gap: 4,
            }}
          >
            {FILTERS.map((f) => (
              <button
                key={f.key}
                onClick={() => setFilter(f.key)}
                style={{
                  background:
                    filter === f.key ? '#00d4aa' : 'transparent',
                  color: filter === f.key ? '#0a0e1a' : '#64748b',
                  border: 'none',
                  padding: '6px 16px',
                  borderRadius: 16,
                  fontSize: 13,
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  fontWeight: filter === f.key ? 600 : 400,
                  fontFamily: 'inherit',
                }}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        <div style={{ padding: 20 }}>
          {/* Timeline Header */}
          <div
            style={{
              display: 'flex',
              marginBottom: 12,
              paddingLeft: 160,
            }}
          >
            {HOURS.map((h) => (
              <div
                key={h}
                style={{
                  flex: 1,
                  textAlign: 'center',
                  fontSize: 11,
                  color: '#64748b',
                }}
              >
                {h}
              </div>
            ))}
          </div>

          {/* Grid Lines */}
          <div style={{ position: 'relative' }}>
            <div
              style={{
                position: 'absolute',
                left: 160,
                right: 0,
                top: 0,
                bottom: 0,
                display: 'flex',
              }}
            >
              {HOURS.map((_, i) => (
                <div
                  key={i}
                  style={{
                    flex: 1,
                    borderLeft:
                      i > 0
                        ? '1px dashed rgba(30,42,51,0.5)'
                        : 'none',
                  }}
                />
              ))}
            </div>

            {/* Workflow Rows */}
            {filteredRuns.map((run) => (
              <div
                key={run.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  height: 40,
                  marginBottom: 8,
                  position: 'relative',
                  zIndex: 1,
                }}
              >
                <div
                  style={{
                    width: 140,
                    fontSize: 13,
                    color: '#e2e8f0',
                    fontWeight: 500,
                    paddingRight: 20,
                    textAlign: 'right',
                    flexShrink: 0,
                  }}
                >
                  {run.name}
                </div>
                <div style={{ flex: 1, position: 'relative', height: 24 }}>
                  <div
                    style={{
                      position: 'absolute',
                      left: `${((run.startHour - 9) / 3) * 100}%`,
                      width: `${(run.duration / 3) * 100}%`,
                      height: '100%',
                      background: run.color,
                      borderRadius: 4,
                      opacity: 0.8,
                      minWidth: 4,
                    }}
                  />
                </div>
              </div>
            ))}

            {filteredRuns.length === 0 && (
              <div
                style={{
                  textAlign: 'center',
                  padding: '40px 0',
                  color: '#64748b',
                  fontSize: 13,
                }}
              >
                No workflows match the selected filter
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Recent Runs Table */}
      <div
        style={{
          marginTop: 24,
          background: '#111827',
          borderRadius: 8,
          border: '1px solid #1e2a33',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid #1e2a33',
            fontSize: 14,
            fontWeight: 600,
            color: '#e2e8f0',
          }}
        >
          Recent Runs
        </div>
        <div style={{ padding: '0 20px' }}>
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: 13,
            }}
          >
            <thead>
              <tr>
                <th
                  style={{
                    padding: '12px 16px',
                    textAlign: 'left',
                    color: '#64748b',
                    fontWeight: 500,
                    borderBottom: '1px solid #1e2a33',
                  }}
                >
                  Workflow
                </th>
                <th
                  style={{
                    padding: '12px 16px',
                    textAlign: 'left',
                    color: '#64748b',
                    fontWeight: 500,
                    borderBottom: '1px solid #1e2a33',
                  }}
                >
                  Status
                </th>
                <th
                  style={{
                    padding: '12px 16px',
                    textAlign: 'left',
                    color: '#64748b',
                    fontWeight: 500,
                    borderBottom: '1px solid #1e2a33',
                  }}
                >
                  Started
                </th>
                <th
                  style={{
                    padding: '12px 16px',
                    textAlign: 'left',
                    color: '#64748b',
                    fontWeight: 500,
                    borderBottom: '1px solid #1e2a33',
                  }}
                >
                  Duration
                </th>
              </tr>
            </thead>
            <tbody>
              {filteredRuns.map((run) => (
                <tr key={run.id}>
                  <td
                    style={{
                      padding: '12px 16px',
                      borderBottom: '1px solid rgba(30,42,51,0.5)',
                      color: '#e2e8f0',
                    }}
                  >
                    {run.name}
                  </td>
                  <td
                    style={{
                      padding: '12px 16px',
                      borderBottom: '1px solid rgba(30,42,51,0.5)',
                    }}
                  >
                    <StatusBadge status={run.status} />
                  </td>
                  <td
                    style={{
                      padding: '12px 16px',
                      borderBottom: '1px solid rgba(30,42,51,0.5)',
                      color: '#64748b',
                    }}
                  >
                    {String(Math.floor(run.startHour)).padStart(2, '0')}:
                    {String(
                      Math.floor((run.startHour % 1) * 60)
                    ).padStart(2, '0')}
                  </td>
                  <td
                    style={{
                      padding: '12px 16px',
                      borderBottom: '1px solid rgba(30,42,51,0.5)',
                      color: '#64748b',
                    }}
                  >
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
  color,
}: {
  label: string
  value: number
  color: string
}) {
  return (
    <div
      style={{
        flex: 1,
        background: '#111827',
        borderRadius: 8,
        border: '1px solid #1e2a33',
        padding: '16px 20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}
    >
      <span style={{ fontSize: 13, color: '#64748b' }}>{label}</span>
      <span style={{ fontSize: 20, fontWeight: 700, color }}>{value}</span>
    </div>
  )
}

function StatusBadge({ status }: { status: StatusFilter }) {
  const colorMap: Record<StatusFilter, string> = {
    running: '#00d4aa',
    completed: '#64748b',
    failed: '#ff4757',
    all: '#64748b',
  }

  const labelMap: Record<StatusFilter, string> = {
    running: 'Running',
    completed: 'Completed',
    failed: 'Failed',
    all: 'All',
  }

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '2px 8px',
        borderRadius: 4,
        fontSize: 11,
        fontWeight: 500,
        background: `${colorMap[status]}20`,
        color: colorMap[status],
      }}
    >
      {labelMap[status]}
    </span>
  )
}
