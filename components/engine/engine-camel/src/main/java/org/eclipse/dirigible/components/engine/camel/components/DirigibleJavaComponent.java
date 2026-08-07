/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.components;

import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@org.apache.camel.spi.annotations.Component(DirigibleJavaEndpoint.SCHEME)
public class DirigibleJavaComponent extends DefaultComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleJavaComponent.class);

    public DirigibleJavaComponent() {
        LOGGER.debug("Creating [{}]", this);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        DirigibleJavaEndpoint endpoint = new DirigibleJavaEndpoint(uri, this);
        endpoint.setClassName(remaining);
        setProperties(endpoint, parameters);

        return endpoint;
    }

}
