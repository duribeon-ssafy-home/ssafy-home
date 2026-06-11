import api from '@/api/axios'

export async function getAdminReports(params = {}) {
  const response = await api.get('/admin/reports', {
    params: normalizeAdminReportParams(params),
  })

  return response.data.data
}

function normalizeAdminReportParams(params) {
  const status = typeof params.status === 'string' ? params.status.trim() : ''

  return status ? { status } : {}
}
