@echo off
setlocal enabledelayedexpansion

if "%~1"=="" (
  echo Usage: %~nx0 ^<path-to-windows-jre-zip^>
  exit /b 1
)

set "JRE_ZIP=%~f1"
if not exist "%JRE_ZIP%" (
  echo Failed: JRE zip not found: %JRE_ZIP%
  exit /b 1
)

set "ROOT=%~dp0.."
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
set "SPRING_DIR=%ROOT%\spring"
set "RELEASE_DIR=%SPRING_DIR%\target\release\windows"
set "JRE_DIR=%RELEASE_DIR%\jre"
set "TMP_DIR=%TEMP%\flippers-jre-%RANDOM%%RANDOM%"

echo [1/3] Build bundled release package
call "%~dp0build-release.bat" || exit /b 1

echo [2/3] Extract JRE zip into release package
if exist "%TMP_DIR%" rmdir /s /q "%TMP_DIR%"
mkdir "%TMP_DIR%" || exit /b 1

powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%JRE_ZIP%' -DestinationPath '%TMP_DIR%' -Force" || (
  echo Failed: could not extract zip by PowerShell.
  rmdir /s /q "%TMP_DIR%"
  exit /b 1
)

set "JAVA_EXE="
for /f "delims=" %%F in ('dir /s /b "%TMP_DIR%\java.exe" 2^>nul') do (
  set "JAVA_EXE=%%~fF"
  goto :java_found
)

echo Failed: java.exe not found in zip: %JRE_ZIP%
rmdir /s /q "%TMP_DIR%"
exit /b 1

:java_found
for %%D in ("!JAVA_EXE!") do set "JAVA_BIN_DIR=%%~dpD"
for %%D in ("!JAVA_BIN_DIR!..") do set "JAVA_HOME_DIR=%%~fD"

if exist "%JRE_DIR%" rmdir /s /q "%JRE_DIR%"
mkdir "%JRE_DIR%" || (
  rmdir /s /q "%TMP_DIR%"
  exit /b 1
)

xcopy "!JAVA_HOME_DIR!\*" "%JRE_DIR%\" /E /I /Y >nul || (
  echo Failed: could not copy extracted JRE.
  rmdir /s /q "%TMP_DIR%"
  exit /b 1
)

if not exist "%JRE_DIR%\bin\java.exe" (
  echo Failed: extracted JRE does not contain bin\java.exe
  rmdir /s /q "%TMP_DIR%"
  exit /b 1
)

rmdir /s /q "%TMP_DIR%"
echo [3/3] Done
echo Output: %RELEASE_DIR%
exit /b 0
