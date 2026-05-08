import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import enUS from 'antd/locale/en_US'
import { useTranslation } from 'react-i18next'
import { abyssHiveTheme } from './theme'
import App from './App'
import './i18n'
import './index.css'

// Load Google Fonts
const fontLink = document.createElement('link')
fontLink.href = 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Noto+Sans+SC:wght@400;500;600&display=swap'
fontLink.rel = 'stylesheet'
document.head.appendChild(fontLink)

const antdLocales: Record<string, typeof zhCN> = {
  zh: zhCN,
  en: enUS,
}

// eslint-disable-next-line react-refresh/only-export-components
function AppWithI18n() {
  const { i18n } = useTranslation()
  const antdLocale = antdLocales[i18n.language] || zhCN

  return (
    <ConfigProvider locale={antdLocale} theme={abyssHiveTheme}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  )
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppWithI18n />
  </React.StrictMode>,
)
