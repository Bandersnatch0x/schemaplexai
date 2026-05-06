import { Card, Table, Button, Space, Tag, Timeline, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { getArtifactList } from '@/api/ops'
import type { OpsArtifact } from '@/api/ops'

export default function OpsCenter() {
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
      const msg = err instanceof Error ? err.message : '获取运营数据失败'
      message.error(msg)
      setData([])
    } finally {
      setLoading(false)
    }
  }

  const statusMap: Record<number, { color: string; text: string }> = {
    0: { color: 'orange', text: '待处理' },
    1: { color: 'blue', text: '进行中' },
    2: { color: 'green', text: '已完成' },
    3: { color: 'red', text: '失败' },
  }

  const columns = [
    { title: '任务', dataIndex: 'name', key: 'name' },
    { title: '类型', dataIndex: 'artifactType', key: 'artifactType' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => {
        const map = statusMap[status] || statusMap[0]
        return <Tag color={map.color}>{map.text}</Tag>
      },
    },
    { title: '版本', dataIndex: 'version', key: 'version' },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">详情</Button>
          <Button type="link">编辑</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Card
        title="交付与运营"
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            新建任务
          </Button>
        }
        style={{ marginBottom: 16 }}
      >
        <Table dataSource={data} columns={columns} rowKey="id" loading={loading} />
      </Card>
      <Card title="运营时间线">
        <Timeline
          items={[
            { children: '2024-08-01 完成 v2.0.0 部署' },
            { children: '2024-07-28 系统性能优化完成' },
            { children: '2024-07-20 新增 GitHub 集成' },
          ]}
        />
      </Card>
    </div>
  )
}
