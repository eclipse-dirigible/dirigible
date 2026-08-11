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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.CamelContextHelper;
import org.eclipse.dirigible.components.tracing.TaskState;
import org.eclipse.dirigible.components.tracing.TaskStateUtil;
import org.eclipse.dirigible.components.tracing.TaskType;
import org.eclipse.dirigible.components.tracing.TracingFacade;

import java.util.Map;

class DirigibleJavaProcessor implements Processor {

    private final String className;

    DirigibleJavaProcessor(String className) {
        this.className = className;
    }

    @Override
    public void process(Exchange exchange) {
        TaskState taskState = null;
        if (TracingFacade.isTracingEnabled()) {
            Map<String, String> input = TaskStateUtil.getVariables(exchange.getVariables());
            taskState = TracingFacade.taskStarted(TaskType.ETL, exchange.getExchangeId(), className, input);
            taskState.setDefinition(exchange.getContext()
                                            .getName());
            taskState.setInstance(exchange.getContext()
                                          .getVersion());
        }
        try {
            DirigibleJavaInvoker invoker = getInvoker(exchange.getContext());

            invoker.invoke(exchange, className);

            if (TracingFacade.isTracingEnabled() && exchange.getException() != null) {
                Map<String, String> output = TaskStateUtil.getVariables(exchange.getVariables());
                TracingFacade.taskFailed(taskState, output, exchange.getException()
                                                                    .getMessage());
            }
        } catch (RuntimeException e) {
            if (TracingFacade.isTracingEnabled()) {
                Map<String, String> output = TaskStateUtil.getVariables(exchange.getVariables());
                TracingFacade.taskFailed(taskState, output, e.getMessage());
            }
            throw new DirigibleJavaException("Exception during invocation of: " + DirigibleJavaInvoker.class, e);
        }
    }

    private DirigibleJavaInvoker getInvoker(CamelContext camelContext) {
        try {
            DirigibleJavaInvoker invoker = CamelContextHelper.findSingleByType(camelContext, DirigibleJavaInvoker.class);
            if (invoker == null) {
                invoker = camelContext.getInjector()
                                      .newInstance(DirigibleJavaInvoker.class);
            }
            if (invoker == null) {
                throw new DirigibleJavaException("Cannot get instance of " + DirigibleJavaInvoker.class);
            }

            return invoker;
        } catch (RuntimeException ex) {
            throw new DirigibleJavaException("Cannot get instance of " + DirigibleJavaInvoker.class, ex);
        }
    }

}
