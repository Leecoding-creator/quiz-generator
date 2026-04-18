import { useMemo } from 'react'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism'
import { parseQuizMixedContent } from '../utils/quizTextCodeParts'

const CODE_CUSTOM_STYLE = {
  margin: 0,
  borderRadius: '8px',
  padding: '14px 16px',
  fontSize: '0.875rem',
  lineHeight: 1.55,
}

function safeLanguage(lang) {
  if (!lang || typeof lang !== 'string') return 'javascript'
  const l = lang.trim().toLowerCase()
  if (l === 'none' || l === 'text') return 'javascript'
  return l
}

export default function QuizRichText({ text, className = '' }) {
  const parts = useMemo(() => parseQuizMixedContent(text ?? ''), [text])

  return (
    <div className={`quiz-rich-text ${className}`.trim()}>
      {parts.map((part, i) => {
        if (part.type === 'code') {
          return (
            <div key={i} className="quiz-code-block">
              <SyntaxHighlighter
                language={safeLanguage(part.language)}
                style={vscDarkPlus}
                PreTag="div"
                customStyle={CODE_CUSTOM_STYLE}
                showLineNumbers={false}
              >
                {part.content}
              </SyntaxHighlighter>
            </div>
          )
        }
        return (
          <span key={i} className="quiz-rich-plain">
            {part.content}
          </span>
        )
      })}
    </div>
  )
}
