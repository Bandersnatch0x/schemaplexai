import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { message } from 'antd'
import { KanbanBoard } from '@/components/Hive/KanbanBoard'
import type { SfTask, TaskStatus } from '@/types'
import { getTaskList, updateTaskStatus } from '@/api/task'

const COLUMNS: TaskStatus[] = [
  'BACKLOG',
  'QUEUED',
  'IN_PROGRESS',
  'AWAITING_REVIEW',
  'REVISING',
  'BLOCKED',
  'DONE',
]

export default function TaskBoard() {
  const { t } = useTranslation()
  const [tasks, setTasks] = useState<SfTask[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchTasks()
  }, [])

  const fetchTasks = async () => {
    setLoading(true)
    try {
      const res = await getTaskList({ pageSize: 1000 })
      setTasks(res.list)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskBoard.fetchError'))
      setTasks([])
    } finally {
      setLoading(false)
    }
  }

  const handleMove = async (taskId: string, toStatus: TaskStatus) => {
    try {
      await updateTaskStatus(taskId, toStatus)
      setTasks((prev) =>
        prev.map((task) => (task.id === taskId ? { ...task, status: toStatus } : task))
      )
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskBoard.fetchError'))
    }
  }

  return (
    <div style={{ padding: 24, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <h2 style={{ marginBottom: 16 }}>{t('taskBoard.title')}</h2>
      <div style={{ flex: 1, minHeight: 0 }}>
        <KanbanBoard
          columns={COLUMNS}
          tasks={tasks}
          loading={loading}
          onMove={handleMove}
        />
      </div>
    </div>
  )
}
