import { useEffect } from 'react'
import { Select } from 'antd'
import { useUserStore } from '@/stores/userStore'
import type { Tenant } from '@/types'

const mockTenants: Tenant[] = [
  { id: 'tenant-1', name: '默认租户', code: 'default' },
  { id: 'tenant-2', name: '研发一部', code: 'rd-dept-1' },
  { id: 'tenant-3', name: '测试中心', code: 'qa-center' },
]

export default function TenantSelector() {
  const { currentTenant, setCurrentTenant, tenants, setTenants } = useUserStore()

  useEffect(() => {
    if (tenants.length === 0) {
      setTenants(mockTenants)
      const saved = localStorage.getItem('schemaplexai_tenant')
      const found = mockTenants.find((t) => t.id === saved)
      if (found) setCurrentTenant(found)
    }
  }, [tenants.length, setTenants, setCurrentTenant])

  return (
    <Select
      value={currentTenant?.id}
      placeholder="选择租户"
      style={{ width: 140 }}
      bordered={false}
      onChange={(value) => {
        const tenant = tenants.find((t) => t.id === value)
        if (tenant) setCurrentTenant(tenant)
      }}
      options={tenants.map((t) => ({ label: t.name, value: t.id }))}
    />
  )
}
