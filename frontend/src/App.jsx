import { useCallback, useState } from 'react'
import CodeEditor from './components/CodeEditor'
import FileUpload from './components/FileUpload'
import HistoryList from './components/HistoryList'
import QuizForm from './components/QuizForm'
import QuizList from './components/QuizList'
import Toast from './components/Toast'
import './App.css'

function useToast() {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const showToast = useCallback(
    (message, type = 'error') => {
      const id = Date.now() + Math.random()
      setToasts((prev) => [...prev, { id, message, type }])
      setTimeout(() => dismiss(id), 3000)
    },
    [dismiss],
  )

  return { toasts, showToast, dismiss }
}

function App() {
  const [quizzes, setQuizzes] = useState(null)
  const [quizTopic, setQuizTopic] = useState('')
  const [quizSessionId, setQuizSessionId] = useState(null)
  const [isHistoryReplay, setIsHistoryReplay] = useState(false)
  const [showHistory, setShowHistory] = useState(false)
  const [inputTab, setInputTab] = useState('form')

  const { toasts, showToast, dismiss } = useToast()

  // sessionId는 신규 생성 시에만 존재, 히스토리 리플레이·오답 재풀기 시 null
  const handleQuizGenerated = (items, topic, sessionId = null) => {
    setIsHistoryReplay(false)
    setQuizTopic(topic ?? '')
    setQuizSessionId(sessionId)
    setQuizzes(items)
  }

  const handleResetQuizList = () => {
    setQuizzes(null)
    setQuizTopic('')
    setQuizSessionId(null)
    setIsHistoryReplay(false)
  }

  const handleSelectHistoryEntry = (entry) => {
    setShowHistory(false)
    setIsHistoryReplay(true)
    setQuizTopic(entry.topic ?? '')
    setQuizSessionId(null)
    setQuizzes(entry.items)
  }

  // 오답 재풀기: isHistoryReplay=false로 신규 퀴즈처럼 진행
  const handleRetry = (topic, items) => {
    setShowHistory(false)
    setIsHistoryReplay(false)
    setQuizTopic(topic ?? '')
    setQuizSessionId(null)
    setQuizzes(items)
  }

  const showInputTabs = !showHistory && (quizzes == null || quizzes.length === 0)

  const isCodingTab = !showHistory && quizzes == null && inputTab === 'coding'

  return (
    <div className="app-root">
      {/* ── Header ── */}
      <header className="app-header">
        <div
          className="app-header-brand"
          role="button"
          tabIndex={0}
          onClick={() => {
            setShowHistory(false)
            setQuizzes(null)
            setQuizTopic('')
            setQuizSessionId(null)
            setIsHistoryReplay(false)
            setInputTab('form')
          }}
          onKeyDown={(e) => e.key === 'Enter' && e.currentTarget.click()}
        >
          <span className="app-header-logo">UOJ</span>
          <span className="app-header-sub">Umjun Online Judge</span>
        </div>
        <div className="app-header-actions">
          <button
            type="button"
            className={`app-header-btn${showHistory ? ' app-header-btn--active' : ''}`}
            onClick={() => setShowHistory((v) => !v)}
          >
            {showHistory ? '← 돌아가기' : '기록 보기'}
          </button>
        </div>
      </header>

      {/* ── Input tabs (헤더 바로 아래, 퀴즈 뷰/히스토리 뷰가 아닐 때만) ── */}
      {showInputTabs && (
        <div className="app-tabs-bar">
          <div className="app-tabs">
            <button
              type="button"
              className={`app-tab${inputTab === 'form' ? ' app-tab--active' : ''}`}
              onClick={() => setInputTab('form')}
            >
              직접 입력
            </button>
            <button
              type="button"
              className={`app-tab${inputTab === 'upload' ? ' app-tab--active' : ''}`}
              onClick={() => setInputTab('upload')}
            >
              파일 업로드
            </button>
            <button
              type="button"
              className={`app-tab${inputTab === 'coding' ? ' app-tab--active' : ''}`}
              onClick={() => setInputTab('coding')}
            >
              코딩 실기
            </button>
          </div>
        </div>
      )}

      {/* ── Content ── */}
      <main className={isCodingTab ? 'app-main app-main--full' : 'app-main'}>
        {showHistory ? (
          <HistoryList
            onSelectEntry={handleSelectHistoryEntry}
            onRetry={handleRetry}
            onToast={showToast}
          />
        ) : quizzes != null && quizzes.length > 0 ? (
          <QuizList
            items={quizzes}
            topic={quizTopic}
            sessionId={quizSessionId}
            isHistoryReplay={isHistoryReplay}
            onReset={handleResetQuizList}
            onToast={showToast}
          />
        ) : (
          <>
            {inputTab === 'form' && (
              <QuizForm onQuizGenerated={handleQuizGenerated} onToast={showToast} />
            )}
            {inputTab === 'upload' && (
              <FileUpload onQuizGenerated={handleQuizGenerated} onToast={showToast} />
            )}
            {inputTab === 'coding' && (
              <CodeEditor onToast={showToast} />
            )}
          </>
        )}
      </main>

      <Toast toasts={toasts} onClose={dismiss} />
    </div>
  )
}

export default App
