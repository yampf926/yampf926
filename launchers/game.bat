@echo off
chcp 65001 > nul
title yampf926 - 보드게임 컬렉션
echo.
echo [보드게임 컬렉션]
echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 합니다.
echo.
if not exist "C:\Users\KOSMO\IdeaProjects\yampf926\src\Game" (
    echo 프로젝트 폴더를 찾지 못했습니다.
    echo C:\Users\KOSMO\IdeaProjects\yampf926\src\Game
    echo.
    echo 다른 기기에서 다운받은 배치파일은 실행할 수 없습니다.
    pause
    exit /b 1
)
where java > nul 2> nul
if errorlevel 1 (
    echo java를 찾지 못했습니다. JDK를 설치한 뒤 다시 실행하세요.
    pause
    exit /b 1
)
where javac > nul 2> nul
if errorlevel 1 (
    echo javac를 찾지 못했습니다. JRE가 아니라 JDK가 필요합니다.
    pause
    exit /b 1
)
cd /d "C:\Users\KOSMO\IdeaProjects\yampf926\src\Game"
echo 보드게임 컬렉션 컴파일 중...
if not exist out mkdir out
powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = @(Get-ChildItem -Recurse -Path 'src' -Filter '*.java' | ForEach-Object { $_.FullName }); if ($files.Count -eq 0) { Write-Host 'Java 파일을 찾지 못했습니다.'; exit 2 }; & javac -encoding UTF-8 -d out $files"
if errorlevel 1 (
    echo.
    echo 컴파일에 실패했습니다.
    pause
    exit /b 1
)
echo 보드게임 컬렉션 실행 중...
java -cp "out;src;assets;images;data" Main
if errorlevel 1 (
    echo.
    echo 실행에 실패했습니다. 위 오류 내용을 확인하세요.
)
pause
