import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import {
  getAdminUser,
  getAdminUsers,
  updateAdminUserRole,
  updateAdminUserStatus,
} from '../adminUserApi'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
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

  it('관리자 회원 상세 조회 응답의 data를 반환한다', async () => {
    const user = { id: 3, email: 'buyer@example.com', role: 'BUYER' }
    api.get.mockResolvedValue({ data: { data: user } })

    await expect(getAdminUser(3)).resolves.toBe(user)

    expect(api.get).toHaveBeenCalledWith('/admin/users/3')
  })

  it('관리자 회원 상태 변경 응답의 data를 반환한다', async () => {
    const user = { id: 3, status: 'BANNED' }
    api.patch.mockResolvedValue({ data: { data: user } })

    await expect(updateAdminUserStatus(3, { status: 'BANNED' })).resolves.toBe(user)

    expect(api.patch).toHaveBeenCalledWith('/admin/users/3/status', { status: 'BANNED' })
  })

  it('관리자 회원 역할 변경 시 전화번호를 trim해서 전달한다', async () => {
    const user = { id: 3, role: 'AGENT', phoneNumber: '01012345678' }
    api.patch.mockResolvedValue({ data: { data: user } })

    await expect(
      updateAdminUserRole(3, { role: 'AGENT', phoneNumber: '  01012345678  ' }),
    ).resolves.toBe(user)

    expect(api.patch).toHaveBeenCalledWith('/admin/users/3/role', {
      role: 'AGENT',
      phoneNumber: '01012345678',
    })
  })
})
