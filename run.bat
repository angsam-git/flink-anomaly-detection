@echo off
setlocal enabledelayedexpansion

REM Batch script to set up and run Kafka and Flink containers, create a Kafka topic, and submit a Flink job. For use on Windows systems

REM Build the Flink job JAR
echo Building Flink job JAR...
call mvn clean install
echo Maven finished

SET NETWORK_NAME=shared-kafka-net

REM Create Docker network if it doesn't already exist
docker network inspect %NETWORK_NAME% >nul 2>&1

IF %ERRORLEVEL% NEQ 0 (
    echo Creating Docker network: %NETWORK_NAME%
    docker network create %NETWORK_NAME%
) ELSE (
    echo Docker network %NETWORK_NAME% already exists
)

REM Set up kafka and flink containers
echo Starting Kafka and Flink containers...
docker-compose down -v && docker-compose up -d

REM Create topic 'api-logs'
echo Creating topic 'api-logs'...
docker-compose exec kafka kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic api-logs --partitions 1 --replication-factor 1 >nul 2>&1

REM Wait for topic leader assignment
:waitleader
docker-compose exec kafka kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic api-logs | findstr "Leader:" >nul 2>&1
if errorlevel 1 (
    echo Waiting for topic leader assignment...
    timeout /t 2 >nul
    goto waitleader
)

REM Submit Flink job
echo Submitting Flink job...
for /f "tokens=*" %%i in ('docker-compose exec jobmanager /opt/flink/bin/flink run -d --jobmanager jobmanager:8081 /flink/job/flink-anomaly-detector-1.0.0.jar') do (
    echo %%i
    echo %%i | findstr /r /c:"Job has been submitted with JobID" >nul
    if !errorlevel! == 0 (
        for %%a in (%%i) do set jobid=%%a
    )
)

REM Loop until job is RUNNING
:checkstatus
for /f "tokens=*" %%s in ('docker-compose exec jobmanager curl -s http://localhost:8081/jobs/%JOB_ID%') do (
    set JOB_STATUS_LINE=%%s
    echo !JOB_STATUS_LINE! | findstr "\"state\": \"RUNNING\"" >nul
    if !errorlevel! == 0 (
        goto done
    )
)
echo Waiting for job %JOB_ID% to reach RUNNING status...
timeout /t 2 >nul
goto checkstatus

:done
echo Successfully ran job %jobid%.

endlocal