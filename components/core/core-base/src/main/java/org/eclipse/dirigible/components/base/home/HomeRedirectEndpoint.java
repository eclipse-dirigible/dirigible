/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.home;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * The Home Redirect.
 */
@RestController
class HomeRedirectEndpoint {

    /**
     * Go home.
     *
     * @param request the request
     * @param response the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @RequestMapping(path = {"/home", "/", ""})
    void goHome(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String homeUrl = DirigibleConfig.HOME_URL.getStringValue();
        response.sendRedirect(homeUrl);
    }

}
