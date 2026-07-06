@echo off
chcp 65001 > nul
setlocal

title yampf926 - BoardGame Collection
echo.
echo [BoardGame Collection]
echo Starting from this project folder without local absolute paths.
echo.

set "PROJECT_ROOT=%~dp0.."
set "GAME_DIR=%PROJECT_ROOT%\src\Game"

if not exist "%GAME_DIR%" (
    echo Could not find the game folder:
    echo %GAME_DIR%
    echo.
    pause
    exit /b 1
)

where java > nul 2> nul
if errorlevel 1 (
    echo Java was not found. Install a JDK and try again.
    pause
    exit /b 1
)

where javac > nul 2> nul
if errorlevel 1 (
    echo javac was not found. A JDK is required, not only a JRE.
    pause
    exit /b 1
)

cd /d "%GAME_DIR%"
echo Compiling BoardGame Collection...
if not exist out mkdir out
powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = @(Get-ChildItem -Recurse -Path 'src' -Filter '*.java' | ForEach-Object { $_.FullName }); if ($files.Count -eq 0) { Write-Host 'No Java files found.'; exit 2 }; & javac -encoding UTF-8 -d out $files"
if errorlevel 1 (
    echo.
    echo Compile failed.
    pause
    exit /b 1
)

echo.
echo Running BoardGame Collection for external access...
echo Open the External URL printed below from another device on the same network.
java -cp "out;src;assets;images;data" Main %*
if errorlevel 1 (
    echo.
    echo Run failed. Check the error message above.
)

pause
