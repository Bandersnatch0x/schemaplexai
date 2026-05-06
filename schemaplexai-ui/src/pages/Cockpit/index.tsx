import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getAgentStats } from '@/api/agent'

interface CockpitStats {
  totalAgents: number
  totalExecutions: number
  totalTokens: number
  pendingApprovals: number
}

const COLOR_STRIP = {
  cyan: '#00d4aa',
  amber: '#ff9f43',
  red: '#ff4757',
}

export default function Cockpit() {
  const { t } = useTranslation()
  const [stats, setStats] = useState<CockpitStats>({
    totalAgents: 0,
    totalExecutions: 0,
    totalTokens: 0,
    pendingApprovals: 0,
  })
  const [loading, setLoading] = useState(false)
  const [apiStatus, setApiStatus] = useState<'connected' | 'disconnected'>('connected')

  useEffect(() => {
    fetchStats()
    const interval = setInterval(fetchStats, 30000)
    return () => clearInterval(interval)
  }, [])

  const fetchStats = async () => {
    setLoading(true)
    try {
      const data = await getAgentStats()
      setStats(data)
      setApiStatus('connected')
    } catch {
      setApiStatus('disconnected')
    } finally {
      setLoading(false)
    }
  }

  const statCards = [
    {
      value: loading ? '—' : stats.totalAgents.toString(),
      label: t('cockpit.activeAgents'),
      trend: '+3 this week',
      trendUp: true,
      color: 'cyan' as const,
    },
    {
      value: loading ? '—' : stats.totalExecutions.toLocaleString(),
      label: t('cockpit.executions'),
      trend: '+12%',
      trendUp: true,
      color: 'amber' as const,
    },
    {
      value: loading ? '—' : formatTokens(stats.totalTokens),
      label: t('cockpit.tokensUsed'),
      trend: '-5%',
      trendUp: false,
      color: 'cyan' as const,
    },
    {
      value: loading ? '—' : stats.pendingApprovals.toString(),
      label: t('cockpit.pendingReview'),
      trend: '2 urgent',
      trendUp: true,
      color: 'red' as const,
    },
  ]

  return (
    <div style={{ height: '100%', position: 'relative', overflow: 'hidden' }}>
      {/* Status bar */}
      <div
        style={{
          position: 'absolute',
          top: 12,
          left: '50%',
          transform: 'translateX(-50%)',
          background: 'rgba(17, 24, 39, 0.8)',
          border: '1px solid #1e2a33',
          borderRadius: 20,
          padding: '8px 24px',
          color: '#64748b',
          fontSize: 12,
          backdropFilter: 'blur(12px)',
          display: 'flex',
          gap: 20,
          alignItems: 'center',
          zIndex: 5,
        }}
      >
        <span style={{ color: apiStatus === 'connected' ? '#00d4aa' : '#ff4757' }}>
          {apiStatus === 'connected' ? `● ${t('common.live')}` : `● ${t('common.offline')}`}
        </span>
        <span style={{ color: '#1e2a33' }}>|</span>
        <span>{stats.totalAgents} {t('cockpit.agentsActive')}</span>
        <span style={{ color: '#1e2a33' }}>|</span>
        <span>{t('cockpit.lastSync')}: 2{t('cockpit.secondsAgo')}</span>
      </div>

      {/* API Disconnection Notice */}
      {apiStatus === 'disconnected' && (
        <div
          style={{
            position: 'absolute',
            top: 48,
            left: '50%',
            transform: 'translateX(-50%)',
            background: 'rgba(255,71,87,0.1)',
            border: '1px solid rgba(255,71,87,0.3)',
            borderRadius: 8,
            padding: '8px 16px',
            color: '#ff4757',
            fontSize: 11,
            zIndex: 5,
            textAlign: 'center',
          }}
        >
          {t('cockpit.backendUnavailable')}
        </div>
      )}

      {/* Center content */}
      <div
        style={{
          marginTop: 60,
          textAlign: 'center',
          position: 'relative',
          zIndex: 1,
        }}
      >
        <h1
          style={{
            fontSize: 32,
            fontWeight: 700,
            marginBottom: 8,
            color: '#e2e8f0',
            margin: 0,
            paddingTop: 40,
          }}
        >
          {t('cockpit.title')}
        </h1>
        <p style={{ color: '#64748b', fontSize: 14, margin: '8px 0 0 0' }}>
          {t('cockpit.subtitle')}
        </p>

        {/* Orbital visualization */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            height: 280,
            position: 'relative',
            marginTop: 40,
          }}
        >
          {/* Center circle */}
          <div
            style={{
              width: 120,
              height: 120,
              borderRadius: '50%',
              border: '2px solid #00d4aa',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 40px rgba(0,212,170,0.15)',
            }}
          >
            <div style={{ textAlign: 'center' }}>
              <div
                style={{
                  fontSize: 24,
                  fontWeight: 700,
                  color: '#e2e8f0',
                }}
              >
                {loading ? '—' : stats.totalAgents}
              </div>
              <div style={{ fontSize: 11, color: '#64748b' }}>{t('common.live')}</div>
            </div>
          </div>

          {/* Orbit ring */}
          <div
            style={{
              position: 'absolute',
              width: 200,
              height: 200,
              border: '1px dashed #1e2a33',
              borderRadius: '50%',
            }}
          />

          {/* Orbiting nodes */}
          <HexIcon
            label="A1"
            color="cyan"
            style={{
              position: 'absolute',
              top: 20,
              left: '50%',
              transform: 'translateX(-50%)',
            }}
          />
          <HexIcon
            label="A2"
            color="amber"
            style={{
              position: 'absolute',
              bottom: 20,
              left: '50%',
              transform: 'translateX(-50%)',
            }}
          />
          <HexIcon
            label="A3"
            color="cyan"
            style={{
              position: 'absolute',
              left: 20,
              top: '50%',
              transform: 'translateY(-50%)',
            }}
          />
          <HexIcon
            label="A4"
            color="cyan"
            style={{
              position: 'absolute',
              right: 20,
              top: '50%',
              transform: 'translateY(-50%)',
            }}
          />
        </div>
      </div>

      {/* Bottom stat cards */}
      <div
        style={{
          position: 'absolute',
          bottom: 24,
          left: 24,
          right: 24,
          zIndex: 1,
        }}
      >
        <div style={{ display: 'flex', gap: 16 }}>
          {statCards.map((card, index) => (
            <StatCard key={index} {...card} />
          ))}
        </div>
      </div>
    </div>
  )
}

function HexIcon({
  label,
  color,
  style,
}: {
  label: string
  color: 'cyan' | 'amber'
  style?: React.CSSProperties
}) {
  const bgColor =
    color === 'cyan' ? 'rgba(0,212,170,0.15)' : 'rgba(255,159,67,0.15)'
  const textColor = color === 'cyan' ? '#00d4aa' : '#ff9f43'

  return (
    <div
      style={{
        width: 28,
        height: 28,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        clipPath:
          'polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%)',
        fontSize: 10,
        fontWeight: 600,
        background: bgColor,
        color: textColor,
        ...style,
      }}
    >
      {label}
    </div>
  )
}

function StatCard({
  value,
  label,
  trend,
  trendUp,
  color,
}: {
  value: string
  label: string
  trend: string
  trendUp: boolean
  color: 'cyan' | 'amber' | 'red'
}) {
  const trendColor = trendUp
    ? color === 'red'
      ? '#ff4757'
      : '#00d4aa'
    : '#ff4757'

  return (
    <div
      style={{
        flex: 1,
        background: '#111827',
        borderRadius: 8,
        padding: 20,
        display: 'flex',
        alignItems: 'flex-start',
        gap: 16,
        border: '1px solid #1e2a33',
        boxShadow:
          color === 'cyan' ? '0 0 20px rgba(0,212,170,0.1)' : 'none',
      }}
    >
      <div
        style={{
          width: 3,
          height: 48,
          borderRadius: 2,
          flexShrink: 0,
          background: COLOR_STRIP[color],
        }}
      />
      <div>
        <div
          style={{
            fontSize: 28,
            fontWeight: 700,
            color: '#e2e8f0',
            lineHeight: 1,
          }}
        >
          {value}
        </div>
        <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>
          {label}
        </div>
        <div style={{ fontSize: 11, marginTop: 6, color: trendColor }}>
          {trend}
        </div>
      </div>
    </div>
  )
}

function formatTokens(n: number): string {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K'
  return n.toString()
}
