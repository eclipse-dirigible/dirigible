/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
export function getTemplate() {
    return {
        name: "Hello",
        label: "Java Service",
        extension: "java",
        data: `import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;

public class Hello implements JavaHandler {

    public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.getWriter().println("Hello World!");
    }

}`,
        order: 3
    }
};
