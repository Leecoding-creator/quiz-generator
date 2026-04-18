import { useState } from 'react'
import QuizForm from './components/QuizForm'
import QuizList from './components/QuizList'
import HistoryList from './components/HistoryList'
import './App.css'

function App() {
  const [quizzes, setQuizzes] = useState(null)
  const [quizTopic, setQuizTopic] = useState('')
  const [isHistoryReplay, setIsHistoryReplay] = useState(false)
  const [showHistory, setShowHistory] = useState(false)

  const handleQuizGenerated = (items, topic) => {
    setIsHistoryReplay(false)
    setQuizTopic(topic ?? '')
    setQuizzes(items)
  }

  const handleResetQuizList = () => {
    setQuizzes(null)
    setQuizTopic('')
    setIsHistoryReplay(false)
  }

  const handleSelectHistoryEntry = (entry) => {
    setShowHistory(false)
    setIsHistoryReplay(true)
    setQuizTopic(entry.topic ?? '')
    setQuizzes(entry.items)
  }

  return (
    <div className="app-root">
      <div className="app-toolbar">
        {showHistory ? (
          <button type="button" onClick={() => setShowHistory(false)}>
            퀴즈로 돌아가기
          </button>
        ) : (
          <button type="button" onClick={() => setShowHistory(true)}>
            기록 보기
          </button>
        )}
      </div>

      {showHistory ? (
        <HistoryList onSelectEntry={handleSelectHistoryEntry} />
      ) : quizzes != null && quizzes.length > 0 ? (
        <QuizList
          items={quizzes}
          topic={quizTopic}
          isHistoryReplay={isHistoryReplay}
          onReset={handleResetQuizList}
        />
      ) : (
        <QuizForm onQuizGenerated={handleQuizGenerated} />
      )}
    </div>
  )
}

export default App
