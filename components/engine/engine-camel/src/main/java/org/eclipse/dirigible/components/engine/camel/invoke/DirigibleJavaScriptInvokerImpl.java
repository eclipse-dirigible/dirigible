/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.dirigible.components.engine.camel.invoke;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.spi.Synchronization;
import org.eclipse.dirigible.components.engine.camel.components.DirigibleJavaScriptInvoker;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.nio.file.Path;

@Transactional
@Component
class DirigibleJavaScriptInvokerImpl implements DirigibleJavaScriptInvoker {

    private final ClassLoader currentClassLoader;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    public DirigibleJavaScriptInvokerImpl(
            @Qualifier("defaultDbTransactionManagerDataSource") PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        this.currentClassLoader = Thread.currentThread()
                                        .getContextClassLoader();
    }

    @Transactional
    @Override
    public void invoke(Message camelMessage, String javaScriptPath) {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
        try {
            invokeInternal(camelMessage, javaScriptPath);
            transactionManager.commit(status);
        } catch (Exception ex) {
            transactionManager.rollback(status);
            throw ex;
        }
    }

    private void invokeInternal(Message camelMessage, String javaScriptPath) {
        Thread.currentThread()
              .setContextClassLoader(currentClassLoader);
        DirigibleJavascriptCodeRunner runner = new DirigibleJavascriptCodeRunner();

        var module = runner.run(Path.of(javaScriptPath));
        var result = runner.runMethod(module, "onMessage", wrapCamelMessage(camelMessage));

        if (result != null) {
            camelMessage.getExchange()
                        .setMessage(unwrapCamelMessage(result));
            camelMessage.getExchange()
                        .getExchangeExtension()
                        .addOnCompletion(new Synchronization() {
                            @Override
                            public void onComplete(Exchange exchange) {
                                runner.close();
                            }

                            @Override
                            public void onFailure(Exchange exchange) {
                                runner.close();
                            }
                        });
        } else {
            runner.close();
        }
    }

    /**
     * Wrap camel message.
     *
     * @param camelMessage the camel message
     * @return the integration message
     */
    private IntegrationMessage wrapCamelMessage(Message camelMessage) {
        return new IntegrationMessage(camelMessage);
    }

    /**
     * Unwrap camel message.
     *
     * @param value the value
     * @return the message
     */
    private Message unwrapCamelMessage(Value value) {
        validateIntegrationMessage(value);
        IntegrationMessage message = value.asHostObject();
        return message.getCamelMessage();
    }

    /**
     * Validate integration message.
     *
     * @param value the value
     */
    private void validateIntegrationMessage(Value value) {
        if (!value.isHostObject() || !(value.asHostObject() instanceof IntegrationMessage)) {
            throw new IllegalArgumentException(
                    "Unexpected return received from sdk/integrations::onMessage(). Expected return type: IntegrationMessage.");
        }
    }

}
