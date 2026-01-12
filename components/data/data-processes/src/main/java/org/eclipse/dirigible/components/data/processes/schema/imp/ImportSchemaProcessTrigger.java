/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.imp;

import org.eclipse.dirigible.components.data.processes.schema.imp.tasks.ImportProcessContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImportSchemaProcessTrigger {

    private final BpmService bpmService;

    ImportSchemaProcessTrigger(BpmService bpmService) {
        this.bpmService = bpmService;
    }

    public String trigger(ImportSchemaProcessParams params) {
        validateParams(params);

        Map<String, Object> variables = ImportProcessContext.createInitialVariables(params);

        return bpmService.startProcess("import-schema", null, variables);

    }

    private void validateParams(ImportSchemaProcessParams params) {
        // TODO: validate params from business perspective
    }
}
