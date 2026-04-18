import { useEffect, useState } from 'react'
import { getHistory, getHistoryDetail, retrySession } from '../api/quizApi'
import './HistoryList.css'

function formatDate(iso) {
  try {
    return new Date(iso).toLocaleString('ko-KR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  } catch {
    return iso
  }
}

function shuffle(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

function HistoryList({ onSelectEntry, onRetry, onToast }) {
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [viewLoadingId, setViewLoadingId] = useState(null)
  const [retryLoadingId, setRetryLoadingId] = useState(null)

  const isBusy = viewLoadingId !== null || retryLoadingId !== null

  useEffect(() => {
    getHistory()
      .then(setSessions)
      .catch(() => setError('기록을 불러오지 못했습니다. 서버 연결을 확인해주세요.'))
      .finally(() => setLoading(false))
  }, [])

  const handleSelect = async (session) => {
    setViewLoadingId(session.id)
    try {
      const { items } = await getHistoryDetail(session.id)
      onSelectEntry({ topic: session.topic, items })
    } catch {
      onToast?.('세션 데이터를 불러오지 못했습니다.', 'error')
    } finally {
      setViewLoadingId(null)
    }
  }

  const handleRetry = async (e, session) => {
    e.stopPropagation()
    setRetryLoadingId(session.id)
    try {
      const { items } = await retrySession(session.id)
      onRetry(session.topic, shuffle(items))
    } catch (err) {
      const msg = err.response?.data?.message ?? '오답 문항을 불러오지 못했습니다.'
      onToast?.(msg, 'error')
    } finally {
      setRetryLoadingId(null)
    }
  }

  if (loading) {
    return (
      <div className="history-list-page">
        <p className="history-list-empty">불러오는 중...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="history-list-page">
        <p className="history-list-error" role="alert">{error}</p>
      </div>
    )
  }

  if (sessions.length === 0) {
    return (
      <div className="history-list-page">
        <p className="history-list-empty">저장된 기록이 없습니다.</p>
      </div>
    )
  }

  return (
    <div className="history-list-page">
      <h1 className="history-list-title">오답 노트 · 기록</h1>
      <p className="history-list-hint">항목을 누르면 당시 풀이 화면으로 돌아갑니다.</p>
      <ul className="history-list">
        {sessions.map((session) => (
          <li key={session.id}>
            <div className="history-list-row">
              <button
                type="button"
                className="history-list-item"
                onClick={() => handleSelect(session)}
                disabled={isBusy}
              >
                <span className="history-list-item-date">{formatDate(session.createdAt)}</span>
                <span className="history-list-item-topic">{session.topic || '(주제 없음)'}</span>
                <span className="history-list-item-meta">
                  {session.difficulty} · {session.questionCount}문항
                </span>
                {viewLoadingId === session.id && (
                  <span className="history-list-item-loading">불러오는 중...</span>
                )}
              </button>

              {session.wrongCount > 0 && (
                <button
                  type="button"
                  className="history-retry-btn"
                  onClick={(e) => handleRetry(e, session)}
                  disabled={isBusy}
                  title="틀린 문항만 다시 풀기"
                >
                  {retryLoadingId === session.id
                    ? '...'
                    : `오답 ${session.wrongCount}개 재풀기`}
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default HistoryList
