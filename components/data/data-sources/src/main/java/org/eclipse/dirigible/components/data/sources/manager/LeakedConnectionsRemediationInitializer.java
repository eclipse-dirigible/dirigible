/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.manager;

import com.zaxxer.hikari.pool.LeakedConnectionsDoctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LeakedConnectionsRemediationInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeakedConnectionsRemediationInitializer.class);

    static {
        LeakedConnectionsDoctor.init();
        LOGGER.info("Initialized [{}]", LeakedConnectionsDoctor.class);
    }
}
