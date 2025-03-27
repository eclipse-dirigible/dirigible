/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(Integer.MAX_VALUE) // This bean will be destroyed last
@Component
class DirigibleWorkspaceCleanupBean implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleWorkspaceCleanupBean.class);

    private final DirigibleCleaner dirigibleCleaner;

    DirigibleWorkspaceCleanupBean(DirigibleCleaner dirigibleCleaner) {
        this.dirigibleCleaner = dirigibleCleaner;
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying [{}]...", this.getClass());
        DirigibleCleaner.deleteDirigibleFolder();
    }
}

