import { Card, Table, Button, Space, Tag, Input } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useState } from 'react'

const mockData = [
  { id: '1', name: 'API 规范 v1.0', version: '1.0.0', type: 'api', status: 'published', createdAt: '2024-07-01', updatedAt: '2024-07-15' },
  { id: '2', name: '数据模型规范', version: '2.1.0', type: 'model', status: 'published', createdAt: '2024-06-20', updatedAt: '2024-07-10' },
  { id: '3', name: 'UI 设计规范', version: '0.5.0', type: 'ui', status: 'draft', createdAt: '2024-07-05', updatedAt: '2024-07-05' },
]

export default function SpecCenter() {
  const [keyword, setKeyword] = useState('')

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '版本', dataIndex: 'version', key: 'version' },
    { title: '类型', dataIndex: 'type', key: 'type' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <Tag color={status === 'published' ? 'green' : 'orange'}>{status}</Tag>,
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">查看</Button>
          <Button type="link">编辑</Button>
        </Space>
      ),
    },
  ]

  const filtered = mockData.filter((d) => d.name.includes(keyword))

  return (
    <Card
      title="Spec 规范中心"
      extra={
        <Button type="primary" icon={<PlusOutlined />}>
          新建规范
        </Button>
      }
    >
      <Input.Search
        placeholder="搜索规范"
        allowClear
        style={{ width: 300, marginBottom: 16 }}
        onSearch={setKeyword}
      />
      <Table dataSource={filtered} columns={columns} rowKey="id" />
    </Card>
  )
}
