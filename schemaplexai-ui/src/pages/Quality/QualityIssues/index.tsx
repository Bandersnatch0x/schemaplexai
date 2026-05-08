import { useEffect, useState } from 'react'
import { Card, Table, Tag, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getQualityIssues, type QualityIssue } from '@/api/quality'

export default function QualityIssues() {
  const { t } = useTranslation()
  const [data, setData] = useState<QualityIssue[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getQualityIssues()
      setData(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('qualityIssues.fetchError'))
      setData([])
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: t('qualityIssues.issueTitle'), dataIndex: 'title', key: 'title' },
    { title: t('qualityIssues.category'), dataIndex: 'category', key: 'category' },
    {
      title: t('qualityCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'resolved' ? 'green' : status === 'open' ? 'red' : 'orange'}>{status}</Tag>
      ),
    },
    { title: t('qualityCenter.score'), dataIndex: 'score', key: 'score' },
    { title: t('qualityCenter.checkedAt'), dataIndex: 'checkedAt', key: 'checkedAt' },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('qualityIssues.title')}</h2>
      <Card>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          locale={{ emptyText: t('common.noData') }}
        />
      </Card>
    </div>
  )
}
