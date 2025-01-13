/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.database.ConnectionEnhancer;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

@Component
class HanaConnectionEnhancer implements ConnectionEnhancer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HanaConnectionEnhancer.class);

    @Override
    public boolean isApplicable(DatabaseSystem databaseSystem) {
        return databaseSystem.isHANA();
    }

    @Override
    public void apply(Connection connection) throws SQLException {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        String userName;
        if (authentication != null) {
            userName = authentication.getName();
        } else {
            userName = UserFacade.getName();
        }
        LOGGER.debug("Setting APPLICATIONUSER:{} for connection: {}", userName, connection);
        connection.setClientInfo("APPLICATIONUSER", userName);

        LOGGER.debug("Setting XS_APPLICATIONUSER:{} for connection: {}", userName, connection);
        connection.setClientInfo("XS_APPLICATIONUSER", userName);
    }
}
