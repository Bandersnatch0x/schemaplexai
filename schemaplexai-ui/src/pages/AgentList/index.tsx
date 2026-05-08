import { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Card, Drawer } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { getAgentList, createAgent, updateAgent, deleteAgent } from '@/api/agent-config'
import { useAgentStore } from '@/stores/agentStore'
import type { Agent } from '@/types'
import type { CreateAgentPayload } from '@/api/agent-config'
import './AgentList.css'

const { Option } = Select

export default function AgentList() {
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
      const msg = err instanceof Error ? err.message : t('agentList.fetchError')
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
        message.success(t('agentList.updateSuccess'))
      } else {
        const created = await createAgent(values)
        setAgents([...agents, created])
        setTotal(total + 1)
        message.success(t('agentList.createSuccess'))
      }
      setIsModalOpen(false)
      form.resetFields()
      setEditingAgent(null)
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('agentList.operationError')
      message.error(msg)
    }
  }

  const handleDelete = async (id: string) => {
    Modal.confirm({
      title: t('agentList.confirmDelete'),
      content: t('agentList.deleteWarning'),
      onOk: async () => {
        try {
          await deleteAgent(id)
          removeAgentFromList(id)
          setTotal(total - 1)
          message.success(t('agentList.deleteSuccess'))
        } catch (err) {
          const msg = err instanceof Error ? err.message : t('agentList.deleteError')
          message.error(msg)
        }
      },
    })
  }

  const typeMap: Record<string, string> = {
    review: t('agentList.typeReview'),
    test: t('agentList.typeTest'),
    doc: t('agentList.typeDoc'),
    chat: t('agentList.typeChat'),
    custom: t('agentList.typeCustom'),
  }

  const statusMap: Record<string, string> = {
    active: t('agentList.statusActive'),
    inactive: t('agentList.statusInactive'),
    draft: t('agentList.statusDraft'),
  }

  const columns = [
    { title: t('agentList.name'), dataIndex: 'name', key: 'name' },
    { title: t('agentList.description'), dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: t('agentList.type'),
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => typeMap[type] || type,
    },
    {
      title: t('agentList.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag className={`agent-mgr-status-${status}`}>
          {statusMap[status] || status}
        </Tag>
      ),
    },
    { title: t('agentList.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: t('agentList.action'),
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
    <div className="agent-list-page">
      <Card
        title={t('agentList.title')}
        className="agent-mgr-card"
        extra={
          <Button type="primary" icon={<PlusOutlined />} className="agent-mgr-btn-create" onClick={() => { setEditingAgent(null); form.resetFields(); setIsModalOpen(true) }}>
            {t('agentList.newAgent')}
          </Button>
        }
      >
        <Space className="agent-mgr-search">
          <Input.Search
            placeholder={t('agentList.searchPlaceholder')}
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
        title={editingAgent ? t('agentList.editAgent') : t('agentList.newAgent')}
        open={isModalOpen}
        onOk={() => form.submit()}
        onCancel={() => { setIsModalOpen(false); form.resetFields(); setEditingAgent(null) }}
        destroyOnClose
        className="agent-mgr-modal"
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="name" label={t('agentList.name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t('agentList.description')}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="type" label={t('agentList.type')} rules={[{ required: true }]}>
            <Select placeholder={t('agentList.selectType')}>
              <Option value="review">{t('agentList.typeReview')}</Option>
              <Option value="test">{t('agentList.typeTest')}</Option>
              <Option value="doc">{t('agentList.typeDoc')}</Option>
              <Option value="chat">{t('agentList.typeChat')}</Option>
              <Option value="custom">{t('agentList.typeCustom')}</Option>
            </Select>
          </Form.Item>
          <Form.Item name="status" label={t('agentList.status')} rules={[{ required: true }]}>
            <Select placeholder={t('agentList.selectStatus')}>
              <Option value="active">{t('agentList.statusActive')}</Option>
              <Option value="inactive">{t('agentList.statusInactive')}</Option>
              <Option value="draft">{t('agentList.statusDraft')}</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Drawer title={t('agentList.detailTitle')} width={480} open={!!detailAgent} onClose={() => setDetailAgent(null)} className="agent-mgr-drawer">
        {detailAgent && (
          <div className="agent-mgr-detail">
            <p><strong>{t('agentList.id')}:</strong> {detailAgent.id}</p>
            <p><strong>{t('agentList.name')}:</strong> {detailAgent.name}</p>
            <p><strong>{t('agentList.description')}:</strong> {detailAgent.description || '-'}</p>
            <p><strong>{t('agentList.type')}:</strong> {typeMap[detailAgent.type] || detailAgent.type}</p>
            <p><strong>{t('agentList.status')}:</strong> {statusMap[detailAgent.status] || detailAgent.status}</p>
            <p><strong>{t('agentList.createdAt')}:</strong> {detailAgent.createdAt}</p>
            <p><strong>{t('agentList.updatedAt')}:</strong> {detailAgent.updatedAt}</p>
            {detailAgent.modelConfig && (
              <div className="agent-mgr-detail-section">
                <p><strong>{t('agentList.model')}:</strong> {detailAgent.modelConfig.model}</p>
                <p><strong>{t('agentList.temperature')}:</strong> {detailAgent.modelConfig.temperature}</p>
                <p><strong>{t('agentList.maxTokens')}:</strong> {detailAgent.modelConfig.maxTokens}</p>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}
