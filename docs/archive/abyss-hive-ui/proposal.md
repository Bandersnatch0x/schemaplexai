---
change_id: abyss-hive-ui
status: adopted
created_at: 2026-05-01
author: wangbinyu
---

# Proposal: Abyss Hive UI/UE Design System

## Problem Statement

SchemaPlexAI frontend currently uses a generic Ant Design 5 light theme with classic sidebar+content layout. As the platform expands with multi-Agent orchestration, workflow visualization, and real-time monitoring, the UI lacks:

1. **Brand identity** — No visual differentiation from typical admin dashboards
2. **Information density balance** — Management pages need clarity; core pages need immersion
3. **Consistent dark theme** — No unified dark mode design system
4. **Signature page experiences** — Cockpit, DAG canvas, monitoring need custom designs

## Scope

### In Scope
- Global design system: colors, typography, spacing, shadows, components
- Two layout frameworks: Immersive + Progressive
- Four signature pages: Cockpit, Agent Canvas, Workflow Monitor, Agent Detail
- Ant Design 5 theme token override
- Component library: HexIcon, StatCard, PillNav, TerminalLog
- CSS variable system
- Google Fonts integration (Inter, Noto Sans SC, JetBrains Mono)
- Vitest + React Testing Library test coverage

### Out of Scope
- Custom component library (use AntD token override instead)
- Animation framework (use CSS transitions only)
- x6 graph engine integration (Phase 2)
- ECharts integration for performance curves (Phase 2)
- Responsive mobile adaptation
- Accessibility audit (WCAG)

## Success Criteria

- [ ] All 4 signature pages render correctly with new design system
- [ ] Component tests pass (80%+ coverage)
- [ ] TypeScript compilation succeeds (`tsc --noEmit`)
- [ ] Dev server runs without errors
- [ ] Design matches spec: colors within ±2% of specified hex values

## Non-Goals

- Rewrite existing pages (Dashboard, AgentManager, etc.) — they continue using ProgressiveLayout
- Dark mode toggle — abyss theme is the only mode
- Custom icon font — use Unicode hexagon symbols + inline SVG

## Related Work

- Frontend structure: `wiki/frontend/structure.md`
- Current routing: `schemaplexai-ui/src/router/index.tsx`
