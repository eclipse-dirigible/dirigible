/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;

/**
 * A {@code JavaHandler} that is also a {@code @Component} must be dispatched as the injected container
 * bean (constructor injection), not instantiated via a no-arg constructor.
 */
@Component
public class HelloHandler implements JavaHandler {

    private final GreetingService greetings;

    public HelloHandler(GreetingService greetings) {
        this.greetings = greetings;
    }

    public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.getWriter()
            .print(greetings.greet("Handler"));
    }
}
