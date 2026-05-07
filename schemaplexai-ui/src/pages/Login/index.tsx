import { useEffect, useRef, useState } from 'react'
import type { CSSProperties, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { message } from 'antd'
import { useUserStore } from '@/stores/userStore'
import { LanguageSwitcher } from '@/components/LanguageSwitcher'
import { login, getTenantList, saveAuth } from '@/api/auth'
import { setTenantId, getTenantId } from '@/utils/token'
import type { Tenant } from '@/types'
import './Login.css'

interface DefaultTenantHint {
  id: string
  code: string
  name: string
  badge?: string
}

const DEFAULT_TENANT_HINT: DefaultTenantHint = {
  id: 'default',
  code: 'DEFAULT',
  name: 'Default Workspace',
  badge: 'PRIMARY',
}

const TERMINAL_FEED: ReadonlyArray<{
  level: 'info' | 'warn' | 'ok' | 'err'
  label: string
  msg: string
}> = [
  { level: 'info', label: 'INFO', msg: 'Tunnel handshake → <em>tls1.3 / cn-shanghai-2</em>' },
  { level: 'info', label: 'INFO', msg: 'Edge node <em>cn-sh-2-pop3</em> ready · 12 hops' },
  { level: 'warn', label: 'WAIT', msg: 'Awaiting operator pheromone key' },
  { level: 'info', label: 'INFO', msg: 'Hive subscription confirmed → <em>aero-labs/prod</em>' },
]

const ALT_METHODS: ReadonlyArray<{ key: string; label: string; icon: JSX.Element }> = [
  {
    key: 'github',
    label: 'GitHub',
    icon: (
      <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path d="M12 .3a12 12 0 00-3.8 23.4c.6.1.8-.3.8-.6v-2.2c-3.3.7-4-1.4-4-1.4-.5-1.4-1.3-1.7-1.3-1.7-1-.7.1-.7.1-.7 1.2.1 1.8 1.2 1.8 1.2 1 1.8 2.8 1.3 3.5 1 .1-.8.4-1.3.8-1.6-2.7-.3-5.5-1.3-5.5-6 0-1.3.5-2.4 1.2-3.2-.1-.3-.5-1.5.1-3.2 0 0 1-.3 3.3 1.2 1-.3 2-.4 3-.4s2 .1 3 .4c2.3-1.5 3.3-1.2 3.3-1.2.7 1.7.2 2.9.1 3.2.8.8 1.2 1.9 1.2 3.2 0 4.7-2.8 5.7-5.5 6 .4.4.8 1.1.8 2.2v3.3c0 .3.2.7.8.6A12 12 0 0012 .3" />
      </svg>
    ),
  },
  {
    key: 'gitlab',
    label: 'GitLab',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} aria-hidden="true">
        <path d="M12 21l-4-12h8l-4 12zM4 9h4l-2-6-2 6zM20 9h-4l2-6 2 6z" />
      </svg>
    ),
  },
  {
    key: 'sso',
    label: 'SSO/SAML',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} aria-hidden="true">
        <path d="M3 11l9-9 9 9M5 9v12h14V9" />
      </svg>
    ),
  },
]

interface TerminalLine {
  id: number
  ts: string
  level: 'info' | 'warn' | 'ok' | 'err'
  label: string
  msg: string
}

function formatTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function passwordStrengthPercent(value: string): number {
  if (!value) return 0
  let score = 0
  if (value.length >= 8) score += 35
  if (value.length >= 12) score += 15
  if (/[A-Z]/.test(value)) score += 15
  if (/[0-9]/.test(value)) score += 15
  if (/[^A-Za-z0-9]/.test(value)) score += 20
  return Math.min(100, score)
}

export default function Login() {
  const navigate = useNavigate()
  const { t } = useTranslation()
  const { setUserInfo, setCurrentTenant, setTenants } = useUserStore()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [remember, setRemember] = useState(true)
  const [loading, setLoading] = useState(false)

  const [tenantHint] = useState<DefaultTenantHint>(() => {
    const lastId = getTenantId()
    if (lastId && lastId !== 'default') {
      return { id: lastId, code: lastId.slice(0, 6).toUpperCase(), name: `Workspace · ${lastId}`, badge: 'LAST' }
    }
    return DEFAULT_TENANT_HINT
  })

  const [agentCount, setAgentCount] = useState(0)
  const [workflowCount, setWorkflowCount] = useState(0)
  const [terminalLines, setTerminalLines] = useState<TerminalLine[]>([])

  const pheromonesRef = useRef<HTMLDivElement>(null)
  const usernameInputRef = useRef<HTMLInputElement>(null)

  const strength = passwordStrengthPercent(password)

  useEffect(() => {
    usernameInputRef.current?.focus()
  }, [])

  // telemetry counter
  useEffect(() => {
    const targets = { agents: 247, workflows: 38 }
    const duration = 1400
    const start = performance.now()
    let raf = 0
    const step = (now: number) => {
      const t = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - t, 3)
      setAgentCount(Math.floor(targets.agents * eased))
      setWorkflowCount(Math.floor(targets.workflows * eased))
      if (t < 1) raf = requestAnimationFrame(step)
    }
    const delay = window.setTimeout(() => {
      raf = requestAnimationFrame(step)
    }, 1100)
    return () => {
      window.clearTimeout(delay)
      cancelAnimationFrame(raf)
    }
  }, [])

  // terminal feed rotation
  useEffect(() => {
    const seed: TerminalLine[] = TERMINAL_FEED.slice(0, 3).map((l, i) => ({
      id: i,
      ts: formatTime(new Date(Date.now() - (3 - i) * 1000)),
      ...l,
    }))
    setTerminalLines(seed)

    let cursor = 3
    let nextId = seed.length
    const tick = window.setInterval(() => {
      const item = TERMINAL_FEED[cursor % TERMINAL_FEED.length]
      cursor += 1
      setTerminalLines(prev => {
        const next = [...prev, { id: nextId++, ts: formatTime(new Date()), ...item }]
        return next.length > 4 ? next.slice(next.length - 4) : next
      })
    }, 4500)
    return () => window.clearInterval(tick)
  }, [])

  // pheromone particle drift
  useEffect(() => {
    const layer = pheromonesRef.current
    if (!layer) return
    const prefersReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    const spawn = () => {
      const node = document.createElement('div')
      node.className = 'login-pher'
      const startX = Math.random() * 100
      const startY = 100 + Math.random() * 10
      const dx = (Math.random() * 30 - 15) - 0
      const dy = -(110 + Math.random() * 20)
      const dur = 6 + Math.random() * 6
      const size = 1 + Math.random() * 2.5
      const styleMap: CSSProperties & Record<string, string> = {
        left: `${startX}%`,
        top: `${startY}%`,
        width: `${size}px`,
        height: `${size}px`,
        animation: `login-pher-drift ${dur}s linear forwards`,
        ['--dx' as string]: `${dx}vw`,
        ['--dy' as string]: `${dy}vh`,
      }
      Object.assign(node.style, styleMap)
      layer.appendChild(node)
      window.setTimeout(() => {
        node.remove()
      }, dur * 1000)
    }

    const tick = window.setInterval(spawn, 700)
    for (let i = 0; i < 6; i += 1) window.setTimeout(spawn, i * 400)
    return () => window.clearInterval(tick)
  }, [])

  const handleSubmit = async (event?: FormEvent<HTMLFormElement>) => {
    if (event) event.preventDefault()
    if (loading) return

    const trimmedUser = username.trim()
    if (!trimmedUser) {
      message.warning(t('login.inputIdentifier'))
      usernameInputRef.current?.focus()
      return
    }
    if (!password) {
      message.warning(t('login.inputPassword'))
      return
    }

    setLoading(true)
    try {
      const result = await login({ username: trimmedUser, password })
      saveAuth(result, tenantHint.id)

      const tenants = await getTenantList()
      setTenants(tenants)

      const resolvedTenant: Tenant =
        tenants.find(t => t.id === tenantHint.id) ||
        tenants[0] ||
        { id: tenantHint.id, name: tenantHint.name, code: tenantHint.code }

      setTenantId(resolvedTenant.id)
      setCurrentTenant(resolvedTenant)

      setUserInfo({
        id: trimmedUser,
        username: trimmedUser,
        nickname: trimmedUser,
        roles: ['admin'],
      })

      if (remember) {
        localStorage.setItem('schemaplexai_last_user', trimmedUser)
      } else {
        localStorage.removeItem('schemaplexai_last_user')
      }

      message.success(t('login.welcomeSuccess'))
      navigate('/cockpit')
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('login.loginFailed')
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  // restore remembered user on mount
  useEffect(() => {
    const last = localStorage.getItem('schemaplexai_last_user')
    if (last) setUsername(last)
  }, [])

  const altMethodNotice = (label: string) => () => {
    message.info(t('login.altLoginNotOpen', { label }))
  }

  return (
    <div className="login-page" data-testid="login-page">
      <div className="login-abyss" aria-hidden="true" />
      <div className="login-grid" aria-hidden="true" />
      <div className="login-scanline" aria-hidden="true" />
      <div className="login-vignette" aria-hidden="true" />

      {/* Top status rail */}
      <div className="login-top-rail" aria-hidden="true">
        <div className="login-top-rail-inner">
          <span>
            <span className="live-dot" />
            HIVE.LIVE
          </span>
          <span className="sep">/</span>
          <span>
            GATEWAY <strong>8ms</strong>
          </span>
          <span className="sep">/</span>
          <span>
            REGION <strong>cn-shanghai-2</strong>
          </span>
          <span className="sep">/</span>
          <span>v2.4.1-beta</span>
        </div>
      </div>

      <div className="login-stage">
        {/* ======== LEFT — Hive Showcase ======== */}
        <section className="login-hive-side">
          <div className="login-pheromones" ref={pheromonesRef} aria-hidden="true" />

          <div className="login-brand-stamp">
            <div className="login-brand-hex">SP</div>
            <div className="login-brand-text">
              <div className="login-brand-mark">SchemaPlex</div>
              <div className="login-brand-tag">{t('login.brandTag')}</div>
            </div>
          </div>

          <div className="login-hive-center">
            <div className="login-eyebrow">{t('login.hiveAwakening')}</div>

            <h1 className="login-hero-title">
              {t('login.heroTitle')}
            </h1>

            <p className="login-hero-sub">
              {t('login.heroSub')}
            </p>

            <div className="login-orbital" aria-hidden="true">
              <svg viewBox="0 0 420 420">
                <circle cx="210" cy="210" r="80" className="login-orbit-ring lit" />
                <circle cx="210" cy="210" r="130" className="login-orbit-ring" />
                <circle cx="210" cy="210" r="180" className="login-orbit-ring" />

                <circle cx="210" cy="130" r="2" fill="#00d4aa">
                  <animateTransform
                    attributeName="transform"
                    type="rotate"
                    from="0 210 210"
                    to="360 210 210"
                    dur="6s"
                    repeatCount="indefinite"
                  />
                </circle>
                <circle cx="210" cy="130" r="1.5" fill="#00d4aa" opacity="0.5">
                  <animateTransform
                    attributeName="transform"
                    type="rotate"
                    from="180 210 210"
                    to="540 210 210"
                    dur="6s"
                    repeatCount="indefinite"
                  />
                </circle>

                <g className="login-core-hex">
                  <polygon
                    points="210,150 262,180 262,240 210,270 158,240 158,180"
                    fill="rgba(0,212,170,0.08)"
                    stroke="#00d4aa"
                    strokeWidth={1.5}
                  />
                  <polygon
                    points="210,170 245,190 245,230 210,250 175,230 175,190"
                    fill="none"
                    stroke="rgba(0,212,170,0.4)"
                    strokeWidth={1}
                  />
                  <text
                    x="210"
                    y="208"
                    textAnchor="middle"
                    fontFamily="JetBrains Mono"
                    fontSize="12"
                    fontWeight={700}
                    fill="#00d4aa"
                    letterSpacing="2"
                  >
                    CORE
                  </text>
                  <text
                    x="210"
                    y="226"
                    textAnchor="middle"
                    fontFamily="JetBrains Mono"
                    fontSize="9"
                    fill="#94a3b8"
                    letterSpacing="1"
                  >
                    12 nodes
                  </text>
                </g>

                <g className="login-agent-node r1">
                  <g transform="translate(210, 130)">
                    <polygon
                      points="-12,-7 0,-14 12,-7 12,7 0,14 -12,7"
                      fill="#0d1117"
                      stroke="#00d4aa"
                      strokeWidth={1.2}
                    />
                    <text
                      x="0"
                      y="3"
                      textAnchor="middle"
                      fontFamily="JetBrains Mono"
                      fontSize="8"
                      fontWeight={700}
                      fill="#00d4aa"
                    >
                      A1
                    </text>
                  </g>
                  <g transform="translate(290, 210)">
                    <polygon
                      points="-12,-7 0,-14 12,-7 12,7 0,14 -12,7"
                      fill="#0d1117"
                      stroke="#ff9f43"
                      strokeWidth={1.2}
                    />
                    <text
                      x="0"
                      y="3"
                      textAnchor="middle"
                      fontFamily="JetBrains Mono"
                      fontSize="8"
                      fontWeight={700}
                      fill="#ff9f43"
                    >
                      B2
                    </text>
                  </g>
                  <g transform="translate(210, 290)">
                    <polygon
                      points="-12,-7 0,-14 12,-7 12,7 0,14 -12,7"
                      fill="#0d1117"
                      stroke="#00d4aa"
                      strokeWidth={1.2}
                    />
                    <text
                      x="0"
                      y="3"
                      textAnchor="middle"
                      fontFamily="JetBrains Mono"
                      fontSize="8"
                      fontWeight={700}
                      fill="#00d4aa"
                    >
                      C3
                    </text>
                  </g>
                  <g transform="translate(130, 210)">
                    <polygon
                      points="-12,-7 0,-14 12,-7 12,7 0,14 -12,7"
                      fill="#0d1117"
                      stroke="#00d4aa"
                      strokeWidth={1.2}
                    />
                    <text
                      x="0"
                      y="3"
                      textAnchor="middle"
                      fontFamily="JetBrains Mono"
                      fontSize="8"
                      fontWeight={700}
                      fill="#00d4aa"
                    >
                      D4
                    </text>
                  </g>
                </g>

                <g className="login-agent-node r2">
                  <g transform="translate(294, 116)">
                    <polygon
                      points="-9,-5 0,-10 9,-5 9,5 0,10 -9,5"
                      fill="#0d1117"
                      stroke="#00d4aa"
                      strokeWidth={1}
                      opacity="0.7"
                    />
                  </g>
                  <g transform="translate(304, 304)">
                    <polygon
                      points="-9,-5 0,-10 9,-5 9,5 0,10 -9,5"
                      fill="#0d1117"
                      stroke="#ff9f43"
                      strokeWidth={1}
                      opacity="0.7"
                    />
                  </g>
                  <g transform="translate(116, 304)">
                    <polygon
                      points="-9,-5 0,-10 9,-5 9,5 0,10 -9,5"
                      fill="#0d1117"
                      stroke="#00d4aa"
                      strokeWidth={1}
                      opacity="0.7"
                    />
                  </g>
                  <g transform="translate(116, 116)">
                    <polygon
                      points="-9,-5 0,-10 9,-5 9,5 0,10 -9,5"
                      fill="#0d1117"
                      stroke="#ff4757"
                      strokeWidth={1}
                      opacity="0.7"
                    />
                  </g>
                </g>

                <g className="login-agent-node r3">
                  <circle cx="210" cy="30" r="2" fill="#00d4aa" opacity="0.6" />
                  <circle cx="390" cy="210" r="2" fill="#00d4aa" opacity="0.4" />
                  <circle cx="210" cy="390" r="2" fill="#ff9f43" opacity="0.5" />
                  <circle cx="30" cy="210" r="2" fill="#00d4aa" opacity="0.6" />
                  <circle cx="350" cy="100" r="1.5" fill="#00d4aa" opacity="0.4" />
                  <circle cx="100" cy="350" r="1.5" fill="#00d4aa" opacity="0.4" />
                </g>
              </svg>
            </div>
          </div>

          {/* telemetry */}
          <div className="login-telemetry" aria-label="Hive telemetry snapshot">
            <div className="login-telemetry-cell">
              <div className="label">{t('login.activeAgents')}</div>
              <div className="value">{agentCount}</div>
              <div className="delta">{t('login.agentsDelta')}</div>
            </div>
            <div className="login-telemetry-cell">
              <div className="label">{t('login.workflowsLive')}</div>
              <div className="value">{workflowCount}</div>
              <div className="delta warm">{t('login.workflowsDelta')}</div>
            </div>
            <div className="login-telemetry-cell">
              <div className="label">{t('login.tokensToday')}</div>
              <div className="value">
                8.2<span className="unit">M</span>
              </div>
              <div className="delta">{t('login.tokensDelta')}</div>
            </div>
            <div className="login-telemetry-cell">
              <div className="label">{t('login.uptime')}</div>
              <div className="value">
                99.97<span className="unit">%</span>
              </div>
              <div className="delta">{t('login.uptimeDelta')}</div>
            </div>
          </div>
        </section>

        {/* ======== RIGHT — Auth Console ======== */}
        <section className="login-auth-side">
          <div className="login-auth-meta">
            <div className="crumbs">
              / auth / <span>identify</span>
            </div>
            <div className="build">
              <span className="pulse" />
              build · a3f2c7d
            </div>
            <LanguageSwitcher />
          </div>

          <form className="login-auth-form" onSubmit={handleSubmit} autoComplete="on" noValidate>
            <h2 className="login-auth-title">
              {t('login.identify')}
            </h2>
            <p className="login-auth-sub">
              {t('login.newToColony')}{' '}
              <a href="#" onClick={e => { e.preventDefault(); message.info(t('login.contactAdminForAccess')) }}>
                {t('login.requestAccess')} →
              </a>
            </p>

            <div className="login-tenant-select" role="button" tabIndex={-1}>
              <div className="login-tenant-hex">{tenantHint.code.slice(0, 2).toUpperCase()}</div>
              <div className="login-tenant-info">
                <div className="label">{t('login.tenantWorkspace')}</div>
                <div className="name">
                  {tenantHint.name}
                  {tenantHint.badge && <span className="badge">{tenantHint.badge}</span>}
                </div>
              </div>
              <div className="login-tenant-chevron" aria-hidden="true">
                ▾
              </div>
            </div>

            <div className="login-field f-user">
              <div className="login-field-label">
                <span>{t('login.identifier')}</span>
                <span className="hint">{t('login.identifierHint')}</span>
              </div>
              <div className="login-field-input">
                <span className="login-field-prefix" aria-hidden="true">$</span>
                <input
                  ref={usernameInputRef}
                  type="text"
                  name="username"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="aurora.han@aerolabs.cn"
                  autoComplete="username"
                  aria-label={t('login.username')}
                  data-testid="login-username"
                />
              </div>
            </div>

            <div className="login-field f-pass">
              <div className="login-field-label">
                <span>{t('login.pheromoneKey')}</span>
                <span className="hint">
                  <a
                    href="#"
                    onClick={e => {
                      e.preventDefault()
                      message.info(t('login.contactAdminForReset'))
                    }}
                  >
                    forgot?
                  </a>
                </span>
              </div>
              <div className="login-field-input">
                <span className="login-field-prefix" aria-hidden="true">▣</span>
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  autoComplete="current-password"
                  aria-label={t('login.password')}
                  data-testid="login-password"
                />
                <button
                  type="button"
                  className="login-field-suffix"
                  onClick={() => setShowPassword(v => !v)}
                  aria-label={showPassword ? t('login.hidePassword') : t('login.showPassword')}
                  title={showPassword ? t('login.hide') : t('login.show')}
                >
                  {showPassword ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
                      <path d="M3 3l18 18" />
                      <path d="M10.6 6.1A10.5 10.5 0 0112 6c5 0 9.3 3.4 10.5 8a13 13 0 01-3.3 4.6M6.7 7.7A12.4 12.4 0 001.5 12c1.2 4.6 5.5 8 10.5 8a10.7 10.7 0 005.3-1.4" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  ) : (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5}>
                      <path d="M1.5 12C2.7 7.4 7 4 12 4s9.3 3.4 10.5 8c-1.2 4.6-5.5 8-10.5 8S2.7 16.6 1.5 12z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
              <div className="login-strength-bar" aria-hidden="true">
                <div className="fill" style={{ width: `${strength}%` }} />
              </div>
            </div>

            <div className="login-options">
              <label className="login-checkbox">
                <input
                  type="checkbox"
                  checked={remember}
                  onChange={e => setRemember(e.target.checked)}
                  data-testid="login-remember"
                />
                <span className="box" aria-hidden="true" />
                {t('login.rememberMe')}
              </label>
              <span className="login-ttl">
                {t('login.sessionTtl')} · <span className="num">24h</span>
              </span>
            </div>

            <div className="login-submit">
              <button
                type="submit"
                className="login-btn-primary"
                disabled={loading}
                data-testid="login-submit"
              >
                {loading ? (
                  <>
                    <span className="spinner" />
                    {t('login.awakening')}
                  </>
                ) : (
                  <>
                    {t('login.enterHive')}
                    <span className="arrow" aria-hidden="true" />
                  </>
                )}
              </button>
              <button
                type="button"
                className="login-btn-secondary"
                title={t('login.hardwareKey')}
                onClick={() => message.info(t('login.hardwareKeyNotOpen'))}
                aria-label={t('login.hardwareKeyLogin')}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.6}>
                  <rect x="3" y="11" width="18" height="11" rx="2" />
                  <path d="M7 11V7a5 5 0 0110 0v4" />
                </svg>
              </button>
            </div>

            <div className="login-divider">{t('login.orSignalVia')}</div>

            <div className="login-alt-methods">
              {ALT_METHODS.map(m => (
                <button
                  key={m.key}
                  type="button"
                  className="login-alt-method"
                  onClick={altMethodNotice(m.label)}
                >
                  {m.icon}
                  {m.label}
                </button>
              ))}
            </div>

            <div className="login-terminal" aria-hidden="true">
              <div className="login-terminal-head">
                <div className="lights">
                  <div className="light r" />
                  <div className="light y" />
                  <div className="light g" />
                </div>
                <div className="title">auth.console</div>
                <div className="indicator">
                  <span className="ind-dot" />
                  STREAMING
                </div>
              </div>
              <div className="login-terminal-body">
                {terminalLines.map((line, idx) => {
                  const isLast = idx === terminalLines.length - 1
                  return (
                    <div key={line.id} className="line">
                      <span className="ts">{line.ts}</span>
                      <span className={`lvl ${line.level}`}>[{line.label}]</span>
                      <span className="msg">
                        <span dangerouslySetInnerHTML={{ __html: line.msg }} />
                        {isLast && <span className="cursor" />}
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>
          </form>
        </section>
      </div>

      {/* Bottom rail */}
      <div className="login-bottom-rail" aria-hidden="true">
        <div className="col">
          <span>© 2026 SchemaPlex Systems</span>
          <span className="sep">/</span>
          <span>Privacy</span>
          <span className="sep">/</span>
          <span>Terms</span>
          <span className="sep">/</span>
          <span>Status</span>
        </div>
        <div className="col">
          <span>
            deploy <span className="v">2h ago</span>
          </span>
          <span className="sep">/</span>
          <span>
            latency <span className="v lit">8ms</span>
          </span>
          <span className="sep">/</span>
          <span>
            env <span className="v">prod</span>
          </span>
        </div>
      </div>
    </div>
  )
}
