import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  getAdminUser,
  getAdminUsers,
  updateAdminUserRole,
  updateAdminUserStatus,
} from '@/api/adminUserApi'
import AdminUsersView from '../AdminUsersView.vue'

vi.mock('@/api/adminUserApi', () => ({
  getAdminUser: vi.fn(),
  getAdminUsers: vi.fn(),
  updateAdminUserRole: vi.fn(),
  updateAdminUserStatus: vi.fn(),
}))

const routerLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a href="#"><slot /></a>',
}

const adminUser = {
  id: 1,
  email: 'admin@example.com',
  name: '관리자',
  nickname: '운영자',
  phoneNumber: '01012345678',
  role: 'ADMIN',
  status: 'ACTIVE',
  provider: 'LOCAL',
  createdAt: '2026-06-10T09:00:00',
}

const buyerUser = {
  ...adminUser,
  id: 2,
  email: 'buyer@example.com',
  name: '구매자',
  nickname: '집찾는하나',
  role: 'BUYER',
  phoneNumber: '',
}

describe('AdminUsersView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAdminUser.mockResolvedValue({ ...adminUser })
    getAdminUsers.mockResolvedValue([{ ...adminUser }])
    updateAdminUserRole.mockResolvedValue({ ...adminUser, role: 'AGENT', phoneNumber: '01098765432' })
    updateAdminUserStatus.mockResolvedValue({ ...adminUser, status: 'BANNED' })
  })

  it('진입 시 관리자 회원 목록을 조회하고 테이블에 표시한다', async () => {
    const wrapper = mountAdminUsersView()
    await flushPromises()

    expect(getAdminUsers).toHaveBeenCalledWith({})
    expect(wrapper.text()).toContain('운영자')
    expect(wrapper.text()).toContain('admin@example.com')
    expect(wrapper.text()).toContain('관리자')
    expect(wrapper.text()).toContain('활성')
    expect(wrapper.text()).toContain('총 1명')
  })

  it('검색어를 입력하면 keyword로 재조회한다', async () => {
    getAdminUsers.mockResolvedValueOnce([]).mockResolvedValueOnce([
      {
        ...adminUser,
        id: 2,
        email: 'agent@example.com',
        name: '중개인',
        nickname: '매물담당',
        role: 'AGENT',
      },
    ])

    const wrapper = mountAdminUsersView()
    await flushPromises()

    await wrapper.find('input[type="search"]').setValue('  agent  ')
    await wrapper.find('form.toolbar-panel').trigger('submit')
    await flushPromises()

    expect(getAdminUsers).toHaveBeenLastCalledWith({ keyword: 'agent' })
    expect(wrapper.text()).toContain('agent 검색 결과')
    expect(wrapper.text()).toContain('매물담당')
    expect(wrapper.text()).toContain('중개인')
  })

  it('조회 실패 시 서버 메시지를 표시한다', async () => {
    getAdminUsers.mockRejectedValue({
      response: { data: { message: '관리자 권한이 필요합니다.' } },
    })

    const wrapper = mountAdminUsersView()
    await flushPromises()

    expect(wrapper.text()).toContain('관리자 권한이 필요합니다.')
    expect(wrapper.text()).toContain('회원 목록 조회에 실패했습니다.')
  })

  it('상세 관리 버튼을 누르면 회원 상세를 조회하고 패널에 표시한다', async () => {
    const wrapper = mountAdminUsersView()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '상세 관리').trigger('click')
    await flushPromises()

    expect(getAdminUser).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('최근 로그인')
    expect(wrapper.text()).toContain('계정 상태')
    expect(wrapper.text()).toContain('역할')
  })

  it('선택 회원의 상태와 역할을 변경한다', async () => {
    getAdminUsers.mockResolvedValue([{ ...buyerUser }])
    getAdminUser.mockResolvedValue({ ...buyerUser })
    updateAdminUserStatus.mockResolvedValue({ ...buyerUser, status: 'BANNED' })
    updateAdminUserRole.mockResolvedValue({ ...buyerUser, role: 'AGENT', phoneNumber: '01098765432' })

    const wrapper = mountAdminUsersView()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '상세 관리').trigger('click')
    await flushPromises()

    const forms = wrapper.findAll('form.action-form')
    await forms[0].find('select').setValue('BANNED')
    await forms[0].trigger('submit')
    await flushPromises()

    expect(updateAdminUserStatus).toHaveBeenCalledWith(2, { status: 'BANNED' })
    expect(wrapper.text()).toContain('회원 상태를 변경했습니다.')

    await forms[1].find('select').setValue('AGENT')
    await flushPromises()

    const roleForm = wrapper.findAll('form.action-form')[1]
    await roleForm.find('input[type="tel"]').setValue('  01098765432  ')
    await roleForm.trigger('submit')
    await flushPromises()

    expect(updateAdminUserRole).toHaveBeenCalledWith(2, {
      role: 'AGENT',
      phoneNumber: '  01098765432  ',
    })
    expect(wrapper.text()).toContain('회원 역할을 변경했습니다.')
  })
})

function mountAdminUsersView() {
  return mount(AdminUsersView, {
    global: {
      stubs: {
        RouterLink: routerLinkStub,
      },
    },
  })
}
