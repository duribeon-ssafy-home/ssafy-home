import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import { getAdminUsers } from '../adminUserApi'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('adminUserApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('관리자 회원 목록 조회 응답의 data를 반환한다', async () => {
    const users = [{ id: 1, email: 'admin@example.com', role: 'ADMIN' }]
    api.get.mockResolvedValue({ data: { data: users } })

    await expect(getAdminUsers()).resolves.toBe(users)

    expect(api.get).toHaveBeenCalledWith('/admin/users', { params: {} })
  })

  it('검색 keyword를 trim해서 전달한다', async () => {
    const users = [{ id: 2, email: 'agent@example.com', role: 'AGENT' }]
    api.get.mockResolvedValue({ data: { data: users } })

    await expect(getAdminUsers({ keyword: '  agent  ' })).resolves.toBe(users)

    expect(api.get).toHaveBeenCalledWith('/admin/users', { params: { keyword: 'agent' } })
  })
})
