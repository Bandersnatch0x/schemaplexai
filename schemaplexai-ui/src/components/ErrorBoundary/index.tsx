import { Component, type ReactNode } from 'react'
import './ErrorBoundary.css'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error?: Error
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo)
  }

  handleReload = () => {
    window.location.reload()
  }

  handleGoHome = () => {
    window.location.href = '/'
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary-page">
          <div className="error-boundary-card">
            <div className="error-boundary-icon">⚠</div>
            <h1 className="error-boundary-title">Something went wrong</h1>
            <p className="error-boundary-desc">
              An unexpected error occurred. Please try refreshing the page.
            </p>
            {this.state.error && (
              <pre className="error-boundary-trace">{this.state.error.message}</pre>
            )}
            <div className="error-boundary-actions">
              <button className="error-boundary-btn error-boundary-btn--primary" onClick={this.handleReload}>
                Reload Page
              </button>
              <button className="error-boundary-btn error-boundary-btn--ghost" onClick={this.handleGoHome}>
                Go Home
              </button>
            </div>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
