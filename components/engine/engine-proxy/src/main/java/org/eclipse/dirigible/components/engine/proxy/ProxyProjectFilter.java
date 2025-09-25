package org.eclipse.dirigible.components.engine.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class ProxyProjectFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    static final String PROJECT_ATTRIBUTE_NAME = "project";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyProjectFilter.class);
    private static final Pattern PATH_PATTERN = Pattern.compile(ProxyRouterConfig.PATH_PATTERN_REGEX);

    private final ProxyProjectsRegistry projectsRegistry;

    ProxyProjectFilter(ProxyProjectsRegistry projectsRegistry) {
        this.projectsRegistry = projectsRegistry;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        URI requestURI = request.uri();
        LOGGER.debug("Determining project for request with URI {}", requestURI);

        String path = requestURI.getPath();
        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalStateException(
                    "The filter is mapped on an invalid path. Path [" + path + "] doesn't match [" + PATH_PATTERN + "]");
        }
        String projectName = matcher.group(1);

        Optional<ProxyProject> project = projectsRegistry.findByProjectName(projectName);
        if (project.isEmpty()) {
            LOGGER.debug("There is no registered project with name [{}]. Request path [{}]", projectName, path);
            String body = "Project [" + projectName + "] is not registered.";
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                                 .body(body);
        }

        ServerRequest modifiedRequest = ServerRequest.from(request)
                                                     .attribute(PROJECT_ATTRIBUTE_NAME, project.get())
                                                     .build();
        return next.handle(modifiedRequest);
    }
}
