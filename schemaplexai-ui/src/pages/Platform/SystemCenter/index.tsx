import { useState } from 'react'
import { Tabs } from 'antd'
import { useTranslation } from 'react-i18next'
import SystemGeneralTab from './SystemGeneralTab'
import SystemUsersTab from './SystemUsersTab'
import SystemModelsTab from './SystemModelsTab'

export default function SystemCenter() {
  const { t } = useTranslation()
  const [activeKey, setActiveKey] = useState('general')

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('systemCenter.title')}</h2>
      <Tabs activeKey={activeKey} onChange={setActiveKey}>
        <Tabs.TabPane tab={t('systemSettings.general')} key="general">
          <SystemGeneralTab />
        </Tabs.TabPane>
        <Tabs.TabPane tab={t('systemCenter.users')} key="users">
          <SystemUsersTab />
        </Tabs.TabPane>
        <Tabs.TabPane tab={t('systemCenter.models')} key="models">
          <SystemModelsTab />
        </Tabs.TabPane>
      </Tabs>
    </div>
  )
}
