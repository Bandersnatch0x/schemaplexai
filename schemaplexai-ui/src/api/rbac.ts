import request from './request'

export function assignRoleToUser(userId: number, roleId: number) {
  return request.post('/system/role-assignments', { userId, roleId })
}

export function removeRoleFromUser(userId: number, roleId: number) {
  return request.delete(`/system/role-assignments/${userId}/${roleId}`)
}

export function assignPermissionToRole(roleId: number, permissionId: number) {
  return request.post('/system/role-assignments/permissions', { roleId, permissionId })
}

export function removePermissionFromRole(roleId: number, permissionId: number) {
  return request.delete(`/system/role-assignments/permissions/${roleId}/${permissionId}`)
}

export function getTenantPolicies() {
  return request.get('/system/tenant-policies')
}

export function getTenantPolicy(policyType: string) {
  return request.get(`/system/tenant-policies/${policyType}`)
}

export function saveTenantPolicy(policyType: string, configJson: string) {
  return request.put(`/system/tenant-policies/${policyType}`, { configJson })
}
