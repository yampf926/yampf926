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
where java > nul 2> nul
if errorlevel 1 (
    echo java를 찾지 못함. JDK 설치 후 다시 실행하면 됨.
    pause
    exit /b 1
)
where javac > nul 2> nul
if errorlevel 1 (
    echo javac를 찾지 못함. JRE가 아니라 JDK가 필요함.
    pause
    exit /b 1
)
cd /d "C:\Users\KOSMO\IdeaProjects\yampf926\src\yutnori"
echo 윷놀이 컴파일 중...
if not exist out mkdir out
javac -encoding UTF-8 -d out head_Main.java
if errorlevel 1 (
    echo.
    echo 컴파일 실패함.
    pause
    exit /b 1
)
echo 윷놀이 실행 중...
java -cp "out;.;assets;images;data" Main
if errorlevel 1 (
    echo.
    echo 실행 실패함. 위 오류 내용 확인 필요.
)
pause
