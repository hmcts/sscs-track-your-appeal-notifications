FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.0

COPY build/libs/track-your-appeal-notifications.jar /opt/app/

EXPOSE 8081

CMD ["track-your-appeal-notifications.jar"]