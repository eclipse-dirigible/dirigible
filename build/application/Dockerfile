ARG DIRIGIBLE_VERSION=latest
FROM amazoncorretto:21-alpine

VOLUME /tmp

RUN apk update && \
    apk add --no-cache \
    # Consolidating all apk adds to ensure one layer and the latest patches
    ttyd \
    nodejs \
    npm \
    git \
    fontconfig ttf-dejavu ttf-liberation \
    # Clean up apk cache to reduce image size
    && rm -rf /var/cache/apk/*

# Install global npm packages
RUN npm i -g esbuild \
    && npm i -g typescript


COPY target/dirigible-application-*-executable.jar dirigible.jar
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.nio=ALL-UNNAMED", "-jar", "/dirigible.jar"]

# 8080 for the spring boot application,
# 8081 for graalium debug port
EXPOSE 8080 8081
