import { useEffect, useState } from 'react'
import { Table, Switch, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getModelConfigs, updateModelConfig, type ModelConfigItem } from '@/api/system'

export default function SystemModelsTab() {
  const { t } = useTranslation()
  const [data, setData] = useState<ModelConfigItem[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getModelConfigs()
      setData(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('systemCenter.fetchError'))
    } finally {
      setLoading(false)
    }
  }

  const toggleEnabled = async (id: string, enabled: boolean) => {
    try {
      await updateModelConfig(id, { enabled })
      setData((prev) => prev.map((item) => (item.id === id ? { ...item, enabled } : item)))
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('systemCenter.fetchError'))
    }
  }

  const columns = [
    { title: 'Provider', dataIndex: 'provider', key: 'provider' },
    { title: 'Model', dataIndex: 'model', key: 'model' },
    { title: 'Priority', dataIndex: 'priority', key: 'priority' },
    {
      title: 'Enabled',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean, record: ModelConfigItem) => (
        <Switch checked={enabled} onChange={(checked) => toggleEnabled(record.id, checked)} />
      ),
    },
  ]

  return <Table rowKey="id" columns={columns} dataSource={data} loading={loading} locale={{ emptyText: t('common.noData') }} />
}
