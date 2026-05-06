import { Card, Table, Button, Space, Tag, Switch, message } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { getIntegrationList, updateIntegration } from '@/api/integration'
import type { Integration } from '@/api/integration'

export default function IntegrationCenter() {
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
      const msg = err instanceof Error ? err.message : '获取集成列表失败'
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
      message.success('状态已更新')
    } catch (err) {
      const msg = err instanceof Error ? err.message : '更新状态失败'
      message.error(msg)
    }
  }

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '类型', dataIndex: 'type', key: 'type' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => <Tag color={status === 1 ? 'green' : 'default'}>{status === 1 ? '已连接' : '未连接'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Integration) => (
        <Space>
          <Switch checked={record.status === 1} onChange={() => toggleStatus(record.id, record.status)} />
          <Button icon={<EditOutlined />} size="small">配置</Button>
        </Space>
      ),
    },
  ]

  return (
    <Card title="集成与工具">
      <Table dataSource={data} columns={columns} rowKey="id" loading={loading} />
    </Card>
  )
}
