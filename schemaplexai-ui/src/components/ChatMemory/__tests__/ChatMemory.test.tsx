import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import ChatMemory from '../index'
import type { ChatMessage } from '../index'

describe('ChatMemory', () => {
  it('renders empty state when no messages', () => {
    render(<ChatMemory messages={[]} />)
    expect(screen.getByText('暂无对话记录')).toBeInTheDocument()
  })

  it('renders user messages aligned right', () => {
    const messages: ChatMessage[] = [
      { id: '1', role: 'user', content: 'Hello', timestamp: Date.now() },
    ]

    render(<ChatMemory messages={messages} />)
    expect(screen.getByText('Hello')).toBeInTheDocument()
  })

  it('renders assistant messages aligned left', () => {
    const messages: ChatMessage[] = [
      { id: '2', role: 'assistant', content: 'Hi there!', timestamp: Date.now() },
    ]

    render(<ChatMemory messages={messages} />)
    expect(screen.getByText('Hi there!')).toBeInTheDocument()
  })

  it('renders multiple messages in order', () => {
    const messages: ChatMessage[] = [
      { id: '1', role: 'user', content: 'Question 1', timestamp: 1000 },
      { id: '2', role: 'assistant', content: 'Answer 1', timestamp: 2000 },
      { id: '3', role: 'user', content: 'Question 2', timestamp: 3000 },
    ]

    render(<ChatMemory messages={messages} />)
    expect(screen.getByText('Question 1')).toBeInTheDocument()
    expect(screen.getByText('Answer 1')).toBeInTheDocument()
    expect(screen.getByText('Question 2')).toBeInTheDocument()
  })

  it('displays timestamp for each message', () => {
    const now = Date.now()
    const messages: ChatMessage[] = [
      { id: '1', role: 'user', content: 'Test', timestamp: now },
    ]

    render(<ChatMemory messages={messages} />)
    const timeStr = new Date(now).toLocaleTimeString()
    expect(screen.getByText(timeStr)).toBeInTheDocument()
  })

  it('user messages have blue background', () => {
    const messages: ChatMessage[] = [
      { id: '1', role: 'user', content: 'Blue background', timestamp: Date.now() },
    ]

    const { container } = render(<ChatMemory messages={messages} />)
    const bubble = container.querySelector('div')
    expect(bubble).toHaveStyle({ background: '#1677ff' })
  })

  it('assistant messages have gray background', () => {
    const messages: ChatMessage[] = [
      { id: '2', role: 'assistant', content: 'Gray background', timestamp: Date.now() },
    ]

    const { container } = render(<ChatMemory messages={messages} />)
    const bubble = container.querySelector('div')
    expect(bubble).toHaveStyle({ background: '#f6f8fa' })
  })
})
