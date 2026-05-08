import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Table,
  Button,
  Space,
  Tag,
  Drawer,
  Form,
  Input,
  Switch,
  Popconfirm,
  message,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { Skill, SkillPayload } from '@/api/skill'
import {
  getSkillList,
  createSkill,
  updateSkill,
  deleteSkill,
} from '@/api/skill'

export default function SkillsTab() {
  const { t } = useTranslation()
  const [data, setData] = useState<Skill[]>([])
  const [loading, setLoading] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingSkill, setEditingSkill] = useState<Skill | null>(null)
  const [form] = Form.useForm()

  useEffect(() => {
    fetchSkills()
  }, [])

  const fetchSkills = async () => {
    setLoading(true)
    try {
      const res = await getSkillList()
      setData(res)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('skill.fetchError'))
      setData([])
    } finally {
      setLoading(false)
    }
  }

  const openCreate = () => {
    setEditingSkill(null)
    form.resetFields()
    setDrawerOpen(true)
  }

  const openEdit = (skill: Skill) => {
    setEditingSkill(skill)
    form.setFieldsValue({
      name: skill.name,
      code: skill.code,
      description: skill.description,
      content: skill.content,
      status: skill.status === 1,
    })
    setDrawerOpen(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteSkill(id)
      message.success(t('skill.deleteSuccess'))
      setData((prev) => prev.filter((s) => s.id !== id))
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('skill.deleteError'))
    }
  }

  const handleSubmit = async (values: Record<string, unknown>) => {
    const payload: SkillPayload = {
      name: String(values.name),
      code: String(values.code),
      description: values.description ? String(values.description) : undefined,
      content: values.content ? String(values.content) : undefined,
      status: values.status ? 1 : 0,
    }

    try {
      if (editingSkill) {
        await updateSkill(editingSkill.id, payload)
        message.success(t('skill.updateSuccess'))
        setData((prev) =>
          prev.map((s) =>
            s.id === editingSkill.id
              ? { ...s, ...payload, status: payload.status ?? s.status }
              : s
          )
        )
      } else {
        const newId = await createSkill(payload)
        message.success(t('skill.createSuccess'))
        setData((prev) => [
          ...prev,
          {
            ...payload,
            id: newId,
            status: payload.status ?? 0,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          } as Skill,
        ])
      }
      setDrawerOpen(false)
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('skill.saveError'))
    }
  }

  const toggleStatus = async (skill: Skill, checked: boolean) => {
    try {
      await updateSkill(skill.id, {
        name: skill.name,
        code: skill.code,
        description: skill.description,
        content: skill.content,
        status: checked ? 1 : 0,
      })
      message.success(t('skill.updateSuccess'))
      setData((prev) =>
        prev.map((s) =>
          s.id === skill.id ? { ...s, status: checked ? 1 : 0 } : s
        )
      )
    } catch (err) {
      message.error(err instanceof Error ? err.message : t('skill.updateError'))
    }
  }

  const columns = [
    { title: t('skill.name'), dataIndex: 'name', key: 'name' },
    { title: t('skill.code'), dataIndex: 'code', key: 'code' },
    {
      title: t('skill.description'),
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: t('specCenter.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'default'}>
          {status === 1 ? t('skill.enabled') : t('skill.disabled')}
        </Tag>
      ),
    },
    {
      title: t('specCenter.action'),
      key: 'action',
      render: (_: unknown, record: Skill) => (
        <Space>
          <Switch
            checked={record.status === 1}
            onChange={(checked) => toggleStatus(record, checked)}
          />
          <Button
            icon={<EditOutlined />}
            size="small"
            onClick={() => openEdit(record)}
          >
            {t('common.edit')}
          </Button>
          <Popconfirm
            title={t('skill.confirmDelete')}
            onConfirm={() => handleDelete(record.id)}
          >
            <Button icon={<DeleteOutlined />} size="small" danger>
              {t('common.delete')}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          {t('skill.create')}
        </Button>
      </div>
      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        locale={{ emptyText: t('common.noData') }}
      />
      <Drawer
        title={editingSkill ? t('skill.edit') : t('skill.create')}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={520}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ status: true }}
        >
          <Form.Item
            name="name"
            label={t('skill.name')}
            rules={[{ required: true, message: t('skill.nameRequired') }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="code"
            label={t('skill.code')}
            rules={[{ required: true, message: t('skill.codeRequired') }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t('skill.description')}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="content" label={t('skill.content')}>
            <Input.TextArea rows={6} placeholder="Markdown content..." />
          </Form.Item>
          <Form.Item name="status" label={t('skill.enabled')} valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {t('common.save')}
              </Button>
              <Button onClick={() => setDrawerOpen(false)}>
                {t('common.cancel')}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  )
}
