/** 백엔드·프롬프트와 동일한 구분선 (앞뒤 공백·줄바꿈 허용) */
const DELIM_PATTERN = /\s*---WRONG-ANALYSIS---\s*/

/**
 * @param {string | undefined | null} text
 * @returns {{ correctRationale: string, wrongAnalysis: string }}
 */
export function splitDetailedExplanation(text) {
  const raw = (text ?? '').trim()
  if (!raw) {
    return { correctRationale: '', wrongAnalysis: '' }
  }
  const match = raw.match(DELIM_PATTERN)
  if (!match || match.index === undefined) {
    return { correctRationale: raw, wrongAnalysis: '' }
  }
  const head = raw.slice(0, match.index).trim()
  const tail = raw.slice(match.index + match[0].length).trim()
  return { correctRationale: head, wrongAnalysis: tail }
}
