@echo off
pushd "%~dp0" 
bin\java.exe --module-path lib -m mvdserver/com.maxprograms.mvdserver.MVDServer %* 