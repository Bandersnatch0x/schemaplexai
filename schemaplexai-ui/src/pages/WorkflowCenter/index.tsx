import { Card, Table, Button, Space, Tag, Input, message } from 'antd'
import { PlusOutlined, PlayCircleOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getWorkflowList, runWorkflow } from '@/api/workflow'
import type { Workflow } from '@/api/workflow'
import './WorkflowCenter.css'

export default function WorkflowCenter() {
  const { t } = useTranslation()
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
      const msg = err instanceof Error ? err.message : t('workflowCenter.fetchError')
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
      message.success(t('workflowCenter.runSuccess'))
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('workflowCenter.runError')
      message.error(msg)
    }
  }

  const columns = [
    { title: t('workflowCenter.name'), dataIndex: 'name', key: 'name' },
    { title: t('workflowCenter.description'), dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: t('workflowCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag className={status === 'published' ? 'workflow-tag-published' : 'workflow-tag-draft'}>
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
          <Button icon={<PlayCircleOutlined />} size="small" type="primary" ghost onClick={() => handleRun(record.id)}>{t('workflowCenter.run')}</Button>
          <Button icon={<EditOutlined />} size="small">{t('common.edit')}</Button>
          <Button icon={<DeleteOutlined />} size="small" danger>{t('common.delete')}</Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="workflow-page">
      <Card
        className="workflow-card"
        title={t('workflowCenter.title')}
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            {t('workflowCenter.newWorkflow')}
          </Button>
        }
      >
        <Input.Search
          className="workflow-search"
          placeholder={t('workflowCenter.searchPlaceholder')}
          allowClear
          onSearch={(v) => { setKeyword(v); setPage(1) }}
        />
        <Table
          className="workflow-table"
          dataSource={data}
          columns={columns}
          rowKey="id"
          loading={loading}
          locale={{ emptyText: t('common.noData') }}
          pagination={{
            current: page,
            pageSize,
            total,
            onChange: (p) => setPage(p),
          }}
        />
      </Card>
    </div>
  )
}
