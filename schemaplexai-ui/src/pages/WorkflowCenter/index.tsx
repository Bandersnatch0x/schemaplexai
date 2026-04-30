import { Card, Table, Button, Space, Tag, Input } from 'antd'
import { PlusOutlined, PlayCircleOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useState } from 'react'

const mockData = [
  { id: '1', name: '代码评审流程', description: '自动代码评审与反馈', status: 'published', createdAt: '2024-07-01', updatedAt: '2024-07-15' },
  { id: '2', name: '测试生成流程', description: '根据代码自动生成测试', status: 'published', createdAt: '2024-06-20', updatedAt: '2024-07-10' },
  { id: '3', name: '文档同步流程', description: '代码变更同步文档', status: 'draft', createdAt: '2024-07-05', updatedAt: '2024-07-05' },
]

export default function WorkflowCenter() {
  const [keyword, setKeyword] = useState('')

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
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
          <Button icon={<PlayCircleOutlined />} size="small" type="primary" ghost>运行</Button>
          <Button icon={<EditOutlined />} size="small">编辑</Button>
          <Button icon={<DeleteOutlined />} size="small" danger>删除</Button>
        </Space>
      ),
    },
  ]

  const filtered = mockData.filter((d) => d.name.includes(keyword))

  return (
    <Card
      title="工作流中心"
      extra={
        <Button type="primary" icon={<PlusOutlined />}>
          新建工作流
        </Button>
      }
    >
      <Input.Search
        placeholder="搜索工作流"
        allowClear
        style={{ width: 300, marginBottom: 16 }}
        onSearch={setKeyword}
      />
      <Table dataSource={filtered} columns={columns} rowKey="id" />
    </Card>
  )
}
