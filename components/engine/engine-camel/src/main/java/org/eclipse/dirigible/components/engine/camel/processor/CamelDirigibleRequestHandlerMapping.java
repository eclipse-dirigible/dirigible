/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.processor;

import org.apache.camel.component.platform.http.HttpEndpointModel;
import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.apache.camel.component.platform.http.spi.PlatformHttpEngine;
import org.apache.camel.component.platform.http.springboot.CamelRequestHandlerMapping;

/**
 * The Class CamelDirigibleRequestHandlerMapping.
 */
public class CamelDirigibleRequestHandlerMapping extends CamelRequestHandlerMapping {

    /**
     * Instantiates a new camel dirigible request handler mapping.
     *
     * @param component the component
     * @param engine the engine
     */
    public CamelDirigibleRequestHandlerMapping(PlatformHttpComponent component, PlatformHttpEngine engine) {
        super(component, engine);
    }

    /**
     * Register http endpoint.
     *
     * @param model the model
     */
    @Override
    public void registerHttpEndpoint(HttpEndpointModel model) {
        var patchedModel = DirigibleHttpEndpointModel.from(model);
        super.registerHttpEndpoint(patchedModel);
    }
}
