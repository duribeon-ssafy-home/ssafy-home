import api from '@/api/axios'

export async function getAdminReports(params = {}) {
  const response = await api.get('/admin/reports', {
    params: normalizeAdminReportParams(params),
  })

  return response.data.data
}

export async function getAdminReport(reportId) {
  const response = await api.get(`/admin/reports/${reportId}`)

  return response.data.data
}

export async function updateAdminReport(reportId, payload) {
  const response = await api.patch(`/admin/reports/${reportId}`, payload)

  return response.data.data
}

function normalizeAdminReportParams(params) {
  const status = typeof params.status === 'string' ? params.status.trim() : ''

  return status ? { status } : {}
}
