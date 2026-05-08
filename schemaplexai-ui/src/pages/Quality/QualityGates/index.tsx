import { useEffect, useState } from 'react'
import { Card, Table, Tag, Button, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getQualityGates, type QualityGate } from '@/api/quality'

export default function QualityGates() {
  const { t } = useTranslation()
  const [data, setData] = useState<QualityGate[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getQualityGates()
      setData(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('qualityGates.fetchError'))
      setData([])
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: t('qualityGates.name'), dataIndex: 'name', key: 'name' },
    {
      title: t('qualityCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>{status === 1 ? t('qualityCenter.passTag') : t('qualityCenter.failTag')}</Tag>
      ),
    },
    { title: t('qualityCenter.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt' },
  ]

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('qualityGates.title')}</h2>
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
