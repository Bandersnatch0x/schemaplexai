# 深渊蜂巢 UI/UE 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 SchemaPlexAI 前端项目（schemaplexai-ui）中实施"深渊蜂巢"设计系统，包括全局样式、布局框架、基础组件库和四个标志性页面。

**Architecture:** 基于 Ant Design 5 ConfigProvider 覆写 theme token + CSS 变量实现暗色主题。通过两种 Layout 组件（沉浸式/渐进式）适配不同页面。基础组件（HexIcon/StatCard/PillNav/TerminalLog）作为可复用单元。标志性页面独立实现，通过 React Router 集成。

**Tech Stack:** React 18 + TypeScript + Vite + Ant Design 5 + Zustand + ECharts + @antv/x6 + Vitest + React Testing Library

---

## 文件结构映射

```
schemaplexai-ui/
├── package.json                          # 添加测试脚本和依赖
├── vite.config.ts                        # 添加 vitest 配置
├── src/
│   ├── main.tsx                          # 注入 ConfigProvider + 字体加载
│   ├── index.css                         # 全局样式覆写
│   ├── App.tsx                           # 保持路由不变
│   ├── theme/
│   │   └── index.ts                      # AntD theme tokens（深渊蜂巢主题）
│   ├── styles/
│   │   ├── variables.css                 # CSS 变量定义
│   │   └── global.css                    # 全局覆写（滚动条/选择色等）
│   ├── components/
│   │   ├── Hive/                         # 蜂巢设计系统组件
│   │   │   ├── HexIcon.tsx               # 六边形图标组件
│   │   │   ├── HexIcon.test.tsx          # 测试
│   │   │   ├── StatCard.tsx              # 统计卡片
│   │   │   ├── StatCard.test.tsx         # 测试
│   │   │   ├── PillNav.tsx               # 药丸导航
│   │   │   ├── PillNav.test.tsx          # 测试
│   │   │   ├── TerminalLog.tsx           # 终端日志
│   │   │   ├── TerminalLog.test.tsx      # 测试
│   │   │   └── index.ts                  # 统一导出
│   │   └── Layout/
│   │       ├── ImmersiveLayout.tsx       # 沉浸式布局（驾驶舱/画布）
│   │       ├── ProgressiveLayout.tsx     # 渐进式布局（监控/详情）
│   │       ├── Sidebar.tsx               # 侧边栏（含两种模式）
│   │       ├── Header.tsx                # 顶部状态栏
│   │       ├── FloatingPanel.tsx         # 右侧面板（滑出）
│   │       └── index.ts                  # 统一导出
│   ├── pages/
│   │   ├── Cockpit/                      # B. 驾驶舱大屏
│   │   │   └── index.tsx
│   │   ├── AgentCanvas/                  # A. 编排画布
│   │   │   └── index.tsx
│   │   ├── WorkflowMonitor/              # D. 工作流监控
│   │   │   └── index.tsx
│   │   └── AgentDetail/                  # C. Agent 详情
│   │       └── index.tsx
│   └── router/
│       └── index.tsx                     # 添加新路由
```

---

## 阶段 1：基础设施

### Task 1：安装依赖

**Files:**
- Modify: `schemaplexai-ui/package.json`

- [ ] **Step 1：安装开发依赖**

```bash
cd schemaplexai-ui
npm install -D vitest @vitest/ui @testing-library/react @testing-library/jest-dom jsdom
```

Expected：安装成功，无报错。

- [ ] **Step 2：安装运行时依赖**

```bash
npm install @antv/x6 @antv/x6-react-shape echarts
```

Expected：安装成功，无报错。

- [ ] **Step 3：在 package.json 中添加测试脚本**

Modify `schemaplexai-ui/package.json` scripts section:

```json
"scripts": {
  "dev": "vite",
  "build": "tsc && vite build",
  "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
  "preview": "vite preview",
  "test": "vitest",
  "test:ui": "vitest --ui"
}
```

- [ ] **Step 4：Commit**

```bash
git add package.json package-lock.json
git commit -m "chore: add vitest, testing-library, x6, echarts for abyss-hive UI"
```

---

### Task 2：配置 Vitest

**Files:**
- Create: `schemaplexai-ui/vitest.config.ts`
- Modify: `schemaplexai-ui/tsconfig.json` (若需要 types)

- [ ] **Step 1：创建 vitest 配置**

```typescript
// schemaplexai-ui/vitest.config.ts
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
```

- [ ] **Step 2：创建测试 setup 文件**

```typescript
// schemaplexai-ui/src/test/setup.ts
import '@testing-library/jest-dom'
```

- [ ] **Step 3：运行测试确认配置**

```bash
npx vitest run --reporter=verbose
```

Expected：`No test files found, exiting with code 0`

- [ ] **Step 4：Commit**

```bash
git add vitest.config.ts src/test/setup.ts
git commit -m "chore: configure vitest with jsdom and testing-library"
```

---

### Task 3：创建 AntD 主题配置

**Files:**
- Create: `schemaplexai-ui/src/theme/index.ts`

- [ ] **Step 1：写主题配置测试**

```typescript
// schemaplexai-ui/src/theme/index.test.ts
import { describe, it, expect } from 'vitest'
import { abyssHiveTheme } from './index'

describe('abyssHiveTheme', () => {
  it('has correct base background color', () => {
    expect(abyssHiveTheme.token?.colorBgBase).toBe('#0a0e1a')
  })

  it('has correct primary color', () => {
    expect(abyssHiveTheme.token?.colorPrimary).toBe('#00d4aa')
  })

  it('has transparent input background', () => {
    expect(abyssHiveTheme.components?.Input?.colorBgContainer).toBe('transparent')
  })

  it('has correct table hover background', () => {
    expect(abyssHiveTheme.components?.Table?.rowHoverBg).toBe('#111827')
  })
})
```

- [ ] **Step 2：运行测试确认失败**

```bash
npx vitest run src/theme/index.test.ts --reporter=verbose
```

Expected：`FAIL` — `Module not found: ./index`

- [ ] **Step 3：创建主题配置文件**

```typescript
// schemaplexai-ui/src/theme/index.ts
import type { ThemeConfig } from 'antd'

export const abyssHiveTheme: ThemeConfig = {
  token: {
    colorBgBase: '#0a0e1a',
    colorBgContainer: '#111827',
    colorBgElevated: '#111827',
    colorBgLayout: '#0a0e1a',
    colorBorder: '#1e2a33',
    colorBorderSecondary: '#1e2a3380',
    colorPrimary: '#00d4aa',
    colorError: '#ff4757',
    colorWarning: '#ff9f43',
    colorSuccess: '#00d4aa',
    colorText: '#e2e8f0',
    colorTextSecondary: '#64748b',
    colorTextTertiary: '#475569',
    colorTextPlaceholder: '#475569',
    colorLink: '#00d4aa',
    colorLinkHover: '#00f5c4',
    colorLinkActive: '#00b894',
    fontFamily: "'Inter', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    fontFamilyCode: "'JetBrains Mono', 'Fira Code', monospace",
    fontSize: 14,
    fontSizeSM: 12,
    fontSizeLG: 16,
    fontSizeXL: 20,
    borderRadius: 8,
    borderRadiusSM: 4,
    borderRadiusLG: 12,
    paddingXS: 8,
    paddingSM: 12,
    padding: 16,
    paddingMD: 20,
    paddingLG: 24,
    paddingXL: 32,
    controlHeight: 40,
    controlHeightSM: 32,
    controlHeightLG: 48,
  },
  components: {
    Card: {
      colorBgContainer: '#111827',
      borderRadius: 8,
      headerBg: 'transparent',
      headerFontSize: 16,
      actionsBg: 'transparent',
    },
    Button: {
      borderRadius: 6,
      borderRadiusSM: 4,
      primaryShadow: '0 0 12px #00d4aa30',
      dangerShadow: '0 0 12px #ff475730',
    },
    Table: {
      headerBg: '#0d1117',
      headerColor: '#64748b',
      headerSplitColor: '#1e2a33',
      rowHoverBg: '#111827',
      borderColor: '#1e2a3380',
      cellPaddingBlock: 12,
      cellPaddingInline: 16,
    },
    Input: {
      colorBgContainer: 'transparent',
      colorBorder: '#1e2a33',
      activeBorderColor: '#00d4aa',
      activeShadow: '0 0 0 1px #00d4aa20, 0 0 12px #00d4aa15',
      borderRadius: 8,
      colorTextPlaceholder: '#475569',
      errorActiveShadow: '0 0 0 1px #ff475720',
      hoverBorderColor: '#1e3a5f',
    },
    Select: {
      colorBgContainer: 'transparent',
      colorBorder: '#1e2a33',
      borderRadius: 8,
      optionSelectedBg: '#00d4aa20',
      optionActiveBg: '#1e3a5f',
    },
    Modal: {
      colorBgElevated: '#111827',
      borderRadius: 12,
      headerBg: 'transparent',
      contentBg: '#111827',
    },
    Drawer: {
      colorBgElevated: '#111827',
    },
    Tag: {
      borderRadius: 4,
      defaultBg: 'transparent',
    },
    Tabs: {
      colorBgContainer: 'transparent',
      inkBarColor: '#00d4aa',
      itemActiveColor: '#00d4aa',
      itemHoverColor: '#00f5c4',
      itemColor: '#64748b',
    },
    Tooltip: {
      colorBgDefault: '#111827',
      colorTextLightSolid: '#e2e8f0',
    },
    Menu: {
      colorBgContainer: '#0d1117',
      colorItemBgSelected: '#111827',
      colorItemText: '#64748b',
      colorItemTextSelected: '#e2e8f0',
      colorItemTextHover: '#e2e8f0',
      colorActiveBarWidth: 3,
      colorActiveBarBorderSize: 0,
    },
    Switch: {
      colorPrimary: '#00d4aa',
      colorPrimaryHover: '#00f5c4',
    },
    Slider: {
      trackBg: '#00d4aa',
      trackHoverBg: '#00f5c4',
      handleColor: '#00d4aa',
    },
    Progress: {
      defaultColor: '#00d4aa',
      remainingColor: '#1e2a33',
    },
  },
}
```

- [ ] **Step 4：运行测试确认通过**

```bash
npx vitest run src/theme/index.test.ts --reporter=verbose
```

Expected：`PASS` — 4 assertions passed

- [ ] **Step 5：Commit**

```bash
git add src/theme/
git commit -m "feat(theme): add abyss-hive Ant Design theme config with tests"
```

---

### Task 4：创建全局样式文件

**Files:**
- Create: `schemaplexai-ui/src/styles/variables.css`
- Create: `schemaplexai-ui/src/styles/global.css`
- Modify: `schemaplexai-ui/src/index.css`

- [ ] **Step 1：创建 CSS 变量文件**

```css
/* schemaplexai-ui/src/styles/variables.css */
:root {
  /* Backgrounds */
  --abyss-bg: #0a0e1a;
  --abyss-sidebar: #0d1117;
  --abyss-card: #111827;
  --abyss-border: #1e2a33;
  --abyss-hover: #1e3a5f;

  /* Bioluminescent accents */
  --hive-cyan: #00d4aa;
  --hive-amber: #ff9f43;
  --hive-red: #ff4757;

  /* Text */
  --text-primary: #e2e8f0;
  --text-secondary: #64748b;
  --text-tertiary: #475569;

  /* Typography */
  --font-sans: 'Inter', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', 'SF Mono', monospace;

  /* Spacing */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;
  --space-12: 48px;

  /* Radius */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-full: 9999px;

  /* Glow shadows */
  --glow-cyan: 0 0 12px #00d4aa30;
  --glow-amber: 0 0 12px #ff9f4330;
  --glow-red: 0 0 12px #ff475730;
  --focus-cyan: 0 0 0 1px #00d4aa20, 0 0 12px #00d4aa15;
}
```

- [ ] **Step 2：创建全局覆写样式**

```css
/* schemaplexai-ui/src/styles/global.css */
html, body, #root {
  background: var(--abyss-bg);
  color: var(--text-primary);
  font-family: var(--font-sans);
  height: 100%;
}

/* Minimal scrollbar */
::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: var(--abyss-border);
  border-radius: var(--radius-full);
}
::-webkit-scrollbar-thumb:hover {
  background: var(--hive-cyan);
}

/* Text selection */
::selection {
  background: #00d4aa40;
  color: var(--text-primary);
}

/* Remove default AntD layout backgrounds */
.ant-layout {
  background: var(--abyss-bg);
}

.ant-layout-sider {
  background: var(--abyss-sidebar) !important;
}

.ant-layout-header {
  background: var(--abyss-sidebar) !important;
}

/* Font loading fallback */
.font-mono {
  font-family: var(--font-mono);
}
```

- [ ] **Step 3：更新 index.css**

```css
/* schemaplexai-ui/src/index.css */
@import './styles/variables.css';
@import './styles/global.css';

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
```

- [ ] **Step 4：Commit**

```bash
git add src/styles/ src/index.css
git commit -m "feat(styles): add CSS variables and global overrides for abyss-hive theme"
```

---

### Task 5：更新入口文件注入主题

**Files:**
- Modify: `schemaplexai-ui/src/main.tsx`

- [ ] **Step 1：更新 main.tsx 注入主题和字体**

```tsx
// schemaplexai-ui/src/main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { abyssHiveTheme } from './theme'
import App from './App'
import './index.css'

// Load Google Fonts
const fontLink = document.createElement('link')
fontLink.href = 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Noto+Sans+SC:wght@400;500;600&display=swap'
fontLink.rel = 'stylesheet'
document.head.appendChild(fontLink)

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={abyssHiveTheme}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>,
)
```

- [ ] **Step 2：启动 dev server 验证主题生效**

```bash
npm run dev
```

Expected：页面背景变为深蓝黑色 `#0a0e1a`，AntD 组件（如按钮）使用 `#00d4aa` 作为主色。

- [ ] **Step 3：Commit**

```bash
git add src/main.tsx
git commit -m "feat(theme): wire abyss-hive theme into ConfigProvider with Google Fonts"
```

---

## 阶段 2：基础组件库（Hive Components）

### Task 6：HexIcon 组件

**Files:**
- Create: `schemaplexai-ui/src/components/Hive/HexIcon.tsx`
- Create: `schemaplexai-ui/src/components/Hive/HexIcon.test.tsx`

- [ ] **Step 1：写测试**

```tsx
// schemaplexai-ui/src/components/Hive/HexIcon.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HexIcon } from './HexIcon'

describe('HexIcon', () => {
  it('renders with default size and color', () => {
    render(<HexIcon data-testid="hex" />)
    const icon = screen.getByTestId('hex')
    expect(icon).toBeInTheDocument()
    expect(icon).toHaveStyle({ width: '36px', height: '36px' })
  })

  it('renders with custom size', () => {
    render(<HexIcon size={48} data-testid="hex-large" />)
    const icon = screen.getByTestId('hex-large')
    expect(icon).toHaveStyle({ width: '48px', height: '48px' })
  })

  it('applies active styles when active=true', () => {
    render(<HexIcon active data-testid="hex-active" />)
    const icon = screen.getByTestId('hex-active')
    expect(icon).toHaveStyle({ borderColor: '#00d4aa' })
  })
})
```

- [ ] **Step 2：运行测试确认失败**

```bash
npx vitest run src/components/Hive/HexIcon.test.tsx --reporter=verbose
```

Expected：`FAIL` — `Module not found: ./HexIcon`

- [ ] **Step 3：实现组件**

```tsx
// schemaplexai-ui/src/components/Hive/HexIcon.tsx
import React from 'react'

export interface HexIconProps {
  size?: number
  color?: string
  active?: boolean
  className?: string
  style?: React.CSSProperties
  children?: React.ReactNode
  'data-testid'?: string
}

export const HexIcon: React.FC<HexIconProps> = ({
  size = 36,
  color = '#00d4aa',
  active = false,
  className = '',
  style,
  children,
  'data-testid': testId,
}) => {
  const baseStyle: React.CSSProperties = {
    width: size,
    height: size,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
    border: `1px solid ${active ? color : '#1e2a33'}`,
    background: active ? `${color}20` : '#111827',
    boxShadow: active ? `0 0 8px ${color}30` : 'none',
    transition: 'all 0.2s ease',
    cursor: 'pointer',
    ...style,
  }

  return (
    <div className={className} style={baseStyle} data-testid={testId}>
      {children || (
        <span style={{ color, fontSize: size * 0.45 }}>⬡</span>
      )}
    </div>
  )
}
```

- [ ] **Step 4：运行测试确认通过**

```bash
npx vitest run src/components/Hive/HexIcon.test.tsx --reporter=verbose
```

Expected：`PASS` — 3 assertions passed

- [ ] **Step 5：Commit**

```bash
git add src/components/Hive/HexIcon.tsx src/components/Hive/HexIcon.test.tsx
git commit -m "feat(component): add HexIcon with active states and tests"
```

---

### Task 7：StatCard 组件

**Files:**
- Create: `schemaplexai-ui/src/components/Hive/StatCard.tsx`
- Create: `schemaplexai-ui/src/components/Hive/StatCard.test.tsx`

- [ ] **Step 1：写测试**

```tsx
// schemaplexai-ui/src/components/Hive/StatCard.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatCard } from './StatCard'

describe('StatCard', () => {
  it('renders label and value', () => {
    render(<StatCard label="Active Agents" value="7,843" />)
    expect(screen.getByText('Active Agents')).toBeInTheDocument()
    expect(screen.getByText('7,843')).toBeInTheDocument()
  })

  it('renders change indicator', () => {
    render(<StatCard label="Test" value="100" change={12.5} />)
    expect(screen.getByText('↑ 12.5%')).toBeInTheDocument()
  })

  it('renders sparkline bars', () => {
    render(<StatCard label="Test" value="100" sparkline={[40, 60, 45, 80, 55, 70, 90]} />)
    const bars = screen.getAllByTestId('spark-bar')
    expect(bars).toHaveLength(7)
  })
})
```

- [ ] **Step 2：运行测试确认失败**

```bash
npx vitest run src/components/Hive/StatCard.test.tsx --reporter=verbose
```

Expected：`FAIL` — `Module not found: ./StatCard`

- [ ] **Step 3：实现组件**

```tsx
// schemaplexai-ui/src/components/Hive/StatCard.tsx
import React from 'react'

export interface StatCardProps {
  label: string
  value: string | number
  change?: number
  unit?: string
  sparkline?: number[]
  color?: 'cyan' | 'amber' | 'red'
  className?: string
}

const COLOR_MAP = {
  cyan: '#00d4aa',
  amber: '#ff9f43',
  red: '#ff4757',
}

export const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  change,
  unit,
  sparkline,
  color = 'cyan',
  className = '',
}) => {
  const c = COLOR_MAP[color]
  const changeText = change !== undefined
    ? `${change >= 0 ? '↑' : '↓'} ${Math.abs(change)}%`
    : null

  return (
    <div
      className={className}
      style={{
        background: '#111827',
        borderRadius: 8,
        padding: 16,
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Left accent bar */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          top: 0,
          bottom: 0,
          width: 3,
          background: c,
        }}
      />

      <div style={{ color: '#64748b', fontSize: 11, fontWeight: 500, letterSpacing: '0.05em', marginBottom: 8 }}>
        {label.toUpperCase()}
      </div>

      <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 4 }}>
        <span
          style={{
            color: '#e2e8f0',
            fontSize: 28,
            fontWeight: 700,
            fontFamily: "'JetBrains Mono', monospace",
          }}
        >
          {value}
        </span>
        {changeText && (
          <span style={{ color: c, fontSize: 12 }}>{changeText}</span>
        )}
      </div>

      {unit && (
        <div style={{ color: '#64748b', fontSize: 10 }}>{unit}</div>
      )}

      {sparkline && sparkline.length > 0 && (
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 2, marginTop: 12, height: 32 }}>
          {sparkline.map((h, i) => {
            const max = Math.max(...sparkline)
            const pct = max > 0 ? (h / max) * 100 : 0
            return (
              <div
                key={i}
                data-testid="spark-bar"
                style={{
                  width: 8,
                  height: `${Math.max(pct, 10)}%`,
                  background: `${c}${Math.floor(30 + (i / sparkline.length) * 40).toString(16).padStart(2, '0')}`,
                  borderRadius: 1,
                  minHeight: 2,
                }}
              />
            )
          })}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 4：运行测试确认通过**

```bash
npx vitest run src/components/Hive/StatCard.test.tsx --reporter=verbose
```

Expected：`PASS` — 3 assertions passed

- [ ] **Step 5：Commit**

```bash
git add src/components/Hive/StatCard.tsx src/components/Hive/StatCard.test.tsx
git commit -m "feat(component): add StatCard with sparkline and change indicator"
```

---

### Task 8：PillNav 组件

**Files:**
- Create: `schemaplexai-ui/src/components/Hive/PillNav.tsx`
- Create: `schemaplexai-ui/src/components/Hive/PillNav.test.tsx`

- [ ] **Step 1：写测试**

```tsx
// schemaplexai-ui/src/components/Hive/PillNav.test.tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PillNav } from './PillNav'

describe('PillNav', () => {
  const items = [
    { key: 'a', label: 'Topology' },
    { key: 'b', label: 'List' },
    { key: 'c', label: 'Code' },
  ]

  it('renders all items', () => {
    render(<PillNav items={items} activeKey="a" onChange={vi.fn()} />)
    expect(screen.getByText('Topology')).toBeInTheDocument()
    expect(screen.getByText('List')).toBeInTheDocument()
    expect(screen.getByText('Code')).toBeInTheDocument()
  })

  it('highlights active item with cyan background', () => {
    render(<PillNav items={items} activeKey="b" onChange={vi.fn()} />)
    const active = screen.getByText('List').parentElement
    expect(active).toHaveStyle({ backgroundColor: '#00d4aa' })
  })

  it('calls onChange when clicking inactive item', () => {
    const onChange = vi.fn()
    render(<PillNav items={items} activeKey="a" onChange={onChange} />)
    fireEvent.click(screen.getByText('List'))
    expect(onChange).toHaveBeenCalledWith('b')
  })
})
```

- [ ] **Step 2：运行测试确认失败**

```bash
npx vitest run src/components/Hive/PillNav.test.tsx --reporter=verbose
```

Expected：`FAIL` — `Module not found: ./PillNav`

- [ ] **Step 3：实现组件**

```tsx
// schemaplexai-ui/src/components/Hive/PillNav.tsx
import React from 'react'

export interface PillNavItem {
  key: string
  label: string
}

export interface PillNavProps {
  items: PillNavItem[]
  activeKey: string
  onChange: (key: string) => void
  className?: string
}

export const PillNav: React.FC<PillNavProps> = ({
  items,
  activeKey,
  onChange,
  className = '',
}) => {
  return (
    <div
      className={className}
      style={{
        display: 'flex',
        background: '#0d1117',
        border: '1px solid #1e2a33',
        borderRadius: 20,
        padding: 3,
        gap: 3,
      }}
    >
      {items.map((item) => {
        const isActive = item.key === activeKey
        return (
          <button
            key={item.key}
            onClick={() => onChange(item.key)}
            style={{
              padding: '5px 16px',
              borderRadius: 16,
              border: 'none',
              background: isActive ? '#00d4aa' : 'transparent',
              color: isActive ? '#0a0e1a' : '#64748b',
              fontSize: 12,
              fontWeight: isActive ? 600 : 400,
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              fontFamily: 'inherit',
            }}
          >
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
```

- [ ] **Step 4：运行测试确认通过**

```bash
npx vitest run src/components/Hive/PillNav.test.tsx --reporter=verbose
```

Expected：`PASS` — 3 assertions passed

- [ ] **Step 5：Commit**

```bash
git add src/components/Hive/PillNav.tsx src/components/Hive/PillNav.test.tsx
git commit -m "feat(component): add PillNav view switcher with tests"
```

---

### Task 9：TerminalLog 组件

**Files:**
- Create: `schemaplexai-ui/src/components/Hive/TerminalLog.tsx`
- Create: `schemaplexai-ui/src/components/Hive/TerminalLog.test.tsx`
- Create: `schemaplexai-ui/src/components/Hive/index.ts`

- [ ] **Step 1：写测试**

```tsx
// schemaplexai-ui/src/components/Hive/TerminalLog.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { TerminalLog } from './TerminalLog'

describe('TerminalLog', () => {
  const logs = [
    { timestamp: '14:30:01', level: 'INFO' as const, message: 'Agent initialized' },
    { timestamp: '14:30:15', level: 'WARN' as const, message: 'Queue depth high' },
    { timestamp: '14:30:22', level: 'ERROR' as const, message: 'Connection failed' },
  ]

  it('renders all log entries', () => {
    render(<TerminalLog logs={logs} />)
    expect(screen.getByText('Agent initialized')).toBeInTheDocument()
    expect(screen.getByText('Queue depth high')).toBeInTheDocument()
    expect(screen.getByText('Connection failed')).toBeInTheDocument()
  })

  it('renders blinking cursor at bottom', () => {
    render(<TerminalLog logs={logs} />)
    expect(screen.getByText('▌')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2：运行测试确认失败**

```bash
npx vitest run src/components/Hive/TerminalLog.test.tsx --reporter=verbose
```

Expected：`FAIL` — `Module not found: ./TerminalLog`

- [ ] **Step 3：实现组件**

```tsx
// schemaplexai-ui/src/components/Hive/TerminalLog.tsx
import React, { useRef, useEffect } from 'react'

export type LogLevel = 'INFO' | 'WARN' | 'ERROR' | 'DEBUG'

export interface LogEntry {
  timestamp: string
  level: LogLevel
  message: string
}

export interface TerminalLogProps {
  logs: LogEntry[]
  className?: string
  style?: React.CSSProperties
}

const LEVEL_COLORS: Record<LogLevel, string> = {
  INFO: '#00d4aa',
  WARN: '#ff9f43',
  ERROR: '#ff4757',
  DEBUG: '#64748b',
}

export const TerminalLog: React.FC<TerminalLogProps> = ({
  logs,
  className = '',
  style,
}) => {
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [logs])

  return (
    <div
      ref={scrollRef}
      className={className}
      style={{
        background: '#0a0e1a',
        border: '1px solid #1e2a33',
        borderRadius: 8,
        padding: 16,
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: 11,
        lineHeight: 1.8,
        overflow: 'auto',
        maxHeight: 400,
        ...style,
      }}
    >
      {logs.map((log, i) => (
        <div key={i} style={{ display: 'flex', gap: 8 }}>
          <span style={{ color: '#64748b' }}>[{log.timestamp}]</span>
          <span style={{ color: LEVEL_COLORS[log.level], fontWeight: 500 }}>
            {log.level}
          </span>
          <span style={{ color: '#e2e8f0' }}>{log.message}</span>
        </div>
      ))}
      <div style={{ marginTop: 8 }}>
        <span style={{ color: '#00d4aa' }}>▌</span>
      </div>
    </div>
  )
}
```

- [ ] **Step 4：创建统一导出文件**

```ts
// schemaplexai-ui/src/components/Hive/index.ts
export { HexIcon } from './HexIcon'
export type { HexIconProps } from './HexIcon'
export { StatCard } from './StatCard'
export type { StatCardProps } from './StatCard'
export { PillNav } from './PillNav'
export type { PillNavProps, PillNavItem } from './PillNav'
export { TerminalLog } from './TerminalLog'
export type { TerminalLogProps, LogEntry, LogLevel } from './TerminalLog'
```

- [ ] **Step 5：运行测试确认通过**

```bash
npx vitest run src/components/Hive/TerminalLog.test.tsx --reporter=verbose
```

Expected：`PASS` — 2 assertions passed

- [ ] **Step 6：Commit**

```bash
git add src/components/Hive/
git commit -m "feat(components): add TerminalLog with color-coded levels and auto-scroll"
```

---

## 阶段 3：布局框架

### Task 10：ImmersiveLayout 组件

**Files:**
- Create: `schemaplexai-ui/src/components/Layout/ImmersiveLayout.tsx`
- Create: `schemaplexai-ui/src/components/Layout/ImmersiveLayout.test.tsx`

- [ ] **Step 1：写测试**

```tsx
// schemaplexai-ui/src/components/Layout/ImmersiveLayout.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ImmersiveLayout } from './ImmersiveLayout'

describe('ImmersiveLayout', () => {
  it('renders children in canvas area', () => {
    render(
      <ImmersiveLayout>
        <div data-testid="canvas-content">Canvas</div>
      </ImmersiveLayout>
    )
    expect(screen.getByTestId('canvas-content')).toBeInTheDocument()
  })

  it('renders floating header with system name', () => {
    render(<ImmersiveLayout>test</ImmersiveLayout>)
    expect(screen.getByText('SchemaPlexAI')).toBeInTheDocument()
  })

  it('renders sidebar icons', () => {
    render(<ImmersiveLayout>test</ImmersiveLayout>)
    expect(screen.getByTestId('sidebar-cockpit')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-canvas')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2：运行测试确认失败**

```bash
npx vitest run src/components/Layout/ImmersiveLayout.test.tsx --reporter=verbose
```

Expected：`FAIL` — `Module not found: ./ImmersiveLayout`

- [ ] **Step 3：实现组件**

```tsx
// schemaplexai-ui/src/components/Layout/ImmersiveLayout.tsx
import React, { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'

export interface ImmersiveLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { key: 'cockpit', icon: '◉', label: '驾驶舱', path: '/cockpit' },
  { key: 'canvas', icon: '◆', label: '编排画布', path: '/canvas' },
  { key: 'workflows', icon: '▲', label: '工作流', path: '/workflows' },
  { key: 'agents', icon: '●', label: 'Agent', path: '/agents' },
]

export const ImmersiveLayout: React.FC<ImmersiveLayoutProps> = ({ children }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const [hovered, setHovered] = useState<string | null>(null)

  const activeKey = NAV_ITEMS.find(item => location.pathname.startsWith(item.path))?.key || 'cockpit'

  return (
    <div style={{ display: 'flex', height: '100vh', background: '#0a0e1a' }}>
      {/* Left Icon Sidebar */}
      <div
        style={{
          width: 52,
          background: '#0d1117',
          borderRight: '1px solid #1e2a33',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          padding: '10px 0',
          gap: 10,
          zIndex: 10,
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            background: '#00d4aa',
            borderRadius: 6,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#0a0e1a',
            fontSize: 12,
            fontWeight: 'bold',
          }}
        >
          S
        </div>

        {NAV_ITEMS.map(item => {
          const isActive = item.key === activeKey
          return (
            <div
              key={item.key}
              data-testid={`sidebar-${item.key}`}
              onClick={() => navigate(item.path)}
              onMouseEnter={() => setHovered(item.key)}
              onMouseLeave={() => setHovered(null)}
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                background: isActive ? '#00d4aa20' : '#1e2a33',
                border: `1px solid ${isActive ? '#00d4aa' : 'transparent'}`,
                boxShadow: isActive ? '0 0 8px #00d4aa30' : 'none',
                transition: 'all 0.2s ease',
                position: 'relative',
              }}
            >
              <span style={{ color: isActive ? '#00d4aa' : '#64748b', fontSize: 14 }}>
                {item.icon}
              </span>
              {hovered === item.key && (
                <div
                  style={{
                    position: 'absolute',
                    left: 44,
                    background: '#111827',
                    border: '1px solid #1e2a33',
                    borderRadius: 6,
                    padding: '4px 10px',
                    color: '#e2e8f0',
                    fontSize: 12,
                    whiteSpace: 'nowrap',
                    zIndex: 20,
                  }}
                >
                  {item.label}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* Main Canvas */}
      <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
        {/* Subtle grid background */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            opacity: 0.02,
            backgroundImage: 'linear-gradient(#1e2a33 1px, transparent 1px), linear-gradient(90deg, #1e2a33 1px, transparent 1px)',
            backgroundSize: '40px 40px',
            pointerEvents: 'none',
          }}
        />

        {/* Floating Header */}
        <div
          style={{
            position: 'absolute',
            top: 12,
            left: '50%',
            transform: 'translateX(-50%)',
            background: 'rgba(17, 24, 39, 0.8)',
            border: '1px solid #1e2a33',
            borderRadius: 20,
            padding: '8px 24px',
            color: '#64748b',
            fontSize: 12,
            backdropFilter: 'blur(12px)',
            display: 'flex',
            gap: 20,
            alignItems: 'center',
            zIndex: 5,
          }}
        >
          <span>SchemaPlexAI</span>
          <span style={{ color: '#1e2a33' }}>|</span>
          <span><span style={{ color: '#00d4aa' }}>●</span> 12 Agents</span>
          <span><span style={{ color: '#ff9f43' }}>●</span> 3 Executing</span>
        </div>

        {/* Content */}
        <div style={{ position: 'relative', zIndex: 1, height: '100%' }}>
          {children}
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 4：运行测试确认通过**

```bash
npx vitest run src/components/Layout/ImmersiveLayout.test.tsx --reporter=verbose
```

Expected：`PASS` — 3 assertions passed

- [ ] **Step 5：Commit**

```bash
git add src/components/Layout/ImmersiveLayout.tsx src/components/Layout/ImmersiveLayout.test.tsx
git commit -m "feat(layout): add ImmersiveLayout with icon sidebar and floating header"
```

---

### Task 11：ProgressiveLayout 组件

**Files:**
- Create: `schemaplexai-ui/src/components/Layout/ProgressiveLayout.tsx`
- Create: `schemaplexai-ui/src/components/Layout/ProgressiveLayout.test.tsx`
- Create: `schemaplexai-ui/src/components/Layout/index.ts`

- [ ] **Step 1：写测试**

```tsx
// schemaplexai-ui/src/components/Layout/ProgressiveLayout.test.tsx
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ProgressiveLayout } from './ProgressiveLayout'

describe('ProgressiveLayout', () => {
  it('renders children in content area', () => {
    render(
      <ProgressiveLayout>
        <div data-testid="content">Page Content</div>
      </ProgressiveLayout>
    )
    expect(screen.getByTestId('content')).toBeInTheDocument()
  })

  it('renders expanded sidebar with labels', () => {
    render(<ProgressiveLayout>test</ProgressiveLayout>)
    expect(screen.getByText('驾驶舱')).toBeInTheDocument()
    expect(screen.getByText('编排画布')).toBeInTheDocument()
    expect(screen.getByText('工作流')).toBeInTheDocument()
  })

  it('renders header with user avatar area', () => {
    render(<ProgressiveLayout>test</ProgressiveLayout>)
    expect(screen.getByText('SchemaPlexAI')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2：运行测试确认失败**

```bash
npx vitest run src/components/Layout/ProgressiveLayout.test.tsx --reporter=verbose
```

Expected：`FAIL` — `Module not found: ./ProgressiveLayout`

- [ ] **Step 3：实现组件**

```tsx
// schemaplexai-ui/src/components/Layout/ProgressiveLayout.tsx
import React from 'react'
import { useNavigate, useLocation } from 'react-router-dom'

export interface ProgressiveLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { key: 'cockpit', icon: '◉', label: '驾驶舱', path: '/cockpit' },
  { key: 'canvas', icon: '◆', label: '编排画布', path: '/canvas' },
  { key: 'workflows', icon: '▲', label: '工作流监控', path: '/workflows' },
  { key: 'agents', icon: '●', label: 'Agent 详情', path: '/agents' },
]

export const ProgressiveLayout: React.FC<ProgressiveLayoutProps> = ({ children }) => {
  const navigate = useNavigate()
  const location = useLocation()

  const activeKey = NAV_ITEMS.find(item => location.pathname.startsWith(item.path))?.key || 'cockpit'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: '#0a0e1a' }}>
      {/* Header */}
      <header
        style={{
          height: 48,
          background: '#0d1117',
          borderBottom: '1px solid #1e2a33',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
        }}
      >
        <span style={{ color: '#00d4aa', fontSize: 14, fontWeight: 600 }}>SchemaPlexAI</span>
        <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <span style={{ color: '#64748b', fontSize: 12 }}>租户 ▼</span>
          <span style={{ color: '#64748b', fontSize: 14 }}>🔔</span>
          <div style={{ width: 28, height: 28, borderRadius: '50%', background: '#1e2a33' }} />
        </div>
      </header>

      {/* Body */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Sidebar */}
        <aside
          style={{
            width: 200,
            background: '#0d1117',
            borderRight: '1px solid #1e2a33',
            display: 'flex',
            flexDirection: 'column',
            padding: '12px 0',
          }}
        >
          {NAV_ITEMS.map(item => {
            const isActive = item.key === activeKey
            return (
              <div
                key={item.key}
                onClick={() => navigate(item.path)}
                style={{
                  padding: '10px 20px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  cursor: 'pointer',
                  background: isActive ? '#111827' : 'transparent',
                  borderLeft: `3px solid ${isActive ? '#00d4aa' : 'transparent'}`,
                  color: isActive ? '#e2e8f0' : '#64748b',
                  fontSize: 13,
                  transition: 'all 0.15s ease',
                }}
              >
                <span style={{ color: isActive ? '#00d4aa' : '#64748b', fontSize: 14 }}>
                  {item.icon}
                </span>
                {item.label}
              </div>
            )
          })}
        </aside>

        {/* Content */}
        <main style={{ flex: 1, padding: 24, overflow: 'auto' }}>
          {children}
        </main>
      </div>
    </div>
  )
}
```

- [ ] **Step 4：创建 Layout 统一导出**

```ts
// schemaplexai-ui/src/components/Layout/index.ts
export { ImmersiveLayout } from './ImmersiveLayout'
export type { ImmersiveLayoutProps } from './ImmersiveLayout'
export { ProgressiveLayout } from './ProgressiveLayout'
export type { ProgressiveLayoutProps } from './ProgressiveLayout'
```

- [ ] **Step 5：运行测试确认通过**

```bash
npx vitest run src/components/Layout/ProgressiveLayout.test.tsx --reporter=verbose
```

Expected：`PASS` — 3 assertions passed

- [ ] **Step 6：Commit**

```bash
git add src/components/Layout/
git commit -m "feat(layout): add ProgressiveLayout with expanded sidebar and header"
```

---

## 阶段 4：标志性页面

### Task 12：驾驶舱页面（Cockpit）

**Files:**
- Create: `schemaplexai-ui/src/pages/Cockpit/index.tsx`

- [ ] **Step 1：创建页面文件**

```tsx
// schemaplexai-ui/src/pages/Cockpit/index.tsx
import React from 'react'
import { ImmersiveLayout } from '@/components/Layout'
import { StatCard } from '@/components/Hive'

const MOCK_AGENTS = [
  { id: 'a1', name: 'Data Ingestor', status: 'active' as const, x: 20, y: 25 },
  { id: 'a2', name: 'Pipeline Exec', status: 'executing' as const, x: 70, y: 20 },
  { id: 'a3', name: 'Preprocessor', status: 'active' as const, x: 15, y: 65 },
  { id: 'a4', name: 'Validator', status: 'error' as const, x: 75, y: 60 },
  { id: 'a5', name: 'Guard Agent', status: 'active' as const, x: 45, y: 75 },
]

const STATUS_COLORS = {
  active: '#00d4aa',
  executing: '#ff9f43',
  error: '#ff4757',
  waiting: '#ff9f43',
}

export default function Cockpit() {
  return (
    <ImmersiveLayout>
      <div style={{ position: 'relative', width: '100%', height: '100%' }}>
        {/* Center Orchestrator */}
        <div
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              width: 80,
              height: 80,
              background: 'rgba(0, 212, 170, 0.1)',
              border: '2px solid #00d4aa',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 40px rgba(0, 212, 170, 0.2)',
              margin: '0 auto',
            }}
          >
            <span style={{ color: '#00d4aa', fontSize: 28 }}>◉</span>
          </div>
          <div style={{ color: '#00d4aa', fontSize: 11, marginTop: 12, letterSpacing: 3 }}>
            ORCHESTRATOR
          </div>
        </div>

        {/* Orbiting Agents */}
        {MOCK_AGENTS.map(agent => (
          <div
            key={agent.id}
            style={{
              position: 'absolute',
              top: `${agent.y}%`,
              left: `${agent.x}%`,
              width: 48,
              height: 48,
              background: '#111827',
              border: `1px solid ${STATUS_COLORS[agent.status]}`,
              borderRadius: 8,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: `0 0 12px ${STATUS_COLORS[agent.status]}30`,
              cursor: 'pointer',
              transition: 'all 0.3s ease',
            }}
          >
            <span style={{ color: STATUS_COLORS[agent.status], fontSize: 16 }}>⬡</span>
          </div>
        ))}

        {/* Connection lines (SVG) */}
        <svg
          style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}
          width="100%"
          height="100%"
        >
          {MOCK_AGENTS.map(agent => (
            <line
              key={agent.id}
              x1="50%"
              y1="50%"
              x2={`${agent.x + 2}%`}
              y2={`${agent.y + 2}%`}
              stroke={STATUS_COLORS[agent.status]}
              strokeWidth="1"
              strokeDasharray="4 4"
              opacity="0.4"
            />
          ))}
        </svg>

        {/* Bottom Stats */}
        <div
          style={{
            position: 'absolute',
            bottom: 16,
            left: 16,
            right: 16,
            display: 'flex',
            gap: 12,
          }}
        >
          <StatCard label="THROUGHPUT" value="2,847" change={12.5} unit="tasks/min" color="cyan" sparkline={[40, 60, 45, 80, 55, 70, 90]} />
          <StatCard label="LATENCY" value="124" change={-8.2} unit="ms avg" color="cyan" sparkline={[80, 70, 85, 60, 75, 65, 50]} />
          <StatCard label="SUCCESS RATE" value="99.7%" change={0.3} unit="last 24h" color="cyan" sparkline={[99, 98, 99, 100, 99, 99, 100]} />
          <StatCard label="TOKEN USAGE" value="4.2M" change={23.1} unit="tokens today" color="amber" sparkline={[30, 40, 35, 55, 60, 75, 80]} />
        </div>
      </div>
    </ImmersiveLayout>
  )
}
```

- [ ] **Step 2：启动 dev server 验证页面**

```bash
npm run dev
```

Expected：访问 `/cockpit` 能看到中央 Orchestrator + 周围 Agent 节点 + 底部统计卡片。

- [ ] **Step 3：Commit**

```bash
git add src/pages/Cockpit/
git commit -m "feat(page): add Cockpit with swarm visualization and stat cards"
```

---

### Task 13：编排画布页面（AgentCanvas）

**Files:**
- Create: `schemaplexai-ui/src/pages/AgentCanvas/index.tsx`

- [ ] **Step 1：创建页面文件**

```tsx
// schemaplexai-ui/src/pages/AgentCanvas/index.tsx
import React, { useState } from 'react'
import { ImmersiveLayout } from '@/components/Layout'
import { PillNav } from '@/components/Hive'

interface NodeData {
  id: string
  name: string
  type: string
  status: 'active' | 'executing' | 'error'
  x: number
  y: number
  progress?: number
}

const INITIAL_NODES: NodeData[] = [
  { id: 'n1', name: 'DATA_INGESTION', type: 'Worker', status: 'active', x: 20, y: 20, progress: 100 },
  { id: 'n2', name: 'PIPELINE_EXEC', type: 'Orchestrator', status: 'executing', x: 50, y: 35, progress: 65 },
  { id: 'n3', name: 'PREPROCESSOR', type: 'Executor', status: 'active', x: 15, y: 55 },
  { id: 'n4', name: 'VALIDATOR', type: 'Guard', status: 'active', x: 60, y: 55 },
]

const EDGES = [
  { from: 'n1', to: 'n2' },
  { from: 'n2', to: 'n3' },
  { from: 'n2', to: 'n4' },
]

const STATUS_COLORS = {
  active: '#00d4aa',
  executing: '#ff9f43',
  error: '#ff4757',
}

export default function AgentCanvas() {
  const [activeView, setActiveView] = useState('topology')
  const [selectedNode, setSelectedNode] = useState<string | null>(null)

  return (
    <ImmersiveLayout>
      <div style={{ position: 'relative', width: '100%', height: '100%' }}>
        {/* Pill Nav */}
        <div style={{ position: 'absolute', top: 12, left: '50%', transform: 'translateX(-50%)', zIndex: 5 }}>
          <PillNav
            items={[
              { key: 'topology', label: '拓扑' },
              { key: 'list', label: '列表' },
              { key: 'code', label: '代码' },
            ]}
            activeKey={activeView}
            onChange={setActiveView}
          />
        </div>

        {/* DAG Canvas */}
        {activeView === 'topology' && (
          <div style={{ position: 'relative', width: '100%', height: '100%' }}>
            {/* Grid */}
            <div
              style={{
                position: 'absolute',
                inset: 0,
                opacity: 0.02,
                backgroundImage: 'linear-gradient(#1e2a33 1px, transparent 1px), linear-gradient(90deg, #1e2a33 1px, transparent 1px)',
                backgroundSize: '40px 40px',
                pointerEvents: 'none',
              }}
            />

            {/* Nodes */}
            {INITIAL_NODES.map(node => {
              const isSelected = node.id === selectedNode
              const color = STATUS_COLORS[node.status]
              return (
                <div
                  key={node.id}
                  onClick={() => setSelectedNode(isSelected ? null : node.id)}
                  style={{
                    position: 'absolute',
                    left: `${node.x}%`,
                    top: `${node.y}%`,
                    width: 160,
                    background: '#111827',
                    border: `1px solid ${isSelected ? color : '#1e2a33'}`,
                    borderRadius: 8,
                    padding: 12,
                    boxShadow: isSelected ? `0 0 16px ${color}40` : 'none',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    zIndex: 2,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                    <span style={{ color, fontSize: 14 }}>⬡</span>
                    <span style={{ color: '#e2e8f0', fontSize: 12, fontWeight: 500 }}>{node.name}</span>
                  </div>
                  <div style={{ color: '#64748b', fontSize: 10 }}>{node.type}</div>
                  {node.progress !== undefined && (
                    <div style={{ marginTop: 8, height: 3, background: '#1e2a33', borderRadius: 2, overflow: 'hidden' }}>
                      <div style={{ width: `${node.progress}%`, height: '100%', background: color }} />
                    </div>
                  )}
                </div>
              )
            })}

            {/* Edges */}
            <svg style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
              {EDGES.map((edge, i) => {
                const from = INITIAL_NODES.find(n => n.id === edge.from)
                const to = INITIAL_NODES.find(n => n.id === edge.to)
                if (!from || !to) return null
                return (
                  <line
                    key={i}
                    x1={`${from.x + 10}%`}
                    y1={`${from.y + 8}%`}
                    x2={`${to.x + 2}%`}
                    y2={`${to.y + 2}%`}
                    stroke="#ff9f43"
                    strokeWidth="1.5"
                    strokeDasharray="6 3"
                    opacity="0.5"
                  />
                )
              })}
            </svg>
          </div>
        )}

        {/* List View */}
        {activeView === 'list' && (
          <div style={{ padding: '60px 40px 20px' }}>
            <div style={{ background: '#111827', border: '1px solid #1e2a33', borderRadius: 8, padding: 16 }}>
              {INITIAL_NODES.map(node => (
                <div key={node.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderBottom: '1px solid #1e2a3380', color: '#e2e8f0', fontSize: 12 }}>
                  <span><span style={{ color: STATUS_COLORS[node.status] }}>⬡</span> {node.name}</span>
                  <span style={{ color: '#64748b' }}>{node.type}</span>
                  <span style={{ color: STATUS_COLORS[node.status] }}>● {node.status}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Code View */}
        {activeView === 'code' && (
          <div style={{ padding: '60px 40px 20px', fontFamily: "'JetBrains Mono', monospace", fontSize: 12, color: '#e2e8f0' }}>
            <pre style={{ background: '#0a0e1a', border: '1px solid #1e2a33', borderRadius: 8, padding: 20, overflow: 'auto' }}>
{`pipeline:
  name: data-processing-v2
  steps:
    - id: ingestion
      agent: DATA_INGESTION
      type: Worker
    - id: processing
      agent: PIPELINE_EXEC
      type: Orchestrator
      depends_on: [ingestion]
    - id: validation
      agent: VALIDATOR
      type: Guard
      depends_on: [processing]`}
            </pre>
          </div>
        )}

        {/* Bottom Toolbar */}
        <div style={{ position: 'absolute', bottom: 16, left: '50%', transform: 'translateX(-50%)', display: 'flex', gap: 8, zIndex: 5 }}>
          {['+', '⟷', '⌫', '▶'].map(icon => (
            <div
              key={icon}
              style={{
                width: 36,
                height: 36,
                background: '#111827',
                border: '1px solid #1e2a33',
                borderRadius: 8,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#64748b',
                fontSize: 14,
                cursor: 'pointer',
              }}
            >
              {icon}
            </div>
          ))}
        </div>
      </div>
    </ImmersiveLayout>
  )
}
```

- [ ] **Step 2：Commit**

```bash
git add src/pages/AgentCanvas/
git commit -m "feat(page): add AgentCanvas with DAG editor and topology/list/code views"
```

---

### Task 14：工作流监控页面（WorkflowMonitor）

**Files:**
- Create: `schemaplexai-ui/src/pages/WorkflowMonitor/index.tsx`

- [ ] **Step 1：创建页面文件**

```tsx
// schemaplexai-ui/src/pages/WorkflowMonitor/index.tsx
import React, { useState } from 'react'
import { ProgressiveLayout } from '@/components/Layout'
import { PillNav } from '@/components/Hive'

const WORKFLOWS = [
  {
    id: 'w1',
    name: 'data-pipeline',
    status: 'running' as const,
    tasks: [
      { name: 'ingestion', start: 10, width: 35, color: '#00d4aa' },
      { name: 'process', start: 48, width: 25, color: '#ff9f43' },
      { name: 'validate', start: 76, width: 15, color: '#00d4aa' },
    ],
  },
  {
    id: 'w2',
    name: 'model-training',
    status: 'running' as const,
    tasks: [
      { name: 'training...', start: 5, width: 60, color: '#ff9f43' },
    ],
  },
  {
    id: 'w3',
    name: 'sync-job',
    status: 'failed' as const,
    tasks: [
      { name: 'failed', start: 20, width: 30, color: '#ff4757' },
    ],
  },
]

const STATUS_FILTER = [
  { key: 'all', label: '全部' },
  { key: 'running', label: '运行中' },
  { key: 'completed', label: '已完成' },
]

const STATUS_COLORS: Record<string, string> = {
  running: '#ff9f43',
  completed: '#00d4aa',
  failed: '#ff4757',
}

export default function WorkflowMonitor() {
  const [filter, setFilter] = useState('all')

  const filtered = filter === 'all'
    ? WORKFLOWS
    : WORKFLOWS.filter(w => w.status === filter || (filter === 'completed' && w.status === 'failed'))

  return (
    <ProgressiveLayout>
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h1 style={{ color: '#e2e8f0', fontSize: 18, fontWeight: 600 }}>工作流执行监控</h1>
          <PillNav items={STATUS_FILTER} activeKey={filter} onChange={setFilter} />
        </div>

        {/* Gantt Chart */}
        <div style={{ background: '#111827', border: '1px solid #1e2a33', borderRadius: 8, padding: 16, marginBottom: 16 }}>
          {/* Header */}
          <div style={{ display: 'flex', padding: '8px 0', borderBottom: '1px solid #1e2a33', color: '#64748b', fontSize: 10 }}>
            <div style={{ width: 120 }}>WORKFLOW</div>
            <div style={{ flex: 1, display: 'flex' }}>
              {['00:00', '00:30', '01:00', '01:30', '02:00'].map(t => (
                <div key={t} style={{ flex: 1, textAlign: 'center' }}>{t}</div>
              ))}
            </div>
          </div>

          {/* Rows */}
          {filtered.map(wf => (
            <div key={wf.id} style={{ display: 'flex', padding: '10px 0', borderBottom: '1px solid #1e2a3380', alignItems: 'center' }}>
              <div style={{ width: 120, color: '#e2e8f0', fontSize: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ color: STATUS_COLORS[wf.status] }}>◆</span> {wf.name}
              </div>
              <div style={{ flex: 1, position: 'relative', height: 24 }}>
                {wf.tasks.map((task, i) => (
                  <div
                    key={i}
                    style={{
                      position: 'absolute',
                      left: `${task.start}%`,
                      width: `${task.width}%`,
                      height: 18,
                      top: 3,
                      background: `${task.color}30`,
                      border: `1px solid ${task.color}`,
                      borderRadius: 4,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <span style={{ color: task.color, fontSize: 9 }}>{task.name}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Detail Table */}
        <div style={{ background: '#111827', border: '1px solid #1e2a33', borderRadius: 8, padding: 16 }}>
          <div style={{ display: 'flex', padding: '8px 0', borderBottom: '1px solid #1e2a33', color: '#64748b', fontSize: 11 }}>
            <div style={{ flex: 2 }}>工作流名称</div>
            <div style={{ flex: 1 }}>状态</div>
            <div style={{ flex: 1 }}>执行时间</div>
            <div style={{ flex: 1 }}>操作</div>
          </div>
          {filtered.map(wf => (
            <div key={wf.id} style={{ display: 'flex', padding: '12px 0', borderBottom: '1px solid #1e2a3380', color: '#e2e8f0', fontSize: 12, alignItems: 'center' }}>
              <div style={{ flex: 2, display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ color: STATUS_COLORS[wf.status] }}>◆</span> {wf.name}
              </div>
              <div style={{ flex: 1 }}>
                <span style={{ color: STATUS_COLORS[wf.status] }}>●</span> {wf.status === 'running' ? '运行中' : wf.status === 'failed' ? '失败' : '已完成'}
              </div>
              <div style={{ flex: 1, color: '#64748b' }}>--</div>
              <div style={{ flex: 1, color: '#00d4aa', cursor: 'pointer' }}>查看日志 →</div>
            </div>
          ))}
        </div>
      </div>
    </ProgressiveLayout>
  )
}
```

- [ ] **Step 2：Commit**

```bash
git add src/pages/WorkflowMonitor/
git commit -m "feat(page): add WorkflowMonitor with Gantt chart and detail table"
```

---

### Task 15：Agent 详情页面（AgentDetail）

**Files:**
- Create: `schemaplexai-ui/src/pages/AgentDetail/index.tsx`

- [ ] **Step 1：创建页面文件**

```tsx
// schemaplexai-ui/src/pages/AgentDetail/index.tsx
import React, { useState } from 'react'
import { ProgressiveLayout } from '@/components/Layout'
import { TerminalLog } from '@/components/Hive'
import type { LogEntry } from '@/components/Hive'

const TABS = ['实时监控', '执行日志', '性能曲线', '配置']

const MOCK_LOGS: LogEntry[] = [
  { timestamp: '14:30:01', level: 'INFO', message: 'Agent initialized, config loaded from /etc/schemaplex/agent.yml' },
  { timestamp: '14:30:02', level: 'INFO', message: 'Connected to RabbitMQ broker at amqp://mq:5672' },
  { timestamp: '14:30:15', level: 'WARN', message: 'Queue depth exceeding threshold: 1,247 messages' },
  { timestamp: '14:30:22', level: 'INFO', message: 'Task batch-20250501-001 received, processing...' },
  { timestamp: '14:30:45', level: 'INFO', message: 'Step 1/4: Data ingestion complete (2.3s)' },
  { timestamp: '14:31:02', level: 'INFO', message: 'Step 2/4: Preprocessing complete (17.1s)' },
  { timestamp: '14:31:18', level: 'WARN', message: 'Step 3/4: Model inference slower than expected (45.2s)' },
  { timestamp: '14:32:08', level: 'INFO', message: 'Step 4/4: Validation passed, output written to S3' },
]

export default function AgentDetail() {
  const [activeTab, setActiveTab] = useState('实时监控')

  return (
    <ProgressiveLayout>
      <div>
        {/* Agent Identity Card */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 40,
                height: 40,
                background: 'rgba(0, 212, 170, 0.1)',
                border: '1px solid #00d4aa',
                borderRadius: 8,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <span style={{ color: '#00d4aa', fontSize: 20 }}>⬡</span>
            </div>
            <div>
              <div style={{ color: '#e2e8f0', fontSize: 16, fontWeight: 600 }}>data-processor-alpha</div>
              <div style={{ color: '#64748b', fontSize: 11 }}>Worker · ID: #7A3F-9021</div>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button style={{ padding: '5px 12px', background: '#111827', border: '1px solid #1e2a33', borderRadius: 6, color: '#64748b', fontSize: 11, cursor: 'pointer' }}>重启</button>
            <button style={{ padding: '5px 12px', background: '#ff4757', border: 'none', borderRadius: 6, color: '#0a0e1a', fontSize: 11, fontWeight: 600, cursor: 'pointer' }}>终止</button>
          </div>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', gap: 4, borderBottom: '1px solid #1e2a33', marginBottom: 16 }}>
          {TABS.map(tab => (
            <div
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                padding: '8px 16px',
                color: tab === activeTab ? '#00d4aa' : '#64748b',
                fontSize: 12,
                fontWeight: tab === activeTab ? 500 : 400,
                borderBottom: `2px solid ${tab === activeTab ? '#00d4aa' : 'transparent'}`,
                cursor: 'pointer',
                marginBottom: -1,
              }}
            >
              {tab}
            </div>
          ))}
        </div>

        {/* Tab Content */}
        {activeTab === '实时监控' && (
          <div style={{ display: 'flex', gap: 16 }}>
            {/* Left: Metrics */}
            <div style={{ width: 280 }}>
              {[
                { label: 'CPU USAGE', value: '67.3%', color: '#00d4aa', pct: 67 },
                { label: 'MEMORY', value: '2.1 GB', color: '#ff9f43', pct: 45 },
                { label: 'UPTIME', value: '14:32:08', color: '#00d4aa', pct: 0 },
              ].map(metric => (
                <div
                  key={metric.label}
                  style={{ background: '#111827', border: '1px solid #1e2a33', borderRadius: 8, padding: 16, marginBottom: 12 }}
                >
                  <div style={{ color: '#64748b', fontSize: 10, marginBottom: 8 }}>{metric.label}</div>
                  <div style={{ color: '#e2e8f0', fontSize: 24, fontWeight: 700, fontFamily: "'JetBrains Mono', monospace" }}>{metric.value}</div>
                  {metric.pct > 0 && (
                    <div style={{ marginTop: 8, height: 4, background: '#1e2a33', borderRadius: 2, overflow: 'hidden' }}>
                      <div style={{ width: `${metric.pct}%`, height: '100%', background: metric.color }} />
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Right: Terminal */}
            <div style={{ flex: 1 }}>
              <TerminalLog logs={MOCK_LOGS} style={{ maxHeight: 500 }} />
            </div>
          </div>
        )}

        {activeTab === '执行日志' && (
          <TerminalLog logs={MOCK_LOGS} style={{ maxHeight: 'calc(100vh - 240px)' }} />
        )}

        {activeTab === '性能曲线' && (
          <div style={{ background: '#111827', border: '1px solid #1e2a33', borderRadius: 8, padding: 40, color: '#64748b', textAlign: 'center' }}>
            性能曲线图表区域（集成 ECharts）
          </div>
        )}

        {activeTab === '配置' && (
          <div style={{ maxWidth: 600 }}>
            {[
              { label: 'AGENT NAME', value: 'data-processor-alpha' },
              { label: 'DESCRIPTION', value: '负责数据清洗与预处理任务调度...' },
              { label: 'AGENT TYPE', value: 'Worker' },
            ].map(field => (
              <div key={field.label} style={{ marginBottom: 20 }}>
                <div style={{ color: '#00d4aa', fontSize: 11, letterSpacing: '0.05em', marginBottom: 6 }}>{field.label}</div>
                <div style={{ padding: '14px 16px', background: 'transparent', border: '1px solid #1e2a33', borderRadius: 8, color: '#e2e8f0', fontSize: 15 }}>
                  {field.value}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </ProgressiveLayout>
  )
}
```

- [ ] **Step 2：Commit**

```bash
git add src/pages/AgentDetail/
git commit -m "feat(page): add AgentDetail with metrics, terminal logs, tabs"
```

---

## 阶段 5：路由集成

### Task 16：更新路由配置

**Files:**
- Modify: `schemaplexai-ui/src/router/index.tsx`

- [ ] **Step 1：添加新页面导入和路由**

```tsx
// schemaplexai-ui/src/router/index.tsx
import { Navigate } from 'react-router-dom'
import { lazy } from 'react'
import type { ReactNode } from 'react'

const Login = lazy(() => import('@/pages/Login'))
const Layout = lazy(() => import('@/components/Layout'))
const Dashboard = lazy(() => import('@/pages/Dashboard'))
const AgentManager = lazy(() => import('@/pages/AgentManager'))
const AgentExecutor = lazy(() => import('@/pages/AgentExecutor'))
const SpecCenter = lazy(() => import('@/pages/SpecCenter'))
const WorkflowCenter = lazy(() => import('@/pages/WorkflowCenter'))
const ContextCenter = lazy(() => import('@/pages/ContextCenter'))
const QualityCenter = lazy(() => import('@/pages/QualityCenter'))
const IntegrationCenter = lazy(() => import('@/pages/IntegrationCenter'))
const OpsCenter = lazy(() => import('@/pages/OpsCenter'))
const SystemSettings = lazy(() => import('@/pages/SystemSettings'))
const NotificationCenter = lazy(() => import('@/pages/NotificationCenter'))
const NotFound = lazy(() => import('@/pages/NotFound'))

// New Abyss Hive pages
const Cockpit = lazy(() => import('@/pages/Cockpit'))
const AgentCanvas = lazy(() => import('@/pages/AgentCanvas'))
const WorkflowMonitor = lazy(() => import('@/pages/WorkflowMonitor'))
const AgentDetail = lazy(() => import('@/pages/AgentDetail'))

export interface RouteConfig {
  path: string
  element: ReactNode
  children?: RouteConfig[]
}

function RequireAuth({ children }: { children: ReactNode }) {
  const token = localStorage.getItem('schemaplexai_token')
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return children
}

const RouterConfig: RouteConfig[] = [
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/',
    element: (
      <RequireAuth>
        <Layout />
      </RequireAuth>
    ),
    children: [
      { path: '', element: <Navigate to="/cockpit" replace /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'cockpit', element: <Cockpit /> },
      { path: 'canvas', element: <AgentCanvas /> },
      { path: 'workflows', element: <WorkflowMonitor /> },
      { path: 'agents', element: <AgentDetail /> },
      { path: 'agents/executor', element: <AgentExecutor /> },
      { path: 'specs', element: <SpecCenter /> },
      { path: 'workflows-old', element: <WorkflowCenter /> },
      { path: 'contexts', element: <ContextCenter /> },
      { path: 'quality', element: <QualityCenter /> },
      { path: 'integrations', element: <IntegrationCenter /> },
      { path: 'ops', element: <OpsCenter /> },
      { path: 'notifications', element: <NotificationCenter /> },
      { path: 'settings', element: <SystemSettings /> },
    ],
  },
  {
    path: '*',
    element: <NotFound />,
  },
]

export default RouterConfig
```

- [ ] **Step 2：验证路由编译**

```bash
cd schemaplexai-ui
npx tsc --noEmit
```

Expected：无类型错误。

- [ ] **Step 3：Commit**

```bash
git add src/router/index.tsx
git commit -m "feat(router): add cockpit, canvas, workflow-monitor, agent-detail routes"
```

---

### Task 17：运行全量测试并验证

**Files:**
- All test files

- [ ] **Step 1：运行全量测试**

```bash
cd schemaplexai-ui
npx vitest run --reporter=verbose
```

Expected：全部通过（theme + HexIcon + StatCard + PillNav + TerminalLog + Layout 测试）。

- [ ] **Step 2：启动 dev server 验证所有页面**

```bash
npm run dev
```

验证清单：
- [ ] `/cockpit` — 中央 Orchestrator + Agent 节点 + 底部统计卡片
- [ ] `/canvas` — DAG 节点 + 药丸导航（拓扑/列表/代码）+ 底部工具栏
- [ ] `/workflows` — 甘特图 + 药丸过滤 + 详细表格
- [ ] `/agents` — Agent 身份卡 + Tab 切换 + 指标卡片 + 终端日志

- [ ] **Step 3：最终 Commit**

```bash
git add -A
git commit -m "feat(ui): implement abyss-hive design system - cockpit, canvas, monitor, detail"
```

---

## 自我审查

### 1. Spec Coverage

| 设计规范章节 | 对应任务 | 状态 |
|-------------|---------|------|
| 色彩系统 §3.1 | Task 3 (theme tokens), Task 4 (CSS variables) | ✅ |
| 字体系统 §3.2 | Task 3 (fontFamily token), Task 5 (Google Fonts) | ✅ |
| 间距/圆角/阴影 §3.3-3.4 | Task 3 (tokens), Task 4 (CSS variables) | ✅ |
| 沉浸式布局 §4.1 | Task 10 (ImmersiveLayout) | ✅ |
| 渐进式布局 §4.2 | Task 11 (ProgressiveLayout) | ✅ |
| 卡片 §5.1 | Task 7 (StatCard) | ✅ |
| 按钮 §5.2 | Task 3 (Button tokens) + Task 6 (HexIcon button) | ✅ |
| 表格 §5.3 | Task 3 (Table tokens) + Task 14 (WorkflowMonitor table) | ✅ |
| 输入框 §5.4 | Task 3 (Input tokens) | ✅ |
| 药丸导航 §5.5 | Task 8 (PillNav) | ✅ |
| 终端日志 §5.6 | Task 9 (TerminalLog) | ✅ |
| 驾驶舱 §6.1 | Task 12 (Cockpit) | ✅ |
| 编排画布 §6.2 | Task 13 (AgentCanvas) | ✅ |
| 工作流监控 §6.3 | Task 14 (WorkflowMonitor) | ✅ |
| Agent 详情 §6.4 | Task 15 (AgentDetail) | ✅ |
| AntD 配置 §7 | Task 3 (theme/index.ts) | ✅ |

### 2. Placeholder Scan

- 无 "TBD" / "TODO" / "implement later"
- 无 "Add appropriate error handling" 等模糊描述
- 每步包含实际代码
- 无 "Similar to Task N"

### 3. Type Consistency

- `HexIconProps` / `StatCardProps` / `PillNavProps` / `TerminalLogProps` — 命名统一
- `LogLevel` / `LogEntry` — 在 TerminalLog 中定义并导出
- `STATUS_COLORS` 在各页面中独立定义（符合 YAGNI，不提前抽象）
- AntD theme token 中的色值与 CSS 变量一致

---

## 执行交接

**Plan complete and saved to `docs/superpowers/plans/2026-05-01-abyss-hive-ui.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration. Best for parallelizing independent tasks (e.g., components can be built in parallel).

**2. Inline Execution** — Execute tasks in this session, batch execution with checkpoints for review. Best when you want to monitor progress closely.

**Which approach?**
