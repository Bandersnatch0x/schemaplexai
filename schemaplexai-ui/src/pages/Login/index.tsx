import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Form, Input, Button, Card, message, Typography } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useUserStore } from '@/stores/userStore'
import { setToken, setTenantId } from '@/utils/token'

const { Title } = Typography

export default function Login() {
  const navigate = useNavigate()
  const { setUserInfo, setCurrentTenant } = useUserStore()
  const [loading, setLoading] = useState(false)

  const handleLogin = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      // TODO: replace with real API call
      await new Promise((resolve) => setTimeout(resolve, 1000))

      const mockToken = 'mock_jwt_token_' + Date.now()
      const mockUser = {
        id: 'user-1',
        username: values.username,
        nickname: values.username,
        roles: ['admin'],
      }
      const mockTenant = { id: 'tenant-1', name: '默认租户', code: 'default' }

      setToken(mockToken)
      setTenantId(mockTenant.id)
      setUserInfo(mockUser)
      setCurrentTenant(mockTenant)

      message.success('登录成功')
      navigate('/dashboard')
    } catch (err) {
      message.error('登录失败')
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
