"""
시험용 가짜 구급차: 서버(Supabase)에 1초마다 구급차 위치를 보낸다.
송신 앱이 생기기 전에 수신 앱(R-2 수신, R-3 거리·방향, R-4 접근 판정)을 확인하는 용도다.

쓰는 법 (집 PC에서 Claude Code가 실행한다):
    python tools/fake_ambulance.py 등록코드 [내차위도 내차경도] [시작거리m] [시속km]

기본: 에뮬레이터 기본 위치(성남 시흥동)의 남쪽 600m에서 출발해 북쪽으로 시속 60km로 달려온다.
내 차를 지나 북쪽 300m까지 간 뒤 긴급출동을 끝낸다 (서버에서 위치가 바로 지워진다).
등록 코드는 비밀이라 이 파일에 적지 않는다.
"""

import json
import sys
import time
import urllib.request

SERVER = "https://fywibzrjqjdtntdhungh.supabase.co"
PUBLISHABLE_KEY = "sb_publishable_mgRw67mksqkDiKK5beWAgA_XVNQtqVP"
METERS_PER_DEGREE = 111_195.0  # 위도 1도의 길이


def call(function_name, arguments):
    """서버의 송신용 기능(start_dispatch, send_position, end_dispatch)을 부른다"""
    request = urllib.request.Request(
        f"{SERVER}/rest/v1/rpc/{function_name}",
        data=json.dumps(arguments).encode("utf-8"),
        headers={
            "apikey": PUBLISHABLE_KEY,
            "Authorization": f"Bearer {PUBLISHABLE_KEY}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        body = response.read().decode("utf-8")
        return json.loads(body) if body else None


def main():
    reg_code = sys.argv[1]
    my_lat = float(sys.argv[2]) if len(sys.argv) > 3 else 37.383537
    my_lon = float(sys.argv[3]) if len(sys.argv) > 3 else 126.934173
    start_meters = float(sys.argv[4]) if len(sys.argv) > 4 else 600.0
    speed_kmh = float(sys.argv[5]) if len(sys.argv) > 5 else 60.0

    dispatch_id = call("start_dispatch", {"p_reg_code": reg_code})
    print(f"긴급출동 시작: {dispatch_id}", flush=True)

    north = -start_meters  # 내 차 기준 남북 위치(m). 음수 = 남쪽
    try:
        while north <= 300:
            call("send_position", {
                "p_reg_code": reg_code,
                "p_dispatch_id": dispatch_id,
                "p_latitude": my_lat + north / METERS_PER_DEGREE,
                "p_longitude": my_lon,
                "p_heading": 0,  # 북쪽으로 달린다
                "p_speed_kmh": speed_kmh,
                "p_accuracy_m": 5,
            })
            print(f"{time.strftime('%H:%M:%S')} 보냄: 내 차 기준 {'남' if north < 0 else '북'}쪽 {abs(north):.0f}m", flush=True)
            time.sleep(1)
            north += speed_kmh / 3.6  # 1초 동안 간 거리
    finally:
        call("end_dispatch", {"p_reg_code": reg_code, "p_dispatch_id": dispatch_id, "p_end_type": "manual"})
        print("긴급출동 종료 (서버에서 위치 삭제)", flush=True)


if __name__ == "__main__":
    main()
