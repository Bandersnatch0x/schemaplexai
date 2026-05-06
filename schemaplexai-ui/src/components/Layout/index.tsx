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
import './Layout.css'

// Re-export other layout variants
export { ImmersiveLayout } from './ImmersiveLayout'
export type { ImmersiveLayoutProps } from './ImmersiveLayout'
export { ProgressiveLayout } from './ProgressiveLayout'
export type { ProgressiveLayoutProps } from './ProgressiveLayout'

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
    <AntLayout className="layout-root">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="dark"
        className="layout-sider"
      >
        <div className={`layout-logo${collapsed ? ' layout-logo--collapsed' : ''}`}>
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
      <AntLayout className={collapsed ? 'layout-main layout-main--collapsed' : 'layout-main'}>
        <Header className="layout-header" style={{ background: colorBgContainer }}>
          <div className="layout-header-title">
            {menuItems.find((m) => m.key === location.pathname)?.label || 'SchemaPlexAI'}
          </div>
          <div className="layout-header-actions">
            <TenantSelector />
            <Badge count={5} size="small">
              <BellOutlined
                className="layout-header-bell"
                onClick={() => navigate('/notifications')}
              />
            </Badge>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <div className="layout-header-user">
                <Avatar src={userInfo?.avatar} icon={<UserOutlined />} size="small" />
                <span>{userInfo?.nickname || userInfo?.username || '用户'}</span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content className="layout-content" style={{ background: colorBgContainer }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  )
}
