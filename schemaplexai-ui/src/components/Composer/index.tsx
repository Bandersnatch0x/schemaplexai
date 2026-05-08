import { useState, useRef, useCallback, useEffect } from 'react'
import { Input, Button, Tooltip, Badge, Spin, Tabs } from 'antd'
import {
  SendOutlined,
  PaperClipOutlined,
  CloseCircleOutlined,
  FileImageOutlined,
  FileTextOutlined,
  FileOutlined,
  EyeOutlined,
  EditOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { marked } from 'marked'
import { useMentions } from './useMentions'
import { useFileUpload } from './useFileUpload'
import type { ComposerValue, MentionCandidate, ComposerAttachment } from './types'
import './Composer.css'

const { TextArea } = Input

interface ComposerProps {
  placeholder?: string
  disabled?: boolean
  loading?: boolean
  onSend: (value: ComposerValue) => void
}

function getFileIcon(mimeType: string) {
  if (mimeType.startsWith('image/')) return <FileImageOutlined />
  if (mimeType.startsWith('text/')) return <FileTextOutlined />
  return <FileOutlined />
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function Composer({ placeholder, disabled, loading, onSend }: ComposerProps) {
  const { t } = useTranslation()
  const [text, setText] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const dropRef = useRef<HTMLDivElement>(null)
  const [dragOver, setDragOver] = useState(false)
  const [previewMode, setPreviewMode] = useState(false)

  const mentions = useMentions()
  const files = useFileUpload()

  useEffect(() => {
    files.checkScanStatus()
  }, [files.checkScanStatus])

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const val = e.target.value
      const cursor = e.target.selectionStart
      setText(val)
      mentions.onInputChange(val, cursor)
    },
    [mentions]
  )

  const handleSelectMention = useCallback(
    (candidate: MentionCandidate) => {
      const insert = mentions.onSelect(candidate)
      if (insert === undefined || mentions.query === undefined) return
      const start = text.lastIndexOf('@' + mentions.query)
      if (start === -1) return
      const before = text.slice(0, start)
      const after = text.slice(start + 1 + mentions.query.length)
      const next = before + insert + after
      setText(next)
      mentions.reset()
      setTimeout(() => {
        const el = textareaRef.current
        if (el) {
          const pos = before.length + insert.length
          el.setSelectionRange(pos, pos)
          el.focus()
        }
      }, 0)
    },
    [mentions, text]
  )

  const handleSend = useCallback(() => {
    if (disabled || loading) return
    const trimmed = text.trim()
    if (!trimmed && files.attachments.length === 0) return
    onSend({ text: trimmed, attachments: files.attachments, mentions: [] })
    setText('')
    files.reset()
    mentions.reset()
  }, [disabled, loading, text, files, mentions, onSend])

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        handleSend()
      }
    },
    [handleSend]
  )

  const handlePaste = useCallback(
    async (e: React.ClipboardEvent<HTMLTextAreaElement>) => {
      await files.handlePaste(e)
    },
    [files]
  )

  const handleDrop = useCallback(
    (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault()
      setDragOver(false)
      files.addFiles(e.dataTransfer.files)
    },
    [files]
  )

  const handleDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    setDragOver(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    setDragOver(false)
  }, [])

  const handleFileInput = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      files.addFiles(e.target.files)
      e.target.value = ''
    },
    [files]
  )

  const canSend = !disabled && !loading && (text.trim().length > 0 || files.attachments.length > 0)

  return (
    <div
      ref={dropRef}
      className={`composer-root ${dragOver ? 'composer-dragover' : ''}`}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
    >
      {mentions.active && mentions.candidates.length > 0 && (
        <div className="composer-mention-popover">
          {mentions.candidates.map((c) => (
            <div
              key={`${c.type}-${c.id}`}
              className="composer-mention-item"
              onClick={() => handleSelectMention(c)}
            >
              <span className="composer-mention-type">{c.type}</span>
              <span className="composer-mention-name">{c.name}</span>
            </div>
          ))}
        </div>
      )}

      {files.attachments.length > 0 && (
        <div className="composer-attachments">
          {files.attachments.map((att: ComposerAttachment) => (
            <div key={att.id} className="composer-attachment-chip">
              <Tooltip title={att.name}>
                <span className="composer-attachment-icon">{getFileIcon(att.mimeType)}</span>
              </Tooltip>
              <span className="composer-attachment-name">{att.name}</span>
              <span className="composer-attachment-size">{formatSize(att.size)}</span>
              <CloseCircleOutlined
                className="composer-attachment-remove"
                onClick={() => files.removeAttachment(att.id)}
              />
            </div>
          ))}
        </div>
      )}

      <div className="composer-input-row">
        {previewMode ? (
          <div className="composer-preview">
            {text.trim() ? (
              <div
                className="composer-preview-content"
                dangerouslySetInnerHTML={{ __html: marked.parse(text) as string }}
              />
            ) : (
              <div className="composer-preview-empty">无内容可预览</div>
            )}
          </div>
        ) : (
          <TextArea
            ref={textareaRef}
            rows={1}
            placeholder={placeholder || t('agentExecutor.promptPlaceholder')}
            value={text}
            disabled={disabled || loading}
            className="composer-textarea"
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            onPaste={handlePaste}
          />
        )}

        <div className="composer-actions">
          <Tooltip title={previewMode ? '编辑模式' : 'Markdown 预览'}>
            <Button
              type="text"
              icon={previewMode ? <EditOutlined /> : <EyeOutlined />}
              disabled={disabled || loading}
              onClick={() => setPreviewMode((prev) => !prev)}
            />
          </Tooltip>

          <input
            type="file"
            multiple
            style={{ display: 'none' }}
            id="composer-file-input"
            onChange={handleFileInput}
          />
          <Tooltip
            title={
              files.scanHealthy === false
                ? '文件扫描服务不可用，上传已禁用'
                : t('agentExecutor.attachFile')
            }
          >
            <Button
              type="text"
              icon={<PaperClipOutlined />}
              disabled={files.scanHealthy === false || disabled || loading}
              onClick={() => document.getElementById('composer-file-input')?.click()}
            />
          </Tooltip>

          {files.scanHealthy === false && (
            <Badge status="error" text="扫描不可用" className="composer-scan-badge" />
          )}

          {files.uploading && <Spin size="small" className="composer-upload-spin" />}

          <Button
            type="primary"
            icon={<SendOutlined />}
            loading={loading}
            disabled={!canSend}
            onClick={handleSend}
          >
            {t('agentExecutor.execute')}
          </Button>
        </div>
      </div>
    </div>
  )
}
