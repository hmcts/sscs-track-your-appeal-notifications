FROM hmcts/cnp-java-base:openjdk-jre-8-alpine-1.2

COPY build/libs/track-your-appeal-notifications.jar ./

EXPOSE 8081

CMD ["/usr/bin/java", "-jar", "/opt/app/track-your-appeal-notifications.jar"]
