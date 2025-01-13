/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.components;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.support.CamelContextHelper;

class DirigibleJavaScriptProcessor implements Processor {

    private final String javaScriptPath;

    DirigibleJavaScriptProcessor(String javaScriptPath) {
        this.javaScriptPath = javaScriptPath;
    }

    @Override
    public void process(Exchange exchange) {
        DirigibleJavaScriptInvoker invoker = getInvoker(exchange.getContext());
        Message message = exchange.getMessage();

        invoker.invoke(message, javaScriptPath);
    }

    private DirigibleJavaScriptInvoker getInvoker(CamelContext camelContext) {
        try {
            DirigibleJavaScriptInvoker invoker = CamelContextHelper.findSingleByType(camelContext, DirigibleJavaScriptInvoker.class);
            if (invoker == null) {
                invoker = camelContext.getInjector()
                                      .newInstance(DirigibleJavaScriptInvoker.class);
            }
            if (invoker == null) {
                throw new DirigibleJavaScriptException("Cannot get instance of " + DirigibleJavaScriptInvoker.class);
            }

            return invoker;
        } catch (RuntimeException ex) {
            throw new DirigibleJavaScriptException("Cannot get instance of " + DirigibleJavaScriptInvoker.class, ex);
        }
    }

}
