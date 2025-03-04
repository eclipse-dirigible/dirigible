/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.dirigible.integration.tests.ui.tests;

enum NewFileOption {
    ENTITY_DATA_MODEL("Entity Data Model", "edm.edm"), //
    JAVASCRIPT_SERVICE("JavaScript Service", "javascript-esm.mjs"), //
    TYPESCRIPT_SERVICE("TypeScript Service", "typescript.ts"), //
    BUSINESS_PROCESS_MODEL("Business Process Model", "bpmn-new.bpmn"), //
    ACCESS_CONSTRAINTS("Access Constraints", "access.access"), //
    DATABASE_SCHEMA_MODEL("Database Schema Model", "schema.dsm"), //
    DATABASE_TABLE("Database Table", "database-table.table"), //
    DATABASE_VIEW("Database View", "database-view.view"), //
    EXTENSION("Extension", "extension.extension"), //
    EXTENSION_POINT("Extension Point", "extensionpoint.extensionpoint"), //
    FORM_DEFINITION("Form Definition", "form.form"), //
    MESSAGE_LISTENER("Message Listener", "listener.listener"), //
    REPORT_MODEL("Report Model", "report-model.report"), //
    ROLES_DEFINITION("Roles Definition", "roles.roles"), //
    SCHEDULED_JOB("Scheduled Job", "job.job"), //
    WEBSOCKET("WebSocket", "websocket.websocket"), //
    HTML5_PAGE("HTML5 Page", "html.html"); //

    private final String optionName;

    private final String newFileName;

    NewFileOption(String optionName, String newFileName) {
        this.optionName = optionName;
        this.newFileName = newFileName;
    }

    public String getOptionName() {
        return optionName;
    }

    public String getNewFileName() {
        return newFileName;
    }

}
