"""
동(dong) 단위 주변 시설 카운트 전처리 스크립트

동네 추천 기능에서 동 단위 시설 환경 비교에 사용.
center_lat/lng는 해당 동 소속 매물들의 위경도 평균값.

처리 순서:
  1. properties 테이블에서 동별 중심 좌표(AVG lat/lng) 계산
  2. 카카오 로컬 카테고리 검색 API로 동별 시설 수 조회
  3. area_facility_counts 테이블에 INSERT (이미 있으면 스킵)

API 호출 수: 고유 동 수 × 7 (매물 수와 무관)

사용법:
  pip install pymysql requests
  python add_facility_counts.py --password YOUR_PW
"""

import argparse
import time

import pymysql
import requests
from pymysql.cursors import DictCursor

from config import KAKAO_API_KEY

KAKAO_CATEGORY_URL = "https://dapi.kakao.com/v2/local/search/category.json"
HEADERS    = {"Authorization": f"KakaoAK {KAKAO_API_KEY}"}
DELAY      = 0.12   # 초당 최대 10 req (무료 키 기준)
BATCH_SIZE = 50

CATEGORIES = [
    ("SW8", "subway_count_500m",       500),
    ("MT1", "mart_count_1km",         1000),
    ("CS2", "convenience_count_500m",  500),
    ("HP8", "hospital_count_1km",     1000),
    ("PM9", "pharmacy_count_500m",     500),
    ("CE7", "cafe_count_500m",         500),
    ("FD6", "restaurant_count_500m",   500),
]


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host",     default="localhost")
    parser.add_argument("--port",     default=3306, type=int)
    parser.add_argument("--user",     default="root")
    parser.add_argument("--password", default="", help="MySQL 비밀번호")
    parser.add_argument("--db",       default="ssafy_home")
    return parser.parse_args()


def fetch_count(lat: float, lng: float, category_code: str, radius: int) -> int:
    params = {
        "category_group_code": category_code,
        "x": lng,
        "y": lat,
        "radius": radius,
        "size": 15,
    }
    try:
        resp = requests.get(KAKAO_CATEGORY_URL, headers=HEADERS, params=params, timeout=5)
        resp.raise_for_status()
        return resp.json().get("meta", {}).get("total_count", 0)
    except Exception as e:
        print(f"    API 오류 ({category_code}): {e}")
        return 0


def main():
    args = get_args()

    conn = pymysql.connect(
        host=args.host, port=args.port,
        user=args.user, password=args.password,
        db=args.db, charset="utf8mb4",
        cursorclass=DictCursor, autocommit=False,
    )

    try:
        # 1. 동별 중심좌표 계산 (API 없이 DB만으로)
        with conn.cursor() as cur:
            cur.execute("""
                SELECT
                    sido,
                    gugun,
                    dong,
                    AVG(latitude)  AS center_lat,
                    AVG(longitude) AS center_lng
                FROM properties
                WHERE latitude  IS NOT NULL
                  AND longitude IS NOT NULL
                GROUP BY sido, gugun, dong
            """)
            all_areas = cur.fetchall()

        # 2. 이미 처리된 동 제외 (재시작 지원)
        with conn.cursor() as cur:
            cur.execute("SELECT sido, gugun, dong FROM area_facility_counts")
            done = {(r["sido"], r["gugun"], r["dong"]) for r in cur.fetchall()}

        targets = [
            a for a in all_areas
            if (a["sido"], a["gugun"], a["dong"]) not in done
        ]

        total = len(targets)
        print(f"전체 동: {len(all_areas)}개 / 처리 대상: {total}개 (기완료: {len(done)}개)\n")

        processed = 0
        for area in targets:
            sido   = area["sido"]
            gugun  = area["gugun"]
            dong   = area["dong"]
            lat    = float(area["center_lat"])
            lng    = float(area["center_lng"])

            counts = {}
            for category_code, col, radius in CATEGORIES:
                counts[col] = fetch_count(lat, lng, category_code, radius)
                time.sleep(DELAY)

            col_names  = ", ".join(counts.keys())
            col_values = ", ".join(["%s"] * len(counts))

            with conn.cursor() as cur:
                cur.execute(f"""
                    INSERT INTO area_facility_counts
                        (sido, gugun, dong, center_lat, center_lng, {col_names})
                    VALUES
                        (%s, %s, %s, %s, %s, {col_values})
                """, [sido, gugun, dong, lat, lng] + list(counts.values()))

            processed += 1
            if processed % BATCH_SIZE == 0:
                conn.commit()
                print(f"  [{processed}/{total}] 완료 — 마지막: {sido} {gugun} {dong}")

        conn.commit()
        print(f"\n완료!  처리: {processed}개 동")

    except Exception as e:
        conn.rollback()
        raise e
    finally:
        conn.close()


if __name__ == "__main__":
    main()
