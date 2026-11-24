@echo off
cd /d "%~dp0"
python "tools/balance_editor.py"
if errorlevel 1 pause
