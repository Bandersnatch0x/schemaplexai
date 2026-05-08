import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getAgentStats } from '@/api/agent-engine'
import { HexIcon, StatCard } from '@/components/Hive'
import './Cockpit.css'

interface CockpitStats {
  totalAgents: number
  totalExecutions: number
  totalTokens: number
  pendingApprovals: number
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
      unit: '+3 this week',
      color: 'cyan' as const,
    },
    {
      value: loading ? '—' : stats.totalExecutions.toLocaleString(),
      label: t('cockpit.executions'),
      change: 12,
      color: 'amber' as const,
    },
    {
      value: loading ? '—' : formatTokens(stats.totalTokens),
      label: t('cockpit.tokensUsed'),
      change: -5,
      color: 'cyan' as const,
    },
    {
      value: loading ? '—' : stats.pendingApprovals.toString(),
      label: t('cockpit.pendingReview'),
      unit: '2 urgent',
      color: 'red' as const,
    },
  ]

  return (
    <div className="cockpit-container">
      {/* Status bar */}
      <div className="cockpit-status-bar">
        <span className={apiStatus === 'connected' ? 'status-connected' : 'status-disconnected'}>
          {apiStatus === 'connected' ? `● ${t('common.live')}` : `● ${t('common.offline')}`}
        </span>
        <span className="divider">|</span>
        <span>{stats.totalAgents} {t('cockpit.agentsActive')}</span>
        <span className="divider">|</span>
        <span>{t('cockpit.lastSync')}: 2{t('cockpit.secondsAgo')}</span>
      </div>

      {/* API Disconnection Notice */}
      {apiStatus === 'disconnected' && (
        <div className="cockpit-disconnect-notice">
          {t('cockpit.backendUnavailable')}
        </div>
      )}

      {/* Center content */}
      <div className="cockpit-center">
        <h1 className="cockpit-title">{t('cockpit.title')}</h1>
        <p className="cockpit-subtitle">{t('cockpit.subtitle')}</p>

        {/* Orbital visualization */}
        <div className="cockpit-orbit">
          {/* Center circle */}
          <div className="cockpit-orbit-center">
            <div className="cockpit-orbit-center-inner">
              <div className="cockpit-orbit-center-value">
                {loading ? '—' : stats.totalAgents}
              </div>
              <div className="cockpit-orbit-center-label">{t('common.live')}</div>
            </div>
          </div>

          {/* Orbit ring */}
          <div className="cockpit-orbit-ring" />

          {/* Orbiting nodes */}
          <HexIcon size={28} color="#00d4aa" className="cockpit-hex-top">A1</HexIcon>
          <HexIcon size={28} color="#ff9f43" className="cockpit-hex-bottom">A2</HexIcon>
          <HexIcon size={28} color="#00d4aa" className="cockpit-hex-left">A3</HexIcon>
          <HexIcon size={28} color="#00d4aa" className="cockpit-hex-right">A4</HexIcon>
        </div>
      </div>

      {/* Bottom stat cards */}
      <div className="cockpit-bottom-cards">
        <div className="cockpit-cards-row">
          {statCards.map((card, index) => (
            <StatCard key={index} {...card} />
          ))}
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
