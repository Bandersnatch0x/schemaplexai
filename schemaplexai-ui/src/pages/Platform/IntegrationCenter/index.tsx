import { Card, Table, Button, Space, Tag, Switch, message, Tabs } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getIntegrationList, updateIntegration } from '@/api/integration'
import type { Integration } from '@/api/integration'
import SkillsTab from './SkillsTab'
import './IntegrationCenter.css'

export default function IntegrationCenter() {
  const { t } = useTranslation()
  const [data, setData] = useState<Integration[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchIntegrations()
  }, [])

  const fetchIntegrations = async () => {
    setLoading(true)
    try {
      const res = await getIntegrationList()
      setData(res)
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('integrationCenter.fetchError')
      message.error(msg)
      setData([])
    } finally {
      setLoading(false)
    }
  }

  const toggleStatus = async (id: string, currentStatus: number) => {
    const newStatus = currentStatus === 1 ? 0 : 1
    try {
      await updateIntegration(id, { status: newStatus })
      setData((prev) =>
        prev.map((d) =>
          d.id === id ? { ...d, status: newStatus } : d
        )
      )
      message.success(t('integrationCenter.updateSuccess'))
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('integrationCenter.updateError')
      message.error(msg)
    }
  }

  const columns = [
    { title: t('specCenter.name'), dataIndex: 'name', key: 'name' },
    { title: t('specCenter.type'), dataIndex: 'type', key: 'type' },
    {
      title: t('specCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag
          color={status === 1 ? 'green' : 'default'}
          className={status === 1 ? 'integration-tag--connected' : 'integration-tag--disconnected'}
        >
          {status === 1 ? t('integrationCenter.connected') : t('integrationCenter.disconnected')}
        </Tag>
      ),
    },
    {
      title: t('specCenter.action'),
      key: 'action',
      render: (_: unknown, record: Integration) => (
        <Space>
          <Switch checked={record.status === 1} onChange={() => toggleStatus(record.id, record.status)} />
          <Button icon={<EditOutlined />} size="small">{t('integrationCenter.configure')}</Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="integration-page">
      <Card className="integration-card">
        <Tabs
          defaultActiveKey="integrations"
          items={[
            {
              key: 'integrations',
              label: t('integrationCenter.integrationsTab'),
              children: (
                <Table dataSource={data} columns={columns} rowKey="id" loading={loading} locale={{ emptyText: t('common.noData') }} />
              ),
            },
            {
              key: 'skills',
              label: t('integrationCenter.skillsTab'),
              children: <SkillsTab />,
            },
          ]}
        />
      </Card>
    </div>
  )
}
