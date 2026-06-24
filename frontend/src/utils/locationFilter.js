const SIDO_ALIASES = {
  서울: '서울특별시',
  서울시: '서울특별시',
  부산: '부산광역시',
  부산시: '부산광역시',
  대구: '대구광역시',
  대구시: '대구광역시',
  인천: '인천광역시',
  인천시: '인천광역시',
  광주: '광주광역시',
  광주시: '광주광역시',
  대전: '대전광역시',
  대전시: '대전광역시',
  울산: '울산광역시',
  울산시: '울산광역시',
  세종: '세종특별자치시',
  세종시: '세종특별자치시',
  경기: '경기도',
  강원: '강원특별자치도',
  강원도: '강원특별자치도',
  충북: '충청북도',
  충남: '충청남도',
  전남: '전라남도',
  전북: '전북특별자치도',
  경남: '경상남도',
  경북: '경상북도',
  제주: '제주특별자치도',
  제주도: '제주특별자치도',
}

export function resolveLocationParam(input) {
  const value = String(input || '').trim()
  if (!value) return {}

  const parts = value.split(/\s+/)

  if (parts.length === 1) {
    const part = parts[0]
    if (SIDO_ALIASES[part]) return { sido: SIDO_ALIASES[part] }
    if (/(특별시|광역시|특별자치시|특별자치도|도)$/.test(part)) return { sido: part }
    if (/[구군시]$/.test(part)) return { gugun: part }
    return { dong: part }
  }

  const result = {}
  let lastUnmatched = null

  for (const part of parts) {
    if (SIDO_ALIASES[part]) {
      result.sido = SIDO_ALIASES[part]
    } else if (/(특별시|광역시|특별자치시|특별자치도|도)$/.test(part)) {
      result.sido = part
    } else if (/[구군시]$/.test(part)) {
      result.gugun = part
    } else {
      lastUnmatched = part
    }
  }

  if (lastUnmatched) result.dong = lastUnmatched
  return Object.keys(result).length ? result : { dong: value }
}

export function buildLocationText(location = {}) {
  return [
    location.preferredSido || location.sido,
    location.preferredGugun || location.gugun,
    location.preferredDong || location.dong,
  ]
    .filter(Boolean)
    .join(' ')
}

export function extractPreferredLocation(filterPreset = {}) {
  return {
    sido: filterPreset.preferredSido || null,
    gugun: filterPreset.preferredGugun || null,
    dong: filterPreset.preferredDong || null,
  }
}
