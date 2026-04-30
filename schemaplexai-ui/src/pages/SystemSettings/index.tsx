import { Card, Form, Input, Button, Tabs, Switch, message } from 'antd'

const { TabPane } = Tabs

export default function SystemSettings() {
  const [form] = Form.useForm()

  const handleSave = () => {
    message.success('保存成功')
  }

  return (
    <Card title="系统设置">
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
          <Form layout="vertical" style={{ maxWidth: 600 }}>
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
          <Form layout="vertical" style={{ maxWidth: 600 }}>
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
