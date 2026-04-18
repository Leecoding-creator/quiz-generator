import { useEffect, useRef, useState } from 'react'
import Editor from '@monaco-editor/react'
import { fetchRandomProblem, submitCode } from '../api/quizApi'
import './CodeEditor.css'

const LANGUAGES = [
  { label: 'Java', value: 'java', id: 62 },
  { label: 'Python', value: 'python', id: 71 },
  { label: 'C', id: 50, value: 'c' },
]

const STARTER = {
  java: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // 여기에 코드를 작성하세요

    }
}`,
  python: `import sys
input = sys.stdin.readline

# 여기에 코드를 작성하세요
`,
  c: `#include <stdio.h>

int main() {
    // 여기에 코드를 작성하세요

    return 0;
}`,
}

function StatusBadge({ status }) {
  const map = {
    Accepted: 'badge--accepted',
    'Wrong Answer': 'badge--wrong',
    'Time Limit Exceeded': 'badge--tle',
    'Compilation Error': 'badge--error',
  }
  const cls = map[status] ?? 'badge--error'
  return <span className={`result-badge ${cls}`}>{status}</span>
}

function CodeEditor({ onToast }) {
  const [problem, setProblem] = useState(null)
  const [loadingProblem, setLoadingProblem] = useState(true)
  const [lang, setLang] = useState(LANGUAGES[0])
  const [code, setCode] = useState(STARTER.java)
  const [result, setResult] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const prevLangRef = useRef('java')

  const loadProblem = async () => {
    setLoadingProblem(true)
    setResult(null)
    try {
      const p = await fetchRandomProblem()
      setProblem(p)
    } catch {
      onToast?.('문제를 불러오지 못했습니다.', 'error')
    } finally {
      setLoadingProblem(false)
    }
  }

  useEffect(() => { loadProblem() }, [])

  const handleLangChange = (e) => {
    const selected = LANGUAGES.find((l) => l.value === e.target.value)
    if (!selected) return
    const prevStarter = STARTER[prevLangRef.current]
    if (code === prevStarter || code.trim() === '') {
      setCode(STARTER[selected.value])
    }
    prevLangRef.current = selected.value
    setLang(selected)
    setResult(null)
  }

  const handleSubmit = async () => {
    if (!problem) return
    setSubmitting(true)
    setResult(null)
    try {
      const res = await submitCode(problem.id, code, lang.id)
      setResult(res)
    } catch (err) {
      onToast?.(err.response?.data?.message ?? '제출 중 오류가 발생했습니다.', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  if (loadingProblem) {
    return <div className="code-editor-loading">문제 불러오는 중...</div>
  }

  if (!problem) {
    return (
      <div className="code-editor-loading">
        <button type="button" onClick={loadProblem}>다시 시도</button>
      </div>
    )
  }

  return (
    <div className="code-editor-root">
      {/* ── 문제 패널 ── */}
      <aside className="problem-panel">
        <div className="problem-panel-header">
          <div className="problem-meta">
            <span className={`problem-difficulty difficulty--${problem.difficulty?.toLowerCase()}`}>
              {problem.difficulty}
            </span>
            <span className="problem-category">{problem.category}</span>
          </div>
          <button type="button" className="problem-refresh-btn" onClick={loadProblem}>
            다른 문제
          </button>
        </div>

        <h2 className="problem-title">{problem.title}</h2>
        <p className="problem-desc">{problem.description}</p>

        {problem.inputDescription && (
          <>
            <h3 className="problem-section-title">입력</h3>
            <p className="problem-section-body">{problem.inputDescription}</p>
          </>
        )}

        {problem.outputDescription && (
          <>
            <h3 className="problem-section-title">출력</h3>
            <p className="problem-section-body">{problem.outputDescription}</p>
          </>
        )}

        <div className="problem-io-grid">
          <div className="problem-io-box">
            <p className="problem-io-label">입력 예시</p>
            <pre className="problem-io-pre">{problem.sampleInput}</pre>
          </div>
          <div className="problem-io-box">
            <p className="problem-io-label">출력 예시</p>
            <pre className="problem-io-pre">{problem.sampleOutput}</pre>
          </div>
        </div>

        <div className="problem-limits">
          <span>시간 제한 {problem.timeLimit}ms</span>
          <span>메모리 {problem.memoryLimit}MB</span>
        </div>
      </aside>

      {/* ── 에디터 패널 ── */}
      <main className="editor-panel">
        <div className="editor-toolbar">
          <select
            className="lang-select"
            value={lang.value}
            onChange={handleLangChange}
          >
            {LANGUAGES.map((l) => (
              <option key={l.value} value={l.value}>{l.label}</option>
            ))}
          </select>

          <button
            type="button"
            className="submit-btn"
            onClick={handleSubmit}
            disabled={submitting}
          >
            {submitting ? '채점 중...' : '제출'}
          </button>
        </div>

        <div className="monaco-wrapper">
          <Editor
            height="100%"
            language={lang.value}
            value={code}
            onChange={(val) => setCode(val ?? '')}
            theme="vs-dark"
            options={{
              fontSize: 14,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              tabSize: 4,
              wordWrap: 'on',
            }}
          />
        </div>

        {/* ── 결과 ── */}
        {result && (
          <div className={`result-panel${result.correct ? ' result-panel--correct' : ' result-panel--wrong'}`}>
            <div className="result-row">
              <StatusBadge status={result.status} />
              {result.correct && <span className="result-correct-msg">정답입니다!</span>}
              {result.time && (
                <span className="result-meta">{result.time}s · {result.memory}KB</span>
              )}
            </div>

            {!result.correct && result.stdout && (
              <div className="result-block">
                <p className="result-block-label">실제 출력</p>
                <pre className="result-block-pre">{result.stdout.trim()}</pre>
              </div>
            )}

            {result.stderr && (
              <div className="result-block result-block--error">
                <p className="result-block-label">오류 메시지</p>
                <pre className="result-block-pre">{result.stderr.trim()}</pre>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  )
}

export default CodeEditor
