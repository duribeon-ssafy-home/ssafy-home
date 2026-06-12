<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  properties: { type: Array, default: () => [] },
  selectedId: { type: [Number, null], default: null },
})
const emit = defineEmits(['select'])

const mapContainer = ref(null)
const mapError = ref('')
let map = null
const overlayMap = {}  // propertyId -> { overlay, el }

function loadKakaoScript() {
  return new Promise((resolve, reject) => {
    if (window.kakao?.maps) { resolve(); return }
    const s = document.createElement('script')
    s.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${import.meta.env.VITE_KAKAO_MAP_KEY}&autoload=false`
    s.onload = () => window.kakao.maps.load(resolve)
    s.onerror = () => reject(new Error('카카오맵 SDK 로드 실패'))
    document.head.appendChild(s)
  })
}

function formatPrice(property) {
  if (property.rentType === 'JEONSE') {
    const d = property.deposit
    return d >= 10000
      ? `전세 ${(d / 10000).toFixed(d % 10000 === 0 ? 0 : 1)}억`
      : `전세 ${d.toLocaleString()}만`
  }
  return `월 ${property.monthlyRent}만`
}

function makeOverlayEl(property) {
  const el = document.createElement('div')
  el.className = 'map-price-marker'
  el.textContent = formatPrice(property)
  el.addEventListener('click', (e) => {
    e.stopPropagation()
    emit('select', property.propertyId)
  })
  return el
}

function clearOverlays() {
  Object.values(overlayMap).forEach(({ overlay }) => overlay.setMap(null))
  Object.keys(overlayMap).forEach((k) => delete overlayMap[k])
}

function renderMarkers(properties) {
  clearOverlays()
  properties.forEach((p) => {
    if (p.latitude == null || p.longitude == null) return
    const pos = new kakao.maps.LatLng(Number(p.latitude), Number(p.longitude))
    const el = makeOverlayEl(p)
    const overlay = new kakao.maps.CustomOverlay({ position: pos, content: el, yAnchor: 1.2 })
    overlay.setMap(map)
    overlayMap[p.propertyId] = { overlay, el }
  })
}

function applySelectedStyle(id) {
  Object.entries(overlayMap).forEach(([pid, { el }]) => {
    el.classList.toggle('map-price-marker--active', Number(pid) === id)
  })
  if (id != null && overlayMap[id]) {
    map.panTo(overlayMap[id].overlay.getPosition())
  }
}

onMounted(async () => {
  try {
    await loadKakaoScript()
    const center = new kakao.maps.LatLng(37.5665, 126.9780)
    map = new kakao.maps.Map(mapContainer.value, { center, level: 7 })
    kakao.maps.event.addListener(map, 'click', () => emit('select', null))
    if (props.properties.length) renderMarkers(props.properties)
  } catch (e) {
    console.error(e)
    mapError.value = e.message || '지도 로드 실패'
  }
})

onUnmounted(clearOverlays)

watch(() => props.properties, renderMarkers)
watch(() => props.selectedId, applySelectedStyle)
</script>

<template>
  <div class="kakao-map-wrapper">
    <div ref="mapContainer" class="kakao-map" />
    <div v-if="mapError" class="kakao-map-error">
      <p>🗺️ 지도 로드 실패</p>
      <p class="kakao-map-error__msg">{{ mapError }}</p>
      <p class="kakao-map-error__hint">카카오 개발자 콘솔에서 JavaScript 키와 localhost 도메인이 등록되어 있는지 확인해주세요.</p>
    </div>
  </div>
</template>

<style>
.kakao-map-wrapper { position: absolute; inset: 0; }
.kakao-map { position: absolute; inset: 0; }

.kakao-map-error {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #f8fafc;
  color: #667085;
  font-size: 14px;
  text-align: center;
  padding: 24px;
}

.kakao-map-error__msg { font-weight: 700; color: #b42318; font-size: 13px; }
.kakao-map-error__hint { font-size: 12px; max-width: 320px; line-height: 1.6; }

.map-price-marker {
  background: #1f344f;
  color: white;
  border-radius: 20px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.28);
  transition: background 0.15s, transform 0.15s;
  position: relative;
  user-select: none;
}

.map-price-marker::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: #1f344f;
  border-bottom: none;
}

.map-price-marker:hover {
  background: #2563eb;
  transform: scale(1.08);
}

.map-price-marker:hover::after { border-top-color: #2563eb; }

.map-price-marker--active {
  background: #2563eb;
  transform: scale(1.12);
  z-index: 10;
}

.map-price-marker--active::after { border-top-color: #2563eb; }
</style>
