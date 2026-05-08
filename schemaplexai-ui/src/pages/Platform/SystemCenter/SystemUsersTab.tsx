import { useEffect, useState } from 'react'
import { Table, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getUserList, type User } from '@/api/system'

export default function SystemUsersTab() {
  const { t } = useTranslation()
  const [data, setData] = useState<User[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getUserList()
      setData(res.list)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('systemCenter.fetchError'))
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: t('agentManager.name'), dataIndex: 'username', key: 'username' },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    { title: t('agentManager.status'), dataIndex: 'status', key: 'status' },
  ]

  return <Table rowKey="id" columns={columns} dataSource={data} loading={loading} locale={{ emptyText: t('common.noData') }} />
}
