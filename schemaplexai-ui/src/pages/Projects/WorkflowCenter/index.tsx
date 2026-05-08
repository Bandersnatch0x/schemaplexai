import { useState } from 'react'
import { Tabs } from 'antd'
import { useTranslation } from 'react-i18next'
import WorkflowTemplateTab from './WorkflowTemplateTab'
import WorkflowInstanceTab from './WorkflowInstanceTab'
import './WorkflowCenter.css'

export default function WorkflowCenter() {
  const { t } = useTranslation()
  const [activeKey, setActiveKey] = useState('templates')

  return (
    <div className="workflow-center-page">
      <h2 className="workflow-center-title">{t('workflowCenter.title')}</h2>
      <Tabs activeKey={activeKey} onChange={setActiveKey}>
        <Tabs.TabPane tab={t('workflowCenter.templates')} key="templates">
          <WorkflowTemplateTab />
        </Tabs.TabPane>
        <Tabs.TabPane tab={t('workflowCenter.instances')} key="instances">
          <WorkflowInstanceTab />
        </Tabs.TabPane>
      </Tabs>
    </div>
  )
}
