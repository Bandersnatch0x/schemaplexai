import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Tag } from 'antd'
import type { SfTask } from '@/types'

export interface TaskCardProps {
  task: SfTask
}

const PRIORITY_COLORS: Record<string, string> = {
  P0: '#ff4757',
  P1: '#ff9f43',
  P2: '#00d4aa',
  P3: '#64748b',
}

export function TaskCard({ task }: TaskCardProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <div
      ref={setNodeRef}
      style={{
        ...style,
        background: '#111827',
        borderRadius: 8,
        padding: 12,
        border: '1px solid #1f2937',
        cursor: 'grab',
      }}
      {...attributes}
      {...listeners}
      data-testid={`task-card-${task.id}`}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
        <span style={{ fontWeight: 500, fontSize: 14, color: '#e5e7eb', lineHeight: 1.4 }}>
          {task.title}
        </span>
        <Tag color={PRIORITY_COLORS[task.priority] || '#64748b'} style={{ fontSize: 11, marginLeft: 8 }}>
          {task.priority}
        </Tag>
      </div>

      {task.description && (
        <div style={{ fontSize: 12, color: '#9ca3af', marginBottom: 8, lineHeight: 1.4 }}>
          {task.description.slice(0, 60)}{task.description.length > 60 ? '...' : ''}
        </div>
      )}

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginBottom: 8 }}>
        {task.skillTags?.map((tag) => (
          <Tag key={tag} style={{ fontSize: 11, background: '#1f2937', border: 'none', color: '#9ca3af' }}>
            {tag}
          </Tag>
        ))}
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11, color: '#6b7280' }}>
        <span>{task.assignmentType}</span>
        {task.specId && <span>Spec: {task.specId.slice(0, 6)}</span>}
      </div>
    </div>
  )
}
