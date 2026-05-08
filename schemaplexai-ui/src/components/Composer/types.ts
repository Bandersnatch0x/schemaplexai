export interface ComposerAttachment {
  id: string
  name: string
  url: string
  mimeType: string
  size: number
}

export interface ComposerMention {
  id: string
  type: 'file' | 'session' | 'skill' | 'agent'
  name: string
}

export interface ComposerValue {
  text: string
  attachments: ComposerAttachment[]
  mentions: ComposerMention[]
}

export interface MentionCandidate {
  id: string
  type: ComposerMention['type']
  name: string
  icon?: string
}
