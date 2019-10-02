ARG APP_INSIGHTS_AGENT_VERSION=2.5.0
	
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.2

COPY lib/applicationinsights-agent-2.5.0.jar lib/AI-Agent.xml /opt/app/
COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/track-your-appeal-notifications.jar /opt/app/

CMD ["track-your-appeal-notifications.jar"]
EXPOSE 8081