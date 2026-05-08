import { Card, Form, Input, Button, Switch, message } from 'antd'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getSystemConfigs, updateSystemConfig } from '@/api/system'
import type { SystemConfig } from '@/api/system'

export default function SystemGeneralTab() {
  const { t } = useTranslation()
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
      const msg = err instanceof Error ? err.message : t('systemSettings.fetchError')
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
      message.success(t('systemSettings.saveSuccess'))
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('systemSettings.saveError')
      message.error(msg)
    }
  }

  return (
    <Card loading={loading}>
      <Form form={form} layout="vertical" className="settings-form">
        <Form.Item label={t('systemSettings.platformName')} name="platformName" initialValue="SchemaPlexAI">
          <Input />
        </Form.Item>
        <Form.Item label="Logo URL" name="logoUrl">
          <Input />
        </Form.Item>
        <Form.Item label={t('systemSettings.enableRegister')} name="enableRegister" valuePropName="checked" initialValue={true}>
          <Switch />
        </Form.Item>
        <Form.Item label={t('systemSettings.smtpHost')} name="smtpHost">
          <Input />
        </Form.Item>
        <Form.Item label={t('systemSettings.smtpPort')} name="smtpPort" initialValue={587}>
          <Input type="number" />
        </Form.Item>
        <Form.Item label={t('systemSettings.fromEmail')} name="fromEmail">
          <Input />
        </Form.Item>
        <Form.Item>
          <Button className="settings-save-btn" onClick={handleSave}>{t('systemSettings.save')}</Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
