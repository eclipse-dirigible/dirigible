package org.eclipse.dirigible.components.engine.nodejs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Component
class NodejsRequestDispatcher implements HandlerFunction<ServerResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodejsRequestDispatcher.class);

    @Override
    public ServerResponse handle(ServerRequest request) throws Exception {
        String proxyURL = "http://localhost:3000";

        URI requestURI = request.uri();
        LOGGER.info("--- Dispatching request with path [{}] to [{}]", requestURI, proxyURL);

        return http(proxyURL).handle(request);
    }
}
