import './Toast.css'

const ICONS = { error: '✕', success: '✓', warning: '!' }

function Toast({ toasts, onClose }) {
  if (toasts.length === 0) return null

  return (
    <div className="toast-container" aria-live="polite">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast--${toast.type}`} role="alert">
          <span className="toast-icon" aria-hidden="true">{ICONS[toast.type]}</span>
          <span className="toast-message">{toast.message}</span>
          <button
            type="button"
            className="toast-close"
            onClick={() => onClose(toast.id)}
            aria-label="닫기"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  )
}

export default Toast
