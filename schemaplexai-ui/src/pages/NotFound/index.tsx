import { Result, Button } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import './NotFound.css'

export default function NotFound() {
  const navigate = useNavigate()
  const { t } = useTranslation()
  return (
    <div className="notfound-page">
      <Result
        status="404"
        title="404"
        subTitle={t('notFound.subTitle')}
        extra={<Button type="primary" onClick={() => navigate('/')}>{t('notFound.backHome')}</Button>}
      />
    </div>
  )
}
