@echo off
rem ---------------------------------------------------------------------------
rem build.bat  -  compiles the Kotlin backend into build\classes
rem
rem Everything it needs is bundled inside this folder, so you do NOT have to
rem install Kotlin or Java:
rem   tools\kotlinc   the Kotlin compiler
rem   tools\jre       a Java runtime
rem Just run this file (or "npm run build" from the project root).
rem ---------------------------------------------------------------------------
setlocal

rem The folder this script lives in (%~dp0 ends with a backslash).
set "HERE=%~dp0"
set "KOTLINC=%HERE%tools\kotlinc"
set "JAVA_HOME=%HERE%tools\jre"

cd /d "%HERE%"

echo [build] compiling Kotlin sources...

if exist build\classes rmdir /s /q build\classes

rem  -cp        : the libraries our code needs (JSON + coroutines)
rem  -Xplugin   : turns @Serializable classes into JSON code
rem  -jvm-target: bytecode version (21 works on any modern JDK)
"%KOTLINC%\bin\kotlinc.bat" src ^
    -cp "lib\kotlinx-serialization-core-jvm-1.11.0.jar;lib\kotlinx-serialization-json-jvm-1.11.0.jar;lib\kotlinx-coroutines-core-jvm.jar" ^
    "-Xplugin=%KOTLINC%\lib\kotlin-serialization-compiler-plugin.jar" ^
    -jvm-target 21 ^
    -d build\classes

if errorlevel 1 (
    echo [build] FAILED - see the errors above.
    exit /b 1
)

echo [build] OK -^> build\classes
