import { describe, it, expect } from 'vitest'
import { abyssHiveTheme } from './index'

describe('abyssHiveTheme', () => {
  it('has correct base background color', () => {
    expect(abyssHiveTheme.token?.colorBgBase).toBe('#0a0e1a')
  })

  it('has correct primary color', () => {
    expect(abyssHiveTheme.token?.colorPrimary).toBe('#00d4aa')
  })

  it('has transparent input background', () => {
    expect(abyssHiveTheme.components?.Input?.colorBgContainer).toBe('transparent')
  })

  it('has correct table hover background', () => {
    expect(abyssHiveTheme.components?.Table?.rowHoverBg).toBe('#111827')
  })
})
