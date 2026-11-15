@echo off
setlocal
if not exist out mkdir out

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$files = Get-ChildItem -Recurse -Path 'src' -Filter '*.java' | ForEach-Object { $_.FullName }; " ^
  "& javac -encoding UTF-8 -d out $files; exit $LASTEXITCODE"
set RESULT=%ERRORLEVEL%

if %RESULT% neq 0 (
    echo.
    echo BUILD FAILED - read the errors above.
    exit /b %RESULT%
)

echo.
echo BUILD OK.
endlocal