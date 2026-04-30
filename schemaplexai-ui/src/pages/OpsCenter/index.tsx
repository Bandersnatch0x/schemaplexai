import { Card, Table, Button, Space, Tag, Timeline } from 'antd'
import { PlusOutlined } from '@ant-design/icons'

const taskData = [
  { id: '1', title: '部署 v2.0.0', status: 'completed', type: 'deploy', assignee: '张三', createdAt: '2024-08-01' },
  { id: '2', title: '修复生产 Bug', status: 'running', type: 'incident', assignee: '李四', createdAt: '2024-08-02' },
  { id: '3', title: '性能优化', status: 'pending', type: 'optimize', assignee: '王五', createdAt: '2024-08-03' },
]

export default function OpsCenter() {
  const columns = [
    { title: '任务', dataIndex: 'title', key: 'title' },
    { title: '类型', dataIndex: 'type', key: 'type' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const colorMap: Record<string, string> = { completed: 'green', running: 'blue', pending: 'orange', failed: 'red' }
        const textMap: Record<string, string> = { completed: '已完成', running: '进行中', pending: '待处理', failed: '失败' }
        return <Tag color={colorMap[status]}>{textMap[status] || status}</Tag>
      },
    },
    { title: '负责人', dataIndex: 'assignee', key: 'assignee' },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">详情</Button>
          <Button type="link">编辑</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Card
        title="交付与运营"
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            新建任务
          </Button>
        }
        style={{ marginBottom: 16 }}
      >
        <Table dataSource={taskData} columns={columns} rowKey="id" />
      </Card>
      <Card title="运营时间线">
        <Timeline
          items={[
            { children: '2024-08-01 完成 v2.0.0 部署' },
            { children: '2024-07-28 系统性能优化完成' },
            { children: '2024-07-20 新增 GitHub 集成' },
          ]}
        />
      </Card>
    </div>
  )
}
