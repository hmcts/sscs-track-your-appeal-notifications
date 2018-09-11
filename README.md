# SSCS - Track Your Appeal Notifications

##Purpose
Track Your Appeal Notifications is a spring boot based application used to send notifications to gov notify. 

The [Job-Scheduler](https://github.com/hmcts/job-scheduler) is used to start notification requests using callbacks. 


###Prerequisites

- JDK 8

##Building and deploying the application
  
###Building the application

To build the project execute the following command:

```
./gradlew build
```

### Running the application

Run the application by executing:

```
./gradlew bootRun
```

###Running in Docker
Create the image of the application by executing the following command:

```
  ./gradlew installDist
```

Create docker image:

```
  docker-compose build
```

Run the distribution by executing the following command:

```
  docker-compose up
```

This will start the API container exposing the application's port

In order to test if the application is up, you can call its health endpoint:

```
  curl http://localhost:8081/health
```

You should get a response similar to this:

```
  {"status":"UP"}
```


###DB initialisation

Running the application will apply db migration scripts automatically.

They can also run on demand using the following Gradle task:

```
./gradlew flywayMigrate
```

###Unit tests

To run all unit tests execute the following command:

```
./gradlew test
```

### Debugging locally with CCD

Setup callbacks by finding out IP address:
```
ifconfig
```
Copy the inet value from en0 and place this value in the callback column in the definition spreadsheet.

Start CCD application 
```
./compose-frontend.sh up -d
```
In IDE, start the application in Debug mode and put a breakpoint in the appropriate place. Then login to CCD and start an event which would trigger a callback.

### Run locally in Docker with CCD using an alias

Create an alias
```
alias run-notify='docker-compose -f compose/backend.yml -f compose/frontend.yml -f ../track-your-appeal-notifications/docker-compose.yml'
```
Run the alias
```
run-notify up -d 
```
This starts the CCD applications with Track-your-appeal-notifications in Docker with just one command

##Job scheduler service
This project imports the sscs job scheduler service JAR (https://github.com/hmcts/sscs-job-scheduler).
This is used to schedule reminders in the future. The management of the database is handled within this 
project using a PostgreSQL database. 