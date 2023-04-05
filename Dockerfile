ARG APP_INSIGHTS_AGENT_VERSION=3.4.11
FROM hmctspublic.azurecr.io/base/java:11-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/track-your-appeal-notifications.jar /opt/app/

CMD ["track-your-appeal-notifications.jar"]
EXPOSE 8081