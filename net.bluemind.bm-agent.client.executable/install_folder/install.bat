@echo on
setlocal
cd /d %~dp0

nssm.exe install bm-agent-client bm-agent.bat
nssm.exe set bm-agent-client AppDirectory %~dp0
pause