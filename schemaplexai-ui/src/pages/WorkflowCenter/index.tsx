import { Card, Table, Button, Space, Tag, Input, message } from 'antd'
import { PlusOutlined, PlayCircleOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { getWorkflowList, runWorkflow } from '@/api/workflow'
import type { Workflow } from '@/api/workflow'

export default function WorkflowCenter() {
  const [keyword, setKeyword] = useState('')
  const [data, setData] = useState<Workflow[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const pageSize = 10

  useEffect(() => {
    fetchWorkflows()
  }, [keyword, page])

  const fetchWorkflows = async () => {
    setLoading(true)
    try {
      const res = await getWorkflowList({ page, pageSize, keyword })
      setData(res.list)
      setTotal(res.total)
    } catch (err) {
      const msg = err instanceof Error ? err.message : '获取工作流列表失败'
      message.error(msg)
      setData([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const handleRun = async (id: string) => {
    try {
      await runWorkflow(id)
      message.success('工作流已启动')
    } catch (err) {
      const msg = err instanceof Error ? err.message : '启动工作流失败'
      message.error(msg)
    }
  }

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
      render: (_: unknown, record: Workflow) => (
        <Space>
          <Button icon={<PlayCircleOutlined />} size="small" type="primary" ghost onClick={() => handleRun(record.id)}>运行</Button>
          <Button icon={<EditOutlined />} size="small">编辑</Button>
          <Button icon={<DeleteOutlined />} size="small" danger>删除</Button>
        </Space>
      ),
    },
  ]

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
        onSearch={(v) => { setKeyword(v); setPage(1) }}
      />
      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize,
          total,
          onChange: (p) => setPage(p),
        }}
      />
    </Card>
  )
}
