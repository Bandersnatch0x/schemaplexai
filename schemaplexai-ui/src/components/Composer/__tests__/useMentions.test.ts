import { describe, it, expect } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useMentions } from '../useMentions'

describe('useMentions', () => {
  it('starts inactive', () => {
    const { result } = renderHook(() => useMentions())
    expect(result.current.active).toBe(false)
    expect(result.current.candidates).toEqual([])
  })

  it('activates on @ with empty candidate source (no mock data)', () => {
    const { result } = renderHook(() => useMentions())
    act(() => result.current.onInputChange('Hello @co', 9))
    expect(result.current.active).toBe(true)
    expect(result.current.query).toBe('co')
    // Candidate source is backend-driven and currently empty — hardcoded
    // demo fixtures must not leak into production (spec quality gate REQ-28).
    expect(result.current.candidates).toEqual([])
  })

  it('deactivates when space after @', () => {
    const { result } = renderHook(() => useMentions())
    act(() => result.current.onInputChange('Hello @ co', 10))
    expect(result.current.active).toBe(false)
  })

  it('returns insert text on select', () => {
    const { result } = renderHook(() => useMentions())
    act(() => result.current.onInputChange('@co', 3))
    const candidate = { id: 'f1', type: 'file' as const, name: 'design-spec.md' }
    const insert = result.current.onSelect(candidate)
    expect(insert).toContain('[@design-spec.md]')
    expect(insert).toContain('(file:f1)')
  })

  it('returns undefined when selecting without an active mention', () => {
    const { result } = renderHook(() => useMentions())
    const candidate = { id: 'f1', type: 'file' as const, name: 'design-spec.md' }
    expect(result.current.onSelect(candidate)).toBeUndefined()
  })

  it('resets to inactive', () => {
    const { result } = renderHook(() => useMentions())
    act(() => result.current.onInputChange('@a', 2))
    expect(result.current.active).toBe(true)
    act(() => result.current.reset())
    expect(result.current.active).toBe(false)
    expect(result.current.candidates).toEqual([])
  })
})
