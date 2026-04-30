import { create } from 'zustand'
import type { UserInfo, Tenant } from '@/types'

interface UserState {
  userInfo: UserInfo | null
  currentTenant: Tenant | null
  tenants: Tenant[]
  setUserInfo: (userInfo: UserInfo | null) => void
  setCurrentTenant: (tenant: Tenant | null) => void
  setTenants: (tenants: Tenant[]) => void
  isLoggedIn: () => boolean
}

const savedTenant = localStorage.getItem('schemaplexai_tenant')

export const useUserStore = create<UserState>((set, get) => ({
  userInfo: null,
  currentTenant: savedTenant
    ? ({ id: savedTenant } as Tenant)
    : null,
  tenants: [],
  setUserInfo: (userInfo) => set({ userInfo }),
  setCurrentTenant: (currentTenant) => {
    if (currentTenant) {
      localStorage.setItem('schemaplexai_tenant', currentTenant.id)
    }
    set({ currentTenant })
  },
  setTenants: (tenants) => set({ tenants }),
  isLoggedIn: () => !!get().userInfo,
}))
