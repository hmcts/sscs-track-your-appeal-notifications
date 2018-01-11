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


###Unit tests

To run all unit tests execute the following command:

```
./gradlew test
```
