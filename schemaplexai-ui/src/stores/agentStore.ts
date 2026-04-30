import { create } from 'zustand'
import type { Agent, ExecutionRecord } from '@/types'

interface AgentState {
  agents: Agent[]
  currentAgent: Agent | null
  executionRecords: ExecutionRecord[]
  loading: boolean
  setAgents: (agents: Agent[]) => void
  setCurrentAgent: (agent: Agent | null) => void
  setExecutionRecords: (records: ExecutionRecord[]) => void
  setLoading: (loading: boolean) => void
  updateAgentInList: (agent: Agent) => void
  removeAgentFromList: (id: string) => void
}

export const useAgentStore = create<AgentState>((set) => ({
  agents: [],
  currentAgent: null,
  executionRecords: [],
  loading: false,
  setAgents: (agents) => set({ agents }),
  setCurrentAgent: (currentAgent) => set({ currentAgent }),
  setExecutionRecords: (executionRecords) => set({ executionRecords }),
  setLoading: (loading) => set({ loading }),
  updateAgentInList: (agent) =>
    set((state) => ({
      agents: state.agents.map((a) => (a.id === agent.id ? agent : a)),
    })),
  removeAgentFromList: (id) =>
    set((state) => ({
      agents: state.agents.filter((a) => a.id !== id),
    })),
}))
