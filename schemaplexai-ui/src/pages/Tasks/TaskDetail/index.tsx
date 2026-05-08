import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Descriptions, Tag, List, Input, Button, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getTaskDetail, getTaskComments, addTaskComment, type TaskComment } from '@/api/task'
import type { SfTask } from '@/types'

export default function TaskDetail() {
  const { t } = useTranslation()
  const { id } = useParams<{ id: string }>()
  const [task, setTask] = useState<SfTask | null>(null)
  const [comments, setComments] = useState<TaskComment[]>([])
  const [commentText, setCommentText] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (id) {
      fetchTask()
      fetchComments()
    }
  }, [id])

  const fetchTask = async () => {
    if (!id) return
    try {
      const res = await getTaskDetail(id)
      setTask(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskDetail.fetchError'))
    }
  }

  const fetchComments = async () => {
    if (!id) return
    try {
      const res = await getTaskComments(id)
      setComments(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('taskDetail.fetchError'))
    }
  }

  const handleAddComment = async () => {
    if (!id || !commentText.trim()) return
    setLoading(true)
    try {
      await addTaskComment(id, commentText.trim())
      setCommentText('')
      fetchComments()
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('common.error'))
    } finally {
      setLoading(false)
    }
  }

  if (!task) {
    return <div style={{ padding: 24 }}>{t('common.loading')}</div>
  }

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{task.title}</h2>
      <Card title={t('taskDetail.title')} style={{ marginBottom: 24 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="ID">{task.id}</Descriptions.Item>
          <Descriptions.Item label={t('agentManager.status')}>
            <Tag>{task.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label={t('taskBoard.title')}>{task.priority}</Descriptions.Item>
          <Descriptions.Item label={t('common.createdAt')}>{task.createdAt}</Descriptions.Item>
        </Descriptions>
        <div style={{ marginTop: 16 }}>{task.description}</div>
      </Card>

      <Card title={t('taskDetail.comments')}>
        <List
          dataSource={comments}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={`${item.authorName || item.authorId} · ${item.createdAt}`}
                description={item.content}
              />
            </List.Item>
          )}
          locale={{ emptyText: t('common.noData') }}
        />
        <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
          <Input
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            placeholder={t('taskDetail.addComment')}
            onPressEnter={handleAddComment}
          />
          <Button type="primary" onClick={handleAddComment} loading={loading}>
            {t('common.confirm')}
          </Button>
        </div>
      </Card>
    </div>
  )
}
