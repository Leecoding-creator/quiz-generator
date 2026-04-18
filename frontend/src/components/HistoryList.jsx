import { useEffect, useState } from 'react'
import { getHistory, getHistoryDetail } from '../api/quizApi'
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

function HistoryList({ onSelectEntry }) {
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [loadingId, setLoadingId] = useState(null)

  useEffect(() => {
    getHistory()
      .then(setSessions)
      .catch(() => setError('기록을 불러오지 못했습니다. 서버 연결을 확인해주세요.'))
      .finally(() => setLoading(false))
  }, [])

  const handleSelect = async (session) => {
    setLoadingId(session.id)
    try {
      const { items } = await getHistoryDetail(session.id)
      onSelectEntry({ topic: session.topic, items })
    } catch {
      setError('세션 데이터를 불러오지 못했습니다.')
    } finally {
      setLoadingId(null)
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
            <button
              type="button"
              className="history-list-item"
              onClick={() => handleSelect(session)}
              disabled={loadingId !== null}
            >
              <span className="history-list-item-date">{formatDate(session.createdAt)}</span>
              <span className="history-list-item-topic">{session.topic || '(주제 없음)'}</span>
              <span className="history-list-item-meta">
                {session.difficulty} · {session.questionCount}문항
              </span>
              {loadingId === session.id && (
                <span className="history-list-item-loading">불러오는 중...</span>
              )}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default HistoryList
