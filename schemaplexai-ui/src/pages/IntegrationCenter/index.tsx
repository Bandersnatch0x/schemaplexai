import { Card, Table, Button, Space, Tag, Switch } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { useState } from 'react'

const mockData = [
  { id: '1', name: 'GitHub', type: 'git', status: 'connected', description: '代码仓库集成' },
  { id: '2', name: 'Jira', type: 'pm', status: 'connected', description: '项目管理集成' },
  { id: '3', name: 'Slack', type: 'im', status: 'disconnected', description: '消息通知集成' },
  { id: '4', name: 'SonarQube', type: 'quality', status: 'disconnected', description: '代码质量集成' },
]

export default function IntegrationCenter() {
  const [data, setData] = useState(mockData)

  const toggleStatus = (id: string) => {
    setData((prev) =>
      prev.map((d) =>
        d.id === id ? { ...d, status: d.status === 'connected' ? 'disconnected' : 'connected' } : d
      )
    )
  }

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '类型', dataIndex: 'type', key: 'type' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <Tag color={status === 'connected' ? 'green' : 'default'}>{status === 'connected' ? '已连接' : '未连接'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: (typeof mockData)[0]) => (
        <Space>
          <Switch checked={record.status === 'connected'} onChange={() => toggleStatus(record.id)} />
          <Button icon={<EditOutlined />} size="small">配置</Button>
        </Space>
      ),
    },
  ]

  return (
    <Card title="集成与工具">
      <Table dataSource={data} columns={columns} rowKey="id" />
    </Card>
  )
}
