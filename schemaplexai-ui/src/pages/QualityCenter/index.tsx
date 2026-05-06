import { Card, Table, Tag, Statistic, Row, Col, message } from 'antd'
import { CheckCircleOutlined, ExclamationCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { getQualityGates, getQualityIssues } from '@/api/quality'
import type { QualityGate, QualityIssue } from '@/api/quality'
import './QualityCenter.css'

interface DisplayItem {
  id: string
  name: string
  category: string
  status: string
  score: number
  checkedAt: string
}

export default function QualityCenter() {
  const [gates, setGates] = useState<QualityGate[]>([])
  const [issues, setIssues] = useState<QualityIssue[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const [gatesRes, issuesRes] = await Promise.all([
        getQualityGates(),
        getQualityIssues(),
      ])
      setGates(gatesRes)
      setIssues(issuesRes)
    } catch (err) {
      const msg = err instanceof Error ? err.message : '获取质量数据失败'
      message.error(msg)
      setGates([])
      setIssues([])
    } finally {
      setLoading(false)
    }
  }

  const displayData: DisplayItem[] = issues.length > 0
    ? issues.map((i) => ({
        id: i.id,
        name: i.title,
        category: i.category,
        status: i.status,
        score: i.score,
        checkedAt: i.checkedAt,
      }))
    : gates.map((g) => ({
        id: g.id,
        name: g.name,
        category: '质量门禁',
        status: g.status === 1 ? 'pass' : 'fail',
        score: g.status === 1 ? 95 : 45,
        checkedAt: g.updatedAt,
      }))

  const columns = [
    { title: '检查项', dataIndex: 'name', key: 'name' },
    { title: '类别', dataIndex: 'category', key: 'category' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const map: Record<string, { color: string; icon: React.ReactNode; text: string; className: string }> = {
          pass: { color: 'green', icon: <CheckCircleOutlined />, text: '通过', className: 'quality-tag--pass' },
          warn: { color: 'orange', icon: <ExclamationCircleOutlined />, text: '警告', className: 'quality-tag--warn' },
          fail: { color: 'red', icon: <CloseCircleOutlined />, text: '失败', className: 'quality-tag--fail' },
        }
        const item = map[status] || map.pass
        return <Tag color={item.color} icon={item.icon} className={item.className}>{item.text}</Tag>
      },
    },
    { title: '评分', dataIndex: 'score', key: 'score', render: (score: number) => <span className="quality-score">{score}</span> },
    { title: '检查时间', dataIndex: 'checkedAt', key: 'checkedAt' },
  ]

  const passCount = displayData.filter((d) => d.status === 'pass').length

  return (
    <div className="quality-page">
      <Row gutter={[16, 16]} className="quality-stats-row">
        <Col span={8}>
          <Card className="quality-stat-card"><Statistic title="总检查项" value={displayData.length} /></Card>
        </Col>
        <Col span={8}>
          <Card className="quality-stat-card quality-stat-card--pass"><Statistic title="通过" value={passCount} valueStyle={{ color: 'var(--hive-cyan)' }} /></Card>
        </Col>
        <Col span={8}>
          <Card className="quality-stat-card quality-stat-card--fail"><Statistic title="待修复" value={displayData.length - passCount} valueStyle={{ color: 'var(--hive-red)' }} /></Card>
        </Col>
      </Row>
      <Card title="质量与安全检查" className="quality-table-card">
        <Table dataSource={displayData} columns={columns} rowKey="id" loading={loading} pagination={false} />
      </Card>
    </div>
  )
}
