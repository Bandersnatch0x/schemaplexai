import { useState, useCallback, useRef } from 'react'
import type { MentionCandidate } from './types'

const MOCK_CANDIDATES: MentionCandidate[] = [
  { id: 'f1', type: 'file', name: 'design-spec.md' },
  { id: 'f2', type: 'file', name: 'api-contract.yaml' },
  { id: 's1', type: 'session', name: 'Session #42' },
  { id: 'sk1', type: 'skill', name: 'code-review' },
  { id: 'sk2', type: 'skill', name: 'security-audit' },
  { id: 'a1', type: 'agent', name: 'planner' },
  { id: 'a2', type: 'agent', name: 'code-reviewer' },
]

interface UseMentionsReturn {
  query: string
  candidates: MentionCandidate[]
  active: boolean
  onInputChange: (text: string, cursorPos: number) => void
  onSelect: (candidate: MentionCandidate) => string | undefined
  reset: () => void
}

export function useMentions(): UseMentionsReturn {
  const [query, setQuery] = useState('')
  const [candidates, setCandidates] = useState<MentionCandidate[]>([])
  const [active, setActive] = useState(false)
  const mentionStartRef = useRef<number | null>(null)

  const onInputChange = useCallback((text: string, cursorPos: number) => {
    const beforeCursor = text.slice(0, cursorPos)
    const lastAt = beforeCursor.lastIndexOf('@')

    if (lastAt === -1) {
      setActive(false)
      mentionStartRef.current = null
      return
    }

    const afterAt = beforeCursor.slice(lastAt + 1)
    const hasSpaceAfterAt = afterAt.includes(' ')

    if (hasSpaceAfterAt) {
      setActive(false)
      mentionStartRef.current = null
      return
    }

    mentionStartRef.current = lastAt
    const q = afterAt.toLowerCase()
    setQuery(q)
    setCandidates(
      MOCK_CANDIDATES.filter((c) => c.name.toLowerCase().includes(q)).slice(0, 6)
    )
    setActive(true)
  }, [])

  const onSelect = useCallback(
    (candidate: MentionCandidate): string | undefined => {
      if (mentionStartRef.current === null) return undefined
      return `[@${candidate.name}](${candidate.type}:${candidate.id}) `
    },
    []
  )

  const reset = useCallback(() => {
    setActive(false)
    setCandidates([])
    setQuery('')
    mentionStartRef.current = null
  }, [])

  return { query, candidates, active, onInputChange, onSelect, reset }
}
