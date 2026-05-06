import { useEffect, useState } from 'react'
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

export default function Dashboard() {
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
      const msg = err instanceof Error ? err.message : '获取统计数据失败'
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
      const msg = err instanceof Error ? err.message : '获取执行记录失败'
      message.error(msg)
      setRecords([])
    } finally {
      setLoading(false)
    }
  }

  const chartData = [
    { date: '08-01', value: 45, type: '执行次数' },
    { date: '08-02', value: 52, type: '执行次数' },
    { date: '08-03', value: 38, type: '执行次数' },
    { date: '08-04', value: 65, type: '执行次数' },
    { date: '08-05', value: 48, type: '执行次数' },
    { date: '08-06', value: 70, type: '执行次数' },
    { date: '08-07', value: 55, type: '执行次数' },
  ]

  const columns = [
    { title: 'Agent', dataIndex: 'agentName', key: 'agentName' },
    { title: 'Prompt', dataIndex: 'prompt', key: 'prompt', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'success' ? 'green' : status === 'running' ? 'blue' : 'red'}>
          {status}
        </Tag>
      ),
    },
    { title: 'Token', dataIndex: 'tokenUsed', key: 'tokenUsed' },
    { title: '耗时(ms)', dataIndex: 'duration', key: 'duration' },
  ]

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={statsLoading}>
            <Statistic title="Agent 数量" value={stats.totalAgents} prefix={<RobotOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={statsLoading}>
            <Statistic title="执行次数" value={stats.totalExecutions} prefix={<ThunderboltOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={statsLoading}>
            <Statistic title="Token 消耗" value={stats.totalTokens} prefix={<DollarOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={statsLoading}>
            <Statistic title="待处理审批" value={stats.pendingApprovals} prefix={<ClockCircleOutlined />} />
          </Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="最近7天执行趋势">
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
          <Card title="最近执行记录">
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
