import { useRef, useState } from 'react'
import { generateQuiz, uploadFile } from '../api/quizApi'
import './FileUpload.css'

const ACCEPTED_TYPES = ['application/pdf', 'image/jpeg', 'image/png']
const ACCEPTED_EXT = '.pdf,.jpg,.jpeg,.png'

// idle → uploading → processing → done
//                              ↘ idle (error shown via toast)

function FileUpload({ onQuizGenerated, onToast }) {
  const [status, setStatus] = useState('idle')
  const [progress, setProgress] = useState(0)
  const [fileName, setFileName] = useState('')
  const [summary, setSummary] = useState('')
  const [extractedText, setExtractedText] = useState('')
  const [isDragOver, setIsDragOver] = useState(false)

  const [topic, setTopic] = useState('')
  const [difficulty, setDifficulty] = useState('medium')
  const [questionCount, setQuestionCount] = useState(10)
  const [generating, setGenerating] = useState(false)

  const inputRef = useRef(null)

  const processFile = async (file) => {
    if (!file) return
    if (!ACCEPTED_TYPES.includes(file.type)) {
      onToast('PDF, JPG, PNG 파일만 업로드할 수 있습니다.', 'warning')
      return
    }

    setFileName(file.name)
    setStatus('uploading')
    setProgress(0)

    try {
      const result = await uploadFile(file, (pct) => {
        setProgress(pct)
        if (pct === 100) setStatus('processing')
      })
      setSummary(result.summary)
      setExtractedText(result.extractedText)
      setStatus('done')
    } catch (err) {
      onToast(err.response?.data?.message ?? '파일 처리 중 오류가 발생했습니다.', 'error')
      setStatus('idle')
    }
  }

  const handleInputChange = (e) => processFile(e.target.files[0])

  const handleDrop = (e) => {
    e.preventDefault()
    setIsDragOver(false)
    processFile(e.dataTransfer.files[0])
  }

  const handleDragOver = (e) => {
    e.preventDefault()
    setIsDragOver(true)
  }

  const handleDragLeave = () => setIsDragOver(false)

  const handleReset = () => {
    setStatus('idle')
    setProgress(0)
    setFileName('')
    setSummary('')
    setExtractedText('')
    setTopic('')
    if (inputRef.current) inputRef.current.value = ''
  }

  const handleGenerate = async () => {
    if (!topic.trim()) return
    setGenerating(true)
    try {
      const { items, sessionId } = await generateQuiz(topic, difficulty, questionCount, extractedText)
      onQuizGenerated(items, topic, sessionId)
    } catch (err) {
      onToast(err.response?.data?.message ?? '퀴즈 생성 중 오류가 발생했습니다.', 'error')
    } finally {
      setGenerating(false)
    }
  }

  if (status === 'done') {
    return (
      <section className="file-upload-wrapper">
        <div className="file-upload-result-header">
          <span className="file-upload-filename">{fileName}</span>
          <button type="button" className="file-upload-reset-btn" onClick={handleReset}>
            다시 업로드
          </button>
        </div>

        <div className="file-upload-summary-box">
          <p className="file-upload-summary-label">요약 미리보기</p>
          <p className="file-upload-summary-text">{summary}</p>
        </div>

        <div className="file-upload-quiz-form">
          <label htmlFor="fu-topic">주제</label>
          <input
            id="fu-topic"
            type="text"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            placeholder="예: 이 문서의 핵심 주제"
          />

          <label htmlFor="fu-difficulty">난이도</label>
          <select
            id="fu-difficulty"
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value)}
          >
            <option value="easy">Easy</option>
            <option value="medium">Medium</option>
            <option value="hard">Hard</option>
          </select>

          <label htmlFor="fu-count">문항 수</label>
          <input
            id="fu-count"
            type="number"
            min="1"
            max="20"
            value={questionCount}
            onChange={(e) => setQuestionCount(Number(e.target.value))}
          />

          <button
            type="button"
            className="file-upload-generate-btn"
            onClick={handleGenerate}
            disabled={generating || !topic.trim()}
          >
            {generating ? '퀴즈 생성 중...' : '이 내용으로 퀴즈 생성'}
          </button>
        </div>
      </section>
    )
  }

  return (
    <section className="file-upload-wrapper">
      {status === 'idle' ? (
        <>
          <div
            className={`file-upload-zone${isDragOver ? ' file-upload-zone--dragover' : ''}`}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onClick={() => inputRef.current?.click()}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && inputRef.current?.click()}
            aria-label="파일 업로드 영역"
          >
            <span className="file-upload-icon">📄</span>
            <p className="file-upload-zone-text">
              PDF, JPG, PNG 파일을 끌어다 놓거나
            </p>
            <span className="file-upload-zone-btn">파일 선택</span>
            <p className="file-upload-zone-hint">최대 10MB</p>
          </div>

          <input
            ref={inputRef}
            type="file"
            accept={ACCEPTED_EXT}
            onChange={handleInputChange}
            className="file-upload-input"
            aria-hidden="true"
          />
        </>
      ) : (
        <div className="file-upload-progress-wrap">
          <p className="file-upload-filename">{fileName}</p>

          <div
            className="file-upload-progress-track"
            role="progressbar"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <div className="file-upload-progress-fill" style={{ width: `${progress}%` }} />
          </div>

          <p className="file-upload-progress-label">
            {status === 'processing' ? '요약 생성 중...' : `업로드 중 ${progress}%`}
          </p>
        </div>
      )}
    </section>
  )
}

export default FileUpload
