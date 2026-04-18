import QuizRichText from './QuizRichText'
import { splitDetailedExplanation } from '../utils/splitDetailedExplanation'
import './QuizExplanation.css'

export default function QuizExplanation({ detailedExplanation, visible }) {
  if (!visible) return null

  const { correctRationale, wrongAnalysis } = splitDetailedExplanation(detailedExplanation)
  if (!correctRationale && !wrongAnalysis) return null

  return (
    <div className="quiz-explanation-panel" aria-label="심층 해설">
      <h3 className="quiz-explanation-panel-title">심층 해설</h3>
      <div className="quiz-explanation-sections">
        {correctRationale ? (
          <section className="quiz-explanation-section quiz-explanation-section--correct">
            <h4 className="quiz-explanation-section-heading">정답 근거</h4>
            <div className="quiz-explanation-section-body">
              <QuizRichText text={correctRationale} />
            </div>
          </section>
        ) : null}
        {wrongAnalysis ? (
          <section className="quiz-explanation-section quiz-explanation-section--wrong">
            <h4 className="quiz-explanation-section-heading">오답 분석</h4>
            <div className="quiz-explanation-section-body">
              <QuizRichText text={wrongAnalysis} />
            </div>
          </section>
        ) : null}
      </div>
    </div>
  )
}
