import { useTranslation } from 'react-i18next'
import './LanguageSwitcher.css'

const LANGUAGES = [
  { key: 'zh', label: '中' },
  { key: 'en', label: 'EN' },
] as const

export function LanguageSwitcher() {
  const { i18n } = useTranslation()

  const currentLang = i18n.language?.startsWith('zh') ? 'zh' : 'en'

  const switchLang = (lang: 'zh' | 'en') => {
    i18n.changeLanguage(lang)
    localStorage.setItem('schemaplexai_lang', lang)
  }

  return (
    <div className="lang-switcher">
      {LANGUAGES.map((lang) => (
        <button
          key={lang.key}
          onClick={() => switchLang(lang.key)}
          className={`lang-switcher-btn ${currentLang === lang.key ? 'lang-switcher-btn--active' : ''}`}
        >
          {lang.label}
        </button>
      ))}
    </div>
  )
}
