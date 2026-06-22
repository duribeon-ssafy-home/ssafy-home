import api from '@/api/axios'

export async function compareWithAi({ propertyIds, question }) {
  const response = await api.post('/ai/chat', {
    message: question,
    propertyIds,
  })

  return response.data.data.compare ?? response.data.data
}
