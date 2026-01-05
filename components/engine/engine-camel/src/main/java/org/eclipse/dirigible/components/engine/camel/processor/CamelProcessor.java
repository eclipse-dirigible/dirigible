/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.platform.http.springboot.CamelRequestHandlerMapping;
import org.apache.camel.impl.engine.DefaultRoutesLoader;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.spring.boot.SpringBootCamelContext;
import org.apache.camel.support.ResourceHelper;
import org.eclipse.dirigible.components.engine.camel.domain.Camel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class CamelProcessor.
 */
@Component
public class CamelProcessor {

    /** The context. */
    private final SpringBootCamelContext context;

    /** The camel request handler mapping. */
    private final CamelRequestHandlerMapping camelRequestHandlerMapping;

    /** The loader. */
    private final RoutesLoader loader;

    /** The camels. */
    private final Map<Long, Resource> camels = new HashMap<>();

    /**
     * Instantiates a new camel processor.
     *
     * @param context the context
     * @param camelRequestHandlerMapping the camel request handler mapping
     */
    @Autowired
    public CamelProcessor(SpringBootCamelContext context, CamelRequestHandlerMapping camelRequestHandlerMapping) {
        this.context = context;
        this.camelRequestHandlerMapping = camelRequestHandlerMapping;
        loader = new DefaultRoutesLoader(context);
    }

    /**
     * On create or update.
     *
     * @param camel the camel
     */
    public void onCreateOrUpdate(Camel camel) {
        Resource resource = ResourceHelper.fromString("any.yaml", camel.getContent());
        camels.put(camel.getId(), resource);
        removeAllRoutes();
        addAllRoutes();
    }

    /**
     * Adds the all routes.
     */
    private void addAllRoutes() {
        camels.values()
              .forEach(routesResource -> {
                  try {
                      loader.loadRoutes(routesResource);
                  } catch (Exception e) {
                      throw new CamelProcessorException(e);
                  }
              });
    }

    /**
     * Removes the all routes.
     */
    private void removeAllRoutes() {
        try {
            context.stopAllRoutes();
            context.removeAllRoutes();
            camelRequestHandlerMapping.getHandlerMethods()
                                      .forEach((info, method) -> camelRequestHandlerMapping.unregisterMapping(info));
        } catch (Exception e) {
            throw new CamelProcessorException(e);
        }
    }

    /**
     * On remove.
     *
     * @param camel the camel
     */
    public void onRemove(Camel camel) {
        camels.remove(camel.getId());
        removeAllRoutes();
        addAllRoutes();
    }

    /**
     * Invoke route.
     *
     * @param routeId the route id
     * @param payload the payload
     * @param headers the headers
     * @param exchangeProperties
     * @return the object
     */
    public Object invokeRoute(String routeId, Object payload, Map<String, Object> headers, Map<String, Object> exchangeProperties) {
        try (FluentProducerTemplate producer = context.createFluentProducerTemplate()) {
            return producer.withHeaders(headers)
                           .withBody(payload)
                           .withExchangeProperties(exchangeProperties)
                           .to(routeId)
                           .request();
        } catch (IOException e) {
            throw new CamelProcessorException("Could not invoke route: " + routeId, e);
        }
    }
}
