import { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Card, Drawer } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { getAgentList, createAgent, updateAgent, deleteAgent } from '@/api/agent-config'
import { useAgentStore } from '@/stores/agentStore'
import type { Agent } from '@/types'
import type { CreateAgentPayload } from '@/api/agent-config'
import './AgentManager.css'

const { Option } = Select

export default function AgentManager() {
  const { t } = useTranslation()
  const { agents, setAgents, loading, setLoading, updateAgentInList, removeAgentFromList } = useAgentStore()
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingAgent, setEditingAgent] = useState<Agent | null>(null)
  const [detailAgent, setDetailAgent] = useState<Agent | null>(null)
  const [form] = Form.useForm()
  const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: '' })
  const [total, setTotal] = useState(0)

  useEffect(() => {
    fetchAgents()
  }, [query])

  const fetchAgents = async () => {
    setLoading(true)
    try {
      const data = await getAgentList(query)
      setAgents(data.list)
      setTotal(data.total)
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('agentManager.fetchError')
      message.error(msg)
      setAgents([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (values: CreateAgentPayload) => {
    try {
      if (editingAgent) {
        const updated = await updateAgent(editingAgent.id, values)
        updateAgentInList(updated)
        message.success(t('agentManager.updateSuccess'))
      } else {
        const created = await createAgent(values)
        setAgents([...agents, created])
        setTotal(total + 1)
        message.success(t('agentManager.createSuccess'))
      }
      setIsModalOpen(false)
      form.resetFields()
      setEditingAgent(null)
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('agentManager.operationError')
      message.error(msg)
    }
  }

  const handleDelete = async (id: string) => {
    Modal.confirm({
      title: t('agentManager.confirmDelete'),
      content: t('agentManager.deleteWarning'),
      onOk: async () => {
        try {
          await deleteAgent(id)
          removeAgentFromList(id)
          setTotal(total - 1)
          message.success(t('agentManager.deleteSuccess'))
        } catch (err) {
          const msg = err instanceof Error ? err.message : t('agentManager.deleteError')
          message.error(msg)
        }
      },
    })
  }

  const typeMap: Record<string, string> = {
    review: t('agentManager.typeReview'),
    test: t('agentManager.typeTest'),
    doc: t('agentManager.typeDoc'),
    chat: t('agentManager.typeChat'),
    custom: t('agentManager.typeCustom'),
  }

  const statusMap: Record<string, string> = {
    active: t('agentManager.statusActive'),
    inactive: t('agentManager.statusInactive'),
    draft: t('agentManager.statusDraft'),
  }

  const columns = [
    { title: t('agentManager.name'), dataIndex: 'name', key: 'name' },
    { title: t('agentManager.description'), dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: t('agentManager.type'),
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => typeMap[type] || type,
    },
    {
      title: t('agentManager.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag className={`agent-mgr-status-${status}`}>
          {statusMap[status] || status}
        </Tag>
      ),
    },
    { title: t('agentManager.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: t('agentManager.action'),
      key: 'action',
      render: (_: unknown, record: Agent) => (
        <Space>
          <Button icon={<EyeOutlined />} size="small" className="agent-mgr-action-btn" onClick={() => setDetailAgent(record)} />
          <Button icon={<EditOutlined />} size="small" className="agent-mgr-action-btn" onClick={() => { setEditingAgent(record); form.setFieldsValue(record); setIsModalOpen(true) }} />
          <Button icon={<DeleteOutlined />} size="small" className="agent-mgr-action-btn agent-mgr-action-btn--danger" onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ]

  return (
    <div className="agent-mgr-container">
      <Card
        title={t('agentManager.title')}
        className="agent-mgr-card"
        extra={
          <Button type="primary" icon={<PlusOutlined />} className="agent-mgr-btn-create" onClick={() => { setEditingAgent(null); form.resetFields(); setIsModalOpen(true) }}>
            {t('agentManager.newAgent')}
          </Button>
        }
      >
        <Space className="agent-mgr-search">
          <Input.Search
            placeholder={t('agentManager.searchPlaceholder')}
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
          className="agent-mgr-table"
          locale={{ emptyText: t('common.noData') }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total,
            onChange: (page, pageSize) => setQuery({ ...query, page, pageSize: pageSize || 10 }),
          }}
        />
      </Card>

      <Modal
        title={editingAgent ? t('agentManager.editAgent') : t('agentManager.newAgent')}
        open={isModalOpen}
        onOk={() => form.submit()}
        onCancel={() => { setIsModalOpen(false); form.resetFields(); setEditingAgent(null) }}
        destroyOnClose
        className="agent-mgr-modal"
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="name" label={t('agentManager.name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t('agentManager.description')}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="type" label={t('agentManager.type')} rules={[{ required: true }]}>
            <Select placeholder={t('agentManager.selectType')}>
              <Option value="review">{t('agentManager.typeReview')}</Option>
              <Option value="test">{t('agentManager.typeTest')}</Option>
              <Option value="doc">{t('agentManager.typeDoc')}</Option>
              <Option value="chat">{t('agentManager.typeChat')}</Option>
              <Option value="custom">{t('agentManager.typeCustom')}</Option>
            </Select>
          </Form.Item>
          <Form.Item name="status" label={t('agentManager.status')} rules={[{ required: true }]}>
            <Select placeholder={t('agentManager.selectStatus')}>
              <Option value="active">{t('agentManager.statusActive')}</Option>
              <Option value="inactive">{t('agentManager.statusInactive')}</Option>
              <Option value="draft">{t('agentManager.statusDraft')}</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Drawer title={t('agentManager.detailTitle')} width={480} open={!!detailAgent} onClose={() => setDetailAgent(null)} className="agent-mgr-drawer">
        {detailAgent && (
          <div className="agent-mgr-detail">
            <p><strong>{t('agentManager.id')}:</strong> {detailAgent.id}</p>
            <p><strong>{t('agentManager.name')}:</strong> {detailAgent.name}</p>
            <p><strong>{t('agentManager.description')}:</strong> {detailAgent.description || '-'}</p>
            <p><strong>{t('agentManager.type')}:</strong> {typeMap[detailAgent.type] || detailAgent.type}</p>
            <p><strong>{t('agentManager.status')}:</strong> {statusMap[detailAgent.status] || detailAgent.status}</p>
            <p><strong>{t('agentManager.createdAt')}:</strong> {detailAgent.createdAt}</p>
            <p><strong>{t('agentManager.updatedAt')}:</strong> {detailAgent.updatedAt}</p>
            {detailAgent.modelConfig && (
              <div className="agent-mgr-detail-section">
                <p><strong>{t('agentManager.model')}:</strong> {detailAgent.modelConfig.model}</p>
                <p><strong>{t('agentManager.temperature')}:</strong> {detailAgent.modelConfig.temperature}</p>
                <p><strong>{t('agentManager.maxTokens')}:</strong> {detailAgent.modelConfig.maxTokens}</p>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}
