import { Card, Form, Input, Button, Tabs, Switch, message } from 'antd'
import { useEffect, useState } from 'react'
import { getSystemConfigs, updateSystemConfig } from '@/api/system'
import type { SystemConfig } from '@/api/system'

const { TabPane } = Tabs

export default function SystemSettings() {
  const [form] = Form.useForm()
  const [configs, setConfigs] = useState<SystemConfig[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchConfigs()
  }, [])

  const fetchConfigs = async () => {
    setLoading(true)
    try {
      const res = await getSystemConfigs()
      setConfigs(res.list)
      const initialValues: Record<string, string | boolean> = {}
      res.list.forEach((c) => {
        if (c.configValue === 'true' || c.configValue === 'false') {
          initialValues[c.configKey] = c.configValue === 'true'
        } else {
          initialValues[c.configKey] = c.configValue
        }
      })
      form.setFieldsValue(initialValues)
    } catch (err) {
      const msg = err instanceof Error ? err.message : '获取系统配置失败'
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      const updates = Object.entries(values).map(([key, value]) => {
        const config = configs.find((c) => c.configKey === key)
        return config ? updateSystemConfig(config.id, String(value)) : Promise.resolve()
      })
      await Promise.all(updates)
      message.success('保存成功')
    } catch (err) {
      const msg = err instanceof Error ? err.message : '保存失败'
      message.error(msg)
    }
  }

  return (
    <Card title="系统设置" loading={loading}>
      <Tabs defaultActiveKey="general">
        <TabPane tab="通用设置" key="general">
          <Form form={form} layout="vertical" style={{ maxWidth: 600 }}>
            <Form.Item label="平台名称" name="platformName" initialValue="SchemaPlexAI">
              <Input />
            </Form.Item>
            <Form.Item label="Logo URL" name="logoUrl">
              <Input />
            </Form.Item>
            <Form.Item label="是否开启注册" name="enableRegister" valuePropName="checked" initialValue={true}>
              <Switch />
            </Form.Item>
            <Form.Item>
              <Button type="primary" onClick={handleSave}>保存</Button>
            </Form.Item>
          </Form>
        </TabPane>
        <TabPane tab="模型配置" key="model">
          <Form form={form} layout="vertical" style={{ maxWidth: 600 }}>
            <Form.Item label="默认模型" name="defaultModel" initialValue="gpt-4">
              <Input />
            </Form.Item>
            <Form.Item label="API Key" name="apiKey">
              <Input.Password />
            </Form.Item>
            <Form.Item>
              <Button type="primary" onClick={handleSave}>保存</Button>
            </Form.Item>
          </Form>
        </TabPane>
        <TabPane tab="通知设置" key="notification">
          <Form form={form} layout="vertical" style={{ maxWidth: 600 }}>
            <Form.Item label="邮件服务器" name="smtpHost">
              <Input />
            </Form.Item>
            <Form.Item label="邮件端口" name="smtpPort" initialValue={587}>
              <Input type="number" />
            </Form.Item>
            <Form.Item label="发送邮箱" name="fromEmail">
              <Input />
            </Form.Item>
            <Form.Item>
              <Button type="primary" onClick={handleSave}>保存</Button>
            </Form.Item>
          </Form>
        </TabPane>
      </Tabs>
    </Card>
  )
}
