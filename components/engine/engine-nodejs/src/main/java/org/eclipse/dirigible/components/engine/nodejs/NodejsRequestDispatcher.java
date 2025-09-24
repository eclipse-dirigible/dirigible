package org.eclipse.dirigible.components.engine.nodejs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class NodejsRequestDispatcher implements Function<ServerRequest, ServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodejsRequestDispatcher.class);

    private static final Pattern PATH_PATTERN = Pattern.compile(NodejsProxyConfig.PATH_PATTERN_REGEX);

    private final NodejsProjectsRegistry projectsRegistry;

    NodejsRequestDispatcher(NodejsProjectsRegistry projectsRegistry) {
        this.projectsRegistry = projectsRegistry;
    }

    @Override
    public ServerRequest apply(ServerRequest request) {
        URI requestURI = request.uri();
        LOGGER.debug("Dispatching request with URI {}", requestURI);

        String path = requestURI.getPath();
        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalStateException(
                    "Dispatcher is mapped on an invalid path. Path [" + path + "] doesn't match [" + PATH_PATTERN + "]");
        }
        String projectName = matcher.group(1);
        Optional<NodejsProject> project = projectsRegistry.findByProjectName(projectName);
        if (project.isEmpty()) {
            throw new IllegalArgumentException(
                    "Received request to path [" + path + "] which is for unregistered project with name [" + projectName + "]");
        }

        String projectURL = project.get()
                                   .getMappedURL();
        setRequestURL(request, projectURL);

        return request;
    }

    private static void setRequestURL(ServerRequest request, String newRequestURL) {
        MvcUtils.setRequestUrl(request, URI.create(newRequestURL));
    }
}
