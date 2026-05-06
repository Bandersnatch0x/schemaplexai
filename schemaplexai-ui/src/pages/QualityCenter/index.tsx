import { Card, Table, Tag, Statistic, Row, Col, message } from 'antd'
import { CheckCircleOutlined, ExclamationCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { getQualityGates, getQualityIssues } from '@/api/quality'
import type { QualityGate, QualityIssue } from '@/api/quality'

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
        const map: Record<string, { color: string; icon: React.ReactNode; text: string }> = {
          pass: { color: 'green', icon: <CheckCircleOutlined />, text: '通过' },
          warn: { color: 'orange', icon: <ExclamationCircleOutlined />, text: '警告' },
          fail: { color: 'red', icon: <CloseCircleOutlined />, text: '失败' },
        }
        const item = map[status] || map.pass
        return <Tag color={item.color} icon={item.icon}>{item.text}</Tag>
      },
    },
    { title: '评分', dataIndex: 'score', key: 'score' },
    { title: '检查时间', dataIndex: 'checkedAt', key: 'checkedAt' },
  ]

  const passCount = displayData.filter((d) => d.status === 'pass').length

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card><Statistic title="总检查项" value={displayData.length} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="通过" value={passCount} valueStyle={{ color: '#3f8600' }} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="待修复" value={displayData.length - passCount} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
      </Row>
      <Card title="质量与安全检查">
        <Table dataSource={displayData} columns={columns} rowKey="id" loading={loading} />
      </Card>
    </div>
  )
}
