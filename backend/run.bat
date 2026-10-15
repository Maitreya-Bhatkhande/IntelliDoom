@echo off
rem ---------------------------------------------------------------------------
rem run.bat  -  starts the Kotlin backend on http://localhost:4000
rem
rem A Java runtime and the Kotlin compiler are bundled in the tools\ folder, so
rem nothing has to be installed. If build\classes is missing this compiles the
rem code first, then starts the server.
rem ---------------------------------------------------------------------------
setlocal

set "HERE=%~dp0"
set "JAVA_HOME=%HERE%tools\jre"

cd /d "%HERE%"

if not exist build\classes (
    echo [run] build\classes not found - compiling first...
    call build.bat || exit /b 1
)

rem  build\classes : our compiled Kotlin code
rem  lib\*         : every library the app needs (JSON, coroutines, Kotlin stdlib)
"%JAVA_HOME%\bin\java.exe" -cp "build\classes;lib\*" com.intdoom.MainKt
