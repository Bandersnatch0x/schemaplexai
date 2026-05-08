import { useEffect, useState } from 'react'
import { Table, Button, Tag, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getJobList, retryJob, cancelJob, type JobRecord } from '@/api/task'

export default function TaskJobs() {
  const { t } = useTranslation()
  const [data, setData] = useState<JobRecord[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getJobList()
      setData(res.list)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskJobs.fetchError'))
    } finally {
      setLoading(false)
    }
  }

  const handleRetry = async (id: string) => {
    try {
      await retryJob(id)
      message.success(t('common.success'))
      fetchData()
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('common.error'))
    }
  }

  const handleCancel = async (id: string) => {
    try {
      await cancelJob(id)
      message.success(t('common.success'))
      fetchData()
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('common.error'))
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: t('taskJobs.queue'), dataIndex: 'queue', key: 'queue' },
    {
      title: t('agentManager.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'running' ? 'blue' : status === 'failed' ? 'red' : 'green'}>{status}</Tag>
      ),
    },
    { title: 'Retry', dataIndex: 'retryCount', key: 'retryCount', render: (r: number, record: JobRecord) => `${r}/${record.maxRetries}` },
    {
      title: t('workflowCenter.action'),
      key: 'action',
      render: (_: unknown, record: JobRecord) => (
        <>
          <Button size="small" onClick={() => handleRetry(record.id)} style={{ marginRight: 8 }}>
            {t('taskJobs.retry')}
          </Button>
          <Button size="small" danger onClick={() => handleCancel(record.id)}>
            {t('taskJobs.cancel')}
          </Button>
        </>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('taskJobs.title')}</h2>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        locale={{ emptyText: t('common.noData') }}
      />
    </div>
  )
}
