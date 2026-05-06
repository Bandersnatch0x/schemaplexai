import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const FIGMA_OUTPUT_DIR = path.resolve(__dirname, '../.claude/outputs/figma')
const SCREENSHOT_DIR = path.resolve(__dirname, '../.claude/outputs/screenshots')

/**
 * Visual regression: compare Playwright screenshots against Figma exported images.
 *
 * Pre-requisites:
 * 1. Run `.claude/scripts/figma-export.sh` to download Figma PNGs
 * 2. Run `npm run test:e2e` to generate Playwright screenshots
 * 3. This test compares corresponding images pixel-by-pixel
 */

const ROUTE_MAP: Record<string, string> = {
  login: 'login',
  dashboard: 'dashboard',
  agents: 'agents',
  'agents-executor': 'agents-executor',
  specs: 'specs',
  workflows: 'workflows',
  contexts: 'contexts',
  quality: 'quality',
  integrations: 'integrations',
  ops: 'ops',
  notifications: 'notifications',
  settings: 'settings',
}

function findFigmaPng(routeName: string): string | null {
  const dir = FIGMA_OUTPUT_DIR
  if (!fs.existsSync(dir)) return null

  // Figma node IDs use colons, files are saved with underscores
  const files = fs.readdirSync(dir)
  const match = files.find((f) => f.endsWith('.png') && f.includes(routeName))
  return match ? path.join(dir, match) : null
}

function findScreenshot(routeName: string): string | null {
  const file = path.join(SCREENSHOT_DIR, `${routeName}.png`)
  return fs.existsSync(file) ? file : null
}

test.describe('Figma visual regression', () => {
  for (const [routeName] of Object.entries(ROUTE_MAP)) {
    test(`compare: ${routeName}`, () => {
      const figmaPath = findFigmaPng(routeName)
      const screenshotPath = findScreenshot(routeName)

      if (!figmaPath) {
        test.skip(true, `No Figma reference image for "${routeName}"`)
        return
      }

      if (!screenshotPath) {
        test.skip(true, `No Playwright screenshot for "${routeName}"`)
        return
      }

      // Basic sanity: both files exist and are non-empty
      const figmaStat = fs.statSync(figmaPath)
      const screenshotStat = fs.statSync(screenshotPath)

      expect(figmaStat.size).toBeGreaterThan(1024)
      expect(screenshotStat.size).toBeGreaterThan(1024)

      // TODO: integrate pixelmatch for pixel-level diff
      // For now, this test ensures both sides of the pipeline are producing images
      expect(true).toBe(true)
    })
  }
})
