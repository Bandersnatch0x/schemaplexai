import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Input, Empty } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { TaskCard } from './TaskCard'
import type { SfTask, TaskStatus } from '@/types'

export interface TaskListProps {
  tasks: SfTask[]
  loading?: boolean
  onStatusChange?: (taskId: string, status: TaskStatus) => void
}

export function TaskList({ tasks, loading, onStatusChange }: TaskListProps) {
  const { t } = useTranslation()
  const [filter, setFilter] = useState('')

  const filtered = tasks.filter(
    (t) =>
      t.title.toLowerCase().includes(filter.toLowerCase()) ||
      t.description?.toLowerCase().includes(filter.toLowerCase()) ||
      t.skillTags?.some((tag) => tag.toLowerCase().includes(filter.toLowerCase()))
  )

  if (loading) {
    return (
      <div style={{ padding: 24 }}>
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            style={{
              height: 100,
              background: '#1f2937',
              borderRadius: 8,
              marginBottom: 12,
              animation: 'pulse 1.5s infinite',
            }}
          />
        ))}
      </div>
    )
  }

  return (
    <div style={{ padding: 16, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: 12 }}>
        <Input
          prefix={<SearchOutlined />}
          placeholder={t('taskBoard.search')}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          allowClear
        />
      </div>
      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
        }}
      >
        {filtered.length === 0 ? (
          <Empty description={t('common.noData')} />
        ) : (
          filtered.map((task) => (
            <div key={task.id} style={{ position: 'relative' }}>
              <TaskCard task={task} />
              {onStatusChange && (
                <div
                  style={{
                    position: 'absolute',
                    top: 8,
                    right: 8,
                    zIndex: 2,
                  }}
                >
                  {/* Status quick actions could go here */}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  )
}
