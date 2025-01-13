/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.invoke;

import org.apache.camel.Message;
import org.eclipse.dirigible.components.engine.camel.components.DirigibleJavaScriptInvoker;
import org.eclipse.dirigible.components.engine.camel.processor.CamelProcessor;
import org.eclipse.dirigible.graalium.core.javascript.CalledFromJS;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Do NOT rename the class name or the package. Otherwise, you will introduce backwards incompatible
 * change.
 */
@Component
public class Invoker {

    private static final String RESOURCE_PATH_PROPERTY_NAME = "resource";

    private final DirigibleJavaScriptInvoker javaScriptInvoker;
    private final CamelProcessor processor;

    public Invoker(DirigibleJavaScriptInvoker javaScriptInvoker, CamelProcessor processor) {
        this.processor = processor;
        this.javaScriptInvoker = javaScriptInvoker;
    }

    /**
     * Invoke.
     *
     * @param camelMessage the camel message
     */
    public void invoke(Message camelMessage) {
        String resourcePath = (String) camelMessage.getExchange()
                                                   .getProperty(RESOURCE_PATH_PROPERTY_NAME);

        javaScriptInvoker.invoke(camelMessage, resourcePath);
    }

    /**
     * Invoke route.
     *
     * @param routeId the route id
     * @param payload the payload
     * @param headers the headers
     * @return the object
     */
    @CalledFromJS
    public Object invokeRoute(String routeId, Object payload, Map<String, Object> headers) {
        return processor.invokeRoute(routeId, payload, headers);
    }
}
