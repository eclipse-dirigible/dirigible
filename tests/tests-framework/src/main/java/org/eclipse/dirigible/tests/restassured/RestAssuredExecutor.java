/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.restassured;

import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.DirigibleTestTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static io.restassured.specification.ProxySpecification.host;
import static org.awaitility.Awaitility.await;

@Lazy
@Component
public class RestAssuredExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestAssuredExecutor.class);

    static {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    private final int port;

    public RestAssuredExecutor(@LocalServerPort int port) {
        this.port = port;

        RestAssured.proxy = host("127.0.0.1").withPort(port);
    }

    /**
     * Execute REST Assured validation in tenant scope.
     *
     * @param tenant tenant
     * @param callable rest assured validation
     */
    public void execute(DirigibleTestTenant tenant, CallableNoResultAndNoException callable) {
        this.execute(callable, tenant.getHost(), tenant.getUsername(), tenant.getPassword());
    }

    public void execute(CallableNoResultAndNoException callable, String host, String user, String password) {
        String configuredBaseURI = RestAssured.baseURI;
        int configuredPort = RestAssured.port;
        AuthenticationScheme configuredAuthentication = RestAssured.authentication;

        try {
            RestAssured.baseURI = "http://" + host;
            RestAssured.port = port;

            RestAssured.authentication = RestAssured.preemptive()
                                                    .basic(user, password);

            callable.call();
        } finally {
            RestAssured.baseURI = configuredBaseURI;
            RestAssured.port = configuredPort;
            RestAssured.authentication = configuredAuthentication;
        }
    }

    public void execute(DirigibleTestTenant tenant, CallableNoResultAndNoException callable, long timeoutSeconds) {
        await().atMost(timeoutSeconds, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .until(() -> {
                   try {
                       this.execute(callable, tenant.getHost(), tenant.getUsername(), tenant.getPassword());
                       return true;
                   } catch (AssertionError err) {
                       LOGGER.warn("Assertion error. Will try again until timeout is reached.", err);
                       return false;
                   }
               });
    }

    public void execute(CallableNoResultAndNoException callable, String host) {
        String configuredBaseURI = RestAssured.baseURI;
        int configuredPort = RestAssured.port;

        try {
            RestAssured.baseURI = "http://" + host;
            RestAssured.port = port;

            callable.call();
        } finally {
            RestAssured.baseURI = configuredBaseURI;
            RestAssured.port = configuredPort;
        }
    }

    public void execute(CallableNoResultAndNoException callable, long timeoutSeconds) {
        await().atMost(timeoutSeconds, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .until(() -> {
                   try {
                       this.execute(callable);
                       return true;
                   } catch (AssertionError err) {
                       LOGGER.warn("Assertion error. Will try again until timeout is reached.", err);
                       return false;
                   }
               });
    }

    public void execute(CallableNoResultAndNoException callable) {
        String user = DirigibleConfig.BASIC_ADMIN_USERNAME.getFromBase64Value();
        String pass = DirigibleConfig.BASIC_ADMIN_PASS.getFromBase64Value();
        this.execute(callable, "localhost", user, pass);
    }

    public void execute(CallableNoResultAndNoException callable, String user, String password) {
        this.execute(callable, "localhost", user, password);
    }

}
