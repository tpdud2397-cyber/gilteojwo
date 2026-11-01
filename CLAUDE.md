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
- 사용자는 평일 07:30~16:30에 회사에 있고, 회사 PC는 파일 업로드가 막혀 있다. 빌드와 에뮬레이터 작업은 퇴근 후 이 집 PC에서 한다.
- 기획·사양 변경은 claude.ai의 "현대앱공모전" 프로젝트에서 한다. 구현하다 사양과 다르게 가야 하면 먼저 사용자에게 알리고, `docs/사양서.md` 수정은 사용자 확인 후에 한다.

## GitHub (중요)

- 저장소: https://github.com/tpdud2397-cyber/gilteojwo (계정 tpdud2397-cyber, 비공개 유지)
- 커밋 작성자는 항상 tpdud2397-cyber로 한다: 이름 `tpdud2397-cyber`, 이메일 `tpdud2397@gmail.com`. 저장소 로컬 설정으로 지정한다.
- 클라우드 세션(claude.ai/code, claude.ai 대화)은 Claude 계정에 연결된 chungyakgo-maker 계정을 통해 push한다(협업자 초대 + Claude GitHub 앱 설치 완료). 커밋 작성자만 위처럼 맞추면 된다.
- 로컬 세션(집 PC)에는 다른 GitHub 계정(chungyakgo-maker)을 쓰는 프로젝트도 있을 수 있다. 섞이지 않게 한다.
  - git 사용자 이름·이메일은 이 저장소에만 설정한다 (`git config --local`). 전역 설정은 건드리지 않는다.
  - 원격 주소에 계정 이름을 넣는다: `https://tpdud2397-cyber@github.com/tpdud2397-cyber/gilteojwo.git` (자격 증명이 계정별로 따로 저장된다).
  - 첫 push 때 브라우저 로그인 창이 뜨면 **tpdud2397-cyber**로 로그인하라고 사용자에게 안내한다.
- 커밋 메시지는 한국어로 짧게 쓴다.
- **main 브랜치에 바로 커밋하고 push한다** (사용자 결정, 2026-11-01). 별도 브랜치나 PR을 만들지 않는다.
- 비밀 정보는 커밋하지 않는다. Supabase service_role 키는 절대 금지. anon 키는 앱에 넣어도 되지만 RLS(테이블 접근 규칙)를 반드시 건다.
- 예외: 개발용 서명 키 `app/debug.keystore`는 시험판을 덮어 설치하려고 저장소에 넣었다 (사용자 동의, 비밀번호는 공개 기본값 "android"). 제출용 정식판 서명 키는 저장소에 넣지 않고 GitHub Secrets에 둔다.
- 심사용 계정은 주최 측 공지 후 Collaborator로 초대한다.

## 작업 위치

- **클라우드 세션**: 코드 작성 → push → GitHub Actions가 APK 빌드 → 사용자가 집 PC 에뮬레이터에 설치해 시험하고 결과(캡처·오류)를 알려준다. 클라우드에서는 에뮬레이터를 돌릴 수 없고 안드로이드 SDK도 받을 수 없어서(dl.google.com 차단), 빌드 확인은 GitHub Actions 결과로 한다. Pleos 문서와 Maven 저장소(document.pleos.ai, pleos.ai, nexus-playground.pleos.ai)는 클라우드 환경 네트워크 설정에서 허용해 두었다 (2026-10-31).
- **로컬 세션 (집 PC)**: 빌드·에뮬레이터 설치·로그 확인까지 바로 할 수 있다. 아래 환경 정보를 쓴다.

## 개발 환경 (집 PC)

- Windows 11, AMD Ryzen 7 5800X, RAM 32GB, RTX 5070. BIOS에서 SVM(가상화)을 켰고, Windows 하이퍼바이저 플랫폼도 켰다.
- Android Studio Rabbit 2 (2026.2.2). SDK 위치: `C:\Users\bsy92\AppData\Local\Android\Sdk`
- 집 PC 빌드: Android Studio에 들어 있는 Java(jbr)는 25라서 Gradle 8.11.1이 못 쓴다. GitHub Actions와 같은 Java 17(Temurin)을 `C:\Users\bsy92\.jdks\temurin-17`에 받아 두었다 (2026-11-01). JAVA_HOME을 여기로, ANDROID_HOME을 SDK 위치로 정하고 `./gradlew assembleCarDebug`. 집 PC 빌드는 화면에 R1(커밋 local)로 나오고 Releases 판보다 번호가 낮아서 `adb install -r -d`로 덮어 설치한다.
- 에뮬레이터: Pleos Connect v2.1.0 APIs (API 34, x86_64). AVD 이름은 `Pleos_Connect_v2.1.0`, 포트 5554. 권장 설정은 RAM 4GB, Cold boot.
- Virtual Console: 에뮬레이터를 켠 뒤 `adb forward tcp:8088 tcp:8088`을 실행하고, 브라우저에서 http://127.0.0.1:8088/ 을 연다. 사용자가 바탕화면 바로가기로 만들어 뒀을 수 있다. 필요하면 Claude Code가 직접 실행한다.
- 시연용 노트북은 LG 그램 15ZD980 (i5-8250U, RAM 8GB, 여유 공간 약 46GB)이다. 에뮬레이터를 직접 돌리기엔 버겁다. 대안은 크롬 원격 데스크톱으로 이 PC 화면을 띄우는 것이고, 시연 녹화 영상도 백업으로 준비한다.

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
- 주행 제한 강제 켜기/끄기 (집 PC에서 Claude Code가 실행): `adb shell cmd hmg_car_service --service drivingstate driving_regulation on` / `off`.
- FusedLocation: LocationData에 위도·경도·고도·진행 방향·신뢰도·시각이 있고 속도는 없다. isSupported()를 먼저 부른다. 안드로이드 표준 위치 권한의 동작은 보장하지 않는다고 문서에 적혀 있다.
- TTS: ON_DEVICE 모드는 앱 등록이 필요 없다.
- 서버: Supabase (아직 프로젝트를 만들지 않음). 테이블은 vehicles, dispatches, positions_live, yield_acks이고, 실시간 구독을 쓴다. 자세한 건 사양서 참고.
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

## 코드 원칙

- 사용자가 PT에서 설명할 수 있어야 한다. 파일과 함수는 작게, 이름은 하는 일 그대로 짓고, 핵심 로직에는 한국어 주석을 단다.
- 모듈은 다섯 개로 나눈다: 내 위치 / 긴급차량 수신 / 접근 판정 / 알림(화면·소리·조명) / 송신.
- `docs/설명노트.md`에 모듈별로 "무엇을, 왜" 했는지를 계속 업데이트한다 (PT 대비).
