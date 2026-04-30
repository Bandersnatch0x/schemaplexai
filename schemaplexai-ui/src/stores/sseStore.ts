import { create } from 'zustand'
import type { SseEvent } from '@/types'

interface SseState {
  events: SseEvent[]
  connected: boolean
  connecting: boolean
  addEvent: (event: SseEvent) => void
  clearEvents: () => void
  setConnected: (connected: boolean) => void
  setConnecting: (connecting: boolean) => void
}

export const useSseStore = create<SseState>((set) => ({
  events: [],
  connected: false,
  connecting: false,
  addEvent: (event) =>
    set((state) => ({
      events: [...state.events, event].slice(-1000),
    })),
  clearEvents: () => set({ events: [] }),
  setConnected: (connected) => set({ connected }),
  setConnecting: (connecting) => set({ connecting }),
}))
