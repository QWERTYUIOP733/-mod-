@echo off
setlocal
cd /d "%~dp0"

echo ============================================
echo   MARD Pixel Mod - Build Script v1.2.0
echo   Using China mirror for Forge/Fabric deps
echo ============================================
echo.

REM ---- Auto-detect bundled tools in D:\dev ----
if exist "D:\dev\jdk-17.0.20.1+1\bin\java.exe" set "PATH=D:\dev\jdk-17.0.20.1+1\bin;%PATH%"
if exist "D:\dev\node-v20.18.2-win-x64\node.exe" set "PATH=D:\dev\node-v20.18.2-win-x64;%PATH%"
if exist "D:\dev\gradle-8.8\bin\gradle.bat" set "PATH=D:\dev\gradle-8.8\bin;%PATH%"

REM ---- Check Node ----
where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js not found in D:\dev\node-v20.18.2-win-x64
  pause
  exit /b 1
)
echo [OK] Node.js found.

REM ---- Generate resources ----
echo [1/4] Generating 295 blockstates + item models + lang files...
node tools\generate_resources.js
if errorlevel 1 (
  echo [ERROR] Resource generation failed.
  pause
  exit /b 1
)
echo [OK] Resources generated.

REM ---- Check Java ----
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java not found in D:\dev\jdk-17.0.20.1+1
  pause
  exit /b 1
)
echo [OK] Java found.

REM ---- Check Gradle ----
where gradle >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Gradle not found in D:\dev\gradle-8.8
  pause
  exit /b 1
)
echo [OK] Gradle found.
echo.

REM ---- Kill leftover java processes from previous failed builds ----
echo Killing leftover Java processes from previous build...
taskkill /f /im java.exe >nul 2>&1

REM ---- Clean incomplete forge_gradle cache (prevents remap deadlock) ----
echo Cleaning incomplete forge_gradle cache...
if exist "%USERPROFILE%\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.1.0_mapped_official_1.20.1" (
  rmdir /s /q "%USERPROFILE%\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.1.0_mapped_official_1.20.1"
)

REM ---- Build Forge ----
echo [2/4] Building Forge (downloading deps from China mirror, please wait)...
pushd forge
call gradle build --no-daemon --stacktrace > ..\forge_build.log 2>&1
set FORGE_RESULT=%errorlevel%
popd
if not "%FORGE_RESULT%"=="0" (
  echo.
  echo [ERROR] Forge build failed (exit code %FORGE_RESULT%).
  echo See log: forge_build.log in this folder.
  echo.
  type forge_build.log
  echo.
  pause
  exit /b 1
)
echo [OK] Forge build succeeded.

REM ---- Build Fabric ----
echo [3/4] Building Fabric...
pushd fabric
call gradle build --no-daemon --stacktrace > ..\fabric_build.log 2>&1
set FABRIC_RESULT=%errorlevel%
popd
if not "%FABRIC_RESULT%"=="0" (
  echo.
  echo [ERROR] Fabric build failed (exit code %FABRIC_RESULT%).
  echo See log: fabric_build.log in this folder.
  echo.
  type fabric_build.log
  echo.
  pause
  exit /b 1
)
echo [OK] Fabric build succeeded.

REM ---- Collect jars ----
echo [4/4] Collecting jars to dist folder...
if not exist dist mkdir dist
copy /y forge\build\libs\mard_pixel_forge-1.2.0.jar dist\MARD_Pixel_Forge_1.20.1_v1.2.0.jar >nul
copy /y fabric\build\libs\mard_pixel_fabric-1.2.0.jar dist\MARD_Pixel_Fabric_1.20.1_v1.2.0.jar >nul
echo.
if exist dist\MARD_Pixel_Forge_1.20.1_v1.2.0.jar echo [OK] Forge jar:  dist\MARD_Pixel_Forge_1.20.1_v1.2.0.jar
if exist dist\MARD_Pixel_Fabric_1.20.1_v1.2.0.jar echo [OK] Fabric jar: dist\MARD_Pixel_Fabric_1.20.1_v1.2.0.jar
echo.
echo Build finished successfully!
pause
