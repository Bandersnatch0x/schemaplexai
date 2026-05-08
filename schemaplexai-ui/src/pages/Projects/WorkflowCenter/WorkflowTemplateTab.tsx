import { useEffect, useState } from 'react'
import { Table, Button, Space, Tag, message } from 'antd'
import { PlusOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { getWorkflowList, runWorkflow, type Workflow } from '@/api/workflow'

export default function WorkflowTemplateTab() {
  const { t } = useTranslation()
  const [data, setData] = useState<Workflow[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [query, setQuery] = useState({ page: 1, pageSize: 10 })

  useEffect(() => {
    fetchData()
  }, [query])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getWorkflowList(query)
      setData(res.list)
      setTotal(res.total)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('workflowCenter.fetchError'))
    } finally {
      setLoading(false)
    }
  }

  const handleRun = async (id: string) => {
    try {
      await runWorkflow(id)
      message.success(t('workflowCenter.runSuccess'))
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('workflowCenter.runError'))
    }
  }

  const columns = [
    { title: t('workflowCenter.name'), dataIndex: 'name', key: 'name' },
    { title: t('workflowCenter.description'), dataIndex: 'description', key: 'description' },
    {
      title: t('workflowCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'published' ? 'green' : status === 'draft' ? 'blue' : 'default'}>
          {status}
        </Tag>
      ),
    },
    { title: t('workflowCenter.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: t('workflowCenter.action'),
      key: 'action',
      render: (_: unknown, record: Workflow) => (
        <Space>
          <Button icon={<PlayCircleOutlined />} onClick={() => handleRun(record.id)}>
            {t('workflowCenter.run')}
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />}>
          {t('workflowCenter.newWorkflow')}
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total,
          onChange: (page, pageSize) => setQuery({ page, pageSize: pageSize || 10 }),
        }}
        locale={{ emptyText: t('common.noData') }}
      />
    </div>
  )
}
