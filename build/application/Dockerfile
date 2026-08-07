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

# Drop-in directory for AOT compiled module jars (a jar carrying the module's compiled classes, its
# META-INF/dirigible/<project>/.compiled marker and its registry payload). A downstream image COPYs
# such jars here, or they are mounted at run time - the platform jar is consumed verbatim. Empty or
# missing /modules is a no-op. Set LOADER_PATH to use other locations (comma-separated).
RUN mkdir -p /modules

# PropertiesLauncher (instead of the default JarLauncher) is what makes -Dloader.path work; it reads
# the Start-Class from /dirigible.jar's manifest, so the application boots exactly as with -jar.
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.nio=ALL-UNNAMED", "-cp", "/dirigible.jar", "-Dloader.path=/modules", "org.springframework.boot.loader.launch.PropertiesLauncher"]

# 8080 for the spring boot application,
# 8081 for graalium debug port
EXPOSE 8080 8081
