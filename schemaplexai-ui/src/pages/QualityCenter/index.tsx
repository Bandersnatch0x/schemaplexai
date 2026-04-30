import { Card, Table, Tag, Statistic, Row, Col } from 'antd'
import { CheckCircleOutlined, ExclamationCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'

const mockData = [
  { id: '1', name: '代码规范检查', category: '规范', status: 'pass', score: 95, checkedAt: '2024-08-01' },
  { id: '2', name: '安全漏洞扫描', category: '安全', status: 'warn', score: 78, checkedAt: '2024-08-01' },
  { id: '3', name: '性能基准测试', category: '性能', status: 'fail', score: 45, checkedAt: '2024-07-30' },
]

export default function QualityCenter() {
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

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card><Statistic title="总检查项" value={mockData.length} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="通过" value={mockData.filter((d) => d.status === 'pass').length} valueStyle={{ color: '#3f8600' }} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="待修复" value={mockData.filter((d) => d.status !== 'pass').length} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
      </Row>
      <Card title="质量与安全检查">
        <Table dataSource={mockData} columns={columns} rowKey="id" />
      </Card>
    </div>
  )
}
