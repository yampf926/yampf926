@echo off
chcp 65001 > nul
title yampf926 - Dohwa 웹사이트
echo.
echo [Dohwa 웹사이트]
echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 함.
echo.
if not exist "C:\Users\KOSMO\IdeaProjects\yampf926\src\Dohwa" (
    echo 프로젝트 폴더를 찾지 못함.
    echo C:\Users\KOSMO\IdeaProjects\yampf926\src\Dohwa
    echo.
    echo 다른 기기에서 다운받은 배치파일은 실행할 수 없음.
    pause
    exit /b 1
)
if not exist "C:\Users\KOSMO\IdeaProjects\yampf926\src\Dohwa\scripts\start-dev.ps1" (
    echo Dohwa 실행 스크립트를 찾지 못함.
    echo C:\Users\KOSMO\IdeaProjects\yampf926\src\Dohwa\scripts\start-dev.ps1
    pause
    exit /b 1
)
echo Dohwa 웹사이트 백엔드와 프론트엔드를 함께 실행함.
echo 이미 8080 또는 412 포트에 이전 Dohwa 서버가 있으면 정리한 뒤 다시 시작함.
echo 브라우저가 자동으로 열리지 않으면 http://localhost:412 으로 접속하면 됨.
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Users\KOSMO\IdeaProjects\yampf926\src\Dohwa\scripts\start-dev.ps1"
start "" "http://localhost:412"
if errorlevel 1 (
    echo.
    echo 실행 실패함. 위 오류 내용 확인 필요.
)
pause
