import { useEffect, useMemo, useState } from 'react'
import { appendQuizHistory } from '../utils/quizHistoryStorage'
import QuizRichText from './QuizRichText'
import QuizExplanation from './QuizExplanation'
import './QuizList.css'

function newHistoryId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `quiz-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function QuizList({ items, topic = '', isHistoryReplay = false, onReset }) {
  const [userAnswers, setUserAnswers] = useState(() =>
    isHistoryReplay
      ? items.map((it) => (typeof it.userAnswer === 'number' ? it.userAnswer : null))
      : items.map(() => null),
  )
  const [isSubmitted, setIsSubmitted] = useState(isHistoryReplay)
  const [currentIndex, setCurrentIndex] = useState(0)

  useEffect(() => {
    if (isHistoryReplay) {
      setUserAnswers(
        items.map((it) => (typeof it.userAnswer === 'number' ? it.userAnswer : null)),
      )
      setIsSubmitted(true)
      return
    }
    setUserAnswers(items.map(() => null))
    setIsSubmitted(false)
    setCurrentIndex(0)
  }, [items, isHistoryReplay])

  const { correctCount, total, percent } = useMemo(() => {
    const totalCount = items.length
    if (!isSubmitted || totalCount === 0) {
      return { correctCount: 0, total: totalCount, percent: 0 }
    }
    const correct = items.reduce(
      (acc, item, i) => acc + (userAnswers[i] === item.correctIndex ? 1 : 0),
      0,
    )
    return {
      correctCount: correct,
      total: totalCount,
      percent: Math.round((correct / totalCount) * 100),
    }
  }, [isSubmitted, items, userAnswers])

  const selectOption = (questionIndex, optionIndex) => {
    if (isSubmitted) return
    setUserAnswers((prev) => {
      const next = [...prev]
      next[questionIndex] = optionIndex
      return next
    })
  }

  const handleSubmitScore = () => {
    const score = items.reduce(
      (acc, item, i) => acc + (userAnswers[i] === item.correctIndex ? 1 : 0),
      0,
    )
    const totalCount = items.length
    const snapshotItems = items.map((item, i) => ({
      question: item.question,
      options: item.options,
      correctIndex: item.correctIndex,
      explanation: item.explanation,
      relatedConcept: item.relatedConcept,
      complexityAnalysis: item.complexityAnalysis,
      detailedExplanation: item.detailedExplanation,
      userAnswer: userAnswers[i] ?? null,
    }))

    appendQuizHistory({
      id: newHistoryId(),
      date: new Date().toISOString(),
      topic: topic || '',
      score,
      total: totalCount,
      items: snapshotItems,
    })

    setIsSubmitted(true)
  }

  const optionClassName = (questionIndex, optionIndex, item) => {
    const picked = userAnswers[questionIndex]
    const isAnswer = optionIndex === item.correctIndex

    if (!isSubmitted) {
      const classes = ['quiz-card-option', 'quiz-card-option--selectable']
      if (picked === optionIndex) classes.push('quiz-card-option--selected')
      return classes.join(' ')
    }

    const userPickedThis = picked === optionIndex
    if (isAnswer) {
      return 'quiz-card-option quiz-card-option--answer-key'
    }
    if (userPickedThis) {
      return 'quiz-card-option quiz-card-option--wrong-user'
    }
    return 'quiz-card-option quiz-card-option--neutral'
  }

  const isLast = currentIndex === items.length - 1

  const renderCard = (item, qIndex) => (
    <article key={qIndex} className="quiz-card">
      <span className="quiz-card-badge">Q{qIndex + 1}</span>
      <h2 className="quiz-card-question">
        <QuizRichText text={item.question} />
      </h2>
      <div className="quiz-card-options" role="radiogroup" aria-label={`문항 ${qIndex + 1} 선택`}>
        {item.options?.map((opt, optIndex) => (
          <button
            key={optIndex}
            type="button"
            className={optionClassName(qIndex, optIndex, item)}
            onClick={() => selectOption(qIndex, optIndex)}
            disabled={isSubmitted}
            aria-pressed={userAnswers[qIndex] === optIndex}
          >
            <span className="quiz-card-option-label">{optIndex + 1}</span>
            <div className="quiz-card-option-text">
              <QuizRichText text={opt} />
            </div>
            {isSubmitted && optIndex === item.correctIndex && (
              <span className="quiz-card-answer-tag">정답</span>
            )}
          </button>
        ))}
      </div>
      {item.relatedConcept && (
        <p className="quiz-card-meta">개념: {item.relatedConcept}</p>
      )}
      {isSubmitted && (item.explanation || item.complexityAnalysis) && (
        <div className="quiz-card-explanation-block">
          {item.explanation && (
            <p className="quiz-card-explanation">{item.explanation}</p>
          )}
          {item.complexityAnalysis && (
            <div className="quiz-card-complexity">
              <span className="quiz-card-complexity-label">복잡도 분석</span>
              <p className="quiz-card-complexity-body">{item.complexityAnalysis}</p>
            </div>
          )}
        </div>
      )}
      <QuizExplanation
        detailedExplanation={item.detailedExplanation}
        visible={isSubmitted}
      />
    </article>
  )

  return (
    <div className="quiz-list-page">
      <div
        className={
          isSubmitted ? 'quiz-score-banner quiz-score-banner--done' : 'quiz-score-banner'
        }
      >
        {isSubmitted ? (
          <>
            <span className="quiz-score-main">
              {correctCount} / {total} 맞음
            </span>
            <span className="quiz-score-percent">{percent}%</span>
          </>
        ) : (
          <span className="quiz-score-pending">
            {total}문항 · 답을 고른 뒤 하단의 채점하기를 눌러주세요
          </span>
        )}
      </div>

      <header className="quiz-list-header">
        <h1 className="quiz-list-title">{isSubmitted ? '퀴즈 결과' : '퀴즈'}</h1>
        <button type="button" className="quiz-list-reset" onClick={onReset}>
          다시 생성하기
        </button>
      </header>

      {!isSubmitted && (
        <div className="quiz-progress-bar-wrap">
          <div
            className="quiz-progress-bar-fill"
            style={{ width: `${((currentIndex + 1) / items.length) * 100}%` }}
          />
        </div>
      )}

      {isSubmitted ? (
        <div className="quiz-list-grid">
          {items.map((item, qIndex) => renderCard(item, qIndex))}
        </div>
      ) : (
        <>
          <div className="quiz-list-grid">
            {renderCard(items[currentIndex], currentIndex)}
          </div>

          <div className="quiz-list-actions quiz-list-actions--nav">
            <button
              type="button"
              className="quiz-nav-btn"
              onClick={() => setCurrentIndex((i) => i - 1)}
              disabled={currentIndex === 0}
            >
              이전
            </button>

            {isLast ? (
              <button type="button" className="quiz-list-submit" onClick={handleSubmitScore}>
                채점하기
              </button>
            ) : (
              <button
                type="button"
                className="quiz-nav-btn quiz-nav-btn--next"
                onClick={() => setCurrentIndex((i) => i + 1)}
              >
                다음
              </button>
            )}
          </div>
        </>
      )}
    </div>
  )
}

export default QuizList
