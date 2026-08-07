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

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.support.AsyncProcessorConverterHelper;
import org.apache.camel.support.DefaultAsyncProducer;

class DirigibleJavaProducer extends DefaultAsyncProducer {

    private final AsyncProcessor asyncProcessor;

    DirigibleJavaProducer(DirigibleJavaEndpoint endpoint, String className) {
        super(endpoint);

        DirigibleJavaProcessor dirigibleJavaProcessor = new DirigibleJavaProcessor(className);
        this.asyncProcessor = AsyncProcessorConverterHelper.convert(dirigibleJavaProcessor);
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        return this.asyncProcessor.process(exchange, callback);
    }

}
