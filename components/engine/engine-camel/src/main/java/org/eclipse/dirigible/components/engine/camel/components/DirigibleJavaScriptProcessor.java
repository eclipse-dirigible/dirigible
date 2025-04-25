/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.components;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.support.CamelContextHelper;
import org.eclipse.dirigible.components.tracing.TaskState;
import org.eclipse.dirigible.components.tracing.TaskStateUtil;
import org.eclipse.dirigible.components.tracing.TaskType;
import org.eclipse.dirigible.components.tracing.TracingFacade;

class DirigibleJavaScriptProcessor implements Processor {

    private final String javaScriptPath;

    DirigibleJavaScriptProcessor(String javaScriptPath) {
        this.javaScriptPath = javaScriptPath;
    }

    @Override
    public void process(Exchange exchange) {
        TaskState taskState = null;
        if (TracingFacade.isTracingEnabled()) {
            Map<String, String> input = TaskStateUtil.getVariables(exchange.getVariables());
            taskState = TracingFacade.taskStarted(TaskType.ETL, exchange.getExchangeId(), javaScriptPath, input);
            taskState.setDefinition(exchange.getContext()
                                            .getName());
            taskState.setInstance(exchange.getContext()
                                          .getVersion());
        }
        try {
            DirigibleJavaScriptInvoker invoker = getInvoker(exchange.getContext());
            Message message = exchange.getMessage();

            invoker.invoke(message, javaScriptPath);

            if (TracingFacade.isTracingEnabled() && exchange.getException() != null) {
                Map<String, String> output = TaskStateUtil.getVariables(exchange.getVariables());
                TracingFacade.taskFailed(taskState, output, exchange.getException()
                                                                    .getMessage());
            }
        } catch (Exception e) {
            if (TracingFacade.isTracingEnabled()) {
                Map<String, String> output = TaskStateUtil.getVariables(exchange.getVariables());
                TracingFacade.taskFailed(taskState, output, e.getMessage());
            }
            throw new DirigibleJavaScriptException("Exception during invokation of: " + DirigibleJavaScriptInvoker.class, e);
        }
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
