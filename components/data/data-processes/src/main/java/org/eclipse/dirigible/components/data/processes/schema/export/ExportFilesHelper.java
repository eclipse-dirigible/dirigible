/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.export;

public class ExportFilesHelper {

    public static String createExportTopologyFilePath(String fileFolderPath) {
        return fileFolderPath + "/" + getExportTopologyFilename();
    }

    public static String getExportTopologyFilename() {
        return "export-topology.json";
    }

    public static String createTableDefinitionFilename(String table) {
        return table + ".json";
    }

    public static String createTableDataFilename(String table) {
        return table + ".csv";
    }
}
