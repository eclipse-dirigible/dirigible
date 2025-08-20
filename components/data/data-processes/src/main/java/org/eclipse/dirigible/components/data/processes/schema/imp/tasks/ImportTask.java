/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.imp.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("ImportTask")
public class ImportTask implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportTask.class);

    /**
     * Execute.
     *
     * @param execution the execution
     */
    @Transactional
    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Executing {}", this);
    }

}
