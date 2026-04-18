const STORAGE_KEY = 'quiz_history'

export function readQuizHistory() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function appendQuizHistory(entry) {
  const list = readQuizHistory()
  list.push(entry)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
}
