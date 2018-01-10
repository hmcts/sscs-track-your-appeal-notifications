# SSCS - Track Your Appeal Notifications

### Description

Track Your Appeal Notifications is a spring boot based application used to send notifications to gov notify. 

It uses the [Tribunals-API service](https://github.com/hmcts/tribunals-case-api) to find cases from CCD. 

The [Job-Scheduler](https://github.com/hmcts/job-scheduler) is used to start notification requests using callbacks. 


## Dependencies

For versions and complete list of dependencies see build.gradle

* Java 8
* Spring Boot
* Gradle

