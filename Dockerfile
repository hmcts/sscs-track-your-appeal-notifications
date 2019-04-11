FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0.1

COPY build/libs/track-your-appeal-notifications.jar /opt/app/

EXPOSE 8081

CMD ["track-your-appeal-notifications.jar"]