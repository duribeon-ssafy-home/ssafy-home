import api from '@/api/axios'

export async function getLifestyleQuestions() {
  const response = await api.get('/lifestyle/questions')
  return response.data.data
}

export async function previewLifestyleResult(payload) {
  const response = await api.post('/lifestyle/results/preview', payload)
  return response.data.data
}

export async function saveLifestyleResult(payload) {
  const response = await api.post('/lifestyle/results', payload)
  return response.data.data
}

export async function getMyLatestLifestyleResult() {
  const response = await api.get('/lifestyle/results/me')
  return response.data.data
}
