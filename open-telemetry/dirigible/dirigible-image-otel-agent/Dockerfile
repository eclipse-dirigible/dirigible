ARG DIRIGIBLE_IMAGE=dirigiblelabs/dirigible:latest
FROM $DIRIGIBLE_IMAGE

# Instrument OpenTelemetry agent
ARG OTEL_JAVA_AGENT_VERSION="2.10.0"
WORKDIR /usr/src/app/

ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$OTEL_JAVA_AGENT_VERSION/opentelemetry-javaagent.jar /usr/src/app/opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS=-javaagent:/usr/src/app/opentelemetry-javaagent.jar
