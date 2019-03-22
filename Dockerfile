FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0.1

ENV APP track-your-appeal-notifications.jar
ENV APPLICATION_TOTAL_MEMORY 2048M
ENV APPLICATION_SIZE_ON_DISK_IN_MB 100

COPY build/libs/track-your-appeal-notifications.jar /opt/app/

EXPOSE 8081
