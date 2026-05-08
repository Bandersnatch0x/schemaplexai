import { create } from 'zustand'
import type { SseEvent, SseEventType } from '@/types'

interface SseState {
  events: SseEvent[]
  connected: boolean
  connecting: boolean
  addEvent: (event: SseEvent) => void
  addRawEvent: (type: SseEventType, data: Record<string, unknown>) => void
  clearEvents: () => void
  setConnected: (connected: boolean) => void
  setConnecting: (connecting: boolean) => void
}

let eventIdCounter = 0

function generateEventId(): string {
  return `evt_${Date.now()}_${++eventIdCounter}`
}

function extractContent(data: Record<string, unknown>): string {
  if (typeof data.content === 'string') return data.content
  if (typeof data.message === 'string') return data.message
  if (typeof data.error === 'string') return data.error
  if (data.fromState || data.toState) {
    return `State: ${data.fromState ?? 'null'} → ${data.toState ?? data.state ?? 'unknown'}`
  }
  return JSON.stringify(data)
}

export const useSseStore = create<SseState>((set) => ({
  events: [],
  connected: false,
  connecting: false,
  addEvent: (event) =>
    set((state) => ({
      events: [...state.events, event].slice(-1000),
    })),
  addRawEvent: (type, data) =>
    set((state) => {
      const event: SseEvent = {
        id: generateEventId(),
        type,
        content: extractContent(data),
        timestamp: (data.timestamp as number) || Date.now(),
        metadata: data,
      }
      return { events: [...state.events, event].slice(-1000) }
    }),
  clearEvents: () => set({ events: [] }),
  setConnected: (connected) => set({ connected }),
  setConnecting: (connecting) => set({ connecting }),
}))
