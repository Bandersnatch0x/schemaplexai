import { useMemo } from 'react'
import { DndContext, type DragEndEvent, closestCorners } from '@dnd-kit/core'
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { useTranslation } from 'react-i18next'
import { Spin } from 'antd'
import { TaskCard } from './TaskCard'
import type { SfTask, TaskStatus } from '@/types'

export interface KanbanBoardProps {
  columns: TaskStatus[]
  tasks: SfTask[]
  loading?: boolean
  onMove: (taskId: string, toStatus: TaskStatus) => void
}

export function KanbanBoard({ columns, tasks, loading, onMove }: KanbanBoardProps) {
  const { t } = useTranslation()

  const columnsWithTasks = useMemo(() => {
    return columns.map((status) => ({
      status,
      tasks: tasks.filter((task) => task.status === status),
    }))
  }, [columns, tasks])

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!over) return

    const taskId = active.id as string
    const toStatus = over.id as TaskStatus

    if (columns.includes(toStatus)) {
      onMove(taskId, toStatus)
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <DndContext collisionDetection={closestCorners} onDragEnd={handleDragEnd}>
      <div
        style={{
          display: 'flex',
          gap: 16,
          height: '100%',
          overflowX: 'auto',
          paddingBottom: 8,
        }}
      >
        {columnsWithTasks.map((column) => (
          <div
            key={column.status}
            data-testid={`column-${column.status}`}
            style={{
              minWidth: 240,
              maxWidth: 280,
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              background: '#0d1117',
              borderRadius: 8,
              border: '1px solid #1f2937',
            }}
          >
            <div
              style={{
                padding: '12px 16px',
                fontWeight: 600,
                fontSize: 14,
                borderBottom: '1px solid #1f2937',
                color: '#e5e7eb',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <span>{t(`taskBoard.${column.status.toLowerCase()}`)}</span>
              <span
                style={{
                  background: '#1f2937',
                  borderRadius: 10,
                  padding: '2px 8px',
                  fontSize: 12,
                }}
              >
                {column.tasks.length}
              </span>
            </div>
            <SortableContext
              items={column.tasks.map((t) => t.id)}
              strategy={verticalListSortingStrategy}
            >
              <div
                id={column.status}
                data-testid={`dropzone-${column.status}`}
                style={{
                  flex: 1,
                  padding: 12,
                  overflowY: 'auto',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                {column.tasks.map((task) => (
                  <TaskCard key={task.id} task={task} />
                ))}
              </div>
            </SortableContext>
          </div>
        ))}
      </div>
    </DndContext>
  )
}
