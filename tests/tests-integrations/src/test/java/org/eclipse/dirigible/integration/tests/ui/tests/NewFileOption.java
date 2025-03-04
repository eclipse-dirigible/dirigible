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
    ENTITY_DATA_MODEL("Entity Data Model", "edm.edm");

    // TODO to be added new file name
    // FILE("File"), //
    // FOLDER("Folder"), //
    // JAVASCRIPT_SERVICE("JavaScript Service"), //
    // TYPESCRIPT_SERVICE("TypeScript Service"), //
    // BUSINESS_PROCESS_MODEL("Business Process Model"), //
    // ACCESS_CONSTRAINTS("Access Constraints"), //
    // DATABASE_SCHEMA_MODEL("Database Schema Model"), //
    // DATABASE_TABLE("Database Table"), //
    // DATABASE_VIEW("Database View"), //
    // EXTENSION("Extension"), //
    // EXTENSION_POINT("Extension Point"), //
    // FORM_DEFINITION("Form Definition"), //
    // HTML5_PAGE("HTML5 Page"), //
    // MESSAGE_LISTENER("Message Listener"), //
    // REPORT_MODEL("Report Model"), //
    // ROLES_DEFINITION("Roles Definition"), //
    // SCHEDULED_JOB("Scheduled Job"), //
    // WEBSOCKET("WebSocket");

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
