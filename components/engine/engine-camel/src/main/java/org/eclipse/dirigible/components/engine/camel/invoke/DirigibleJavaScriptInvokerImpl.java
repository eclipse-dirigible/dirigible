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
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
class DirigibleJavaScriptInvokerImpl implements DirigibleJavaScriptInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleJavaScriptInvokerImpl.class);
    private final ClassLoader currentClassLoader;

    @Autowired
    public DirigibleJavaScriptInvokerImpl() {
        this.currentClassLoader = Thread.currentThread()
                                        .getContextClassLoader();
    }

    @Override
    public void invoke(Message camelMessage, String javaScriptPath) {
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
        IntegrationMessage message = exctractIntegrationMessage(value);
        return message.getCamelMessage();
    }

    private IntegrationMessage exctractIntegrationMessage(Value value) {
        if (isIntegrationMessage(value)) {
            LOGGER.debug("Returned value [{}] is of expected type [{}]", value, IntegrationMessage.class);
            return value.asHostObject();
        }

        if (isPromise(value)) {
            LOGGER.debug("Returned value [{}] is a promise.", value);

            return executePromise(value);
        }

        throw new IllegalArgumentException("Unexpected return received from @aerokit/sdk/integrations::onMessage(). Expected return type ["
                + IntegrationMessage.class + "] or a promise of [" + IntegrationMessage.class + "]");
    }

    private boolean isIntegrationMessage(Value value) {
        return value.isHostObject() && (value.asHostObject() instanceof IntegrationMessage);
    }

    private boolean isPromise(Value value) {
        return value.hasMember("then") && value.getMember("then")
                                               .canExecute()
                && getMetaObjectString(value).contains("function Promise");
    }

    private String getMetaObjectString(Value value) {
        Value metaObject = value.getMetaObject();
        String metaObjectString = metaObject.toString();
        return null == metaObject ? "" : (metaObjectString == null ? "" : metaObjectString);
    }

    private IntegrationMessage executePromise(Value promise) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> resultRef = new AtomicReference<>();

        // note that throwing an exceptions in these functions (onFulfilled, onRejected) will not be logged
        // anywhere
        // that's why the errors are propagated to the AtomicReference and thrown there
        promise.invokeMember("then", onFulfilled(promise, resultRef, latch), onRejected(promise, resultRef, latch));
        waitForPromiseToResolve(promise, latch);

        return handlePromiseResult(promise, resultRef);
    }

    private ProxyExecutable onFulfilled(Value promise, AtomicReference<Object> resultRef, CountDownLatch latch) {
        return args -> {
            LOGGER.debug("Calling onFulfilled for promise {}", promise);
            try {
                Value result = args[0];
                if (isIntegrationMessage(result)) {
                    resultRef.set(result.asHostObject());
                } else {
                    IllegalArgumentException ex = new IllegalArgumentException("Unexpected value is returned from promise [" + promise
                            + "] Expected return type [" + IntegrationMessage.class + "]. Returned result [" + result + "]");
                    resultRef.set(ex);
                }
                return null;
            } finally {
                latch.countDown();
            }
        };
    }

    private ProxyExecutable onRejected(Value promise, AtomicReference<Object> resultRef, CountDownLatch latch) {
        return args -> {
            LOGGER.debug("Calling onRejected for promise {}", promise);
            try {
                Value reason = args[0];

                if (!reason.isException()) {
                    IllegalStateException ex =
                            new IllegalStateException("Promise [" + promise + "] was rejected with non-exception reason: " + reason);
                    resultRef.set(ex);
                }

                try {
                    // the invocation of this method throws an exception with java script stack trace
                    reason.throwException();
                } catch (Throwable reasonException) {
                    resultRef.set(reasonException);
                }

                return null;
            } finally {
                latch.countDown();
            }
        };
    }

    private IntegrationMessage handlePromiseResult(Value promise, AtomicReference<Object> resultRef) {
        Object result = resultRef.get();
        if (null == result) {
            throw new IllegalStateException(
                    "Missing integration message in the result of the execution of promise: " + promise + ". Check logs for more details.");
        }
        if (result instanceof IntegrationMessage integrationMessage) {
            return integrationMessage;
        }
        if (result instanceof Exception ex) {
            throw new IllegalStateException("An error occurred during execution of promise: " + promise, ex);
        }

        throw new IllegalStateException("An invalid result [" + result + "] returned by the execution of promise: " + promise);
    }

    private void waitForPromiseToResolve(Value promise, CountDownLatch latch) {
        try {
            latch.await(15, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to await for promise execution for: " + promise, e);
        }
    }

}
