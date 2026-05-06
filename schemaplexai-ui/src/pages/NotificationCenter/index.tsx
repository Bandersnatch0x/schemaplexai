import { useEffect, useState } from 'react'
import { Card, Table, Button, Badge, Tabs, Tag, Empty } from 'antd'
import { BellOutlined, CheckOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { getNotificationPage, markAsRead, markAllAsRead } from '@/api/notification'
import type { Notification } from '@/types/notification'
import './NotificationCenter.css'

export default function NotificationCenter() {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [unreadCount, setUnreadCount] = useState(0)
  const [activeTab, setActiveTab] = useState('all')

  const fetchNotifications = async (p = page, s = size, readFilter?: boolean) => {
    setLoading(true)
    try {
      const res = await getNotificationPage({ page: p, size: s, read: readFilter })
      if (res) {
        setNotifications(res.records)
        setTotal(res.total)
      }
    } catch {
      setNotifications([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const fetchUnreadCount = async () => {
    try {
      const res = await getNotificationPage({ page: 1, size: 1, read: false })
      if (res) {
        setUnreadCount(res.total)
      }
    } catch {
      setUnreadCount(0)
    }
  }

  useEffect(() => {
    const readFilter = activeTab === 'unread' ? false : activeTab === 'read' ? true : undefined
    fetchNotifications(page, size, readFilter)
  }, [page, size, activeTab])

  useEffect(() => {
    fetchUnreadCount()
  }, [notifications])

  const handleMarkAsRead = async (id: string) => {
    try {
      await markAsRead(id)
      setNotifications(prev =>
        prev.map(n => (n.id === id ? { ...n, read: true } : n))
      )
      setUnreadCount(prev => Math.max(0, prev - 1))
    } catch {
      // ignore
    }
  }

  const handleMarkAllAsRead = async () => {
    try {
      await markAllAsRead()
      setNotifications(prev => prev.map(n => ({ ...n, read: true })))
      setUnreadCount(0)
    } catch {
      // ignore
    }
  }

  const columns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      render: (text: string, record: Notification) => (
        <span className={`notification-title ${record.read ? 'notification-title--read' : 'notification-title--unread'}`}>
          {!record.read && <Badge dot className="notification-title-badge" />}
          {text}
        </span>
      ),
    },
    {
      title: '内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: string) => {
        const cls: Record<string, string> = {
          SYSTEM: 'notification-tag-system',
          TASK: 'notification-tag-task',
          WORKFLOW: 'notification-tag-workflow',
        }
        return <Tag className={cls[type] || ''}>{type}</Tag>
      },
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (time: string) => <span className="notification-time">{new Date(time).toLocaleString()}</span>,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: Notification) =>
        !record.read ? (
          <Button
            size="small"
            className="notification-action-btn"
            icon={<CheckOutlined />}
            onClick={() => handleMarkAsRead(record.id)}
          >
            已读
          </Button>
        ) : (
          <CheckCircleOutlined className="notification-read-icon" />
        ),
    },
  ]

  return (
    <div className="notification-page">
      <Card
        title={
          <span className="notification-header">
            <BellOutlined className="notification-header-icon" />
            通知中心
            {unreadCount > 0 && (
              <Badge
                count={unreadCount}
                className="notification-unread-badge"
              />
            )}
          </span>
        }
        extra={
          unreadCount > 0 && (
            <Button className="notification-mark-all-btn" icon={<CheckOutlined />} onClick={handleMarkAllAsRead}>
              全部已读
            </Button>
          )
        }
      >
        <Tabs
          activeKey={activeTab}
          onChange={key => {
            setActiveTab(key)
            setPage(1)
          }}
          items={[
            { key: 'all', label: '全部' },
            { key: 'unread', label: `未读 (${unreadCount})` },
            { key: 'read', label: '已读' },
          ]}
        />
        <Table
          dataSource={notifications}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page,
            pageSize: size,
            total,
            showSizeChanger: true,
            showTotal: t => `共 ${t} 条`,
            onChange: (p, s) => {
              setPage(p)
              setSize(s || 20)
            },
          }}
          locale={{
            emptyText: <Empty description="暂无通知" />,
          }}
        />
      </Card>
    </div>
  )
}
