@REM Maven Wrapper batch script for Windows
@REM Downloads and runs the Maven wrapper if not present

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

for /f "tokens=2 delims==" %%a in ('findstr "distributionUrl" "%WRAPPER_PROPERTIES%"') do set "MAVEN_DIST_URL=%%a"

if not defined MAVEN_DIST_URL set "MAVEN_DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"

set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven distribution...
    mkdir "%USERPROFILE%\.m2\wrapper\dists" 2>nul
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_DIST_URL%' -OutFile '%USERPROFILE%\.m2\wrapper\dists\maven.zip'"
    powershell -Command "Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\dists\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
    del "%USERPROFILE%\.m2\wrapper\dists\maven.zip" 2>nul
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
