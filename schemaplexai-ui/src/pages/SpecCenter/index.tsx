import { Card, Table, Button, Space, Tag, Input, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { getSpecList } from '@/api/spec'
import type { SpecItem } from '@/api/spec'
import './SpecCenter.css'

export default function SpecCenter() {
  const [keyword, setKeyword] = useState('')
  const [data, setData] = useState<SpecItem[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const pageSize = 10

  useEffect(() => {
    fetchSpecs()
  }, [keyword, page])

  const fetchSpecs = async () => {
    setLoading(true)
    try {
      const res = await getSpecList({ page, pageSize, keyword })
      setData(res.list)
      setTotal(res.total)
    } catch (err) {
      const msg = err instanceof Error ? err.message : '获取规范列表失败'
      message.error(msg)
      setData([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '版本', dataIndex: 'version', key: 'version' },
    { title: '类型', dataIndex: 'type', key: 'type' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag className={status === 'published' ? 'spec-tag-published' : 'spec-tag-draft'}>
          {status}
        </Tag>
      ),
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">查看</Button>
          <Button type="link">编辑</Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="spec-page">
      <Card
        className="spec-card"
        title="Spec 规范中心"
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            新建规范
          </Button>
        }
      >
        <Input.Search
          className="spec-search"
          placeholder="搜索规范"
          allowClear
          onSearch={(v) => { setKeyword(v); setPage(1) }}
        />
        <Table
          className="spec-table"
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
    </div>
  )
}
