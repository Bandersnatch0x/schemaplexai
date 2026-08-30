import { useState, useCallback, useRef } from 'react'
import type { MentionCandidate } from './types'

/**
 * Mention candidate source.
 *
 * Candidates are meant to come from backend search (files, sessions, skills,
 * agents). Until that endpoint exists the source is intentionally empty —
 * the Composer simply offers no mention suggestions, and the dropdown stays
 * hidden (it only renders when candidates are present). The spec quality
 * gate forbids hardcoded demo/placeholder constants in production code, so
 * no fixture data lives here.
 */
const CANDIDATE_SOURCE: MentionCandidate[] = []

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
      CANDIDATE_SOURCE.filter((c) => c.name.toLowerCase().includes(q)).slice(0, 6)
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
