# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: screenshots.spec.ts >> Visual capture — all pages >> capture: ops
- Location: e2e\screenshots.spec.ts:37:5

# Error details

```
Error: expect(received).toBeGreaterThan(expected)

Expected: > 0
Received:   0
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test'
  2  | import * as path from 'path'
  3  | 
  4  | const ROUTES = [
  5  |   { path: '/login', name: 'login' },
  6  |   { path: '/dashboard', name: 'dashboard' },
  7  |   { path: '/agents', name: 'agents' },
  8  |   { path: '/agents/executor', name: 'agents-executor' },
  9  |   { path: '/specs', name: 'specs' },
  10 |   { path: '/workflows', name: 'workflows' },
  11 |   { path: '/contexts', name: 'contexts' },
  12 |   { path: '/quality', name: 'quality' },
  13 |   { path: '/integrations', name: 'integrations' },
  14 |   { path: '/ops', name: 'ops' },
  15 |   { path: '/notifications', name: 'notifications' },
  16 |   { path: '/settings', name: 'settings' },
  17 | ]
  18 | 
  19 | const OUTPUT_DIR = path.resolve('../.claude/outputs/screenshots')
  20 | 
  21 | test.describe('Visual capture — all pages', () => {
  22 |   test.beforeEach(async ({ page }) => {
  23 |     // Seed auth state so protected routes render
  24 |     await page.goto('/login')
  25 |     await page.evaluate(() => {
  26 |       const mockToken = 'mock_jwt_token_' + Date.now()
  27 |       localStorage.setItem('schemaplexai_token', mockToken)
  28 |       localStorage.setItem('schemaplexai_tenant', 'default')
  29 |       localStorage.setItem(
  30 |         'schemaplexai_user',
  31 |         JSON.stringify({ id: '1', username: 'admin', nickname: 'Admin' })
  32 |       )
  33 |     })
  34 |   })
  35 | 
  36 |   for (const route of ROUTES) {
  37 |     test(`capture: ${route.name}`, async ({ page }) => {
  38 |       await page.goto(route.path, { waitUntil: 'networkidle' })
  39 |       // Wait for React to hydrate and Ant Design to render
  40 |       await page.waitForTimeout(800)
  41 | 
  42 |       // Ensure something rendered (not a blank page)
  43 |       const bodyText = await page.locator('body').innerText()
> 44 |       expect(bodyText.length).toBeGreaterThan(0)
     |                               ^ Error: expect(received).toBeGreaterThan(expected)
  45 | 
  46 |       const screenshotPath = path.join(OUTPUT_DIR, `${route.name}.png`)
  47 |       await page.screenshot({ path: screenshotPath, fullPage: true })
  48 |     })
  49 |   }
  50 | })
  51 | 
```