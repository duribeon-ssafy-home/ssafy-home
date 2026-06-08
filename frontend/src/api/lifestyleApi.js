import api from '@/api/axios'

export async function getLifestyleQuestions() {
  const response = await api.get('/lifestyle/questions')
  return response.data.data
}
