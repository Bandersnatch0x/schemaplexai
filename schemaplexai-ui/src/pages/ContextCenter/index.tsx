import { Card, Table, Button, Space, Tag, Input } from 'antd'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { useState } from 'react'

const mockData = [
  { id: '1', name: '产品知识库', type: 'knowledge', createdAt: '2024-07-01', updatedAt: '2024-07-15' },
  { id: '2', name: '对话记忆-会话1', type: 'memory', createdAt: '2024-07-10', updatedAt: '2024-07-10' },
  { id: '3', name: 'API 设计文档', type: 'document', createdAt: '2024-06-20', updatedAt: '2024-07-05' },
]

export default function ContextCenter() {
  const [keyword, setKeyword] = useState('')

  const typeMap: Record<string, string> = {
    knowledge: '知识库',
    memory: '记忆',
    document: '文档',
  }

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => <Tag>{typeMap[type] || type}</Tag>,
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button icon={<SearchOutlined />} size="small">检索</Button>
          <Button type="link">编辑</Button>
          <Button type="link" danger>删除</Button>
        </Space>
      ),
    },
  ]

  const filtered = mockData.filter((d) => d.name.includes(keyword))

  return (
    <Card
      title="上下文与知识中心"
      extra={
        <Button type="primary" icon={<PlusOutlined />}>
          新建上下文
        </Button>
      }
    >
      <Input.Search
        placeholder="搜索上下文"
        allowClear
        style={{ width: 300, marginBottom: 16 }}
        onSearch={setKeyword}
      />
      <Table dataSource={filtered} columns={columns} rowKey="id" />
    </Card>
  )
}
