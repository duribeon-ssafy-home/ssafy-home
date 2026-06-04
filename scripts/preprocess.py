"""
국토교통부 전월세 거래 데이터 전처리 스크립트

처리 순서:
  1. CSV 12개 병합
  2. 전체 28만 건 기준 지역 평균가 계산 (위험 라벨용)
  3. (시군구 + 번지 + 건물명 + 전월세구분) 기준 중복 제거
  4. title / room_type / floor / build_year 등 파생 컬럼 생성
  5. Kakao Geocoding API로 위경도 변환 (중간 저장 지원)
  6. processed_properties.csv 저장

사용법:
  pip install pandas requests
  python preprocess.py
"""

import os
import time
import argparse
from io import StringIO

import pandas as pd
import requests

DATA_DIR    = os.path.join(os.path.dirname(__file__), '..', 'data')
OUTPUT_FILE = os.path.join(DATA_DIR, 'processed_properties.csv')
FAILED_FILE = os.path.join(DATA_DIR, 'failed_addresses.csv')
SAVE_EVERY  = 100
DELAY       = 0.12   # 초당 최대 10 req


# ============================================================
# 컬럼명 매핑
# ============================================================
COLUMN_MAP = {
    'NO':          'no',
    '시군구':      'sigungu_full',
    '번지':        'bunji',
    '건물명':      'building_name',
    '전월세구분':  'rent_type_raw',
    '전용면적(㎡)':'area',
    '계약년월':    'deal_ym',
    '계약일':      'deal_day',
    '보증금(만원)':'deposit',
    '월세금(만원)':'monthly_rent',
    '층':          'floor',
    '건축년도':    'build_year',
    '도로명':      'road_name',
}


# ============================================================
# 유틸 함수
# ============================================================
def read_csv(filepath: str) -> pd.DataFrame:
    with open(filepath, encoding='cp949') as f:
        lines = f.readlines()
    header_idx = next(i for i, l in enumerate(lines) if l.strip().startswith('"NO"'))
    content = ''.join(lines[header_idx:])
    return pd.read_csv(StringIO(content), dtype=str)


def parse_sigungu(full: str):
    """'경기도 화성시 효행구 기안동' → ('경기도', '화성시 효행구', '기안동')"""
    parts = full.strip().split()
    if len(parts) < 2:
        return full, '', ''
    return parts[0], ' '.join(parts[1:-1]) if len(parts) > 2 else parts[1], parts[-1]


def to_rent_type(raw: str) -> str:
    if '전세' in str(raw): return 'JEONSE'
    if '월세' in str(raw): return 'MONTHLY'
    return None


def to_room_type(area_str) -> str:
    """연립다세대는 면적 기준으로 ONE_ROOM / TWO_ROOM 근사 분류"""
    try:
        return 'ONE_ROOM' if float(str(area_str).strip()) < 33 else 'TWO_ROOM'
    except Exception:
        return None


def parse_floor(val) -> int:
    try: return int(str(val).strip())
    except Exception: return None


def parse_year(val) -> int:
    try:
        y = int(str(val).strip())
        return y if 1900 < y < 2030 else None
    except Exception:
        return None


def format_date(ym, day) -> str:
    try:
        ym  = str(ym).strip()
        day = str(day).strip().zfill(2)
        return f'{ym[:4]}-{ym[4:6]}-{day}' if len(ym) == 6 else None
    except Exception:
        return None


def to_risk_label(gap_rate) -> str:
    if gap_rate is None or pd.isna(gap_rate): return 'UNKNOWN'
    if gap_rate > 0.5: return 'DANGER'
    if gap_rate > 0.3: return 'CAUTION'
    return 'SAFE'


def geocode(address: str, api_key: str):
    url     = 'https://dapi.kakao.com/v2/local/search/address.json'
    headers = {'Authorization': f'KakaoAK {api_key}'}
    try:
        resp = requests.get(url, headers=headers, params={'query': address}, timeout=5)
        resp.raise_for_status()
        docs = resp.json().get('documents', [])
        if docs:
            return float(docs[0]['y']), float(docs[0]['x'])
    except Exception as e:
        print(f'  [ERROR] {address}: {e}')
    return None, None


# ============================================================
# 메인
# ============================================================
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--key', default=None, help='Kakao REST API 키 (생략 시 config.py 사용)')
    args = parser.parse_args()
    if args.key is None:
        from config import KAKAO_API_KEY
        args.key = KAKAO_API_KEY

    # ── 1. CSV 병합 ──────────────────────────────────────────
    print('1. CSV 파일 병합 중...')
    dfs = []
    SKIP = {os.path.basename(OUTPUT_FILE), os.path.basename(FAILED_FILE)}
    for fname in sorted(f for f in os.listdir(DATA_DIR) if f.endswith('.csv') and f not in SKIP):
        df = read_csv(os.path.join(DATA_DIR, fname))
        dfs.append(df)
        print(f'   {fname}: {len(df)} 건')

    all_df = pd.concat(dfs, ignore_index=True)
    all_df.columns = all_df.columns.str.strip().str.replace('"', '')
    all_df = all_df.rename(columns=COLUMN_MAP)
    for col in all_df.select_dtypes(include='object').columns:
        all_df[col] = all_df[col].str.strip()
    print(f'   합계: {len(all_df)} 건\n')

    # ── 2. 지역 평균가 계산 (전체 28만 건 기준) ───────────────
    print('2. 지역 평균가 계산 중...')
    price_df = all_df.copy()
    price_df['deposit_num'] = pd.to_numeric(
        price_df['deposit'].str.replace(',', ''), errors='coerce'
    )
    parsed_all = price_df['sigungu_full'].apply(
        lambda x: pd.Series(parse_sigungu(x), index=['sido', 'gugun', 'dong'])
    )
    price_df = pd.concat([price_df.reset_index(drop=True), parsed_all], axis=1)

    regional_avg = (
        price_df
        .groupby(['gugun', 'dong', 'rent_type_raw'])['deposit_num']
        .mean()
        .reset_index()
        .rename(columns={'deposit_num': 'market_price_avg'})
    )
    print(f'   {len(regional_avg)} 개 지역-유형 조합 평균가 완료\n')

    # ── 3. 고유 매물 추출 (전월세 분리) ───────────────────────
    print('3. 고유 매물 추출 중...')
    all_df['address_key'] = (
        all_df['sigungu_full']  + '|' +
        all_df['bunji']         + '|' +
        all_df['building_name'] + '|' +
        all_df['rent_type_raw']
    )
    all_df = all_df.sort_values('deal_ym', ascending=False)
    unique_df = all_df.drop_duplicates(subset='address_key', keep='first').reset_index(drop=True)
    print(f'   고유 매물 수: {len(unique_df)} 건\n')

    # ── 4. 파생 컬럼 생성 ─────────────────────────────────────
    parsed = unique_df['sigungu_full'].apply(
        lambda x: pd.Series(parse_sigungu(x), index=['sido', 'gugun', 'dong'])
    )
    unique_df = pd.concat([unique_df, parsed], axis=1)

    unique_df['address']      = unique_df['sigungu_full'] + ' ' + unique_df['bunji']
    unique_df['road_address'] = unique_df.apply(
        lambda r: ' '.join(r['sigungu_full'].split()[:-1]) + ' ' + str(r['road_name'])
        if pd.notna(r.get('road_name')) and str(r.get('road_name', '')).strip()
        else None, axis=1
    )
    unique_df['title']      = unique_df['building_name']
    unique_df['rent_type']  = unique_df['rent_type_raw'].apply(to_rent_type)
    unique_df['room_type']  = unique_df['area'].apply(to_room_type)
    unique_df['floor_val']  = unique_df['floor'].apply(parse_floor)
    unique_df['build_year_val'] = unique_df['build_year'].apply(parse_year)

    # 지역 평균가 JOIN → 위험 라벨 계산
    unique_df = unique_df.merge(regional_avg, on=['gugun', 'dong', 'rent_type_raw'], how='left')
    unique_df['deposit_num'] = pd.to_numeric(
        unique_df['deposit'].str.replace(',', ''), errors='coerce'
    )
    unique_df['price_gap_rate'] = (
        (unique_df['market_price_avg'] - unique_df['deposit_num'])
        / unique_df['market_price_avg']
    ).round(4)
    unique_df['risk_label'] = unique_df['price_gap_rate'].apply(to_risk_label)

    # ── 5. Geocoding (재시작 지원) ────────────────────────────
    if os.path.exists(OUTPUT_FILE):
        done_df   = pd.read_csv(OUTPUT_FILE, dtype=str)
        done_keys = set(done_df['address_key'])
        print(f'   이전 결과 {len(done_keys)} 건 재사용')
    else:
        done_df   = pd.DataFrame()
        done_keys = set()

    todo_df = unique_df[~unique_df['address_key'].isin(done_keys)]
    total   = len(todo_df)
    print(f'4. Kakao Geocoding 시작 (남은 건수: {total})\n')

    results = []
    failed  = []

    for i, (_, row) in enumerate(todo_df.iterrows(), 1):
        lat, lng = geocode(row['address'], args.key)
        time.sleep(DELAY)

        if lat is None:
            failed.append(row['address'])

        deposit_val = str(row.get('deposit', '') or '').replace(',', '').strip() or None
        monthly_val = str(row.get('monthly_rent', '') or '').strip() or None
        if monthly_val == '0':
            monthly_val = None

        gap = row.get('price_gap_rate')
        gap = None if (gap is None or (isinstance(gap, float) and pd.isna(gap))) else float(gap)

        results.append({
            'address_key':      row['address_key'],
            'title':            row['title'],
            'address':          row['address'],
            'road_address':     row.get('road_address'),
            'sido':             row['sido'],
            'gugun':            row['gugun'],
            'dong':             row['dong'],
            'latitude':         lat,
            'longitude':        lng,
            'rent_type':        row['rent_type'],
            'room_type':        row['room_type'],
            'deposit':          deposit_val,
            'monthly_rent':     monthly_val,
            'area':             str(row.get('area', '') or '').strip() or None,
            'floor':            row.get('floor_val'),
            'build_year':       row.get('build_year_val'),
            'data_source':      'PUBLIC',
            'status':           'APPROVED',
            'deal_date':        format_date(row.get('deal_ym'), row.get('deal_day')),
            'market_price_avg': row.get('market_price_avg'),
            'price_gap_rate':   gap,
            'risk_label':       row.get('risk_label', 'UNKNOWN'),
        })

        if i % SAVE_EVERY == 0 or i == total:
            batch_df = pd.DataFrame(results)
            done_df  = pd.concat([done_df, batch_df], ignore_index=True) if not done_df.empty else batch_df
            done_df.to_csv(OUTPUT_FILE, index=False, encoding='utf-8-sig')
            results = []
            print(f'   [{i}/{total}] 저장 완료  (실패 누계: {len(failed)} 건)')

    if failed:
        pd.DataFrame({'address': failed}).to_csv(FAILED_FILE, index=False, encoding='utf-8-sig')
        print(f'\n실패 주소 {len(failed)} 건 -> {FAILED_FILE}')

    print(f'\n완료! 결과 파일: {OUTPUT_FILE}')
    print(f'총 {len(done_df)} 건 저장됨')


if __name__ == '__main__':
    main()
