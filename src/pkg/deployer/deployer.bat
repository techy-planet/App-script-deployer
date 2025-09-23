@echo off
REM Launch the Spring Boot executable jar; forward all args to the app
java -jar script-deployer.jar %*
pause