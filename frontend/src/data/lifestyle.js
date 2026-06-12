const optionA = 'A'

export const fallbackLifestyleQuestions = [
  {
    questionId: 1,
    category: 'LIVING_CONVENIENCE',
    title: '집 주변에서 가장 중요하게 보는 생활 조건은 무엇인가요?',
    optionA: '편의점, 병원, 카페처럼 매일 쓰는 시설이 가까웠으면 해요.',
    optionB: '조금 걸어도 괜찮으니 집 자체의 조건이 더 중요해요.',
    mapping: 'facilityScore',
  },
  {
    questionId: 2,
    category: 'LIVING_CONVENIENCE',
    title: '동네 분위기는 어떤 쪽이 더 편하게 느껴지나요?',
    optionA: '생활 편의시설이 모여 있고 이동 동선이 짧은 동네요.',
    optionB: '편의시설보다 조용하고 차분한 주변 환경이 좋아요.',
    mapping: 'facilityCount, facilityScore',
  },
  {
    questionId: 3,
    category: 'COST_SENSITIVITY',
    title: '매달 나가는 월세는 어느 정도로 관리하고 싶나요?',
    optionA: '월세 부담을 최대한 낮추는 게 가장 중요해요.',
    optionB: '조건이 마음에 들면 월세는 조금 더 감수할 수 있어요.',
    mapping: 'monthlyRentMax',
  },
  {
    questionId: 4,
    category: 'COST_SENSITIVITY',
    title: '초기 보증금은 어떤 쪽이 더 편한가요?',
    optionA: '처음 들어갈 때 보증금이 낮을수록 좋아요.',
    optionB: '보증금이 조금 높아도 전체 조건이 좋으면 괜찮아요.',
    mapping: 'depositMax',
  },
  {
    questionId: 5,
    category: 'HOME_QUALITY',
    title: '집 안에서 가장 크게 체감하는 만족 요소는 무엇인가요?',
    optionA: '공간이 넉넉해서 생활 동선이 편한 게 중요해요.',
    optionB: '공간은 작아도 필요한 조건만 잘 맞으면 충분해요.',
    mapping: 'areaMin',
  },
  {
    questionId: 6,
    category: 'HOME_QUALITY',
    title: '건물 상태는 어느 쪽을 선호하나요?',
    optionA: '오래되어 보이지 않는 깔끔한 집이 좋아요.',
    optionB: '연식이 조금 있어도 가격과 위치가 맞으면 괜찮아요.',
    mapping: 'buildYearMin',
  },
]

export const lifestyleTypeMeta = {
  LIVING_COST_HOME_BALANCED: {
    typeName: '균형 잡힌 생활권 실속형',
    headline: '생활 편의, 비용, 집의 컨디션을 모두 꼼꼼히 보는 타입입니다.',
    summary:
      '매일의 이동과 생활비를 안정적으로 관리하면서도 집 안에서 보내는 시간의 만족도를 놓치지 않는 편이에요.',
    chips: ['생활권 우수', '월세 부담 낮음', '보증금 부담 낮음', '넓은 면적', '신축급'],
    recommendedRoomTypes: ['ONE_ROOM', 'OFFICETEL', 'TWO_ROOM'],
  },
  LIVING_COST_COMPACT: {
    typeName: '생활권 중심 실속형',
    headline: '편리한 동네와 합리적인 비용을 우선하는 타입입니다.',
    summary:
      '집의 크기보다 교통, 편의시설, 매달 나가는 비용을 더 현실적으로 비교하는 성향이에요.',
    chips: ['역세권', '생활 편의시설', '월세 50 이하', '보증금 낮음', '즉시입주'],
    recommendedRoomTypes: ['ONE_ROOM', 'OFFICETEL'],
  },
  LIVING_FLEXIBLE_HOME: {
    typeName: '편안한 공간 우선형',
    headline: '생활권은 챙기되, 집 안의 쾌적함에 더 크게 반응하는 타입입니다.',
    summary:
      '퇴근 후 머무는 시간이 중요해서 채광, 면적, 연식처럼 체감 품질이 좋은 매물과 잘 맞아요.',
    chips: ['채광 좋음', '면적 25m2 이상', '신축급', '수납 넉넉', '조용한 동네'],
    recommendedRoomTypes: ['TWO_ROOM', 'OFFICETEL', 'APARTMENT'],
  },
  LIVING_FLEXIBLE_COMPACT: {
    typeName: '생활권 중심 실용형',
    headline: '주변 편의와 빠른 이동을 중시하는 실용적인 타입입니다.',
    summary:
      '집은 필요한 만큼이면 충분하고, 대신 매일 쓰는 동선이 짧고 편한 환경을 선호해요.',
    chips: ['생활권 우수', '역 도보권', '풀옵션', '관리비 낮음', '원룸'],
    recommendedRoomTypes: ['ONE_ROOM', 'OFFICETEL'],
  },
  LOCATION_FLEXIBLE_COST_HOME: {
    typeName: '조건 꼼꼼 공간형',
    headline: '지역보다 예산과 집 자체의 만족도를 더 꼼꼼히 보는 타입입니다.',
    summary:
      '선호 지역을 조금 넓히더라도 넓이, 연식, 비용의 균형이 좋은 매물을 찾는 쪽이 잘 맞아요.',
    chips: ['예산 균형', '면적 25m2 이상', '신축급', '관리비 확인', '투룸 가능'],
    recommendedRoomTypes: ['TWO_ROOM', 'APARTMENT', 'OFFICETEL'],
  },
  LOCATION_FLEXIBLE_COST_COMPACT: {
    typeName: '비용 절약 실속형',
    headline: '가장 중요한 기준은 무리 없는 주거비인 타입입니다.',
    summary:
      '입지나 크기를 유연하게 보면서 월세와 보증금을 안정적으로 관리할 수 있는 매물이 잘 맞아요.',
    chips: ['월세 50 이하', '보증금 낮음', '관리비 낮음', '가성비', '원룸'],
    recommendedRoomTypes: ['ONE_ROOM'],
  },
  LOCATION_FLEXIBLE_HOME: {
    typeName: '집 자체 만족형',
    headline: '동네보다 집 안에서 느끼는 쾌적함을 더 중요하게 보는 타입입니다.',
    summary:
      '채광, 면적, 건물 상태처럼 매일 머무는 공간의 품질이 만족도를 크게 좌우해요.',
    chips: ['넓은 면적', '신축급', '채광 좋음', '수납 넉넉', '조용함'],
    recommendedRoomTypes: ['TWO_ROOM', 'APARTMENT', 'OFFICETEL'],
  },
  LOCATION_FLEXIBLE_COMPACT: {
    typeName: '조건 유연 탐색형',
    headline: '입지, 비용, 크기를 유연하게 비교하며 현실적인 선택을 하는 타입입니다.',
    summary:
      '하나의 조건에 고정되기보다 전체 균형을 보고, 부담 없이 시작하기 좋은 매물을 선호해요.',
    chips: ['조건 유연', '실속 매물', '즉시입주', '관리비 낮음', '원룸'],
    recommendedRoomTypes: ['ONE_ROOM', 'OFFICETEL'],
  },
}

export function normalizeLifestyleQuestions(questions) {
  const source = Array.isArray(questions) && questions.length ? questions : fallbackLifestyleQuestions

  return source.map((question) => {
    const fallback = fallbackLifestyleQuestions.find((item) => item.questionId === question.questionId)

    if (!fallback) {
      return question
    }

    return {
      ...question,
      title: isBrokenKorean(question.title) ? fallback.title : question.title,
      optionA: isBrokenKorean(question.optionA) ? fallback.optionA : question.optionA,
      optionB: isBrokenKorean(question.optionB) ? fallback.optionB : question.optionB,
    }
  })
}

export function buildLifestyleResult(questions, answerMap) {
  const hasA = (category) =>
    questions.some(
      (question) => question.category === category && answerMap[question.questionId] === optionA,
    )

  const livingConvenienceImportant = hasA('LIVING_CONVENIENCE')
  const costSensitive = hasA('COST_SENSITIVITY')
  const homeQualityImportant = hasA('HOME_QUALITY')
  const lifestyleType = resolveLifestyleType(
    livingConvenienceImportant,
    costSensitive,
    homeQualityImportant,
  )
  const answers = questions.map((question) => ({
    questionId: question.questionId,
    selectedOption: answerMap[question.questionId],
  }))
  const filterPreset = createFilterPreset(answerMap)

  return {
    lifestyleType,
    typeName: lifestyleTypeMeta[lifestyleType].typeName,
    filterPreset,
    answers,
  }
}

export function createLifestylePresetChips(filterPreset = {}, fallbackChips = []) {
  const chips = []

  if (filterPreset.facilityScoreMin) {
    chips.push(`생활 편의 점수 ${filterPreset.facilityScoreMin}+`)
  }

  if (filterPreset.facilityCountMin) {
    chips.push(`편의시설 ${filterPreset.facilityCountMin}개 이상`)
  }

  if (filterPreset.monthlyRentMax) {
    chips.push(`월세 ${filterPreset.monthlyRentMax}만 이하`)
  }

  if (filterPreset.depositMax) {
    chips.push(`보증금 ${Number(filterPreset.depositMax).toLocaleString('ko-KR')}만 이하`)
  }

  if (filterPreset.areaMin) {
    chips.push(`${filterPreset.areaMin}m2 이상`)
  }

  if (filterPreset.buildYearMin) {
    chips.push(`${filterPreset.buildYearMin}년 이후`)
  }

  return chips.length ? chips : fallbackChips
}

export function createLifestyleRecommendationFilters(result = {}) {
  const filterPreset = result?.filterPreset || {}
  const meta = result?.lifestyleType ? lifestyleTypeMeta[result.lifestyleType] : null

  return {
    location: '',
    deposit: filterPreset.depositMax != null ? String(filterPreset.depositMax) : '',
    monthlyRent: filterPreset.monthlyRentMax != null ? String(filterPreset.monthlyRentMax) : '',
    roomType: meta?.recommendedRoomTypes?.[0] || 'ALL',
  }
}

function resolveLifestyleType(livingConvenienceImportant, costSensitive, homeQualityImportant) {
  if (livingConvenienceImportant && costSensitive && homeQualityImportant) {
    return 'LIVING_COST_HOME_BALANCED'
  }

  if (livingConvenienceImportant && costSensitive && !homeQualityImportant) {
    return 'LIVING_COST_COMPACT'
  }

  if (livingConvenienceImportant && !costSensitive && homeQualityImportant) {
    return 'LIVING_FLEXIBLE_HOME'
  }

  if (livingConvenienceImportant && !costSensitive && !homeQualityImportant) {
    return 'LIVING_FLEXIBLE_COMPACT'
  }

  if (!livingConvenienceImportant && costSensitive && homeQualityImportant) {
    return 'LOCATION_FLEXIBLE_COST_HOME'
  }

  if (!livingConvenienceImportant && costSensitive && !homeQualityImportant) {
    return 'LOCATION_FLEXIBLE_COST_COMPACT'
  }

  if (!livingConvenienceImportant && !costSensitive && homeQualityImportant) {
    return 'LOCATION_FLEXIBLE_HOME'
  }

  return 'LOCATION_FLEXIBLE_COMPACT'
}

function createFilterPreset(answerMap) {
  return {
    facilityScoreMin: answerMap[1] === optionA || answerMap[2] === optionA ? 70 : null,
    facilityCountMin: answerMap[2] === optionA ? 20 : null,
    monthlyRentMax: answerMap[3] === optionA ? 50 : null,
    depositMax: answerMap[4] === optionA ? 1000 : null,
    areaMin: answerMap[5] === optionA ? 25 : null,
    buildYearMin: answerMap[6] === optionA ? 2016 : null,
  }
}

function isBrokenKorean(text) {
  if (!text) {
    return true
  }

  return text.includes('�') || text.includes('?앺') || text.includes('吏') || text.includes('蹂')
}
