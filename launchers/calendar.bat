@echo off
chcp 65001 > nul
title yampf926 - 캘린더/가계부
echo.
echo [캘린더/가계부]
echo 이 파일은 yampf926 프로젝트가 있는 PC에서 실행해야 함.
echo.
if not exist "C:\Users\KOSMO\IdeaProjects\yampf926\src\Calendar" (
    echo 프로젝트 폴더를 찾지 못함.
    echo C:\Users\KOSMO\IdeaProjects\yampf926\src\Calendar
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
cd /d "C:\Users\KOSMO\IdeaProjects\yampf926\src\Calendar"
echo 캘린더/가계부 컴파일 중...
if not exist out mkdir out
powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = @(Get-ChildItem -Recurse -Path 'src' -Filter '*.java' | ForEach-Object { $_.FullName }); if ($files.Count -eq 0) { Write-Host 'Java 파일을 찾지 못함.'; exit 2 }; & javac -encoding UTF-8 -d out $files"
if errorlevel 1 (
    echo.
    echo 컴파일 실패함.
    pause
    exit /b 1
)
echo 캘린더/가계부 실행 중...
java -cp "out;src;assets;images;data" Main
if errorlevel 1 (
    echo.
    echo 실행 실패함. 위 오류 내용 확인 필요.
)
pause
