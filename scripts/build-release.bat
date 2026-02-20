@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0.."
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
set "VUE_DIR=%ROOT%\vue"
set "SPRING_DIR=%ROOT%\spring"
set "STATIC_DIR=%SPRING_DIR%\src\main\resources\static"
set "RELEASE_DIR=%SPRING_DIR%\target\release\windows"

echo [1/5] Build Vue app
pushd "%VUE_DIR%" || exit /b 1
call npm ci || exit /b 1
call npm run build || exit /b 1
popd

echo [2/5] Copy Vue dist into Spring static resources
if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%" || exit /b 1
xcopy "%VUE_DIR%\dist\*" "%STATIC_DIR%\" /E /I /Y >nul || exit /b 1

echo [3/5] Build Spring Boot jar
pushd "%SPRING_DIR%" || exit /b 1
call mvnw.cmd -q clean package -DskipTests || exit /b 1
popd

echo [4/5] Prepare Windows runtime package
if not exist "%RELEASE_DIR%" mkdir "%RELEASE_DIR%" || exit /b 1

set "LATEST_JAR="
for /f "delims=" %%F in ('dir /b /o-d "%SPRING_DIR%\target\*.jar"') do (
  set "LATEST_JAR=%%F"
  goto :jar_found
)
echo Failed: jar file not found.
exit /b 1

:jar_found
copy /Y "%SPRING_DIR%\target\%LATEST_JAR%" "%RELEASE_DIR%\flippers.jar" >nul || exit /b 1

(
echo @echo off
echo setlocal
echo set "SCRIPT_DIR=%%~dp0"
echo if exist "%%SCRIPT_DIR%%jre\bin\java.exe" ^(
echo   "%%SCRIPT_DIR%%jre\bin\java.exe" -jar "%%SCRIPT_DIR%%flippers.jar"
echo ^) else ^(
echo   java -jar "%%SCRIPT_DIR%%flippers.jar"
echo ^)
echo pause
) > "%RELEASE_DIR%\run.bat"

(
echo 1. Put Java runtime at ".\jre" ^(optional^).
echo 2. Run "run.bat".
echo 3. Open http://localhost:8080
) > "%RELEASE_DIR%\README.txt"

echo [5/5] Done
echo Output: %RELEASE_DIR%
exit /b 0
