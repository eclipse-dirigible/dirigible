package org.eclipse.dirigible.components.data.processes.schema.export;

import org.eclipse.dirigible.components.data.processes.schema.export.tasks.ExportProcessContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ExportSchemaProcessTrigger {

    private final BpmService bpmService;
    private final DataSourcesManager dataSourcesManager;

    ExportSchemaProcessTrigger(BpmService bpmService, DataSourcesManager dataSourcesManager) {
        this.bpmService = bpmService;
        this.dataSourcesManager = dataSourcesManager;
    }

    public String trigger(ExportSchemaProcessParams params) throws InvalidProcessParameterException {
        validateParams(params);

        Map<String, Object> variables = ExportProcessContext.createInitialVariables(params);

        return bpmService.startProcess("export-schema", null, variables);
    }

    private void validateParams(ExportSchemaProcessParams params) throws InvalidProcessParameterException {
        String dataSourceName = params.getDataSource();
        DirigibleDataSource dataSource = null;
        try {
            dataSource = dataSourcesManager.getDataSource(dataSourceName);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProcessParameterException("Missing registered data source with name " + dataSourceName, ex);
        }

        if (dataSource == null) {
            throw new InvalidProcessParameterException("Missing registered data source with name " + dataSourceName);
        }

        Set<String> includedTables = params.getIncludedTables();
        Set<String> excludedTables = params.getExcludedTables();

        Set<String> intersection = new HashSet<>(includedTables);
        intersection.retainAll(excludedTables);
        if (!intersection.isEmpty()) {
            String errorMessage = "Included tables [" + includedTables + "] cannot share elements with exclude tables [" + excludedTables
                    + "]. Common elements: " + intersection;
            throw new InvalidProcessParameterException(errorMessage);
        }

        // TODO: add more validations in the trigger?
    }
}
