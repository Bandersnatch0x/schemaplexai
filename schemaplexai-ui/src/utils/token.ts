const TOKEN_KEY = 'schemaplexai_token'
const TOKEN_EXPIRY_KEY = 'schemaplexai_token_expiry'
const REFRESH_TOKEN_KEY = 'schemaplexai_refresh_token'
const TENANT_KEY = 'schemaplexai_tenant'

/**
 * SECURITY NOTE: Token is currently stored in localStorage due to frontend
 * limitations (cannot set httpOnly Cookie directly). This makes it vulnerable
 * to XSS attacks. The recommended long-term fix is:
 * 1. Backend sets access token in httpOnly Cookie
 * 2. Frontend reads token from Cookie automatically sent by browser
 * 3. Refresh token rotation should also be Cookie-based
 */

export function getToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY)
  const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY)

  if (expiry && Date.now() > parseInt(expiry)) {
    clearAuth()
    return null
  }

  return token
}

export function setToken(token: string, expiresIn?: number): void {
  localStorage.setItem(TOKEN_KEY, token)
  if (expiresIn) {
    const expiry = Date.now() + expiresIn * 1000
    localStorage.setItem(TOKEN_EXPIRY_KEY, expiry.toString())
  }
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_EXPIRY_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function removeRefreshToken(): void {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function getTenantId(): string | null {
  return localStorage.getItem(TENANT_KEY)
}

export function setTenantId(tenantId: string): void {
  localStorage.setItem(TENANT_KEY, tenantId)
}

export function removeTenantId(): void {
  localStorage.removeItem(TENANT_KEY)
}

export function clearAuth(): void {
  removeToken()
  removeRefreshToken()
  removeTenantId()
}
