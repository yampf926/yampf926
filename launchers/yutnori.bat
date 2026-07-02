@echo off
chcp 65001 > nul
title yampf926 - 윷놀이
echo.
echo [윷놀이]
echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 함.
echo.
if not exist "C:\Users\KOSMO\IdeaProjects\yampf926\src\yutnori" (
    echo 프로젝트 폴더를 찾지 못함.
    echo C:\Users\KOSMO\IdeaProjects\yampf926\src\yutnori
    echo.
    echo 다른 기기에서 다운받은 배치파일은 실행할 수 없음.
    pause
    exit /b 1
)
if not exist "C:\Users\KOSMO\IdeaProjects\yampf926\src\yutnori\run.bat" (
    echo 윷놀이 실행 스크립트를 찾지 못함.
    echo C:\Users\KOSMO\IdeaProjects\yampf926\src\yutnori\run.bat
    pause
    exit /b 1
)
cd /d "C:\Users\KOSMO\IdeaProjects\yampf926\src\yutnori"
echo 개별 실행과 같은 run.bat으로 윷놀이를 실행함.
call "C:\Users\KOSMO\IdeaProjects\yampf926\src\yutnori\run.bat"
if errorlevel 1 (
    echo.
    echo 실행 실패함. 위 오류 내용 확인 필요.
)
pause
