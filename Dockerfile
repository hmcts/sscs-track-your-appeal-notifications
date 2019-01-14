FROM openjdk:8-jre

WORKDIR /opt/app

COPY build/libs/track-your-appeal-notifications.jar ./

EXPOSE 8081

CMD ["/usr/bin/java", "-jar", "/opt/app/track-your-appeal-notifications.jar"]
