ARG DIRIGIBLE_VERSION=latest
FROM amazoncorretto:21-alpine

VOLUME /tmp

RUN apk update && \
    apk add --no-cache \
    ttyd

RUN apk add nodejs npm
RUN npm i -g esbuild
RUN npm i -g typescript
RUN apk add git
RUN apk --no-cache add msttcorefonts-installer fontconfig && update-ms-fonts && fc-cache -f

COPY target/dirigible-application-*-executable.jar dirigible.jar
ENTRYPOINT ["java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.nio=ALL-UNNAMED", "-jar", "/dirigible.jar"]

# 8080 for the spring boot application,
# 8081 for graalium debug port
EXPOSE 8080 8081
