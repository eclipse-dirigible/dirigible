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
    fontconfig ttf-dejavu ttf-liberation

# This forces npm to update itself and its internal dependencies
# to the latest versions available on the npm registry
# RUN npm install -g npm@latest # -> Commented, so that the ARM64 build don't timeout the GitHub Action (currently it takes ~6 hours).

# Install global npm packages
RUN npm i -g esbuild \
    && npm i -g typescript \
    # Clean up npm cache to reduce image size
    && npm cache clean --force \
    # cleanup of apk cache
    && rm -rf /var/cache/apk/*


COPY target/dirigible-application-*-executable.jar dirigible.jar
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.nio=ALL-UNNAMED", "-jar", "/dirigible.jar"]

# 8080 for the spring boot application,
# 8081 for graalium debug port
EXPOSE 8080 8081
