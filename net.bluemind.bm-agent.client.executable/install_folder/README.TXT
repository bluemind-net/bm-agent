How to install bm-client-agent on windows:

- adapt the config file client-config.json as needed
- execute install.bat (this will install a windows service bm-agent-client)
- use the windows service bm-client-agent to start/stop and automate the application

Notes:
If you need to install bm-agent-client on a 32Bit system,
you will need to replace nssm.exe with the corresponding 32Bit install file from 
https://nssm.cc/download
