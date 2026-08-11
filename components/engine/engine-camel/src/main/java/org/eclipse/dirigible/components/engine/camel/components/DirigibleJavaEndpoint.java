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

import org.apache.camel.*;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.util.UnsafeUriCharactersEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invoke a client Java class implementing {@link org.apache.camel.Processor}.
 *
 * <p>
 * Counterpart of {@link DirigibleJavaScriptEndpoint} for the Java side. A route step of the form
 * {@code dirigible-java:<fully.qualified.ClassName>} resolves the class through the currently
 * installed client class loader, instantiates it via its public no-arg constructor on every
 * exchange, and invokes {@link org.apache.camel.Processor#process(Exchange)} with the exchange.
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = DirigibleJavaEndpoint.SCHEME, title = "Dirigible Java",
        syntax = DirigibleJavaEndpoint.SCHEME + ":className", producerOnly = true, remote = false,
        category = {Category.CORE, Category.SCRIPT})
public class DirigibleJavaEndpoint extends DefaultEndpoint {

    static final String SCHEME = "dirigible-java";

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleJavaEndpoint.class);

    @UriPath(label = "common", description = "Sets the fully-qualified name of the client Java class implementing Processor.")
    @Metadata(required = true)
    private String className;

    public DirigibleJavaEndpoint() {
        LOGGER.debug("Creating [{}] without parameters", this);
        setExchangePattern(ExchangePattern.InOut);
    }

    public DirigibleJavaEndpoint(String endpointUri, Component component) {
        super(endpointUri, component);
        LOGGER.debug("Creating [{}] for URI [{}]", this, endpointUri);
        setExchangePattern(ExchangePattern.InOut);
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public Producer createProducer() {
        return new DirigibleJavaProducer(this, className);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("You cannot consume from " + this.getClass());
    }

    @Override
    protected String createEndpointUri() {
        return DirigibleJavaEndpoint.SCHEME + ":" + UnsafeUriCharactersEncoder.encode(getClassName());
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

}
