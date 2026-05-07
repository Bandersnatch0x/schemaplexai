import { Card, Table, Button, Space, Tag, Input, message } from 'antd'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getContextList } from '@/api/context'
import type { ContextItem } from '@/api/context'
import './ContextCenter.css'

export default function ContextCenter() {
  const { t } = useTranslation()
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
      const msg = err instanceof Error ? err.message : t('contextCenter.fetchError')
      message.error(msg)
      setData([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const typeMap: Record<string, string> = {
    knowledge: t('contextCenter.knowledge'),
    memory: t('contextCenter.memory'),
    document: t('contextCenter.document'),
  }

  const columns = [
    { title: t('contextCenter.name'), dataIndex: 'name', key: 'name' },
    {
      title: t('contextCenter.type'),
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => <Tag className={`context-tag context-tag-${type}`}>{typeMap[type] || type}</Tag>,
    },
    { title: t('contextCenter.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: t('contextCenter.action'),
      key: 'action',
      render: () => (
        <Space>
          <Button icon={<SearchOutlined />} size="small">{t('contextCenter.search')}</Button>
          <Button type="link">{t('common.edit')}</Button>
          <Button type="link" danger>{t('common.delete')}</Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="context-page">
      <Card
        className="context-card"
        title={t('contextCenter.title')}
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            {t('contextCenter.newContext')}
          </Button>
        }
      >
        <Input.Search
          className="context-search"
          placeholder={t('contextCenter.searchPlaceholder')}
          allowClear
          onSearch={(v) => { setKeyword(v); setPage(1) }}
        />
        <Table
          className="context-table"
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
