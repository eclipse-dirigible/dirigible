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
