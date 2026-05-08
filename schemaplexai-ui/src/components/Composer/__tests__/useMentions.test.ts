import { describe, it, expect } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useMentions } from '../useMentions'

describe('useMentions', () => {
  it('starts inactive', () => {
    const { result } = renderHook(() => useMentions())
    expect(result.current.active).toBe(false)
    expect(result.current.candidates).toEqual([])
  })

  it('activates on @ and filters candidates', () => {
    const { result } = renderHook(() => useMentions())
    act(() => result.current.onInputChange('Hello @co', 9))
    expect(result.current.active).toBe(true)
    expect(result.current.candidates.length).toBeGreaterThan(0)
    expect(result.current.candidates.every((c) => c.name.toLowerCase().includes('co'))).toBe(true)
  })

  it('deactivates when space after @', () => {
    const { result } = renderHook(() => useMentions())
    act(() => result.current.onInputChange('Hello @ co', 10))
    expect(result.current.active).toBe(false)
  })

  it('returns insert text on select', () => {
    const { result } = renderHook(() => useMentions())
    act(() => result.current.onInputChange('@co', 3))
    const candidate = result.current.candidates[0]
    const insert = result.current.onSelect(candidate)
    expect(insert).toContain(`[@${candidate.name}]`)
  })

  it('resets to inactive', () => {
    const { result } = renderHook(() => useMentions())
    act(() => result.current.onInputChange('@a', 2))
    expect(result.current.active).toBe(true)
    act(() => result.current.reset())
    expect(result.current.active).toBe(false)
  })
})
