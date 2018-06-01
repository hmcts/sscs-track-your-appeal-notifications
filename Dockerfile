FROM openjdk:8-jre

COPY build/install/track-your-appeal-notifications /opt/app/

WORKDIR /opt/app

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" curl --silent --fail http://localhost:8081/health

EXPOSE 8081

ENTRYPOINT ["/opt/app/bin/track-your-appeal-notifications"]
