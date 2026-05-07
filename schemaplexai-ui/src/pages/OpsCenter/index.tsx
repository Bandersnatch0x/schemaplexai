import { Card, Table, Button, Space, Tag, Timeline, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getArtifactList } from '@/api/ops'
import type { OpsArtifact } from '@/api/ops'
import './OpsCenter.css'

export default function OpsCenter() {
  const { t } = useTranslation()
  const [data, setData] = useState<OpsArtifact[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchArtifacts()
  }, [])

  const fetchArtifacts = async () => {
    setLoading(true)
    try {
      const res = await getArtifactList()
      setData(res)
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('opsCenter.fetchError')
      message.error(msg)
      setData([])
    } finally {
      setLoading(false)
    }
  }

  const statusMap: Record<number, { color: string; text: string; className: string }> = {
    0: { color: 'orange', text: t('opsCenter.pending'), className: 'ops-tag--pending' },
    1: { color: 'blue', text: t('opsCenter.running'), className: 'ops-tag--running' },
    2: { color: 'green', text: t('opsCenter.completed'), className: 'ops-tag--done' },
    3: { color: 'red', text: t('opsCenter.failed'), className: 'ops-tag--failed' },
  }

  const columns = [
    { title: t('opsCenter.task'), dataIndex: 'name', key: 'name' },
    { title: t('opsCenter.type'), dataIndex: 'artifactType', key: 'artifactType' },
    {
      title: t('opsCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => {
        const map = statusMap[status] || statusMap[0]
        return <Tag color={map.color} className={map.className}>{map.text}</Tag>
      },
    },
    { title: t('opsCenter.version'), dataIndex: 'version', key: 'version' },
    { title: t('opsCenter.createdAt'), dataIndex: 'createdAt', key: 'createdAt' },
    {
      title: t('opsCenter.action'),
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">{t('opsCenter.detail')}</Button>
          <Button type="link">{t('common.edit')}</Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="ops-page">
      <Card
        title={t('opsCenter.title')}
        extra={
          <Button type="primary" icon={<PlusOutlined />} className="ops-btn-primary">
            {t('opsCenter.newTask')}
          </Button>
        }
        className="ops-table-card"
      >
        <Table dataSource={data} columns={columns} rowKey="id" loading={loading} />
      </Card>
      <Card title={t('opsCenter.timelineTitle')} className="ops-timeline-card">
        <Timeline
          items={[
            { children: '2024-08-01 v2.0.0 deployed' },
            { children: '2024-07-28 Performance optimization completed' },
            { children: '2024-07-20 GitHub integration added' },
          ]}
        />
      </Card>
    </div>
  )
}
