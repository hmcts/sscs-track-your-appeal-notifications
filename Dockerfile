FROM openjdk:8-jre

COPY build/libs/track-your-appeal-notifications.jar /opt/app/

WORKDIR /opt/app

EXPOSE 8081

CMD ["/usr/bin/java", "-jar", "/opt/app/track-your-appeal-notifications.jar"]