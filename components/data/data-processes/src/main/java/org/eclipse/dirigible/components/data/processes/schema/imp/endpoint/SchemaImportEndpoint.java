/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.imp.endpoint;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.processes.schema.imp.ImportSchemaProcessParams;
import org.eclipse.dirigible.components.data.processes.schema.imp.ImportSchemaProcessTrigger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "schema")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
class SchemaImportEndpoint {

    private final ImportSchemaProcessTrigger processTrigger;

    SchemaImportEndpoint(ImportSchemaProcessTrigger processTrigger) {
        this.processTrigger = processTrigger;
    }

    @PostMapping(value = "/importProcesses")
    public ResponseEntity<ImportSchemaProcessDTO> triggerImportProcess(@RequestBody @Valid ImportSchemaParamsDTO paramsDTO) {

        ImportSchemaProcessParams params = createParams(paramsDTO);
        String processInstanceId = processTrigger.trigger(params);

        ImportSchemaProcessDTO body = new ImportSchemaProcessDTO(processInstanceId);
        return ResponseEntity.accepted()
                             .body(body);
    }

    private ImportSchemaProcessParams createParams(ImportSchemaParamsDTO paramsDTO) {
        String dataSource = paramsDTO.getDataSource();
        String exportPath = paramsDTO.getExportPath();

        return new ImportSchemaProcessParams(dataSource, exportPath);
    }

}
