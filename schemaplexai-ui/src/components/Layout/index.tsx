import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  Layout as AntLayout,
  Menu,
  Avatar,
  Dropdown,
  Badge,
  theme,
} from 'antd'
import {
  DashboardOutlined,
  RobotOutlined,
  FileTextOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  SafetyOutlined,
  ApiOutlined,
  CloudServerOutlined,
  SettingOutlined,
  BellOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons'
import TenantSelector from '../TenantSelector'
import { useUserStore } from '@/stores/userStore'
import { clearAuth } from '@/utils/token'

const { Header, Sider, Content } = AntLayout

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '工作台' },
  { key: '/specs', icon: <FileTextOutlined />, label: 'Spec 中心' },
  { key: '/agents', icon: <RobotOutlined />, label: 'Agent 管理' },
  { key: '/workflows', icon: <BranchesOutlined />, label: '工作流' },
  { key: '/contexts', icon: <DatabaseOutlined />, label: '上下文' },
  { key: '/quality', icon: <SafetyOutlined />, label: '质量与安全' },
  { key: '/integrations', icon: <ApiOutlined />, label: '集成与工具' },
  { key: '/ops', icon: <CloudServerOutlined />, label: '交付与运营' },
  { key: '/notifications', icon: <BellOutlined />, label: '通知中心' },
  { key: '/settings', icon: <SettingOutlined />, label: '系统设置' },
]

export default function Layout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { userInfo, setUserInfo } = useUserStore()
  const [collapsed, setCollapsed] = useState(false)
  const {
    token: { colorBgContainer },
  } = theme.useToken()

  const handleLogout = () => {
    clearAuth()
    setUserInfo(null)
    navigate('/login')
  }

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="dark"
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: collapsed ? 14 : 18,
            fontWeight: 'bold',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
          }}
        >
          {collapsed ? 'SPA' : 'SchemaPlexAI'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <AntLayout style={{ marginLeft: collapsed ? 80 : 200, transition: 'all 0.2s' }}>
        <Header
          style={{
            padding: '0 24px',
            background: colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,0,0,0.1)',
            position: 'sticky',
            top: 0,
            zIndex: 1,
          }}
        >
          <div style={{ fontSize: 16, fontWeight: 500 }}>
            {menuItems.find((m) => m.key === location.pathname)?.label || 'SchemaPlexAI'}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <TenantSelector />
            <Badge count={5} size="small">
              <BellOutlined
                style={{ fontSize: 18, cursor: 'pointer' }}
                onClick={() => navigate('/notifications')}
              />
            </Badge>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                <Avatar src={userInfo?.avatar} icon={<UserOutlined />} size="small" />
                <span>{userInfo?.nickname || userInfo?.username || '用户'}</span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: colorBgContainer, borderRadius: 8, overflow: 'auto' }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  )
}
