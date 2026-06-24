import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '@/api/axios'
import { requestTemporaryPassword } from '../authApi'

vi.mock('@/api/axios', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('임시 비밀번호 발급 요청을 보낸다', async () => {
    const payload = { email: 'buyer@example.com' }
    api.post.mockResolvedValue({ data: { data: null } })

    await expect(requestTemporaryPassword(payload)).resolves.toBeNull()

    expect(api.post).toHaveBeenCalledWith('/auth/password/forgot', payload)
  })
})
