package org.eclipse.dirigible.components.engine.nodejs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.net.URI;
import java.util.function.Function;

@Component
class NodejsRequestDispatcher implements Function<ServerRequest, ServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodejsRequestDispatcher.class);

    @Override
    public ServerRequest apply(ServerRequest request) {
        String proxyURL = "http://localhost:3000";

        URI requestURI = request.uri();
        LOGGER.info("--- Dispatching request with path [{}] to [{}]", requestURI, proxyURL);

        setRequestURL(request, proxyURL);

        return request;
    }

    private static void setRequestURL(ServerRequest request, String newRequestURL) {
        MvcUtils.setRequestUrl(request, URI.create(newRequestURL));
    }
}
