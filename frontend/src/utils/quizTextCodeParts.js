/**
 * 문제/선택지 문자열에서 마크다운 펜스 코드와, 펜스 없이 붙은 코드형 덩어리를 구분한다.
 */

const FENCE_RE = /```([\w.-]*)\s*\n?([\s\S]*?)```/g

const LANG_ALIASES = {
  js: 'javascript',
  ts: 'typescript',
  py: 'python',
  sh: 'bash',
  shell: 'bash',
  yml: 'yaml',
  cs: 'csharp',
  cpp: 'cpp',
  'c++': 'cpp',
  cxx: 'cpp',
}

function inferLanguageFromBody(code) {
  const s = (code || '').trim()
  if (!s) return 'plaintext'
  if (/\b(import java|public class|package |interface |@Override)\b/.test(s)) return 'java'
  if (/^\s*(def |class \w+\(|from \w+ import|import \w+)/m.test(s) || /\belif |except |:\s*$/m.test(s))
    return 'python'
  if (/\b(const|let|var|function|=>|async |await )\b/.test(s) || /`[^`]+`/.test(s)) return 'javascript'
  if (/#include\s*[<"]/.test(s) || /\b(int|void|char\*|std::)\b/.test(s)) return 'cpp'
  if (/\bfunc \w+\(|package main\b/.test(s)) return 'go'
  return 'javascript'
}

function normalizeLanguage(declared, body) {
  const d = (declared || '').trim().toLowerCase()
  if (!d || d === 'text' || d === 'txt' || d === 'plain' || d === 'plaintext') {
    return inferLanguageFromBody(body)
  }
  if (LANG_ALIASES[d]) return LANG_ALIASES[d]
  if (/^[a-z][\w-]*$/i.test(d)) return d
  return inferLanguageFromBody(body)
}

function scoreCodeLikeness(chunk) {
  const lines = chunk.split(/\r?\n/).map((l) => l.trim()).filter(Boolean)
  if (lines.length === 0) return 0
  let score = 0
  for (const line of lines) {
    if (
      /^(public|private|protected|class|interface|import|package|return|if|for|while|try|catch|else|new|void|int|boolean|String)\b/.test(
        line,
      )
    )
      score += 2.5
    if (/^(def|class|elif|else|from|import|with|async)\b/.test(line)) score += 2.5
    if (/^(const|let|var|function|async|await|export|default)\b/.test(line)) score += 2.5
    if (/[{}();=<>]|=>|\+\+|--/.test(line)) score += 1
    if (/;$/.test(line)) score += 0.8
  }
  return score / lines.length
}

function looksLikeUnfencedCode(chunk) {
  const t = (chunk || '').trim()
  if (t.length < 12) return false
  const lines = t.split(/\r?\n/).map((l) => l.trim()).filter(Boolean)
  if (lines.length >= 2) {
    return scoreCodeLikeness(t) >= 1.35
  }
  if (lines.length === 1) {
    const line = lines[0]
    return (
      line.length > 36 &&
      /[;{}=<>()[\]]/.test(line) &&
      /(return|if\s*\(|for\s*\(|while\s*\(|def\s|class\s|public\s|import\s|const\s|let\s|var\s|function\s)/.test(line)
    )
  }
  return false
}

function splitPlainIntoTextAndCode(plain) {
  if (!plain) return []
  const chunks = plain.split(/\n{2,}/)
  const out = []
  for (const chunk of chunks) {
    if (chunk === '') continue
    if (looksLikeUnfencedCode(chunk)) {
      out.push({
        type: 'code',
        language: inferLanguageFromBody(chunk),
        content: chunk.trim(),
      })
    } else {
      out.push({ type: 'text', content: chunk })
    }
  }
  return mergeAdjacentTextParts(out)
}

function mergeAdjacentTextParts(parts) {
  const out = []
  for (const p of parts) {
    if (p.type === 'text' && (p.content === '' || p.content == null)) continue
    const prev = out[out.length - 1]
    if (p.type === 'text' && prev?.type === 'text') {
      prev.content += '\n\n' + p.content
    } else {
      out.push({ type: p.type, content: p.content, ...(p.language ? { language: p.language } : {}) })
    }
  }
  return out
}

/**
 * @param {string} source
 * @returns {{ type: 'text' | 'code', content: string, language?: string }[]}
 */
export function parseQuizMixedContent(source) {
  const raw = source == null ? '' : String(source)
  if (!raw) return [{ type: 'text', content: '' }]

  const parts = []
  let last = 0
  FENCE_RE.lastIndex = 0
  let m
  while ((m = FENCE_RE.exec(raw)) !== null) {
    if (m.index > last) {
      parts.push(...splitPlainIntoTextAndCode(raw.slice(last, m.index)))
    }
    const declared = (m[1] || '').trim()
    const body = m[2].replace(/\n?```$/, '').replace(/\s+$/, '')
    parts.push({
      type: 'code',
      language: normalizeLanguage(declared, body),
      content: body,
    })
    last = m.index + m[0].length
  }
  if (last < raw.length) {
    parts.push(...splitPlainIntoTextAndCode(raw.slice(last)))
  }
  if (parts.length === 0) {
    parts.push(...splitPlainIntoTextAndCode(raw))
  }
  return mergeAdjacentTextParts(parts)
}
