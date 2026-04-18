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

export async function uploadFile(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await axios.post(`${BASE}/upload`, formData, {
    onUploadProgress: (e) => {
      if (e.total) onProgress(Math.round((e.loaded / e.total) * 100))
    },
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
