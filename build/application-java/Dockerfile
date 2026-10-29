ARG DIRIGIBLE_VERSION=latest
FROM amazoncorretto:21-alpine

# The Java edition (#8011, #8026): the platform and its IDE without the JavaScript and TypeScript stack,
# so Node (npm, tsc) is not installed. ttyd for the IDE terminal, git for the Git API and the Git
# perspective, fontconfig and the fonts for PDF rendering.

VOLUME /tmp

RUN apk update && \
    apk add --no-cache \
    # Consolidating all apk adds to ensure one layer and the latest patches
    ttyd \
    git \
    fontconfig ttf-dejavu ttf-liberation \
    # cleanup of apk cache
    && rm -rf /var/cache/apk/*

COPY target/dirigible-application-java-*-executable.jar dirigible.jar

# Drop-in directory for AOT compiled module jars and the resolved-modules directory, exactly as in the
# default image (build/application/Dockerfile).
RUN mkdir -p /modules /root/.dirigible/resolved-modules

# The same -jar launch and heap sizing as the default image (see build/application/Dockerfile):
# MaxRAMPercentage sizes the heap to the container, ExitOnOutOfMemoryError exits on heap exhaustion
# instead of staying Up with a poisoned store, and -jar makes the JVM honor the Launcher-Agent-Class
# manifest entry while PropertiesLauncher honors -Dloader.path.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50.0", "-XX:+ExitOnOutOfMemoryError", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.nio=ALL-UNNAMED", "-Dloader.path=/modules", "-jar", "/dirigible.jar"]

# 8080 for the spring boot application
EXPOSE 8080
