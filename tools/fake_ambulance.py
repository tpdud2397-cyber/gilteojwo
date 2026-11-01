"""
시험용 가짜 구급차: 서버(Supabase)에 1초마다 구급차 위치를 보낸다.
송신 앱이 생기기 전에 수신 앱(R-2 수신, R-3 거리·방향, R-4 접근 판정)을 확인하는 용도다.

쓰는 법 (집 PC에서 Claude Code가 실행한다. 사용자는 바탕화면 '가짜 구급차' 아이콘):
    python tools/fake_ambulance.py 등록코드 [내차위도 내차경도] [시작거리m] [시속km] [오는쪽]

기본: 에뮬레이터 기본 위치(성남 시흥동)의 남쪽 1,200m에서 출발해 북쪽으로 시속 60km로 달려온다 (내 차 뒤에서 옴).
오는쪽: '뒤'(기본, 남→북), '앞'(북→남, 마주 옴), '왼쪽'(서→동), '오른쪽'(동→서). 왼쪽·오른쪽은 내 차 앞을 가로지른다 (교차로 측면 접근).
내 차를 지나 300m 더 간 뒤 긴급출동을 끝낸다 (서버에서 위치가 바로 지워진다).
등록 코드는 비밀이라 이 파일에 적지 않는다.
"""

import json
import math
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
    start_meters = float(sys.argv[4]) if len(sys.argv) > 4 else 1200.0  # 관심 단계(1,000m)부터 보이게 1,200m (11/01 사용자 요청)
    speed_kmh = float(sys.argv[5]) if len(sys.argv) > 5 else 60.0
    from_side = sys.argv[6] if len(sys.argv) > 6 else "뒤"
    # 오는 쪽마다 (북쪽으로 움직이는 부호, 동쪽으로 움직이는 부호, 진행 방향). 내 차는 북쪽을 보고 있다
    routes = {"뒤": (1, 0, 0), "앞": (-1, 0, 180), "왼쪽": (0, 1, 90), "오른쪽": (0, -1, 270)}
    north_sign, east_sign, heading = routes.get(from_side, routes["뒤"])
    meters_per_degree_lon = METERS_PER_DEGREE * math.cos(math.radians(my_lat))  # 경도 1도의 길이

    dispatch_id = call("start_dispatch", {"p_reg_code": reg_code})
    print(f"긴급출동 시작: {dispatch_id}", flush=True)

    along = -start_meters  # 달리는 길 위에서 내 차까지 남은 거리(m). 음수 = 아직 내 차에 못 옴
    next_send = time.time()
    try:
        while along <= 300:
            call("send_position", {
                "p_reg_code": reg_code,
                "p_dispatch_id": dispatch_id,
                "p_latitude": my_lat + north_sign * along / METERS_PER_DEGREE,
                "p_longitude": my_lon + east_sign * along / meters_per_degree_lon,
                "p_heading": heading,
                "p_speed_kmh": speed_kmh,
                "p_accuracy_m": 5,
            })
            if north_sign != 0:
                side = '북' if north_sign * along > 0 else '남'
            else:
                side = '동' if east_sign * along > 0 else '서'
            print(f"{time.strftime('%H:%M:%S')} 보냄: 내 차 기준 {side}쪽 {abs(along):.0f}m", flush=True)
            # 서버 응답에 걸린 시간과 상관없이 정확히 1초마다 보낸다
            # (그냥 1초 쉬면 응답 시간만큼 늦어져서 실제 속도가 느려지고 들쭉날쭉해진다)
            next_send += 1
            time.sleep(max(0.0, next_send - time.time()))
            along += speed_kmh / 3.6  # 1초 동안 간 거리
    finally:
        call("end_dispatch", {"p_reg_code": reg_code, "p_dispatch_id": dispatch_id, "p_end_type": "manual"})
        print("긴급출동 종료 (서버에서 위치 삭제)", flush=True)


if __name__ == "__main__":
    main()
