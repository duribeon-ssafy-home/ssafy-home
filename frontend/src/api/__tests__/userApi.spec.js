import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import {
  changeMyPassword,
  getMyProfile,
  updateMyProfile,
  updateMyRole,
  updateMyStatus,
} from '../userApi'

vi.mock('@/api/axios', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
  },
}))

describe('userApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('내 프로필 조회 응답의 data를 반환한다', async () => {
    const profile = { id: 1, email: 'buyer@example.com', role: 'BUYER' }
    api.get.mockResolvedValue({ data: { data: profile } })

    await expect(getMyProfile()).resolves.toBe(profile)

    expect(api.get).toHaveBeenCalledWith('/users/me')
  })

  it('내 프로필 수정 요청을 보낸다', async () => {
    const payload = {
      name: '홍길동',
      nickname: '길동',
      phoneNumber: '01012345678',
    }
    const profile = { id: 1, ...payload }
    api.patch.mockResolvedValue({ data: { data: profile } })

    await expect(updateMyProfile(payload)).resolves.toBe(profile)

    expect(api.patch).toHaveBeenCalledWith('/users/me', payload)
  })

  it('내 계정 상태 변경 요청을 보낸다', async () => {
    const result = { id: 1, status: 'INACTIVE' }
    api.patch.mockResolvedValue({ data: { data: result } })

    await expect(updateMyStatus({ status: 'INACTIVE' })).resolves.toBe(result)

    expect(api.patch).toHaveBeenCalledWith('/users/me/status', { status: 'INACTIVE' })
  })

  it('내 비밀번호 변경 요청을 보낸다', async () => {
    const payload = {
      currentPassword: 'old-password',
      newPassword: 'new-password123',
    }
    api.patch.mockResolvedValue({ data: { data: null } })

    await expect(changeMyPassword(payload)).resolves.toBeNull()

    expect(api.patch).toHaveBeenCalledWith('/users/me/password', payload)
  })

  it('BUYER에서 AGENT로 역할 변경 요청을 보낸다', async () => {
    const payload = { role: 'AGENT', phoneNumber: '01012345678' }
    const result = { id: 1, role: 'AGENT', phoneNumber: '01012345678' }
    api.patch.mockResolvedValue({ data: { data: result } })

    await expect(updateMyRole(payload)).resolves.toBe(result)

    expect(api.patch).toHaveBeenCalledWith('/users/me/role', payload)
  })
})
