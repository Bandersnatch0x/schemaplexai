import { useTranslation } from 'react-i18next'
import { Card, Empty } from 'antd'

export default function SpecCenter() {
  const { t } = useTranslation()

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('specCenter.title')}</h2>
      <Card>
        <Empty description={t('specCenter.fetchError')} />
      </Card>
    </div>
  )
}
