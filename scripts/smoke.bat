@echo off
REM ============================================================
REM stix-feed-raw smoke test for Windows cmd.exe
REM Mirrors scripts\smoke.sh — runs the same end-to-end checks
REM ============================================================

setlocal enabledelayedexpansion

REM Allow override via:  set BASE=http://other:8080  &  scripts\smoke.bat
if "%BASE%"=="" set BASE=http://localhost:8080

REM Pick python executable (py launcher preferred, fall back to python)
where py >nul 2>nul
if %ERRORLEVEL%==0 (set PY=py -3) else (set PY=python)

call :step "Health"
curl -fsS "%BASE%/health"
if errorlevel 1 goto :fail
echo.

call :step "Ready"
curl -fsS "%BASE%/ready"
if errorlevel 1 goto :fail
echo.

call :step "Get token"
for /f "usebackq delims=" %%T in (`curl -fsS -X POST "%BASE%/api/v1/auth/token" -H "Content-Type: application/json" -d "{\"username\":\"analyst\",\"password\":\"analyst-pass\"}" ^| %PY% -c "import sys,json;print(json.load(sys.stdin)['access_token'])"`) do set TOKEN=%%T
if "%TOKEN%"=="" (
    echo Failed to acquire token
    goto :fail
)
echo Token: %TOKEN:~0,32%...

call :step "POST indicator"
for /f "usebackq delims=" %%I in (`%PY% -c "import uuid;print(uuid.uuid4())"`) do set UUID=%%I
for /f "usebackq delims=" %%N in (`%PY% -c "import datetime;print(datetime.datetime.now(datetime.UTC).strftime('%%Y-%%m-%%dT%%H:%%M:%%S.000Z'))"`) do set NOW=%%N
set ID=indicator--%UUID%

set BODY_FILE=%TEMP%\smoke_indicator_%RANDOM%.json
> "%BODY_FILE%" (
  echo {
  echo   "type": "indicator",
  echo   "spec_version": "2.1",
  echo   "id": "%ID%",
  echo   "created":  "%NOW%",
  echo   "modified": "%NOW%",
  echo   "name": "Smoke test",
  echo   "pattern": "[file:hashes.'SHA-256' = 'aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f']",
  echo   "pattern_type": "stix",
  echo   "valid_from": "%NOW%",
  echo   "confidence": 80
  echo }
)
curl -fsS -X POST "%BASE%/api/v1/indicators" -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "@%BODY_FILE%"
set CURL_RC=%ERRORLEVEL%
del "%BODY_FILE%" >nul 2>nul
if not %CURL_RC%==0 goto :fail
echo.

call :step "GET indicator"
curl -fsS -H "Authorization: Bearer %TOKEN%" "%BASE%/api/v1/indicators/%ID%"
if errorlevel 1 goto :fail
echo.

call :step "Query indicators"
curl -fsS -H "Authorization: Bearer %TOKEN%" "%BASE%/api/v1/indicators?pattern_type=stix"
if errorlevel 1 goto :fail
echo.

echo.
echo All smoke checks passed.
exit /b 0

:step
echo.
echo ------------------------------------------------------------
echo [STEP] %~1
echo ------------------------------------------------------------
exit /b 0

:fail
echo.
echo ------------------------------------------------------------
echo Smoke test FAILED.
echo ------------------------------------------------------------
exit /b 1