"""
processed_properties.csv → MySQL INSERT
테이블: properties, property_risk_scores

사용법:
  pip install pandas pymysql
  python insert_to_db.py --password YOUR_PW
"""

import os
import argparse
import pandas as pd
import pymysql
from pymysql.cursors import DictCursor

DATA_DIR   = os.path.join(os.path.dirname(__file__), '..', 'data')
INPUT_FILE = os.path.join(DATA_DIR, 'processed_properties.csv')
BATCH_SIZE = 500


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host',     default='localhost')
    parser.add_argument('--port',     default=3306, type=int)
    parser.add_argument('--user',     default='root')
    parser.add_argument('--password', default='', help='MySQL 비밀번호')
    parser.add_argument('--db',       default='ssafy_home')
    return parser.parse_args()


def to_int(val):
    try:
        v = str(val).replace(',', '').strip()
        return int(float(v)) if v and v.lower() != 'nan' else None
    except Exception:
        return None


def to_float(val):
    try:
        v = str(val).strip()
        return float(v) if v and v.lower() != 'nan' else None
    except Exception:
        return None


def to_str(val):
    v = str(val).strip() if val is not None else None
    return None if (not v or v.lower() == 'nan') else v


def main():
    args = get_args()

    print(f'CSV 읽는 중: {INPUT_FILE}')
    df = pd.read_csv(INPUT_FILE, dtype=str)
    print(f'총 {len(df)} 건\n')

    conn = pymysql.connect(
        host=args.host, port=args.port,
        user=args.user, password=args.password,
        db=args.db, charset='utf8mb4',
        cursorclass=DictCursor, autocommit=False,
    )

    try:
        # 재시작 지원: 이미 삽입된 (address, rent_type) 조합 로드
        with conn.cursor() as cur:
            cur.execute(
                "SELECT address, rent_type FROM properties WHERE data_source = 'PUBLIC'"
            )
            existing = {(r['address'], r['rent_type']) for r in cur.fetchall()}
        print(f'이미 삽입된 건수: {len(existing)} 건 (스킵)\n')

        inserted = 0
        skipped  = 0

        for _, row in df.iterrows():
            address   = to_str(row.get('address'))
            rent_type = to_str(row.get('rent_type'))

            if (address, rent_type) in existing:
                skipped += 1
                continue

            with conn.cursor() as cur:
                cur.execute("""
                    INSERT INTO properties
                        (owner_id, title, address, road_address,
                         sido, gugun, dong,
                         latitude, longitude,
                         rent_type, room_type,
                         deposit, monthly_rent, area,
                         floor, build_year,
                         data_source, deal_date, status)
                    VALUES
                        (NULL, %s, %s, %s,
                         %s, %s, %s,
                         %s, %s,
                         %s, %s,
                         %s, %s, %s,
                         %s, %s,
                         %s, %s, %s)
                """, (
                    to_str(row.get('title')),
                    address,
                    to_str(row.get('road_address')),
                    to_str(row.get('sido')),
                    to_str(row.get('gugun')),
                    to_str(row.get('dong')),
                    to_float(row.get('latitude')),
                    to_float(row.get('longitude')),
                    rent_type,
                    to_str(row.get('room_type')),
                    to_int(row.get('deposit')),
                    to_int(row.get('monthly_rent')),
                    to_float(row.get('area')),
                    to_int(row.get('floor')),
                    to_int(row.get('build_year')),
                    to_str(row.get('data_source')) or 'PUBLIC',
                    to_str(row.get('deal_date')),
                    to_str(row.get('status')) or 'APPROVED',
                ))
                property_id = cur.lastrowid

                cur.execute("""
                    INSERT INTO property_risk_scores
                        (property_id, risk_label, risk_score,
                         market_price_avg, price_gap_rate, report_count)
                    VALUES (%s, %s, 0, %s, %s, 0)
                """, (
                    property_id,
                    to_str(row.get('risk_label')) or 'UNKNOWN',
                    to_float(row.get('market_price_avg')),
                    to_float(row.get('price_gap_rate')),
                ))

            inserted += 1

            if inserted % BATCH_SIZE == 0:
                conn.commit()
                print(f'  [{inserted}건 삽입] (스킵: {skipped}건)')

        conn.commit()
        print(f'\n완료!  삽입: {inserted}건  스킵: {skipped}건')

    except Exception as e:
        conn.rollback()
        raise e
    finally:
        conn.close()


if __name__ == '__main__':
    main()
