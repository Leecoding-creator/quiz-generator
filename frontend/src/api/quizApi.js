import axios from 'axios'

const BASE = 'http://localhost:8080/api/quiz'

export async function generateQuiz(topic, difficulty, questionCount, content = '') {
  const { data } = await axios.post(`${BASE}/generate`, {
    topic,
    difficulty,
    questionCount,
    content,
  })
  return data
}

export async function getHistory() {
  const { data } = await axios.get(`${BASE}/history`)
  return data
}

export async function getHistoryDetail(sessionId) {
  const { data } = await axios.get(`${BASE}/history/${sessionId}`)
  return data
}
