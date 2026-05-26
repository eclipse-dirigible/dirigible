@echo off
rem Copyright (c) 2010-2026 Eclipse Dirigible contributors
rem SPDX-License-Identifier: EPL-2.0
call npm install --prefer-offline --no-audit --no-fund
if errorlevel 1 exit /b 1
call node build.js
if errorlevel 1 exit /b 1
