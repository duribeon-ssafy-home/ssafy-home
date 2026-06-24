<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  properties: { type: Array, default: () => [] },
  selectedId: { type: [Number, null], default: null },
})
const emit = defineEmits(['select', 'bounds-changed'])

const mapContainer = ref(null)
const mapError = ref('')
let map = null
const overlayMap = {}  // propertyId -> { overlay, el }
let clusterer = null
let invisibleImage = null
let suppressMapClick = false

// 클러스터 모드로 전환되는 줌 레벨 기준 (이 값 이상이면 클러스터, 미만이면 개별 오버레이)
const CLUSTER_LEVEL = 6

const clusterCircleSvg = encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="44" height="44">' +
  '<circle cx="22" cy="22" r="20" fill="%231f344f"/>' +
  '</svg>',
)

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
    suppressMapClick = true
    emit('select', property.propertyId)
  })
  return el
}

function clearOverlays() {
  Object.values(overlayMap).forEach(({ overlay }) => overlay.setMap(null))
  Object.keys(overlayMap).forEach((k) => delete overlayMap[k])
}

function exactPos(p) {
  return new kakao.maps.LatLng(Number(p.latitude), Number(p.longitude))
}

function emitBounds() {
  if (!map) return
  const bounds = map.getBounds()
  const sw = bounds.getSouthWest()
  const ne = bounds.getNorthEast()
  emit('bounds-changed', { swLat: sw.getLat(), swLng: sw.getLng(), neLat: ne.getLat(), neLng: ne.getLng() })
}

function updateOverlayVisibility() {
  if (!map) return
  const showIndividual = map.getLevel() < CLUSTER_LEVEL
  Object.values(overlayMap).forEach(({ overlay }) => {
    overlay.setMap(showIndividual ? map : null)
  })
  clusterer?.setMap(showIndividual ? null : map)
}

function renderMarkers(properties, fitBounds = true) {
  if (!map) return
  clearOverlays()
  clusterer.clear()

  const bounds = new kakao.maps.LatLngBounds()
  const markers = []

  properties.forEach((p) => {
    if (p.latitude == null || p.longitude == null) return
    const pos = exactPos(p)

    // 클러스터러용 투명 마커 (위치만 제공, 렌더링은 CustomOverlay가 담당)
    markers.push(new kakao.maps.Marker({ position: pos, image: invisibleImage }))

    const el = makeOverlayEl(p)
    const overlay = new kakao.maps.CustomOverlay({ position: pos, content: el, yAnchor: 1.2 })
    overlayMap[p.propertyId] = { overlay, el }
    bounds.extend(pos)
  })

  clusterer.addMarkers(markers)
  updateOverlayVisibility()

  if (fitBounds && !bounds.isEmpty()) {
    map.setBounds(bounds, 80)
    if (map.getLevel() < CLUSTER_LEVEL) map.setLevel(CLUSTER_LEVEL)
  }
}

function updateMarkers(properties) {
  renderMarkers(properties, false)
}

function applySelectedStyle(id) {
  if (!map) return
  Object.entries(overlayMap).forEach(([pid, { el }]) => {
    el.classList.toggle('map-price-marker--active', Number(pid) === id)
  })
  if (id != null && overlayMap[id]) {
    map.panTo(overlayMap[id].overlay.getPosition())
  }
}

function loadKakaoScript() {
  return new Promise((resolve, reject) => {
    if (window.kakao?.maps?.MarkerClusterer) { resolve(); return }
    const s = document.createElement('script')
    s.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${import.meta.env.VITE_KAKAO_MAP_KEY}&autoload=false&libraries=clusterer`
    s.onload = () => window.kakao.maps.load(resolve)
    s.onerror = () => reject(new Error('카카오맵 SDK 로드 실패'))
    document.head.appendChild(s)
  })
}

onMounted(async () => {
  try {
    await loadKakaoScript()

    invisibleImage = new kakao.maps.MarkerImage(
      'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
      new kakao.maps.Size(1, 1),
    )

    const center = new kakao.maps.LatLng(37.5665, 126.9780)
    map = new kakao.maps.Map(mapContainer.value, { center, level: 7 })

    clusterer = new kakao.maps.MarkerClusterer({
      map,
      averageCenter: true,
      gridSize: 120,
      styles: [{
        width: '44px',
        height: '44px',
        background: `url("data:image/svg+xml,${clusterCircleSvg}") no-repeat center`,
        color: '#fff',
        textAlign: 'center',
        lineHeight: '44px',
        fontSize: '13px',
        fontWeight: '800',
      }],
    })

    kakao.maps.event.addListener(map, 'click', () => {
      if (suppressMapClick) { suppressMapClick = false; return }
      emit('select', null)
    })
    kakao.maps.event.addListener(map, 'zoom_changed', updateOverlayVisibility)
    kakao.maps.event.addListener(map, 'dragend', emitBounds)

    if (props.properties.length) renderMarkers(props.properties)
  } catch (e) {
    console.error(e)
    mapError.value = e.message || '지도 로드 실패'
  }
})

onUnmounted(() => {
  clearOverlays()
  clusterer?.clear()
})

watch(() => props.properties, renderMarkers)
watch(() => props.selectedId, applySelectedStyle)

defineExpose({ updateMarkers })
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
  color: #ffffff;
  border-radius: 20px;
  padding: 7px 13px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.22);
  transition: background 0.12s, box-shadow 0.12s, transform 0.12s;
  position: relative;
  user-select: none;
}

.map-price-marker::after {
  content: '';
  position: absolute;
  bottom: -7px;
  left: 50%;
  transform: translateX(-50%);
  border: 7px solid transparent;
  border-top-color: #1f344f;
  border-bottom: none;
}

.map-price-marker:hover {
  background: #2d4a6e;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transform: scale(1.08);
}

.map-price-marker:hover::after { border-top-color: #2d4a6e; }

.map-price-marker--active {
  background: #2563eb;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.45);
  transform: scale(1.12);
  z-index: 10;
}

.map-price-marker--active::after { border-top-color: #2563eb; }
</style>
