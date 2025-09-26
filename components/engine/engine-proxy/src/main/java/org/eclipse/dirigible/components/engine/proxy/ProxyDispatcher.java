package org.eclipse.dirigible.components.engine.proxy;

import org.eclipse.dirigible.components.engine.proxy.domain.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

@Component
class ProxyDispatcher implements Function<ServerRequest, ServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyDispatcher.class);

    @Override
    public ServerRequest apply(ServerRequest request) {
        Proxy proxy = getProxy(request);

        String proxyUrl = proxy.getUrl();
        setRequestURL(request, proxyUrl);

        return request;
    }

    private Proxy getProxy(ServerRequest request) {
        Optional<Object> proxyAttribute = request.attribute(ProxyFilter.PROXY_ATTRIBUTE_NAME);
        if (proxyAttribute.isEmpty()) {
            throw new IllegalStateException("Missing required proxy attribute with name: " + ProxyFilter.PROXY_ATTRIBUTE_NAME);
        }
        return (Proxy) proxyAttribute.get();
    }

    private static void setRequestURL(ServerRequest request, String newRequestURL) {
        LOGGER.debug("Changing request URL to {}", newRequestURL);
        MvcUtils.setRequestUrl(request, URI.create(newRequestURL));
    }

}
