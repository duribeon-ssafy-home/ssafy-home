<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getMyLatestLifestyleResult } from '@/api/lifestyleApi'
import { changeMyPassword, getMyProfile, updateMyProfile, updateMyStatus } from '@/api/userApi'
import PasswordField from '@/components/PasswordField.vue'
import { createLifestylePresetChips, lifestyleTypeMeta } from '@/data/lifestyle'
import { useAuthStore } from '@/stores/auth'
import balancedCharacter from '@/assets/images/lifestyle/balanced.png'
import carefulCharacter from '@/assets/images/lifestyle/careful.png'
import cozyCharacter from '@/assets/images/lifestyle/cozy.png'
import flexibleCharacter from '@/assets/images/lifestyle/flexible.png'
import practicalCharacter from '@/assets/images/lifestyle/practical.png'
import savingCharacter from '@/assets/images/lifestyle/saving.png'
import spaciousCharacter from '@/assets/images/lifestyle/spacious.png'
import thriftyCharacter from '@/assets/images/lifestyle/thrifty.png'

const router = useRouter()
const authStore = useAuthStore()

const profile = ref(null)
const lifestyleResult = ref(null)
const isLoading = ref(true)
const isEditingProfile = ref(false)
const isSavingProfile = ref(false)
const isChangingPassword = ref(false)
const isChangingStatus = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const passwordErrorMessage = ref('')
const passwordSuccessMessage = ref('')

const profileForm = reactive({
  name: '',
  nickname: '',
  phoneNumber: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const roleLabels = {
  BUYER: '일반 사용자',
  AGENT: '중개인',
  ADMIN: '관리자',
}

const statusLabels = {
  ACTIVE: '활성',
  INACTIVE: '비활성',
  BANNED: '정지',
  DELETED: '탈퇴',
}

const statusToneLabels = {
  ACTIVE: 'status-badge--active',
  INACTIVE: 'status-badge--inactive',
  BANNED: 'status-badge--danger',
  DELETED: 'status-badge--danger',
}
const lifestyleCharacters = {
  LIVING_COST_HOME_BALANCED: balancedCharacter,
  LIVING_COST_COMPACT: thriftyCharacter,
  LIVING_FLEXIBLE_HOME: cozyCharacter,
  LIVING_FLEXIBLE_COMPACT: practicalCharacter,
  LOCATION_FLEXIBLE_COST_HOME: carefulCharacter,
  LOCATION_FLEXIBLE_COST_COMPACT: savingCharacter,
  LOCATION_FLEXIBLE_HOME: spaciousCharacter,
  LOCATION_FLEXIBLE_COMPACT: flexibleCharacter,
}

const displayProfile = computed(() => profile.value || authStore.user || {})
const roleLabel = computed(
  () => roleLabels[displayProfile.value.role] || displayProfile.value.role || '-',
)
const statusLabel = computed(
  () => statusLabels[displayProfile.value.status] || displayProfile.value.status || '-',
)
const statusTone = computed(() => statusToneLabels[displayProfile.value.status] || '')
const isAgent = computed(() => displayProfile.value.role === 'AGENT')
const isAdmin = computed(() => displayProfile.value.role === 'ADMIN')
const profileInitial = computed(() => {
  const source = displayProfile.value.nickname || displayProfile.value.name || displayProfile.value.email || 'U'
  return source.slice(0, 1).toUpperCase()
})
const profileCompletion = computed(() => {
  const checks = [
    displayProfile.value.name,
    displayProfile.value.nickname,
    displayProfile.value.email,
    displayProfile.value.phoneNumber,
  ]
  const filledCount = checks.filter(Boolean).length

  return Math.round((filledCount / checks.length) * 100)
})
const lifestyleMeta = computed(() =>
  lifestyleResult.value?.lifestyleType
    ? lifestyleTypeMeta[lifestyleResult.value.lifestyleType]
    : null,
)
const presetChips = computed(() =>
  createLifestylePresetChips(lifestyleResult.value?.filterPreset, lifestyleMeta.value?.chips || []),
)
const lifestyleCharacterImage = computed(
  () => lifestyleCharacters[lifestyleResult.value?.lifestyleType] || '',
)
const isNewPasswordLengthValid = computed(() => passwordForm.newPassword.length >= 8)
const passwordRuleClass = computed(() => ({
  'password-rule--valid': isNewPasswordLengthValid.value,
  'password-rule--invalid': passwordForm.newPassword.length > 0 && !isNewPasswordLengthValid.value,
}))

onMounted(loadMyPage)

async function loadMyPage() {
  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  const [profileResult, lifestyleResultResponse] = await Promise.allSettled([
    getMyProfile(),
    getMyLatestLifestyleResult(),
  ])

  if (profileResult.status === 'fulfilled') {
    syncProfile(profileResult.value)
    resetProfileForm()
  } else {
    errorMessage.value =
      profileResult.reason?.response?.data?.message || '내 정보를 불러오지 못했습니다.'
  }

  if (lifestyleResultResponse.status === 'fulfilled') {
    lifestyleResult.value = lifestyleResultResponse.value
  } else if (
    lifestyleResultResponse.reason?.response?.data?.errorCode !== 'LIFESTYLE_RESULT_NOT_FOUND'
  ) {
    errorMessage.value =
      lifestyleResultResponse.reason?.response?.data?.message ||
      errorMessage.value ||
      '저장된 선호 유형을 불러오지 못했습니다.'
  }

  isLoading.value = false
}

function startProfileEdit() {
  resetProfileForm()
  isEditingProfile.value = true
  clearFeedback()
}

function cancelProfileEdit() {
  resetProfileForm()
  isEditingProfile.value = false
  clearFeedback()
}

async function submitProfileUpdate() {
  clearFeedback()

  if (!profileForm.name.trim() || !profileForm.nickname.trim()) {
    errorMessage.value = '이름과 닉네임을 입력해주세요.'
    return
  }

  isSavingProfile.value = true

  try {
    const updatedProfile = await updateMyProfile({
      name: profileForm.name.trim(),
      nickname: profileForm.nickname.trim(),
      phoneNumber: normalizeOptionalValue(profileForm.phoneNumber),
    })

    syncProfile(updatedProfile)
    resetProfileForm()
    isEditingProfile.value = false
    successMessage.value = '프로필 정보가 저장되었습니다.'
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '프로필 정보를 저장하지 못했습니다.')
  } finally {
    isSavingProfile.value = false
  }
}

async function submitPasswordChange() {
  clearPasswordFeedback()

  if (!isNewPasswordLengthValid.value) {
    passwordErrorMessage.value = '새 비밀번호는 8자 이상 입력해주세요.'
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordErrorMessage.value = '새 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  isChangingPassword.value = true

  try {
    await changeMyPassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    resetPasswordForm()
    passwordSuccessMessage.value = '비밀번호가 변경되었습니다. 다음 로그인부터 새 비밀번호를 사용해 주세요.'
  } catch (error) {
    passwordErrorMessage.value = getApiErrorMessage(error, '비밀번호를 변경하지 못했습니다.')
  } finally {
    isChangingPassword.value = false
  }
}

async function requestAccountStatusChange(status) {
  const confirmMessage =
    status === 'DELETED'
      ? '탈퇴 처리 후에는 현재 세션이 종료됩니다. 계정을 탈퇴하시겠습니까?'
      : '계정을 비활성화하면 현재 세션이 종료됩니다. 계속하시겠습니까?'

  if (!window.confirm(confirmMessage)) {
    return
  }

  clearFeedback()
  isChangingStatus.value = true

  try {
    await updateMyStatus({ status })
    await authStore.logout({ revoke: false })
    await router.push({
      name: 'login',
      query: { reason: status === 'DELETED' ? 'deleted' : 'inactive' },
    })
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '계정 상태를 변경하지 못했습니다.')
  } finally {
    isChangingStatus.value = false
  }
}

function resetProfileForm() {
  profileForm.name = displayProfile.value.name || ''
  profileForm.nickname = displayProfile.value.nickname || ''
  profileForm.phoneNumber = displayProfile.value.phoneNumber || ''
}

function resetPasswordForm() {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

function syncProfile(nextProfile) {
  profile.value = nextProfile
  authStore.setUser({
    ...authStore.user,
    ...nextProfile,
  })
}

function clearFeedback() {
  errorMessage.value = ''
  successMessage.value = ''
}

function clearPasswordFeedback() {
  passwordErrorMessage.value = ''
  passwordSuccessMessage.value = ''
}

function normalizeOptionalValue(value) {
  const trimmedValue = value.trim()
  return trimmedValue ? trimmedValue : null
}

function getApiErrorMessage(error, fallbackMessage) {
  return error.response?.data?.message || fallbackMessage
}
</script>

<template>
  <main class="page my-page">
    <section class="section-container my-page__inner">
      <div class="page-heading">
        <div>
          <p class="eyebrow">My Page</p>
          <h1>내 주거 프로필</h1>
          <p>계정 정보와 권한, 저장된 선호 유형을 한 곳에서 관리하세요.</p>
        </div>
      </div>

      <div v-if="isLoading" class="loading-panel">
        <span></span>
        <p>마이페이지 정보를 불러오는 중입니다.</p>
      </div>

      <p v-else-if="errorMessage && !displayProfile.id" class="form-message form-message--error" role="alert">
        {{ errorMessage }}
      </p>

      <div v-else class="my-grid">
        <aside class="account-sidebar" aria-label="계정 요약">
          <div class="profile-summary">
            <span class="profile-avatar" aria-hidden="true">{{ profileInitial }}</span>
            <div>
              <strong>{{ displayProfile.nickname || displayProfile.name || '사용자' }}</strong>
              <p>{{ displayProfile.email }}</p>
            </div>
          </div>

          <div class="badge-row">
            <span class="role-badge">{{ roleLabel }}</span>
            <span class="status-badge" :class="statusTone">{{ statusLabel }}</span>
          </div>

          <div class="completion-box">
            <div>
              <span>프로필 완성도</span>
              <strong>{{ profileCompletion }}%</strong>
            </div>
            <meter min="0" max="100" :value="profileCompletion">{{ profileCompletion }}%</meter>
          </div>

          <div class="quick-actions">
            <RouterLink
              v-if="isAgent"
              :to="{ name: 'agent-properties' }"
              data-testid="agent-entry-button"
            >
              내 매물 관리
            </RouterLink>
            <RouterLink
              v-if="isAdmin"
              :to="{ name: 'admin-dashboard' }"
              data-testid="admin-entry-button"
            >
              관리자 페이지
            </RouterLink>
            <RouterLink :to="{ name: 'favorites' }">찜 목록 보기</RouterLink>
          </div>
        </aside>

        <div class="content-stack">
          <p v-if="successMessage" class="form-message form-message--success" role="status">
            {{ successMessage }}
          </p>
          <p v-if="errorMessage" class="form-message form-message--error" role="alert">
            {{ errorMessage }}
          </p>
          <section class="profile-panel">
            <div class="panel-heading panel-heading--inline">
              <div>
                <p class="eyebrow">Account</p>
                <h2>기본 정보</h2>
              </div>
              <button
                v-if="!isEditingProfile"
                class="secondary-button"
                type="button"
                data-testid="edit-profile-button"
                @click="startProfileEdit"
              >
                프로필 수정
              </button>
            </div>

            <form v-if="isEditingProfile" class="profile-form" @submit.prevent="submitProfileUpdate">
              <label>
                이름
                <input
                  v-model="profileForm.name"
                  data-testid="profile-name-input"
                  required
                  type="text"
                  autocomplete="name"
                />
              </label>
              <label>
                닉네임
                <input
                  v-model="profileForm.nickname"
                  data-testid="profile-nickname-input"
                  required
                  type="text"
                />
              </label>
              <label>
                전화번호
                <input
                  v-model="profileForm.phoneNumber"
                  data-testid="profile-phone-input"
                  type="tel"
                  autocomplete="tel"
                  placeholder="010-0000-0000"
                />
              </label>

              <div class="form-actions">
                <button class="primary-button" type="submit" :disabled="isSavingProfile">
                  {{ isSavingProfile ? '저장 중...' : '저장' }}
                </button>
                <button class="secondary-button" type="button" @click="cancelProfileEdit">
                  취소
                </button>
              </div>
            </form>

            <dl v-else class="profile-list">
              <div>
                <dt>이름</dt>
                <dd>{{ displayProfile.name || '-' }}</dd>
              </div>
              <div>
                <dt>닉네임</dt>
                <dd>{{ displayProfile.nickname || '-' }}</dd>
              </div>
              <div>
                <dt>이메일</dt>
                <dd>{{ displayProfile.email || '-' }}</dd>
              </div>
              <div>
                <dt>전화번호</dt>
                <dd>{{ displayProfile.phoneNumber || '-' }}</dd>
              </div>
              <div>
                <dt>역할</dt>
                <dd>{{ roleLabel }}</dd>
              </div>
              <div>
                <dt>계정 상태</dt>
                <dd>{{ statusLabel }}</dd>
              </div>
            </dl>
          </section>

          <section class="preference-panel">
            <div class="panel-heading">
              <p class="eyebrow">자취TI</p>
              <h2>저장된 자취TI 유형</h2>
            </div>

            <div v-if="lifestyleResult && lifestyleMeta" class="preference-summary">
              <div class="preference-summary__content">
                <span>나의 유형</span>
                <h3>{{ lifestyleMeta.typeName }}</h3>
                <p>{{ lifestyleMeta.headline }}</p>

                <div class="chip-list">
                  <span v-for="chip in presetChips" :key="chip">{{ chip }}</span>
                </div>

                <RouterLink class="secondary-link" :to="{ name: 'survey' }">자취TI 다시 하기</RouterLink>
              </div>

              <img
                v-if="lifestyleCharacterImage"
                class="preference-character"
                :src="lifestyleCharacterImage"
                :alt="`${lifestyleMeta.typeName} 자취TI 이미지`"
              />
            </div>

            <div v-else class="empty-preference">
              <h3>아직 저장된 자취TI 결과가 없습니다</h3>
              <p>자취 성향 테스트를 완료하고 결과를 저장하면 여기에서 다시 확인할 수 있습니다.</p>
              <RouterLink class="primary-link" :to="{ name: 'survey' }">자취TI 시작하기</RouterLink>
            </div>
          </section>

          <section class="security-panel">
            <div class="panel-heading">
              <p class="eyebrow">Security</p>
              <h2>비밀번호 변경</h2>
            </div>

            <p
              v-if="passwordSuccessMessage"
              class="form-message form-message--success"
              role="status"
            >
              {{ passwordSuccessMessage }}
            </p>
            <p
              v-if="passwordErrorMessage"
              class="form-message form-message--error"
              role="alert"
            >
              {{ passwordErrorMessage }}
            </p>

            <form class="password-form" @submit.prevent="submitPasswordChange">
              <PasswordField
                id="current-password"
                v-model="passwordForm.currentPassword"
                data-testid="current-password-input"
                label="현재 비밀번호"
                required
              />
              <PasswordField
                id="new-password"
                v-model="passwordForm.newPassword"
                data-testid="new-password-input"
                autocomplete="new-password"
                label="새 비밀번호"
                placeholder="새 비밀번호"
                required
              />
              <p class="password-rule" :class="passwordRuleClass" data-testid="password-rule">
                <span aria-hidden="true">{{ isNewPasswordLengthValid ? '✓' : '!' }}</span>
                새 비밀번호는 8자 이상이어야 합니다.
              </p>
              <PasswordField
                id="confirm-password"
                v-model="passwordForm.confirmPassword"
                data-testid="confirm-password-input"
                autocomplete="new-password"
                label="새 비밀번호 확인"
                placeholder="새 비밀번호 다시 입력"
                required
              />

              <div class="form-actions">
                <button class="primary-button" type="submit" :disabled="isChangingPassword">
                  {{ isChangingPassword ? '변경 중...' : '비밀번호 변경' }}
                </button>
              </div>
            </form>
          </section>

          <section v-if="!isAdmin" class="danger-panel">
            <div>
              <p class="eyebrow">Account Status</p>
              <h2>계정 상태 변경</h2>
              <p>비활성화 또는 탈퇴 후에는 현재 로그인 세션이 종료됩니다.</p>
            </div>
            <div class="danger-actions">
              <button
                class="secondary-button"
                type="button"
                data-testid="inactive-account-button"
                :disabled="isChangingStatus"
                @click="requestAccountStatusChange('INACTIVE')"
              >
                계정 비활성화
              </button>
              <button
                class="danger-button"
                type="button"
                data-testid="delete-account-button"
                :disabled="isChangingStatus"
                @click="requestAccountStatusChange('DELETED')"
              >
                계정 탈퇴
              </button>
            </div>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.my-page {
  padding: 56px 0 76px;
  background:
    linear-gradient(135deg, rgba(238, 244, 255, 0.9) 0%, rgba(246, 247, 249, 0.58) 52%),
    var(--color-bg);
}

.my-page__inner {
  display: grid;
  gap: 24px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;

  h1 {
    margin-top: 8px;
    color: var(--color-heading);
    font-size: 42px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.18;
  }

  p:not(.eyebrow) {
    margin-top: 10px;
    color: var(--color-muted);
    font-weight: 700;
  }
}

.loading-panel,
.account-sidebar,
.profile-panel,
.preference-panel,
.security-panel,
.danger-panel {
  border: 1px solid rgba(208, 213, 221, 0.9);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-panel);
}

.loading-panel {
  min-height: 360px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 16px;
  color: var(--color-muted);
  font-weight: 800;

  span {
    width: 42px;
    height: 42px;
    border: 4px solid var(--color-primary-soft);
    border-top-color: var(--color-primary);
    border-radius: 50%;
    animation: spin 800ms linear infinite;
  }
}

.my-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.36fr) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.account-sidebar {
  position: sticky;
  top: calc(var(--header-height) + 18px);
  display: grid;
  gap: 20px;
  padding: 24px;
}

.profile-summary {
  display: flex;
  gap: 14px;
  align-items: center;
  min-width: 0;

  strong {
    display: block;
    color: var(--color-heading);
    font-size: 20px;
    font-weight: 900;
    overflow-wrap: anywhere;
  }

  p {
    margin-top: 4px;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 700;
    overflow-wrap: anywhere;
  }
}

.profile-avatar {
  width: 54px;
  height: 54px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 24px;
  font-weight: 900;
}

.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-badge,
.status-badge {
  border-radius: var(--radius-xs);
  font-size: 12px;
  font-weight: 900;
  padding: 7px 9px;
}

.role-badge {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.status-badge {
  background: var(--color-surface-muted);
  color: var(--color-muted);
}

.status-badge--active {
  background: #ecfdf3;
  color: #027a48;
}

.status-badge--inactive {
  background: var(--color-accent-soft);
  color: #7a5f31;
}

.status-badge--danger {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.completion-box {
  display: grid;
  gap: 10px;

  div {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 800;
  }

  strong {
    color: var(--color-primary-dark);
  }

  meter {
    width: 100%;
    height: 8px;
  }
}

.quick-actions {
  display: grid;
  gap: 10px;

  button,
  a {
    min-height: 42px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 900;
    padding: 0 14px;
    transition:
      border-color var(--transition-fast),
      box-shadow var(--transition-fast),
      transform var(--transition-fast);
  }

  button:hover,
  a:hover {
    border-color: var(--color-primary);
    box-shadow: 0 10px 24px rgba(54, 95, 145, 0.12);
    transform: translateY(-1px);
  }
}

.content-stack {
  display: grid;
  gap: 18px;
}

.profile-panel,
.preference-panel,
.security-panel,
.danger-panel {
  padding: 28px;
}

.panel-heading {
  display: grid;
  gap: 6px;
  margin-bottom: 22px;

  h2 {
    color: var(--color-heading);
    font-size: 24px;
    font-weight: 900;
  }
}

.panel-heading--inline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.profile-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;

  div {
    min-width: 0;
    border: 1px solid rgba(229, 231, 235, 0.86);
    border-radius: var(--radius-sm);
    background: var(--color-surface-muted);
    padding: 14px;
  }

  dt {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 900;
  }

  dd {
    min-width: 0;
    margin-top: 6px;
    color: var(--color-heading);
    font-weight: 900;
    overflow-wrap: anywhere;
  }
}

.profile-form,
.password-form {
  display: grid;
  gap: 14px;

  label {
    display: grid;
    gap: 8px;
    color: var(--color-muted);
    font-size: 13px;
    font-weight: 900;
  }

  input {
    width: 100%;
    height: 46px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    color: var(--color-heading);
    padding: 0 13px;
    outline: none;
    transition:
      border-color var(--transition-fast),
      box-shadow var(--transition-fast);

    &:focus {
      border-color: var(--color-primary);
      box-shadow: 0 0 0 4px rgba(54, 95, 145, 0.12);
    }
  }
}

.security-panel .form-message {
  margin-bottom: 16px;
}

.password-rule {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: -6px;
  color: var(--color-muted);
  font-size: 12px;
  font-weight: 900;

  span {
    width: 18px;
    height: 18px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--color-surface-muted);
    color: var(--color-muted);
    font-size: 12px;
    line-height: 1;
  }
}

.password-rule--valid {
  color: #027a48;

  span {
    background: #ecfdf3;
    color: #027a48;
  }
}

.password-rule--invalid {
  color: var(--color-danger);

  span {
    background: var(--color-danger-soft);
    color: var(--color-danger);
  }
}

.form-actions,
.danger-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.primary-button,
.secondary-button,
.danger-button,
.primary-link,
.secondary-link {
  width: fit-content;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 900;
  padding: 0 16px;
  white-space: nowrap;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.primary-button,
.primary-link {
  background: var(--color-primary);
  color: var(--color-surface);

  &:hover:not(:disabled) {
    background: var(--color-primary-dark);
    box-shadow: 0 12px 24px rgba(54, 95, 145, 0.2);
    transform: translateY(-1px);
  }

  &:disabled {
    background: var(--color-subtle);
  }
}

.secondary-button,
.secondary-link {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-heading);

  &:hover:not(:disabled) {
    border-color: var(--color-primary);
    transform: translateY(-1px);
  }
}

.danger-button {
  background: var(--color-danger);
  color: var(--color-surface);

  &:hover:not(:disabled) {
    box-shadow: 0 12px 24px rgba(180, 35, 24, 0.18);
    transform: translateY(-1px);
  }

  &:disabled {
    background: var(--color-subtle);
  }
}

.preference-summary,
.empty-preference {
  display: grid;
  gap: 16px;
}

.preference-summary {
  grid-template-columns: minmax(0, 1fr) minmax(150px, 210px);
  align-items: center;

  span {
    color: var(--color-muted);
    font-size: 14px;
    font-weight: 800;
  }

  h3 {
    color: var(--color-heading);
    font-size: 34px;
    font-weight: 900;
    letter-spacing: 0;
    line-height: 1.2;
  }

  p {
    color: var(--color-muted);
    font-weight: 700;
    line-height: 1.7;
  }
}

.preference-summary__content {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.preference-character {
  width: min(210px, 100%);
  aspect-ratio: 1;
  justify-self: end;
  object-fit: contain;
  filter: drop-shadow(0 18px 24px rgba(54, 95, 145, 0.16));
}

.empty-preference {
  align-content: center;
  min-height: 220px;

  h3 {
    color: var(--color-heading);
    font-size: 26px;
    font-weight: 900;
  }

  p {
    color: var(--color-muted);
    font-weight: 700;
    line-height: 1.7;
  }
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;

  span {
    border: 1px solid rgba(54, 95, 145, 0.2);
    border-radius: var(--radius-sm);
    background: var(--color-primary-soft);
    color: var(--color-primary-dark);
    font-size: 13px;
    font-weight: 900;
    padding: 9px 11px;
  }
}

.danger-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-color: rgba(180, 35, 24, 0.18);

  h2 {
    margin-top: 6px;
    color: var(--color-heading);
    font-size: 22px;
    font-weight: 900;
  }

  p:not(.eyebrow) {
    margin-top: 8px;
    color: var(--color-muted);
    font-weight: 700;
  }
}

.form-message {
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 800;
  padding: 11px 12px;
}

.form-message--success {
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.form-message--notice {
  background: var(--color-accent-soft);
  color: #6f5f45;
}

.form-message--error {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 980px) {
  .my-grid {
    grid-template-columns: 1fr;
  }

  .account-sidebar {
    position: static;
  }

  .danger-panel {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 680px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;

    h1 {
      font-size: 34px;
    }
  }

  .profile-panel,
  .preference-panel,
  .security-panel,
  .danger-panel,
  .account-sidebar {
    padding: 22px;
  }

  .panel-heading--inline {
    flex-direction: column;
  }

  .profile-list {
    grid-template-columns: 1fr;
  }

  .preference-summary h3,
  .empty-preference h3 {
    font-size: 25px;
  }

  .preference-summary {
    grid-template-columns: 1fr;
  }

  .preference-character {
    width: 160px;
    justify-self: start;
  }
}
</style>
