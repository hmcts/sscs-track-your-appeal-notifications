ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/track-your-appeal-notifications.jar /opt/app/

CMD ["track-your-appeal-notifications.jar", "-Dhttps.protocols=TLSv1.1,TLSv1.2"]
EXPOSE 8081