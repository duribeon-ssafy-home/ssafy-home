export const reportReasons = [
  { value: 'FAKE_LISTING', label: '허위 매물' },
  { value: 'PRICE_MISMATCH', label: '가격 불일치' },
  { value: 'PHOTO_MISMATCH', label: '사진 불일치' },
  { value: 'NO_CONTACT', label: '연락 불가' },
  { value: 'FRAUD_SUSPECTED', label: '사기 의심' },
  { value: 'ETC', label: '기타' },
]

export const reportReasonLabels = reportReasons.reduce((labels, reason) => {
  labels[reason.value] = reason.label
  return labels
}, {})
