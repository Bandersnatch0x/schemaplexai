import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Row, Col, Card, Statistic, Table, Tag, message } from 'antd'
import {
  RobotOutlined,
  ThunderboltOutlined,
  DollarOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import { Line } from '@ant-design/charts'
import { getAgentStats, getExecutionRecords } from '@/api/agent'
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

  const chartData = [
    { date: '08-01', value: 45, type: t('dashboard.executionCount') },
    { date: '08-02', value: 52, type: t('dashboard.executionCount') },
    { date: '08-03', value: 38, type: t('dashboard.executionCount') },
    { date: '08-04', value: 65, type: t('dashboard.executionCount') },
    { date: '08-05', value: 48, type: t('dashboard.executionCount') },
    { date: '08-06', value: 70, type: t('dashboard.executionCount') },
    { date: '08-07', value: 55, type: t('dashboard.executionCount') },
  ]

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
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
