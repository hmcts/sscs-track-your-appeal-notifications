# SSCS - Track Your Appeal Notifications

Track Your Appeal Notifications is a spring boot based application used to send notifications to gov notify. 

It uses the [Tribunals-API service](https://github.com/hmcts/tribunals-case-api) to find cases from CCD. 

The [Job-Scheduler](https://github.com/hmcts/job-scheduler) is used to start notification requests using callbacks. 


##Getting Started

###Prerequisites

- JDK 8

###Building

To build the project execute the following command:

./gradlew build

### Running

Run the application by executing:

./gradlew bootRun

##Developing

###Unit tests

To run all unit tests execute the following command:

./gradlew test
