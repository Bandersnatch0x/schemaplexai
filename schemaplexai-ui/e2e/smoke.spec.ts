import { test, expect } from '@playwright/test'

test.describe('Smoke — app loads and login page is reachable', () => {
  test('login page renders with expected elements', async ({ page }) => {
    await page.goto('/login')

    // Wait for React hydration and Ant Design to render
    await page.waitForTimeout(800)

    // Verify the page title / brand is present
    await expect(page.locator('text=SchemaPlex')).toBeVisible()

    // Verify login form inputs exist
    await expect(page.locator('[data-testid="login-username"]')).toBeVisible()
    await expect(page.locator('[data-testid="login-password"]')).toBeVisible()
    await expect(page.locator('[data-testid="login-submit"]')).toBeVisible()

    // Verify the body is not blank
    const bodyText = await page.locator('body').innerText()
    expect(bodyText.length).toBeGreaterThan(0)
  })

  test('protected route redirects or shows login when unauthenticated', async ({ page }) => {
    // Clear any residual auth state
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.removeItem('schemaplexai_token')
      localStorage.removeItem('schemaplexai_tenant')
      localStorage.removeItem('schemaplexai_user')
    })

    // Attempt to visit a protected route
    await page.goto('/dashboard')
    await page.waitForTimeout(800)

    // The app should either redirect back to login or still show login elements
    const currentUrl = page.url()
    const isLoginPage = currentUrl.includes('/login')
    const hasLoginElements = await page.locator('[data-testid="login-username"]').count() > 0

    expect(isLoginPage || hasLoginElements).toBe(true)
  })
})
