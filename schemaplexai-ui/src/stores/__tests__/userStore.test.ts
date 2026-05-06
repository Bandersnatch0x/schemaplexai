import { describe, it, expect, beforeEach } from 'vitest'
import { useUserStore } from '@/stores/userStore'
import type { UserInfo, Tenant } from '@/types'

describe('userStore', () => {
  beforeEach(() => {
    // Reset store state and localStorage before each test
    useUserStore.setState({
      userInfo: null,
      currentTenant: null,
      tenants: [],
    })
    localStorage.clear()
  })

  it('should have correct initial state', () => {
    // Arrange & Act
    const state = useUserStore.getState()

    // Assert
    expect(state.userInfo).toBeNull()
    expect(state.currentTenant).toBeNull()
    expect(state.tenants).toEqual([])
  })

  it('should set user info and update state', () => {
    // Arrange
    const userInfo: UserInfo = {
      id: 'user-1',
      username: 'alice',
      nickname: 'Alice',
      avatar: 'https://example.com/avatar.png',
      email: 'alice@example.com',
      roles: ['admin'],
    }

    // Act
    useUserStore.getState().setUserInfo(userInfo)

    // Assert
    expect(useUserStore.getState().userInfo).toEqual(userInfo)
  })

  it('should set current tenant and persist to localStorage', () => {
    // Arrange
    const tenant: Tenant = { id: 'tenant-1', name: 'Default', code: 'default' }

    // Act
    useUserStore.getState().setCurrentTenant(tenant)

    // Assert
    expect(useUserStore.getState().currentTenant).toEqual(tenant)
    expect(localStorage.getItem('schemaplexai_tenant')).toBe('tenant-1')
  })

  it('should set tenants list', () => {
    // Arrange
    const tenants: Tenant[] = [
      { id: 't1', name: 'Tenant 1', code: 't1-code' },
      { id: 't2', name: 'Tenant 2', code: 't2-code' },
    ]

    // Act
    useUserStore.getState().setTenants(tenants)

    // Assert
    expect(useUserStore.getState().tenants).toEqual(tenants)
  })

  it('should return true from isLoggedIn when userInfo is set', () => {
    // Arrange
    const userInfo: UserInfo = {
      id: 'user-1',
      username: 'alice',
      roles: ['user'],
    }

    // Act
    useUserStore.getState().setUserInfo(userInfo)

    // Assert
    expect(useUserStore.getState().isLoggedIn()).toBe(true)
  })

  it('should return false from isLoggedIn when userInfo is null', () => {
    // Arrange & Act — initial state has userInfo as null

    // Assert
    expect(useUserStore.getState().isLoggedIn()).toBe(false)
  })

  it('should restore currentTenant from localStorage on init', async () => {
    // Arrange
    localStorage.setItem('schemaplexai_tenant', 'saved-tenant')
    vi.resetModules()

    // Act — re-import to trigger init logic
    const { useUserStore: freshStore } = await import('@/stores/userStore')
    const state = freshStore.getState()

    // Assert
    expect(state.currentTenant).toEqual({ id: 'saved-tenant' })
  })
})
