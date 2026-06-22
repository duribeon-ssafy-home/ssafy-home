<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getMyProperty,
  createProperty,
  updateProperty,
  uploadPropertyImages,
  deletePropertyImage,
} from '@/api/propertyApi'

const props = defineProps({
  id: { type: String, default: null },
})

const router = useRouter()
const isEditMode = computed(() => Boolean(props.id))

const isLoading = ref(false)
const isSubmitting = ref(false)
const isUploadingImages = ref(false)
const errorMsg = ref('')

const form = ref({
  title: '',
  sido: '',
  gugun: '',
  dong: '',
  address: '',
  roadAddress: '',
  detailAddress: '',
  rentType: 'JEONSE',
  roomType: 'ONE_ROOM',
  deposit: '',
  monthlyRent: '',
  area: '',
  floor: '',
  buildYear: '',
})

const existingImages = ref([])
const pendingFiles = ref([])
const pendingPreviews = ref([])
const fileInput = ref(null)

function loadDaumPostcode() {
  return new Promise((resolve) => {
    if (window.daum?.Postcode) { resolve(); return }
    const s = document.createElement('script')
    s.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
    s.onload = resolve
    document.head.appendChild(s)
  })
}

async function openAddressSearch() {
  await loadDaumPostcode()
  new window.daum.Postcode({
    oncomplete(data) {
      form.value.address     = data.jibunAddress || data.autoJibunAddress || ''
      form.value.roadAddress = data.roadAddress  || data.autoRoadAddress  || ''
      form.value.sido        = data.sido
      form.value.gugun       = data.sigungu
      form.value.dong        = data.bname
    },
  }).open()
}

async function loadProperty() {
  isLoading.value = true
  try {
    const data = await getMyProperty(props.id)
    form.value = {
      title: data.title ?? '',
      sido: data.sido ?? '',
      gugun: data.gugun ?? '',
      dong: data.dong ?? '',
      address: data.address ?? '',
      roadAddress: data.roadAddress ?? '',
      detailAddress: '',
      rentType: data.rentType ?? 'JEONSE',
      roomType: data.roomType ?? 'ONE_ROOM',
      deposit: data.deposit != null ? String(data.deposit) : '',
      monthlyRent: data.monthlyRent != null ? String(data.monthlyRent) : '',
      area: data.area != null ? String(data.area) : '',
      floor: data.floor != null ? String(data.floor) : '',
      buildYear: data.buildYear != null ? String(data.buildYear) : '',
    }
    existingImages.value = data.images ?? []
  } catch {
    errorMsg.value = '매물 정보를 불러올 수 없습니다.'
  } finally {
    isLoading.value = false
  }
}

async function handleSubmit() {
  errorMsg.value = ''
  if (!form.value.address || !form.value.sido) {
    errorMsg.value = '주소를 검색해서 선택해주세요.'
    return
  }

  isSubmitting.value = true
  try {
    const payload = {
      title: form.value.title || null,
      sido: form.value.sido,
      gugun: form.value.gugun,
      dong: form.value.dong,
      address: form.value.detailAddress
        ? `${form.value.address} ${form.value.detailAddress}`
        : form.value.address,
      roadAddress: form.value.roadAddress || null,
      rentType: form.value.rentType,
      roomType: form.value.roomType,
      deposit: form.value.deposit ? Number(form.value.deposit) : null,
      monthlyRent: (form.value.rentType === 'MONTHLY' || form.value.rentType === 'SEMI_JEONSE') && form.value.monthlyRent
        ? Number(form.value.monthlyRent)
        : null,
      area: form.value.area ? Number(form.value.area) : null,
      floor: form.value.floor ? Number(form.value.floor) : null,
      buildYear: form.value.buildYear ? Number(form.value.buildYear) : null,
    }

    if (isEditMode.value) {
      await updateProperty(props.id, payload)
    } else {
      const propertyId = await createProperty(payload)
      if (pendingFiles.value.length > 0) {
        await uploadPropertyImages(propertyId, pendingFiles.value)
      }
    }
    router.push({ name: 'agent-properties' })
  } catch (e) {
    const msg = e?.response?.data?.message
    errorMsg.value = msg || '저장에 실패했습니다. 입력 값을 확인해주세요.'
  } finally {
    isSubmitting.value = false
  }
}

function handlePendingImageSelect(e) {
  const files = Array.from(e.target.files)
  if (!files.length) return
  const remaining = 10 - pendingFiles.value.length
  if (files.length > remaining) {
    alert(`이미지는 최대 10장까지 등록할 수 있습니다.`)
    fileInput.value.value = ''
    return
  }
  pendingFiles.value = [...pendingFiles.value, ...files]
  pendingPreviews.value = [...pendingPreviews.value, ...files.map((f) => URL.createObjectURL(f))]
  fileInput.value.value = ''
}

function removePendingImage(index) {
  URL.revokeObjectURL(pendingPreviews.value[index])
  pendingFiles.value = pendingFiles.value.filter((_, i) => i !== index)
  pendingPreviews.value = pendingPreviews.value.filter((_, i) => i !== index)
}

async function handleImageUpload(e) {
  const files = Array.from(e.target.files)
  if (!files.length) return
  const remaining = 10 - existingImages.value.length
  if (files.length > remaining) {
    alert(`이미지는 최대 10장까지 등록할 수 있습니다. (현재 ${existingImages.value.length}장, ${remaining}장 추가 가능)`)
    fileInput.value.value = ''
    return
  }
  isUploadingImages.value = true
  try {
    const newImages = await uploadPropertyImages(props.id, files)
    existingImages.value = [...existingImages.value, ...newImages]
  } catch {
    alert('이미지 업로드에 실패했습니다.')
  } finally {
    isUploadingImages.value = false
    fileInput.value.value = ''
  }
}

async function handleImageDelete(image) {
  if (!confirm('이미지를 삭제하시겠습니까?')) return
  try {
    await deletePropertyImage(props.id, image.imageId)
    existingImages.value = existingImages.value.filter((i) => i.imageId !== image.imageId)
  } catch {
    alert('이미지 삭제에 실패했습니다.')
  }
}

onMounted(() => {
  if (isEditMode.value) loadProperty()
})
</script>

<template>
  <main class="page form-page">
    <div class="section-container">
      <div class="page-header">
        <button class="back-btn" type="button" @click="router.push({ name: 'agent-properties' })">
          ← 목록으로
        </button>
        <h1>{{ isEditMode ? '매물 수정' : '새 매물 등록' }}</h1>
      </div>

      <div v-if="isLoading" class="state-box">불러오는 중...</div>

      <form v-else class="form-card" @submit.prevent="handleSubmit">

        <section class="form-section">
          <h2 class="section-title">기본 정보</h2>

          <div class="field">
            <label>매물 제목</label>
            <input v-model="form.title" type="text" placeholder="예: 역삼동 햇빛 원룸" />
          </div>

          <div class="field">
            <label>주소 검색 <span class="required">*</span></label>
            <button type="button" class="addr-search-btn" @click="openAddressSearch">
              🔍 주소 검색
            </button>
          </div>

          <div v-if="form.address" class="addr-result">
            <div v-if="form.roadAddress" class="addr-result__row">
              <span class="addr-result__label">도로명</span>
              <span>{{ form.roadAddress }}</span>
            </div>
            <div class="addr-result__row">
              <span class="addr-result__label">지번</span>
              <span>{{ form.address }}</span>
            </div>
            <div class="addr-result__row addr-result__row--muted">
              <span class="addr-result__label">분류</span>
              <span>{{ form.sido }} {{ form.gugun }} {{ form.dong }}</span>
            </div>
          </div>

          <div v-if="form.address" class="field">
            <label>상세 주소</label>
            <input
              v-model="form.detailAddress"
              type="text"
              placeholder="예: 101동 302호, 2층"
            />
            <div class="addr-preview">
              <span class="addr-preview__label">저장될 주소</span>
              <span>{{ form.detailAddress ? `${form.address} ${form.detailAddress}` : form.address }}</span>
            </div>
          </div>
        </section>

        <section class="form-section">
          <h2 class="section-title">매물 정보</h2>

          <div class="field">
            <label>거래 유형</label>
            <div class="chips">
              <button
                type="button"
                class="chip"
                :class="{ 'chip--active': form.rentType === 'JEONSE' }"
                @click="form.rentType = 'JEONSE'"
              >전세</button>
              <button
                type="button"
                class="chip"
                :class="{ 'chip--active': form.rentType === 'MONTHLY' }"
                @click="form.rentType = 'MONTHLY'"
              >월세</button>
              <button
                type="button"
                class="chip"
                :class="{ 'chip--active': form.rentType === 'SEMI_JEONSE' }"
                @click="form.rentType = 'SEMI_JEONSE'"
              >반전세</button>
            </div>
          </div>

          <div class="field">
            <label>방 타입</label>
            <div class="chips">
              <button
                v-for="rt in [
                  { value: 'ONE_ROOM', label: '원룸' },
                  { value: 'TWO_ROOM', label: '투룸' },
                  { value: 'OFFICETEL', label: '오피스텔' },
                  { value: 'APARTMENT', label: '아파트' },
                ]"
                :key="rt.value"
                type="button"
                class="chip"
                :class="{ 'chip--active': form.roomType === rt.value }"
                @click="form.roomType = rt.value"
              >{{ rt.label }}</button>
            </div>
          </div>

          <div class="field-row">
            <div class="field">
              <label>보증금 (만원)</label>
              <input v-model="form.deposit" type="number" min="0" placeholder="예: 5000" />
            </div>
            <div v-if="form.rentType === 'MONTHLY' || form.rentType === 'SEMI_JEONSE'" class="field">
              <label>월세 (만원)</label>
              <input v-model="form.monthlyRent" type="number" min="0" placeholder="예: 60" />
            </div>
          </div>

          <div class="field-row">
            <div class="field">
              <label>면적 (m²)</label>
              <input v-model="form.area" type="number" min="0" step="0.1" placeholder="예: 33.5" />
            </div>
            <div class="field">
              <label>층수</label>
              <input v-model="form.floor" type="number" min="1" placeholder="예: 3" />
            </div>
            <div class="field">
              <label>건축년도</label>
              <input v-model="form.buildYear" type="number" min="1900" max="2099" placeholder="예: 2018" />
            </div>
          </div>
        </section>

        <section class="form-section">
          <h2 class="section-title">이미지 {{ isEditMode ? '관리' : '등록' }} (최대 10장)</h2>

          <template v-if="isEditMode">
            <div v-if="existingImages.length" class="image-grid">
              <div v-for="img in existingImages" :key="img.imageId" class="image-item">
                <img :src="img.imageUrl" :alt="`이미지 ${img.imageId}`" />
                <button class="image-delete" type="button" @click="handleImageDelete(img)">✕</button>
                <span v-if="img.sortOrder === 0" class="image-main-badge">대표</span>
              </div>
            </div>
            <p v-else class="no-image-text">등록된 이미지가 없습니다.</p>
            <label v-if="existingImages.length < 10" class="upload-label">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                multiple
                class="upload-input"
                @change="handleImageUpload"
              />
              <span v-if="isUploadingImages">업로드 중...</span>
              <span v-else>+ 이미지 추가 ({{ existingImages.length }}/10)</span>
            </label>
          </template>

          <template v-else>
            <div v-if="pendingPreviews.length" class="image-grid">
              <div v-for="(preview, i) in pendingPreviews" :key="i" class="image-item">
                <img :src="preview" :alt="`이미지 ${i + 1}`" />
                <button class="image-delete" type="button" @click="removePendingImage(i)">✕</button>
                <span v-if="i === 0" class="image-main-badge">대표</span>
              </div>
            </div>
            <p v-else class="no-image-text">선택된 이미지가 없습니다. (선택 사항)</p>
            <label v-if="pendingFiles.length < 10" class="upload-label">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                multiple
                class="upload-input"
                @change="handlePendingImageSelect"
              />
              <span>+ 이미지 추가 ({{ pendingFiles.length }}/10)</span>
            </label>
          </template>
        </section>

        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

        <div class="form-actions">
          <button
            type="button"
            class="ghost-btn"
            @click="router.push({ name: 'agent-properties' })"
          >취소</button>
          <button type="submit" class="submit-btn" :disabled="isSubmitting">
            {{ isSubmitting ? '저장 중...' : (isEditMode ? '수정 완료' : '매물 등록') }}
          </button>
        </div>
      </form>
    </div>
  </main>
</template>

<style lang="scss" scoped>
.form-page {
  padding: 44px 0 80px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;

  h1 {
    color: var(--color-heading);
    font-size: 26px;
    font-weight: 900;
  }
}

.back-btn {
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 800;
  padding: 6px 0;
  transition: opacity var(--transition-fast);
  &:hover { opacity: 0.7; }
}

.state-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: var(--color-muted);
  font-weight: 700;
}

.form-card {
  display: flex;
  flex-direction: column;
  gap: 32px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  padding: 32px;
  box-shadow: var(--shadow-card);
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--color-border);

  &:last-of-type { border-bottom: none; padding-bottom: 0; }
}

.section-title {
  color: var(--color-heading);
  font-size: 15px;
  font-weight: 900;
  padding-bottom: 4px;
  border-bottom: 2px solid var(--color-primary-soft);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 7px;

  label {
    color: var(--color-muted);
    font-size: 12px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  input {
    height: 42px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
    color: var(--color-heading);
    font-size: 14px;
    font-weight: 700;
    padding: 0 13px;
    outline: none;
    transition: border-color var(--transition-fast);
    &:focus { border-color: var(--color-primary); }
    &::placeholder { color: var(--color-subtle); }
  }
}

.field-row {
  display: flex;
  gap: 16px;

  .field { flex: 1; }
}

.required {
  color: var(--color-danger);
}

.addr-search-btn {
  align-self: flex-start;
  height: 42px;
  padding: 0 18px;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  transition: background-color var(--transition-fast);
  &:hover { background: var(--color-primary); color: var(--color-surface); }
}

.addr-result {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-soft);
  font-size: 13px;
  font-weight: 700;
}

.addr-result__row {
  display: flex;
  gap: 10px;
  color: var(--color-heading);

  &--muted { color: var(--color-muted); }
}

.addr-result__label {
  flex-shrink: 0;
  width: 42px;
  color: var(--color-muted);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  padding-top: 1px;
}

.addr-preview {
  display: flex;
  gap: 8px;
  align-items: baseline;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary-dark);
}

.addr-preview__label {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 800;
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  height: 38px;
  padding: 0 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: border-color var(--transition-fast), background-color var(--transition-fast), color var(--transition-fast);
  &:hover { border-color: var(--color-primary); color: var(--color-primary-dark); }
}

.chip--active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-dark);
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--color-border);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.image-delete {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.55);
  color: white;
  border-radius: 50%;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  &:hover { background: var(--color-danger); }
}

.image-main-badge {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: var(--color-primary);
  color: white;
  font-size: 10px;
  font-weight: 900;
  padding: 2px 6px;
  border-radius: var(--radius-xs);
}

.no-image-text {
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 700;
}

.upload-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 18px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-muted);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: border-color var(--transition-fast), color var(--transition-fast);
  &:hover { border-color: var(--color-primary); color: var(--color-primary); }
}

.upload-input {
  display: none;
}

.error-msg {
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: #fff1f2;
  color: #9f1239;
  font-size: 13px;
  font-weight: 700;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ghost-btn {
  height: 44px;
  padding: 0 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-heading);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: border-color var(--transition-fast);
  &:hover { border-color: var(--color-primary); }
}

.submit-btn {
  height: 44px;
  padding: 0 28px;
  background: var(--color-primary);
  color: var(--color-surface);
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  transition: background-color var(--transition-fast);
  &:hover:not(:disabled) { background: var(--color-primary-dark); }
  &:disabled { opacity: 0.55; cursor: not-allowed; }
}

@media (max-width: 700px) {
  .form-card { padding: 20px; }
  .field-row { flex-direction: column; }
}
</style>
