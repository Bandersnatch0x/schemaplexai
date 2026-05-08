import { useTranslation } from 'react-i18next'
import { Card, Empty } from 'antd'

export default function QualityCenter() {
  const { t } = useTranslation()

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>{t('qualityCenter.title')}</h2>
      <Card>
        <Empty description={t('qualityCenter.fetchError')} />
      </Card>
    </div>
  )
}
