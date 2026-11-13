# 길터줘 (gilteojwo) — Claude Code 작업 안내

## 이 프로젝트

- 현대자동차그룹 "차량용 앱 개발 공모전"(Pleos Connect 앱)에 일반 부문, 개인으로 내는 작품이다.
- 길터줘: 긴급차량이 다가오면 Pleos 차량에서 화면·음성·실내 조명으로 알리고, 차량 상태(속도·브레이크·방향지시등·기어)에 맞는 양보 행동 하나를 안내한다. 일반 차량이 양보하면 차가 자동으로 감지해 긴급차량에 알려준다(양방향 "길 열림 확인").
- 전체 사양: `docs/사양서.md` (원본은 claude.ai 문서, 여기는 사본)
- 설명회·심사·제출 정보: `docs/설명회_정리.md`
- 일정:
  - 11/15 1차 접수 마감. 기획서는 필수, GitHub 저장소(소스 + APK)와 시연 녹화는 선택이지만 가산점이 있다.
  - 11/23 1차 결과, 12/3~4 2차 PT(현장에서 에뮬레이터로 직접 시연, PT 전까지 개발 완료), 12/18 최종 발표.
- 심사 기준: 차량특화성, 창의성, 기술 활용성(Pleos SDK를 핵심 기능에 쓰는지), 사업성.
- AI 도구 사용은 허용된다. 단, 2차 PT 기술 Q&A에서 사용자가 코드 구조와 로직을 직접 설명해야 한다. 그래서 코드는 단순하게 짠다.

## 사용자와 일하는 방식 (꼭 지킬 것)

- 존댓말로 답한다.
- **모든 글은 한국어로만 쓴다.** 최종 답뿐 아니라 도구 사이의 짧은 진행 설명, 요약 상자, 버튼, 사진 설명까지 전부 한국어다. 영어로 쓰면 사용자가 읽지 못한다 (2026-11-01 여러 번 지적받음).
- 사용자는 개발 초보다. 쉬운 말로 설명하고, 전문 용어는 풀어서 쓴다.
- 코드 작성·수정은 사용자가 요청할 때만 한다. 큰 작업은 계획을 먼저 보여주고 확인받은 뒤 진행한다.
- 사용자는 명령어를 직접 입력하지 않는다. 필요한 명령은 Claude Code가 실행하고, 무엇을 왜 실행하는지 한 줄로 알린다. 사용자에게 PowerShell이나 스크립트를 실행하라고 안내하지 않는다.
- SQL(Supabase)은 파일이 아니라 채팅에 코드 블록으로 준다. 사용자가 Supabase SQL 편집기에 붙여 실행한다.
- 빌드와 에뮬레이터 작업은 집 PC에서 한다. 사용자가 언제 집 PC를 쓸 수 있는지는 `CLAUDE.local.md`에 있다.
- **저장소가 공개라서 이 파일에는 개인 정보를 쓰지 않는다** (근무 시간, 집 PC·노트북 사양, 사용자 폴더 경로, 다른 GitHub 계정 이름 등). 그런 내용은 집 PC에만 있는 `CLAUDE.local.md`(GitHub 제외)에 쓴다 (사용자 결정 11/13).
- 기획·사양 변경은 claude.ai의 "현대앱공모전" 프로젝트에서 한다. 구현하다 사양과 다르게 가야 하면 먼저 사용자에게 알리고, 사양서 수정(원본 문서와 `docs/사양서.md`)은 사용자 확인 후에 한다.

## GitHub (중요)

- 저장소: https://github.com/tpdud2397-cyber/gilteojwo (계정 tpdud2397-cyber). **공개(Public)** (11/13 전환, 1차 발표일까지 유지). 이전 커밋 기록에는 공개 전에 쓴 개인 정보가 남아 있다 (사용자가 알고 결정함).
- 커밋 작성자는 항상 tpdud2397-cyber로 한다: 이름 `tpdud2397-cyber`, 이메일 `tpdud2397@gmail.com`. 저장소 로컬 설정으로 지정한다.
- 클라우드 세션(claude.ai/code, claude.ai 대화)은 Claude 계정에 연결된 협업자 계정을 통해 push한다(협업자 초대 + Claude GitHub 앱 설치 완료, 계정 이름은 `CLAUDE.local.md`). 커밋 작성자만 위처럼 맞추면 된다.
- 로컬 세션(집 PC)에는 다른 GitHub 계정을 쓰는 프로젝트도 있을 수 있다. 섞이지 않게 한다.
  - git 사용자 이름·이메일은 이 저장소에만 설정한다 (`git config --local`). 전역 설정은 건드리지 않는다.
  - 원격 주소에 계정 이름을 넣는다: `https://tpdud2397-cyber@github.com/tpdud2397-cyber/gilteojwo.git` (자격 증명이 계정별로 따로 저장된다).
  - 첫 push 때 브라우저 로그인 창이 뜨면 **tpdud2397-cyber**로 로그인하라고 사용자에게 안내한다.
- 커밋 메시지는 한국어로 짧게 쓴다.
- **main 브랜치에 바로 커밋하고 push한다** (사용자 결정, 2026-11-01). 별도 브랜치나 PR을 만들지 않는다.
- 비밀 정보는 커밋하지 않는다. Supabase service_role 키는 절대 금지. anon 키는 앱에 넣어도 되지만 RLS(테이블 접근 규칙)를 반드시 건다.
- 예외: 개발용 서명 키 `app/debug.keystore`는 시험판을 덮어 설치하려고 저장소에 넣었다 (사용자 동의, 비밀번호는 공개 기본값 "android"). 제출용 정식판 서명 키는 저장소에 넣지 않고 GitHub Secrets에 둔다.
- 1차 제출 (운영사무국 메일 11/12, 기록은 `docs/설명회_정리.md`):
  - 심사는 사용자가 메일로 회신한 **최종 커밋 번호**의 코드와 APK로 한다. 접수 마감(11/15 23:59) 뒤에 고친 내용은 심사하지 않는다.
  - 마감 뒤 11/16(월)까지 저장소를 공개(Public)로 바꾸고 1차 발표일까지 유지해야 개발 산출물 가산점이 붙는다 (11/13 공개 완료). 공개하지 않으면 가산점만 빠지고 접수는 유효하다. 대신 소스+APK 압축 파일을 마감 전에 메일로 내도 가산점이 같다.
  - 심사용 GitHub 계정은 따로 없다. Collaborator 초대는 필요 없다.
  - 수정 제출은 작품마다 메일을 따로 보내고 제목에 작품명을 적는다.
  - 제출본 APK: GitHub Actions의 'APK 빌드'를 손으로 실행하며 `submit_tag`에 `v1.0`을 넣으면(또는 `v`로 시작하는 태그를 올리면) 그 커밋으로 빌드해 Releases에 "제출본 v1.0"으로 고정한다 ("개발 최신판"과 달리 덮어쓰지 않는다). README의 실행 방법이 이 릴리즈를 가리킨다. 클라우드 세션에서는 태그 push가 끊겨서(11/13) 손 실행 방식을 쓴다. 1차 제출본은 **v1.1**이다 (v1.0은 개인 정보 정리 전 판이라 쓰지 않는다).
  - 시연(송신)에는 등록 코드가 필요하다. README에는 "심사용 등록 코드는 접수 메일로 따로 보낸다"고 적었다. 코드는 저장소에 적지 않는다.
  - **회신 완료 (11/13)**: 참가 부문(일반), 최종 커밋 `30b11b675809f2ebe9f7858f6ad5fcfb132bf31e`(제출본 v1.1, R31), 심사용 등록 코드(시험 차량 TEST-AMB-1의 코드)를 사무국에 보냈다. 1차 발표일까지 저장소 공개와 Releases의 "제출본 v1.1"을 그대로 둔다. 심사가 끝나면 등록 코드를 바꾼다.

## 작업 위치

- **클라우드 세션**: 코드 작성 → push → GitHub Actions가 APK 빌드 → 사용자가 집 PC 에뮬레이터에 설치해 시험하고 결과(캡처·오류)를 알려준다. 클라우드에서는 에뮬레이터를 돌릴 수 없고 안드로이드 SDK도 받을 수 없어서(dl.google.com 차단), 빌드 확인은 GitHub Actions 결과로 한다. Pleos 문서와 Maven 저장소(document.pleos.ai, pleos.ai, nexus-playground.pleos.ai)는 클라우드 환경 네트워크 설정에서 허용해 두었다 (2026-10-31).
- **로컬 세션 (집 PC)**: 빌드·에뮬레이터 설치·로그 확인까지 바로 할 수 있다. 아래 환경 정보와 `CLAUDE.local.md`를 쓴다.
  - 집 PC에 `CLAUDE.local.md`가 없으면 먼저 만든다. 11/13 이전 커밋의 CLAUDE.md에서 개인 정보 부분(근무 시간, 집 PC 사양과 SDK·JDK 경로, 협업자 계정 이름, 시연용 노트북 사양)을 옮긴다. `.gitignore`에 들어 있어 GitHub에 올라가지 않는다.

## 개발 환경 (집 PC)

- Windows 11. 하드웨어 사양과 가상화 설정은 `CLAUDE.local.md`.
- Android Studio Rabbit 2 (2026.2.2). SDK 위치는 `CLAUDE.local.md`.
- 집 PC 빌드: Android Studio에 들어 있는 Java(jbr)는 25라서 Gradle 8.11.1이 못 쓴다. GitHub Actions와 같은 Java 17(Temurin)을 따로 받아 두었다 (2026-11-01, 경로는 `CLAUDE.local.md`). JAVA_HOME을 여기로, ANDROID_HOME을 SDK 위치로 정하고 `./gradlew assembleCarDebug`. 앱 버전은 항상 R번호로 보인다 (사용자 요청 11/01). GitHub 빌드는 끝에 `R번호` 태그를 남기고, 집 PC 빌드는 화면에 "R15 · PC 11/01 13:13"처럼 '가장 큰 R태그 + 1'(다음에 올릴 번호)로 나온다. 그래서 PC 빌드 전에 `git fetch --tags`를 먼저 한다. 설치는 `adb install -r -d`. 계산 시험은 `./gradlew testCarDebugUnitTest`.
- Git Bash에서 `adb shell` 뒤에 `/sdcard/...` 같은 경로를 쓰면 윈도우 경로로 바뀌어 버린다. 명령 앞에 `MSYS_NO_PATHCONV=1`을 둔다.
- 에뮬레이터: Pleos Connect v2.1.0 APIs (API 34, x86_64). AVD 이름은 `Pleos_Connect_v2.1.0`, 포트 5554. 권장 설정은 RAM 4GB, Cold boot.
- Virtual Console: 에뮬레이터를 켠 뒤 `adb forward tcp:8088 tcp:8088`을 실행하고, 브라우저에서 http://127.0.0.1:8088/ 을 연다. 사용자가 바탕화면 바로가기로 만들어 뒀을 수 있다. 필요하면 Claude Code가 직접 실행한다.
- 시연용 노트북(사양은 `CLAUDE.local.md`)은 에뮬레이터를 직접 돌리기엔 버겁다. 대안은 크롬 원격 데스크톱으로 이 PC 화면을 띄우는 것이고, 시연 녹화 영상도 백업으로 준비한다.

## 기술 스택과 SDK

- Kotlin, minSdk 28, Gradle 8.0 이상.
- Pleos SDK 문서: https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/
- Maven 저장소: https://nexus-playground.pleos.ai/repository/maven-releases/
- 확인된 의존성 (2026-10-31, Maven 저장소에서 직접 확인): `ai.pleos.playground:Vehicle:2.3.2`, `ai.pleos.playground:FusedLocation:1.1.3`, `ai.pleos.playground:TextToSpeech:3.3.1`, `ai.pleos.playground:NaviHelper:3.0.5`, `ai.pleos.playground:SpeechToText:3.3.1`.
  - FusedLocation은 문서에 1.1.2로 적혀 있지만 저장소에는 1.0.4와 1.1.3만 있다. 1.1.3을 쓴다.
  - Vehicle·FusedLocation은 저장소에 소스(-sources.jar)가 있어서 정확한 함수 이름을 확인할 수 있다.
- 권한: 개발 중에는 매니페스트에 선언만 하면 쓸 수 있다(설명회에서 확인). 이름은 `pleos.car.permission.{상수 이름}` 형식이다 (SDK 소스의 PleosPermission).
  - `FUSED_LOCATION`: dangerous 등급이라 앱 실행 중에 사용자 허락도 받아야 한다.
  - `CAR_DRIVE`(브레이크 등), `CAR_LIGHTS`(라이트·방향지시등·앰비언트): normal 등급. 앰비언트 밝기·색·켜기 **설정은 `CONTROL_CAR_LIGHTS`가 필요하다** (SDK 설명서의 @RequiresPermission은 CAR_LIGHTS라고 적혀 있지만, 실제 구현 LightImpl은 CONTROL_CAR_LIGHTS를 검사한다. R4 에뮬레이터에서 "need Permission"으로 확인).
  - `TTS_SERVICE`: Pleos TTS.
  - 안드로이드 표준: `android.car.permission.CAR_SPEED`(속도, dangerous), `CAR_POWERTRAIN`(기어).
- Vehicle SDK: 결과는 비동기 콜백(onComplete/onFailed)으로 온다. initialize()와 release()를 쓰고, checkXXXCapabilities로 지원 여부와 범위를 먼저 확인한다.
- Light 도메인: setAmbientLightBrightness, setAmbientLightBrightnessColor(색은 32비트 ARGB 숫자), setAmbientLightSwitch. 구역은 SeatArea(좌석 영역) 단위이고 checkAmbientLight...Capabilities로 지원 구역을 받는다.
- 속도·기어는 안드로이드 표준 차량 속성으로 읽는다: 속도 `PERF_VEHICLE_SPEED`(Pleos FAQ: 계기판과 같은 값), 기어 `GEAR_SELECTION`(Pleos의 Gear API는 폐지 예정). 브레이크 페달·방향지시등은 Vehicle SDK(Brake, TurnSignal).
- 주행 제한 강제 켜기/끄기 (Pleos ADB 안내 문서): `adb shell cmd hmg_car_service --service drivingstate driving_regulation on` / `off`. 단, 지금 에뮬레이터에서는 "Can't find service: hmg_car_service"로 안 된다 (11/01). Virtual Console에서 기어를 D로 바꾸면 주행 제한이 걸린다.
- FusedLocation: LocationData에 위도·경도·고도·진행 방향·신뢰도·시각이 있고 속도는 없다. isSupported()를 먼저 부른다. 안드로이드 표준 위치 권한의 동작은 보장하지 않는다고 문서에 적혀 있다.
- TTS: ON_DEVICE 모드는 앱 등록이 필요 없다.
- 서버: Supabase 프로젝트 `gilteojwo` (2026-11-01 생성, 도쿄 ap-northeast-1, 주소 https://fywibzrjqjdtntdhungh.supabase.co). 공개 키(publishable)는 `receive/ServerConfig.kt`에 있다. 표는 vehicles, dispatches, positions_live, yield_acks와 목록 positions_active, 송신 기능 start_dispatch·send_position·end_dispatch·check_vehicle(등록 확인)·dispatch_status(알림·양보 수, 11/01 추가). 구조는 `docs/설명노트.md`의 '서버'. 무료 요금제라 일주일 안 쓰면 멈춘다. 그래서 GitHub Actions '서버 깨우기'(`.github/workflows/keepalive.yml`)가 매일 오전 9시 23분에 긴급차량 목록을 한 번 읽는다 (사용자 요청 11/13). 이 작업이 실패(빨간 X)하면 서버가 멈춘 것이니 Supabase 화면에서 다시 켠다.
  - 시험용 구급차 `TEST-AMB-1`이 등록돼 있다. 등록 코드는 저장소에 적지 않는다 (집 PC의 Claude 기억에 있음). 가짜 구급차: `python tools/fake_ambulance.py 등록코드`, 앱 계산 기록: `adb logcat -s Gilteojwo`.
  - 사용자는 바탕화면 '가짜 구급차' 아이콘으로 직접 실행한다 (`tools/가짜구급차.bat`이 `tools/등록코드.txt`를 읽음. 이 코드 파일은 GitHub 제외라 저장소를 새로 받으면 다시 만든다).
- 앱 구조: 한 프로젝트에서 APK 2개를 만든다 (product flavor). 폰용은 송신, 차량용은 수신 + Pleos 송신 모드다. 폰용에는 Pleos SDK와 차량 전용 라이브러리를 넣지 않는다.
- APK 배포: main에 push하면 GitHub Actions(`.github/workflows/build.yml`)가 빌드해 Releases의 "개발 최신판"(태그 `dev`)을 새 파일로 바꾼다. 파일 이름 뒤에 _R번호를 붙인다: `gilteojwo-car_R5.apk`(차량용), `gilteojwo-phone_R5.apk`(폰용). R번호는 GitHub 빌드 번호이고 새 빌드마다 하나씩 올라간다 (사용자 요청, R5부터). 앱 화면 맨 위와 Releases 제목에도 같은 R번호가 보인다. 노트북과 폰은 Releases에서 받아 설치한다.

## 지금까지 확인된 사실 (2026-10-31 기준)

- 에뮬레이터가 정상 부팅된다. 내비 위치는 "수정구 시흥동"(성남)으로 고정돼 있고 지도는 비어 있다.
- Virtual Console이 동작한다. 화면 안내로는 기어 D에서 ACCEL을 누르고 있으면 속도가 오른다.
- Virtual Console의 앰비언트는 켜기/끄기/자동과 색 5개(하늘·초록·주황·분홍·흰색)뿐이다. 좌우 구역 구분이 없고 빨강도 없다.
- 예전에 "기어 D 안 됨"이었던 건 Virtual Console 연결이 끊겨 있었기 때문이다. 다시 연결(DDS ONLINE)하면 기어·속도·브레이크·방향지시등·앰비언트가 모두 차에 반영되고, 위치는 `adb emu geo fix 경도 위도`로 바뀐다 (2026-11-01 확인).
- Virtual Console의 속도는 화면(브라우저)이 직접 계산해 보낸다. 페이지가 열려 있는 동안은 페이지가 가진 페달 값으로 속도를 계속 덮어쓴다.

## 첫 작업 (사용자가 요청하면)

0. **(로컬에서 처음 한 번) 저장소 받기**: 집 PC에 tpdud2397-cyber/gilteojwo를 받아 위 GitHub 규칙대로 설정한다.
1. **시험용 앱 (차량용)**: 사양을 확정하기 위해 아래를 확인한다.
   - FusedLocation 값이 에뮬레이터의 Location 변경을 따라가는지 (화면에 숫자로 표시)
   - 기어 D나 속도가 있을 때 앱 화면이 막히는지 (CarUxRestrictions, isActivityDistractionOptimized)
   - Light의 Capabilities: 구역별 제어가 되는지, 어떤 색을 지원하는지 (빨강 가능?)
   - TTS 한국어 ON_DEVICE 발화
   - 알림음을 좌우(스테레오)로 나눠 재생
   - 내비가 떠 있을 때 우리 알림이 위에 보이는지
   - Virtual Console에서 바꾼 브레이크·방향지시등·기어·속도를 Vehicle SDK로 읽을 수 있는지
   - 진행 상황 (2026-11-01): 시험 앱 완성, 1차(10/31)·2차(11/01) 시험 완료. 코드는 `app/src/car/java/com/gilteojwo/sdktest/`, 결과와 남은 시험은 `docs/시험결과.md`.
2. 결과를 `docs/시험결과.md`에 적고 사용자에게 요약한다.
3. 그다음 본 개발을 한다. 수신은 R-1~R-4 → R-6·R-7·R-12 → R-10·R-15·R-16 → R-11·R-13·R-14·R-17 순서, 송신은 S-1~S-8.
   - 진행 상황 (2026-11-01): R-1(내 위치), R-2(서버 실시간 수신), R-3(거리·방향·도착 시간), R-4(접근 판정)와 대기 화면 완료. 구조는 `docs/설명노트.md`.
   - 다음 단계 계획 (2026-11-01 사용자 확인): 1묶음 도착 시간 고치기 + 내 차 상태(R-5~R-8) → 2묶음 알림 단계·양보 문구 계산 + 8방향(시계 방향, 대각선은 두 면) → 3묶음 알림음·음성(R-12) + 앰비언트(R-15) → 4묶음 알림 화면(R-10, 레이더) + 내비 위 알림(상단 카드 + 지도 칸 가장자리 빛, 멀면 흐리고 가까우면 진하게, 200m 안 깜빡임). 왼쪽 주행 화면(Driving view)은 건드리지 않는다.
   - 원본 사양서(Claude Docs 문서)는 Claude가 Docs 도구로 직접 고칠 수 있다 (사용자 허락 11/01). 고친 뒤 `docs/사양서.md` 사본에도 같은 내용을 반영한다 (내보내기를 쓰면 '시스템 구성' 그림이 빠지므로 직접 고친다). 11/01에 8방향, 레이더·가장자리 빛, 내비 위 알림 둘 다, 앰비언트 구역 1개, 시험으로 확인된 항목을 반영했다.
   - 1묶음 완료 (2026-11-01): 도착 시간은 서버 기록 시각(updated_at)으로 계산하고 접근 중일 때만 낸다. 내 차 상태 읽기(`vehiclestate`)와 대기 화면 표시. 에뮬레이터·Virtual Console로 확인함.
   - 2묶음 완료 (2026-11-01): 8방향(`approach/RelativeDirection`), 알림 단계(`alert/AlertStageJudge`), 양보 문구(`alert/YieldGuide`), 대기 화면에 단계·문구 한 줄. 계산 시험 57개. 가짜 구급차(뒤·왼쪽에서)로 관심→경고→양보→해제와 문구 4가지를 확인함. 
   - 사용자 강조 (11/01): **앱을 켜지 않아도 백그라운드에서 돌고, 기본 내비 화면을 보는 중에 화면·소리로 알리는 것이 핵심이다.** 그래서 3묶음 순서를 바꿨다.
   - 3묶음 완료 (2026-11-01): 수신 서비스(`service/ReceiveService`, 포그라운드 서비스, 부팅·새 판 설치 때 자동 시작), 내비 위 상단 카드(`alert/OverNaviNotifier`)와 내비 칸 가장자리 빛(`alert/EdgeGlowOverlay`), 알림음(`alert/SoundPlayer`)·음성(`alert/VoiceGuide`). 대기 화면은 서비스 결과를 보여주기만 한다. 계산 시험 70개. 내비를 앞에 띄운 채 가짜 구급차로 확인함. 새로 설치하면 '다른 앱 위에 표시'가 꺼질 수 있어 `adb shell appops set --user 10 com.gilteojwo.car SYSTEM_ALERT_WINDOW allow`로 다시 켠다. 앱을 강제 종료하면 자동 시작이 막히니 한 번 연다. 부팅 자동 시작은 아직 미확인.
   - 가장자리 빛 (11/01 사용자 요청): 반타원 그라데이션, 멀면 내비 칸의 5%로 얇고 연하게 → 가까우면 20%까지 크고 진하게. 상하좌우 모두 차 위치에 따라.
   - 4묶음 완료 (2026-11-01): 앰비언트 조명(`alert/AmbientLight`, 관심 주황·경고 빨강·양보 빨강 깜빡임·해제 시 처음 값으로), 음악 소리 줄이기(`alert/MusicDucker`, 경고·양보 동안), 길터줘 화면(대기 / 레이더 접근 알림 / 큰 그림 양보 안내 / 해제). 계산 시험 80개. 조명은 Virtual Console Ambient 칸으로 확인함.
   - 수신 기능 마무리 (2026-11-01): R-14 끊김 추정(`receive/DeadReckoning`, 5초 넘게 끊기면 해제 + "긴급차량 정보가 끊겼습니다. 사이렌에 주의하세요"), R-16 양보 감지·전송(`alert/YieldDetector`, `receive/YieldAckSender` → yield_acks), R-17 양보 리포트(`report/`, 차 안에만 저장). 위치가 5초 넘게 안 오면 다시 연결(`MyLocationTracker`). 계산 시험 95개. 이것으로 수신 R-1~R-17 전부 구현.
   - 송신 1묶음 완료 (2026-11-01): 폰 앱 등록(S-1)·긴급출동 버튼(S-2)·1초 위치 송신(S-3)·백그라운드 송신(S-5)·송신 상태 경고(S-6). 서버 부르기는 공용 `send/`, 폰 화면·서비스는 `app/src/phone/.../sender/`. 폰 앱을 같은 에뮬레이터에 깔아 확인함(설치 후 `pm grant --user 10 com.gilteojwo.phone android.permission.ACCESS_FINE_LOCATION`). 계산 시험 101개.
   - 송신 2묶음 완료 (2026-11-01): S-4 시연 모드(`send/DemoRoute`, 후방 접근 1,200m·교차로 측면 접근 800m), S-7 출동 확인(`send/DispatchCheck`, 60분 뒤 묻고 2분 안에 답 없으면 자동 종료), S-8 '알림 N대 · 양보 N대'(3초마다 dispatch_status). 양보 기록은 출동 번호 기준으로 고침. 계산 시험 111개. 출동 확인 시험은 폰 화면을 `--ei check_after_seconds 10 --ei answer_seconds 60`으로 열면 짧게 볼 수 있다.
   - 송신 3묶음 완료 (2026-11-01): 송신 화면·서비스를 공용 `main/.../sender/`로 옮기고 위치 얻기만 `SenderLocation`(폰 = GPS, 차 = 수신 서비스의 Pleos 위치·계기판 속도)으로 나눔. 차량 앱 설정 화면(`screen/SettingsActivity`: 알림 거리 700/1,000/1,500m, 송신 모드 켜기), 수신 화면에 '설정'·'송신 화면' 버튼. 송신 모드에서도 수신은 계속 돌고, 실제 출동 중에는 내 출동을 수신 목록에서 뺀다(`receive/OwnDispatchFilter`, 시연은 빼지 않아 차량 앱 한 대로 시연 가능). 송신 알림창 번호는 11·12번(수신은 1번). 계산 시험 120개. 차량 앱에 등록하려면 송신 화면에서 등록 코드를 넣는다(폰 앱과 저장소가 따로).
   - Virtual Console 창이 숨어 있으면 BRAKE 페달 값이 차로 가지 않는다. 'Emulation Controls'의 Brake Pedal 칸(input range)을 바꾸면 반영된다 (11/01).
   - 수신 마무리 2 완료 (2026-11-01): R-9 주행 중 화면 단순화(`VehicleState.isDriving` = CarUxRestrictions 주행 제한, 못 읽으면 기어·속도 / `screen/DrivingView` 그림 하나 + 한 줄, 주행 중 버튼·시험용 정보 숨김), R-17 양보 리포트 화면(`screen/ReportActivity`, 기록 = 날짜·시각·차종 한 줄씩 `report/YieldTally`). 계산 시험 129개. 주행 제한이 바뀌면 기록에 "주행 제한: 걸림/풀림".
   - 다음: 제출판 정리(시험용 정보 숨기기, 정식판 서명 키), 제출 준비(11/15: 기획서, README, 시연 영상), 부팅 자동 시작 확인.
   - 시험으로 송신을 강제로 끊으면(timeout) end_dispatch가 안 불려 출동이 열린 채 남는다. 시험 뒤 그 출동 번호로 end_dispatch를 불러 정리한다.

## 코드 원칙

- 사용자가 PT에서 설명할 수 있어야 한다. 파일과 함수는 작게, 이름은 하는 일 그대로 짓고, 핵심 로직에는 한국어 주석을 단다.
- 모듈은 다섯 개로 나눈다: 내 위치 / 긴급차량 수신 / 접근 판정 / 알림(화면·소리·조명) / 송신.
- `docs/설명노트.md`에 모듈별로 "무엇을, 왜" 했는지를 계속 업데이트한다 (PT 대비).
