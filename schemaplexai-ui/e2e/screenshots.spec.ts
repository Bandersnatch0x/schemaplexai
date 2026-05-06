import { test, expect } from '@playwright/test'
import * as path from 'path'

const ROUTES = [
  { path: '/login', name: 'login' },
  { path: '/dashboard', name: 'dashboard' },
  { path: '/agents', name: 'agents' },
  { path: '/agents/executor', name: 'agents-executor' },
  { path: '/specs', name: 'specs' },
  { path: '/workflows', name: 'workflows' },
  { path: '/contexts', name: 'contexts' },
  { path: '/quality', name: 'quality' },
  { path: '/integrations', name: 'integrations' },
  { path: '/ops', name: 'ops' },
  { path: '/notifications', name: 'notifications' },
  { path: '/settings', name: 'settings' },
]

const OUTPUT_DIR = path.resolve('../.claude/outputs/screenshots')

test.describe('Visual capture — all pages', () => {
  test.beforeEach(async ({ page }) => {
    // Seed auth state so protected routes render
    await page.goto('/login')
    await page.evaluate(() => {
      const mockToken = 'mock_jwt_token_' + Date.now()
      localStorage.setItem('schemaplexai_token', mockToken)
      localStorage.setItem('schemaplexai_tenant', 'default')
      localStorage.setItem(
        'schemaplexai_user',
        JSON.stringify({ id: '1', username: 'admin', nickname: 'Admin' })
      )
    })
  })

  for (const route of ROUTES) {
    test(`capture: ${route.name}`, async ({ page }) => {
      await page.goto(route.path, { waitUntil: 'networkidle' })
      // Wait for React to hydrate and Ant Design to render
      await page.waitForTimeout(800)

      // Ensure something rendered (not a blank page)
      const bodyText = await page.locator('body').innerText()
      expect(bodyText.length).toBeGreaterThan(0)

      const screenshotPath = path.join(OUTPUT_DIR, `${route.name}.png`)
      await page.screenshot({ path: screenshotPath, fullPage: true })
    })
  }
})
