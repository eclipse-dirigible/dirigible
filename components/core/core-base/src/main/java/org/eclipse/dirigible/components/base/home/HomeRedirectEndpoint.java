/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.home;

import java.io.IOException;
import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The Home Redirect.
 */
@RestController
class HomeRedirectEndpoint {

    /** The Constant DIRIGIBLE_HOME_URL. */
    private static final String DIRIGIBLE_HOME_URL = "DIRIGIBLE_HOME_URL";

    /**
     * Go home.
     *
     * @param request the request
     * @param response the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @RequestMapping(path = {"/home", "/", ""})
    void goHome(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String homeUrl = Configuration.get(DIRIGIBLE_HOME_URL);
        response.sendRedirect(homeUrl);
    }

}
