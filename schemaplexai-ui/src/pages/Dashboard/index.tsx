import { useEffect, useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Row, Col, Card, Statistic, Table, Tag, message } from 'antd'
import {
  RobotOutlined,
  ThunderboltOutlined,
  DollarOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import { Line } from '@ant-design/charts'
import { getAgentStats, getExecutionRecords } from '@/api/agent-engine'
import type { ExecutionRecord } from '@/types'
import './Dashboard.css'

export default function Dashboard() {
  const { t } = useTranslation()
  const [stats, setStats] = useState({
    totalAgents: 0,
    totalExecutions: 0,
    totalTokens: 0,
    pendingApprovals: 0,
  })
  const [records, setRecords] = useState<ExecutionRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [statsLoading, setStatsLoading] = useState(false)

  useEffect(() => {
    fetchStats()
    fetchRecords()
  }, [])

  const fetchStats = async () => {
    setStatsLoading(true)
    try {
      const data = await getAgentStats()
      setStats(data)
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('common.error')
      message.error(msg)
    } finally {
      setStatsLoading(false)
    }
  }

  const fetchRecords = async () => {
    setLoading(true)
    try {
      const data = await getExecutionRecords()
      setRecords(data)
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('common.error')
      message.error(msg)
      setRecords([])
    } finally {
      setLoading(false)
    }
  }

  const chartData = useMemo(() => {
    const counts = new Map<string, number>()
    records.forEach((r) => {
      const d = new Date(r.createdAt)
      const key = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      counts.set(key, (counts.get(key) || 0) + 1)
    })
    const sorted = Array.from(counts.entries()).sort((a, b) => a[0].localeCompare(b[0]))
    return sorted.map(([date, value]) => ({
      date,
      value,
      type: t('dashboard.executionCount'),
    }))
  }, [records, t])

  const columns = [
    { title: 'Agent', dataIndex: 'agentName', key: 'agentName' },
    { title: t('dashboard.prompt'), dataIndex: 'prompt', key: 'prompt', ellipsis: true },
    {
      title: t('dashboard.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag className={`dashboard-status-${status}`}>
          {status}
        </Tag>
      ),
    },
    { title: 'Token', dataIndex: 'tokenUsed', key: 'tokenUsed' },
    { title: t('dashboard.duration'), dataIndex: 'duration', key: 'duration' },
  ]

  return (
    <div className="dashboard-container">
      <Row gutter={[16, 16]} className="dashboard-stats-row">
        <Col xs={24} sm={12} lg={6}>
          <Card loading={statsLoading} className="dashboard-stat-card">
            <Statistic title={t('dashboard.agentCount')} value={stats.totalAgents} prefix={<RobotOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={statsLoading} className="dashboard-stat-card">
            <Statistic title={t('dashboard.executionCount')} value={stats.totalExecutions} prefix={<ThunderboltOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={statsLoading} className="dashboard-stat-card">
            <Statistic title={t('dashboard.tokenConsumption')} value={stats.totalTokens} prefix={<DollarOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={statsLoading} className="dashboard-stat-card dashboard-stat-card--warning">
            <Statistic title={t('dashboard.pendingApproval')} value={stats.pendingApprovals} prefix={<ClockCircleOutlined />} />
          </Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} className="dashboard-section-row">
        <Col xs={24} lg={12}>
          <Card title={t('dashboard.recentTrend')} className="dashboard-section-card">
            <Line
              data={chartData}
              xField="date"
              yField="value"
              seriesField="type"
              smooth
              height={250}
              legend={false}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title={t('dashboard.recentRecords')} className="dashboard-section-card">
            <Table
              dataSource={records}
              columns={columns}
              rowKey="id"
              loading={loading}
              pagination={false}
              size="small"
              locale={{ emptyText: t('common.noData') }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
