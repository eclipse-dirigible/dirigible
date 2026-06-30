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

import java.util.List;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;

@Controller
public class DemoController {

    private final GreetingService greetings;
    private final List<Greeter> greeters;

    public DemoController(GreetingService greetings, List<Greeter> greeters) {
        this.greetings = greetings;
        this.greeters = greeters;
    }

    @Get("/greet")
    public String greet() {
        return greetings.greet("World");
    }

    @Get("/count")
    public int count() {
        return greeters.size();
    }
}
