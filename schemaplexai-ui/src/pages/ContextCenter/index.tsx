import { Card, Table, Button, Space, Tag, Input, message } from 'antd'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { getContextList } from '@/api/context'
import type { ContextItem } from '@/api/context'

export default function ContextCenter() {
  const [keyword, setKeyword] = useState('')
  const [data, setData] = useState<ContextItem[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const pageSize = 10

  useEffect(() => {
    fetchContexts()
  }, [keyword, page])

  const fetchContexts = async () => {
    setLoading(true)
    try {
      const res = await getContextList({ page, pageSize, keyword })
      setData(res.list)
      setTotal(res.total)
    } catch (err) {
      const msg = err instanceof Error ? err.message : '获取上下文列表失败'
      message.error(msg)
      setData([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const typeMap: Record<string, string> = {
    knowledge: '知识库',
    memory: '记忆',
    document: '文档',
  }

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => <Tag>{typeMap[type] || type}</Tag>,
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button icon={<SearchOutlined />} size="small">检索</Button>
          <Button type="link">编辑</Button>
          <Button type="link" danger>删除</Button>
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="上下文与知识中心"
      extra={
        <Button type="primary" icon={<PlusOutlined />}>
          新建上下文
        </Button>
      }
    >
      <Input.Search
        placeholder="搜索上下文"
        allowClear
        style={{ width: 300, marginBottom: 16 }}
        onSearch={(v) => { setKeyword(v); setPage(1) }}
      />
      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          pageSize,
          total,
          onChange: (p) => setPage(p),
        }}
      />
    </Card>
  )
}
