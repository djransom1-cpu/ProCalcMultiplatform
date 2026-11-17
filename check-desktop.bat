@echo off
rem Compiles the ProCalc desktop app and builds its app folder (with the icon), logging to check-desktop.log.
rem The installer itself comes from GitHub (desktop-build.yml), which numbers each version.
cd /d "%~dp0"
if not defined JAVA_HOME (
  if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)
echo JAVA_HOME=%JAVA_HOME% > check-desktop.log
echo Checking the ProCalc desktop build... this takes a few minutes.
call gradlew.bat :composeApp:createDistributable --console=plain >> check-desktop.log 2>&1
echo EXIT CODE %ERRORLEVEL% >> check-desktop.log
echo Done. Exit code %ERRORLEVEL%. This window closes in 10 seconds.
timeout /t 10 >nul
