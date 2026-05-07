import { Card, Table, Button, Space, Tag, Input, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getSpecList } from '@/api/spec'
import type { SpecItem } from '@/api/spec'
import './SpecCenter.css'

export default function SpecCenter() {
  const { t } = useTranslation()
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
      const msg = err instanceof Error ? err.message : t('specCenter.fetchError')
      message.error(msg)
      setData([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: t('specCenter.name'), dataIndex: 'name', key: 'name' },
    { title: t('specCenter.version'), dataIndex: 'version', key: 'version' },
    { title: t('specCenter.type'), dataIndex: 'type', key: 'type' },
    {
      title: t('specCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag className={status === 'published' ? 'spec-tag-published' : 'spec-tag-draft'}>
          {status}
        </Tag>
      ),
    },
    { title: t('specCenter.updatedAt'), dataIndex: 'updatedAt', key: 'updatedAt' },
    {
      title: t('specCenter.action'),
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">{t('specCenter.view')}</Button>
          <Button type="link">{t('common.edit')}</Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="spec-page">
      <Card
        className="spec-card"
        title={t('specCenter.title')}
        extra={
          <Button type="primary" icon={<PlusOutlined />}>
            {t('specCenter.newSpec')}
          </Button>
        }
      >
        <Input.Search
          className="spec-search"
          placeholder={t('specCenter.searchPlaceholder')}
          allowClear
          onSearch={(v) => { setKeyword(v); setPage(1) }}
        />
        <Table
          className="spec-table"
          dataSource={data}
          columns={columns}
          rowKey="id"
          loading={loading}
          locale={{ emptyText: t('common.noData') }}
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
