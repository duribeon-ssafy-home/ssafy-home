import api from './axios'

export async function chatWithAi({ question, propertyIds = [] }) {
  const response = await api.post('/ai/chat', { question, propertyIds })
  return response.data.data.answer
}
