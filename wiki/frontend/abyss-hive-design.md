---
title: Abyss Hive UI/UE Design System
type: design
source: docs/superpowers/specs/2026-05-01-abyss-hive-ui-design.md
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [frontend, design-system, ui, ux, abyss-hive, ant-design]
confidence: high
---

# Abyss Hive UI/UE Design System

> One-sentence summary: "Abyss Hive" is SchemaPlexAI's dark-themed design system combining TeamCity layout structure, cockpit immersion, hive/ant colony biological metaphor, and Black Mirror futuristic cold aesthetics.

## Design Philosophy

**深渊背景（Abyss Background）**
Extremely deep blue-black canvas where content elements float like bioluminescent organisms. No physical shadows — depth is established purely through background color differences and fluorescent glow.

**蜂巢隐喻（Hive Metaphor）**
Hexagons as the core visual symbol throughout the system: icons, grid textures, node shapes, button styles. Agent communication visualized as "pheromone trails" (dashed lines + flowing light effects).

**功能与美学自适应（Adaptive Aesthetics）**
Same design language, two spatial strategies:
- **Immersive**: Borderless, hidden navigation — for cockpit and canvas
- **Progressive**: Structured, persistent navigation — for monitor and detail pages

**荧光色编码（Bioluminescent Color Coding）**
All states, types, priorities distinguished by fluorescent colors: cyan = normal/communication, amber = execution/energy, red = anomaly/danger.

## Color System

### Background Hierarchy

| Name | Hex | Usage |
|------|-----|-------|
| Abyss Background | `#0a0e1a` | Page base background |
| Hive Wall | `#0d1117` | Sidebar background |
| Honeycomb Card | `#111827` | Cards, panels, table containers |
| Mycelium Divider | `#1e2a33` | Borders, dividers, table headers |
| Hive Hover | `#1e3a5f` | Hover state background |

### Fluorescent Palette

| Name | Hex | Usage |
|------|-----|-------|
| Pheromone Cyan | `#00d4aa` | Normal, success, communication, primary brand |
| Amber Energy | `#ff9f43` | Executing, warning, energy, waiting |
| Danger Red | `#ff4757` | Error, anomaly, danger, alert |
| Info Gray | `#64748b` | Secondary text, placeholder, disabled |
| Primary Text | `#e2e8f0` | Titles, body, primary data |

## Typography

| Purpose | Font | License |
|---------|------|---------|
| English UI | Inter | SIL OFL |
| Chinese UI | Noto Sans SC | SIL OFL |
| Data/Monospace | JetBrains Mono | SIL OFL |

## Layout Modes

### Immersive Layout
- 52px icon-only sidebar (left)
- Floating status bar (top center)
- 40px subtle grid background
- Used by: Cockpit, Agent Canvas

### Progressive Layout
- 200px expanded sidebar with labels
- 48px top header bar
- 3px vertical line for active item indicator
- Used by: Workflow Monitor, Agent Detail

## Component Library

| Component | Location | Description |
|-----------|----------|-------------|
| HexIcon | `components/Hive/HexIcon.tsx` | Hexagon icon with size/color variants |
| StatCard | `components/Hive/StatCard.tsx` | Left 3px color strip + big number + sparkline |
| PillNav | `components/Hive/PillNav.tsx` | Pill-shaped view switcher |
| TerminalLog | `components/Hive/TerminalLog.tsx` | Color-coded log stream + auto-scroll + blinking cursor |
| ImmersiveLayout | `components/Layout/ImmersiveLayout.tsx` | Borderless immersive page shell |
| ProgressiveLayout | `components/Layout/ProgressiveLayout.tsx` | Structured progressive page shell |

## Signature Pages

| Page | Route | Layout | Key Features |
|------|-------|--------|-------------|
| Cockpit | `/cockpit` | Immersive | Central orchestrator hub + orbiting Agent nodes + bottom stat cards |
| Agent Canvas | `/canvas` | Immersive | DAG node editor + topology/list/code views + bottom toolbar |
| Workflow Monitor | `/workflows` | Progressive | Gantt chart timeline + pill filter + detail table |
| Agent Detail | `/agents` | Progressive | Identity card + tabbed content (metrics/logs/charts/config) |

## Ant Design Integration

Theme implemented via Ant Design 5 `ConfigProvider` token override — no custom component library. Key token overrides:

- `colorBgBase`: `#0a0e1a`
- `colorBgContainer`: `#111827`
- `colorPrimary`: `#00d4aa`
- `colorError`: `#ff4757`
- `colorWarning`: `#ff9f43`
- `borderRadius`: 8
- `fontFamily`: Inter + Noto Sans SC stack

## Reference Documents

- **Full design spec**: `docs/superpowers/specs/2026-05-01-abyss-hive-ui-design.md`
- **Implementation plan**: `docs/superpowers/plans/2026-05-01-abyss-hive-ui.md`
- **Change workspace**: `.claude/changes/abyss-hive-ui/`

## Backlinks

- Frontend structure: [[frontend/structure]]
- Project architecture: [[architecture]]
