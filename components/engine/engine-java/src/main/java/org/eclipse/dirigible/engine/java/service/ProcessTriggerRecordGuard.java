/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.service;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.ProcessRecordGuard;
import org.eclipse.dirigible.engine.java.runtime.ClientBeanResolver;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.sdk.bpm.ProcessTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Answers whether the record a client process runs for has been deleted, through the generated
 * {@link ProcessTrigger} that started the process (issue #8108): the trigger owns the record's
 * repository, so it is the one that can look the record up.
 *
 * <p>
 * Only a confirmed miss counts as gone. A process no trigger starts, a resolver not built yet, or a
 * lookup that fails leaves the task to be completed as before - refusing on doubt would cancel a
 * process whose record is still there.
 */
@Component
class ProcessTriggerRecordGuard implements ProcessRecordGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTriggerRecordGuard.class);

    private final ClientBeansHolder clientBeans;

    ProcessTriggerRecordGuard(ClientBeansHolder clientBeans) {
        this.clientBeans = clientBeans;
    }

    @Override
    public boolean isRecordGone(String processDefinitionKey, String recordId) {
        ClientBeanResolver resolver = clientBeans.current();
        if (resolver == null) {
            return false;
        }
        return resolver.getAll(ProcessTrigger.class)
                       .stream()
                       .filter(trigger -> processDefinitionKey.equals(trigger.process()))
                       .anyMatch(trigger -> isGone(trigger, recordId));
    }

    private static boolean isGone(ProcessTrigger trigger, String recordId) {
        try {
            return !trigger.recordExists(recordId);
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not tell whether {} [{}] of process [{}] still exists - its task is completed as before", trigger.entity(),
                    LogSanitizer.sanitize(recordId), trigger.process(), ex);
            return false;
        }
    }
}
