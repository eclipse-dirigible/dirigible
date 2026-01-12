/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import java.util.List;
import javax.sql.DataSource;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.engine.bpm.BpmProvider;
import org.eclipse.dirigible.components.engine.bpm.flowable.diagram.DirigibleProcessDiagramGenerator;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.actuate.endpoint.ProcessEngineEndpoint;
import org.flowable.spring.boot.actuate.info.FlowableInfoContributor;
import org.flowable.variable.api.types.VariableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The Class BpmFlowableConfig.
 */
@Configuration
@EnableAutoConfiguration(exclude = {LiquibaseAutoConfiguration.class, TaskExecutionAutoConfiguration.class})
public class BpmFlowableConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(BpmFlowableConfig.class);

    /**
     * The data sources manager.
     */
    @Autowired
    private DataSourcesManager datasourceManager;

    @Bean("BPM_PROVIDER")
    BpmProvider getBpmProvider(BpmProviderFlowable bpmProviderFlowable) {
        return bpmProviderFlowable;
    }

    /**
     * Enable actuator flowable endpoint
     */
    @Bean
    ProcessEngineEndpoint getProcessEngineEndpoint(ProcessEngine processEngine) {
        return new ProcessEngineEndpoint(processEngine);
    }

    @Bean
    FlowableInfoContributor getFlowableInfoContributor() {
        return new FlowableInfoContributor();
    }

    @Bean
    ProcessEngine getProcessEngine(@Qualifier("SystemDB") DataSource datasource, PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext) {
        LOGGER.info("Initializing the Flowable Process Engine...");

        SpringProcessEngineConfiguration cfg = createProcessEngineConfig(datasource, transactionManager, applicationContext);
        ProcessEngine processEngine = cfg.buildProcessEngine();
        cfg.start();

        LOGGER.info("Done initializing the Flowable Process Engine.");
        return processEngine;
    }

    private SpringProcessEngineConfiguration createProcessEngineConfig(DataSource datasource, PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext) {
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();

        setDatabaseConfig(config, datasource, transactionManager);

        boolean updateSchema = DirigibleConfig.FLOWABLE_DATABASE_SCHEMA_UPDATE.getBooleanValue();
        config.setDatabaseSchemaUpdate(
                updateSchema ? ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE : ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);

        config.setAsyncExecutorActivate(true);
        config.setApplicationContext(applicationContext);

        config.setProcessDiagramGenerator(new DirigibleProcessDiagramGenerator());

        List<VariableType> customPreVariableTypes = List.of(new SimpleCollectionVariableType());
        config.setCustomPreVariableTypes(customPreVariableTypes);

        return config;
    }

    private void setDatabaseConfig(SpringProcessEngineConfiguration config, DataSource datasource,
            PlatformTransactionManager transactionManager) {
        String dataSourceName = DirigibleConfig.FLOWABLE_DATABASE_DATASOURCE_NAME.getStringValue();
        String driver = DirigibleConfig.FLOWABLE_DATABASE_DRIVER.getStringValue();
        String url = DirigibleConfig.FLOWABLE_DATABASE_URL.getStringValue();
        String user = DirigibleConfig.FLOWABLE_DATABASE_USER.getStringValue();
        String password = DirigibleConfig.FLOWABLE_DATABASE_PASSWORD.getStringValue();

        if (dataSourceName != null) {
            LOGGER.info("Initializing the Flowable Process Engine with datasource name");
            DataSource customDataSource = this.datasourceManager.getDataSource(dataSourceName);
            config.setDataSource(customDataSource);
        } else if (driver != null && url != null) {
            LOGGER.info("Initializing the Flowable Process Engine with environment variables datasource parameters");

            config.setJdbcUrl(url);
            config.setJdbcUsername(user);
            config.setJdbcPassword(password);
            config.setJdbcDriver(driver);
        } else {
            LOGGER.info("Initializing the Flowable Process Engine with datasource [{}]", datasource);
            config.setDataSource(datasource);
        }
        config.setTransactionManager(transactionManager);
    }
}
