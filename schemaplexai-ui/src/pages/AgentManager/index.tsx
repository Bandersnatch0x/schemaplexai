import { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Card, Drawer } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons'
import { getAgentList, createAgent, updateAgent, deleteAgent } from '@/api/agent'
import { useAgentStore } from '@/stores/agentStore'
import type { Agent } from '@/types'
import type { CreateAgentPayload } from '@/api/agent'

const { Option } = Select

export default function AgentManager() {
  const { agents, setAgents, loading, setLoading, updateAgentInList, removeAgentFromList } = useAgentStore()
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingAgent, setEditingAgent] = useState<Agent | null>(null)
  const [detailAgent, setDetailAgent] = useState<Agent | null>(null)
  const [form] = Form.useForm()
  const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: '' })

  useEffect(() => {
    fetchAgents()
  }, [query])

  const fetchAgents = async () => {
    setLoading(true)
    try {
      const data = await getAgentList(query)
      setAgents(data.list)
    } catch {
      setAgents([
        { id: '1', name: 'CodeReviewer', description: '代码审查Agent', type: 'review', status: 'active', createdAt: '2024-07-01', updatedAt: '2024-07-15' },
        { id: '2', name: 'TestGenerator', description: '生成单元测试', type: 'test', status: 'active', createdAt: '2024-07-05', updatedAt: '2024-07-20' },
        { id: '3', name: 'DocWriter', description: '编写技术文档', type: 'doc', status: 'draft', createdAt: '2024-07-10', updatedAt: '2024-07-10' },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (values: CreateAgentPayload) => {
    try {
      if (editingAgent) {
        const updated = await updateAgent(editingAgent.id, values)
        updateAgentInList(updated)
        message.success('更新成功')
      } else {
        const created = await createAgent(values)
        setAgents([...agents, created])
        message.success('创建成功')
      }
      setIsModalOpen(false)
      form.resetFields()
      setEditingAgent(null)
    } catch {
      message.error('操作失败')
    }
  }

  const handleDelete = async (id: string) => {
    Modal.confirm({
      title: '确认删除?',
      content: '删除后不可恢复',
      onOk: async () => {
        try {
          await deleteAgent(id)
          removeAgentFromList(id)
          message.success('删除成功')
        } catch {
          message.error('删除失败')
        }
      },
    })
  }

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: '类型', dataIndex: 'type', key: 'type' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'active' ? 'green' : status === 'draft' ? 'orange' : 'default'}>
          {status}
        </Tag>
      ),
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Agent) => (
        <Space>
          <Button icon={<EyeOutlined />} size="small" onClick={() => setDetailAgent(record)} />
          <Button icon={<EditOutlined />} size="small" onClick={() => { setEditingAgent(record); form.setFieldsValue(record); setIsModalOpen(true) }} />
          <Button icon={<DeleteOutlined />} size="small" danger onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Card
        title="Agent 管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingAgent(null); form.resetFields(); setIsModalOpen(true) }}>
            新建 Agent
          </Button>
        }
      >
        <Space style={{ marginBottom: 16 }}>
          <Input.Search
            placeholder="搜索 Agent"
            allowClear
            onSearch={(v) => setQuery({ ...query, keyword: v, page: 1 })}
            style={{ width: 300 }}
          />
        </Space>
        <Table
          dataSource={agents}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: agents.length,
            onChange: (page, pageSize) => setQuery({ ...query, page, pageSize }),
          }}
        />
      </Card>

      <Modal
        title={editingAgent ? '编辑 Agent' : '新建 Agent'}
        open={isModalOpen}
        onOk={() => form.submit()}
        onCancel={() => { setIsModalOpen(false); form.resetFields(); setEditingAgent(null) }}
        destroyOnClose
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true }]}>
            <Select placeholder="选择类型">
              <Option value="review">代码审查</Option>
              <Option value="test">测试生成</Option>
              <Option value="doc">文档编写</Option>
              <Option value="chat">对话</Option>
              <Option value="custom">自定义</Option>
            </Select>
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select placeholder="选择状态">
              <Option value="active">活跃</Option>
              <Option value="inactive">停用</Option>
              <Option value="draft">草稿</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Drawer title="Agent 详情" width={480} open={!!detailAgent} onClose={() => setDetailAgent(null)}>
        {detailAgent && (
          <div>
            <p><strong>ID:</strong> {detailAgent.id}</p>
            <p><strong>名称:</strong> {detailAgent.name}</p>
            <p><strong>描述:</strong> {detailAgent.description || '-'}</p>
            <p><strong>类型:</strong> {detailAgent.type}</p>
            <p><strong>状态:</strong> {detailAgent.status}</p>
            <p><strong>创建时间:</strong> {detailAgent.createdAt}</p>
            <p><strong>更新时间:</strong> {detailAgent.updatedAt}</p>
            {detailAgent.modelConfig && (
              <div>
                <p><strong>模型:</strong> {detailAgent.modelConfig.model}</p>
                <p><strong>Temperature:</strong> {detailAgent.modelConfig.temperature}</p>
                <p><strong>Max Tokens:</strong> {detailAgent.modelConfig.maxTokens}</p>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}
