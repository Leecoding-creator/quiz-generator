import { useState } from 'react'
import { generateQuiz } from '../api/quizApi'
import './QuizForm.css'

function QuizForm({ onQuizGenerated, onToast }) {
  const [topic, setTopic] = useState('')
  const [difficulty, setDifficulty] = useState('easy')
  const [questionCount, setQuestionCount] = useState(10)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()

    try {
      setLoading(true)
      const { items, sessionId } = await generateQuiz(topic, difficulty, questionCount)
      onQuizGenerated(items, topic, sessionId)
    } catch (error) {
      const msg =
        error.response?.data?.message ?? '퀴즈 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
      onToast(msg, 'error')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="quiz-form-wrapper">
      <h1>Quiz Form</h1>
      <form className="quiz-form" onSubmit={handleSubmit}>
        <label htmlFor="topic">주제 (Topic)</label>
        <input
          id="topic"
          name="topic"
          type="text"
          value={topic}
          onChange={(event) => setTopic(event.target.value)}
          placeholder="예: JavaScript"
          required
        />

        <label htmlFor="difficulty">난이도 (Difficulty)</label>
        <select
          id="difficulty"
          name="difficulty"
          value={difficulty}
          onChange={(event) => setDifficulty(event.target.value)}
        >
          <option value="easy">Easy</option>
          <option value="medium">Medium</option>
          <option value="hard">Hard</option>
        </select>

        <label htmlFor="questionCount">문항 수 (Question count)</label>
        <input
          id="questionCount"
          name="questionCount"
          type="number"
          min="1"
          max="50"
          value={questionCount}
          onChange={(event) => setQuestionCount(Number(event.target.value))}
          required
        />

        <button type="submit" disabled={loading}>
          {loading ? '생성 중...' : '퀴즈 생성'}
        </button>
      </form>
    </section>
  )
}

export default QuizForm
