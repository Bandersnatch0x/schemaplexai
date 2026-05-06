import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, message, Typography } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useUserStore } from '@/stores/userStore'
import { login, getTenantList } from '@/api/auth'
import { saveAuth } from '@/api/auth'
import { setTenantId } from '@/utils/token'

const { Title } = Typography

export default function Login() {
  const navigate = useNavigate()
  const { setUserInfo, setCurrentTenant, setTenants } = useUserStore()
  const [loading, setLoading] = useState(false)

  const handleLogin = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const result = await login(values)
      saveAuth(result, 'default')

      const tenants = await getTenantList()
      setTenants(tenants)

      const currentTenant = tenants[0] || { id: 'default', name: '默认租户', code: 'default' }
      setTenantId(currentTenant.id)
      setCurrentTenant(currentTenant)

      setUserInfo({
        id: values.username,
        username: values.username,
        nickname: values.username,
        roles: ['admin'],
      })

      message.success('登录成功')
      navigate('/dashboard')
    } catch (err) {
      const msg = err instanceof Error ? err.message : '登录失败'
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        height: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      <Card style={{ width: 400, borderRadius: 8, boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Title level={3} style={{ margin: 0 }}>
            SchemaPlexAI
          </Title>
          <div style={{ color: '#888', marginTop: 8 }}>AI 研发协作平台</div>
        </div>
        <Form onFinish={handleLogin} autoComplete="off">
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" size="large" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" size="large" block loading={loading}>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
