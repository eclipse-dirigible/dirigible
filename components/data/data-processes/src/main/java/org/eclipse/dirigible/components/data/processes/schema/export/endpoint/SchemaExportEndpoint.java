/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.export.endpoint;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.processes.schema.export.ExportSchemaProcessParams;
import org.eclipse.dirigible.components.data.processes.schema.export.ExportSchemaProcessTrigger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "schema")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
class SchemaExportEndpoint {

    private final ExportSchemaProcessTrigger processTrigger;

    SchemaExportEndpoint(ExportSchemaProcessTrigger processTrigger) {
        this.processTrigger = processTrigger;
    }

    @PostMapping(value = "/exportProcesses")
    public ResponseEntity<ExportSchemaProcessDTO> triggerExportProcess(@RequestBody @Valid ExportSchemaParamsDTO paramsDTO) {

        ExportSchemaProcessParams params = createParams(paramsDTO);
        String processInstanceId = processTrigger.trigger(params);

        ExportSchemaProcessDTO body = new ExportSchemaProcessDTO(processInstanceId);
        return ResponseEntity.accepted()
                             .body(body);
    }

    private ExportSchemaProcessParams createParams(ExportSchemaParamsDTO paramsDTO) {
        String dataSource = paramsDTO.getDataSource();
        String schema = paramsDTO.getSchema();
        String exportPath = paramsDTO.getExportPath();
        Set<String> includedTables = paramsDTO.getIncludedTables();
        Set<String> excludedTables = paramsDTO.getExcludedTables();

        return new ExportSchemaProcessParams(dataSource, schema, exportPath, includedTables, excludedTables);
    }

}
