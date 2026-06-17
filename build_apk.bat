@echo off
set "JAVA_HOME=C:\Users\THISAI\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "C:\Users\THISAI\Downloads\MasterEnglishFluency"
call "%~dp0gradlew.bat" assembleDebug
