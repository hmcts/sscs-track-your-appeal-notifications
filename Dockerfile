ARG APP_INSIGHTS_AGENT_VERSION=2.3.1
	
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.2

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/track-your-appeal-notifications.jar /opt/app/

CMD ["track-your-appeal-notifications.jar"]
EXPOSE 8081