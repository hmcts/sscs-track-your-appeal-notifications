ARG APP_INSIGHTS_AGENT_VERSION=3.4.13

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/track-your-appeal-notifications.jar /opt/app/

CMD ["track-your-appeal-notifications.jar"]
EXPOSE 8081
