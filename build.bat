@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

:: 現在のバージョンを取得
for /f "tokens=2 delims=<>" %%a in ('findstr /r "<version>1\.[0-9]*\.[0-9]*</version>" pom.xml') do (
    set "CURRENT_VERSION=%%a"
    goto :found
)
:found

:: バージョンを分割
for /f "tokens=1,2,3 delims=." %%a in ("%CURRENT_VERSION%") do (
    set "MAJOR=%%a"
    set "MINOR=%%b"
    set /a "PATCH=%%c + 1"
)

set "NEW_VERSION=%MAJOR%.%MINOR%.%PATCH%"

echo ============================================
echo   DiscordWhitelist ビルドスクリプト
echo ============================================
echo.
echo 現在のバージョン: %CURRENT_VERSION%
echo 新しいバージョン: %NEW_VERSION%
echo.

:: pom.xmlのバージョンを更新
powershell -Command "(Get-Content pom.xml) -replace '<version>%CURRENT_VERSION%</version>', '<version>%NEW_VERSION%</version>' | Set-Content pom.xml"

echo バージョンを更新しました。
echo.
echo ビルド中...
echo.

:: Mavenビルド実行
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
call C:\maven\bin\mvn.cmd clean package -DskipTests -q

if %errorlevel%==0 (
    echo.
    echo ============================================
    echo   ビルド成功！
    echo ============================================
    echo.
    echo 生成ファイル: target\discord-whitelist-%NEW_VERSION%.jar
    echo.
) else (
    echo.
    echo ビルドに失敗しました。
)

pause
