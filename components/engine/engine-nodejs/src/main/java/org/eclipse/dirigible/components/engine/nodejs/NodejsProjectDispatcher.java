package org.eclipse.dirigible.components.engine.nodejs;

import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

@Component
class NodejsProjectDispatcher implements Function<ServerRequest, ServerRequest> {

    @Override
    public ServerRequest apply(ServerRequest request) {
        NodejsProject project = getProject(request);

        String projectURL = project.getMappedURL();
        setRequestURL(request, projectURL);

        return request;
    }

    private NodejsProject getProject(ServerRequest request) {
        Optional<Object> projectAttribute = request.attribute(NodejsProjectFilter.PROJECT_ATTRIBUTE_NAME);
        if (projectAttribute.isEmpty()) {
            throw new IllegalStateException("Missing required project attribute with name: " + NodejsProjectFilter.PROJECT_ATTRIBUTE_NAME);
        }
        return (NodejsProject) projectAttribute.get();
    }

    private static void setRequestURL(ServerRequest request, String newRequestURL) {
        MvcUtils.setRequestUrl(request, URI.create(newRequestURL));
    }

}
