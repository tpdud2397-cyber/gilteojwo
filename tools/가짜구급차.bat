@echo off
chcp 65001 > nul
rem 가짜 구급차: 바탕화면 '가짜 구급차' 아이콘을 두 번 눌러 실행한다 (명령어를 칠 필요 없음)
rem 등록 코드는 같은 폴더의 등록코드.txt에서 읽는다. 이 파일은 비밀이라 GitHub에 올리지 않는다 (.gitignore)
rem 뒤에 값을 붙이면 fake_ambulance.py에 그대로 넘긴다 (예: 내차위도 내차경도 시작거리m 시속km)

cd /d "%~dp0.."
set PYTHONIOENCODING=utf-8

if not exist "tools\등록코드.txt" (
    echo 등록코드.txt 파일이 없습니다. Claude Code에게 만들어 달라고 말해 주세요.
    pause
    exit /b 1
)
set /p REG_CODE=<"tools\등록코드.txt"

echo 가짜 구급차를 출발시킵니다. 에뮬레이터에서 길터줘 앱을 열어 두세요.
echo 1분 30초쯤 뒤에 저절로 끝납니다. 중간에 멈추려면 이 창을 닫으세요.
echo.
python tools\fake_ambulance.py %REG_CODE% %*
echo.
echo 끝났습니다. 아무 키나 누르면 창이 닫힙니다.
pause > nul
