/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.engine.bpm.BpmProvider;
import org.eclipse.dirigible.components.engine.bpm.flowable.diagram.DirigibleProcessDiagramGenerator;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskData;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The Class BpmProviderFlowable.
 */
public class BpmProviderFlowable implements BpmProvider {

    /** The Constant EXTENSION_BPMN20_XML. */
    private static final String EXTENSION_BPMN20_XML = "bpmn20.xml";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(BpmProviderFlowable.class);

    /** The Constant DIRIGIBLE_FLOWABLE_DATABASE_DRIVER. */
    private static final String DIRIGIBLE_FLOWABLE_DATABASE_DRIVER = "DIRIGIBLE_FLOWABLE_DATABASE_DRIVER";

    /** The Constant DIRIGIBLE_FLOWABLE_DATABASE_URL. */
    private static final String DIRIGIBLE_FLOWABLE_DATABASE_URL = "DIRIGIBLE_FLOWABLE_DATABASE_URL";

    /** The Constant DIRIGIBLE_FLOWABLE_DATABASE_USER. */
    private static final String DIRIGIBLE_FLOWABLE_DATABASE_USER = "DIRIGIBLE_FLOWABLE_DATABASE_USER";

    /** The Constant DIRIGIBLE_FLOWABLE_DATABASE_PASSWORD. */
    private static final String DIRIGIBLE_FLOWABLE_DATABASE_PASSWORD = "DIRIGIBLE_FLOWABLE_DATABASE_PASSWORD";

    /** The Constant DIRIGIBLE_FLOWABLE_DATABASE_DATASOURCE_NAME. */
    private static final String DIRIGIBLE_FLOWABLE_DATABASE_DATASOURCE_NAME = "DIRIGIBLE_FLOWABLE_DATABASE_DATASOURCE_NAME";

    /** The Constant DIRIGIBLE_FLOWABLE_DATABASE_SCHEMA_UPDATE. */
    private static final String DIRIGIBLE_FLOWABLE_DATABASE_SCHEMA_UPDATE = "DIRIGIBLE_FLOWABLE_DATABASE_SCHEMA_UPDATE";

    /** The process engine. */
    private static ProcessEngine processEngine;

    /** The datasource. */
    private final DataSource datasource;

    /** The repository. */
    private final IRepository repository;

    private final PlatformTransactionManager transactionManager;
    private final ApplicationContext applicationContext;

    /**
     * Instantiates a new bpm provider flowable.
     *
     * @param datasource the datasource
     * @param repository the repository
     */
    public BpmProviderFlowable(DataSource datasource, IRepository repository, PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext) {
        this.datasource = datasource;
        this.repository = repository;
        this.transactionManager = transactionManager;
        this.applicationContext = applicationContext;
    }

    /**
     * Deploy process.
     *
     * @param location the location
     * @return the string
     */
    public String deployProcess(String location) {
        logger.debug("Deploying a BPMN process from location: [{}]", location);
        RepositoryService repositoryService = getProcessEngine().getRepositoryService();
        Deployment deployment;
        if (!location.startsWith(IRepositoryStructure.SEPARATOR))
            location = IRepositoryStructure.SEPARATOR + location;
        String repositoryPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + location;
        if (getRepository().hasResource(repositoryPath)) {
            IResource resource = getRepository().getResource(repositoryPath);
            deployment = repositoryService.createDeployment()
                                          .addBytes(location + EXTENSION_BPMN20_XML, resource.getContent())
                                          .deploy();
        } else {
            try (InputStream in = BpmProviderFlowable.class.getResourceAsStream("/META-INF/dirigible" + location)) {
                if (in != null) {
                    try {
                        byte[] bytes = IOUtils.toByteArray(in);
                        deployment = repositoryService.createDeployment()
                                                      .addBytes(location + EXTENSION_BPMN20_XML, bytes)
                                                      .deploy();
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else {
                    throw new IllegalArgumentException("No BPMN resource found at location: " + location);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Error closing the BPMN resource at location: " + location, e);
            }
        }
        logger.info("Process deployed with deployment id: [{}] and process key: [{}]", deployment.getId(), deployment.getKey());
        logger.debug("Done deploying a BPMN process from location: [{}]", location);
        return deployment.getId();
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public IRepository getRepository() {
        return repository;
    }

    /**
     * Gets the process engine.
     *
     * @return the process engine
     */
    public synchronized ProcessEngine getProcessEngine() {
        if (processEngine == null) {
            logger.info("Initializing the Flowable Process Engine...");

            SpringProcessEngineConfiguration cfg = createProcessEngineConfig();
            processEngine = cfg.buildProcessEngine();
            cfg.start();

            logger.info("Done initializing the Flowable Process Engine.");
        }
        return processEngine;
    }

    private SpringProcessEngineConfiguration createProcessEngineConfig() {
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();

        setDatabaseConfig(config);

        boolean updateSchema = Boolean.parseBoolean(Configuration.get(DIRIGIBLE_FLOWABLE_DATABASE_SCHEMA_UPDATE, "true"));
        config.setDatabaseSchemaUpdate(
                updateSchema ? ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE : ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);

        config.setAsyncExecutorActivate(true);
        config.setApplicationContext(applicationContext);

        config.setProcessDiagramGenerator(new DirigibleProcessDiagramGenerator());

        return config;
    }

    private void setDatabaseConfig(SpringProcessEngineConfiguration config) {
        String dataSourceName = Configuration.get(DIRIGIBLE_FLOWABLE_DATABASE_DATASOURCE_NAME);
        if (dataSourceName != null) {
            logger.info("Initializing the Flowable Process Engine with JNDI datasource name");
            config.setDataSourceJndiName(dataSourceName);
        }

        String driver = Configuration.get(DIRIGIBLE_FLOWABLE_DATABASE_DRIVER);
        String url = Configuration.get(DIRIGIBLE_FLOWABLE_DATABASE_URL);
        String user = Configuration.get(DIRIGIBLE_FLOWABLE_DATABASE_USER);
        String password = Configuration.get(DIRIGIBLE_FLOWABLE_DATABASE_PASSWORD);

        if (driver != null && url != null) {
            logger.info("Initializing the Flowable Process Engine with environment variables datasource parameters");

            config.setJdbcUrl(url);
            config.setJdbcUsername(user);
            config.setJdbcPassword(password);
            config.setJdbcDriver(driver);
        } else {
            logger.info("Initializing the Flowable Process Engine with datasource [{}]", datasource);
            config.setDataSource(datasource);
            config.setTransactionManager(transactionManager);
        }
    }

    /**
     * Undeploy process.
     *
     * @param deploymentId the deployment id
     */
    public void undeployProcess(String deploymentId) {
        RepositoryService repositoryService = getProcessEngine().getRepositoryService();
        repositoryService.deleteDeployment(deploymentId, true);
    }

    /**
     * Start process.
     *
     * @param key the key
     * @param parameters the parameters
     * @return the string
     */
    public String startProcess(String key, String parameters) {
        logger.info("Starting a BPMN process by key: [{}]", key);
        RuntimeService runtimeService = getProcessEngine().getRuntimeService();
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = GsonHelper.fromJson(parameters, HashMap.class);
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key, variables);
            logger.info("Started process instance with id [{}], key [{}]", processInstance.getId(), key);
            return processInstance.getId();
        } catch (Exception e) {
            logger.error("Failed to start process with key [{}]", key, e);
            List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService()
                                                                      .createProcessDefinitionQuery()
                                                                      .list();
            logger.error("Available process definitions:");
            for (ProcessDefinition processDefinition : processDefinitions) {
                logger.error("Deployment: [{}] with key: [{}] and name: [{}]", processDefinition.getDeploymentId(),
                        processDefinition.getKey(), processDefinition.getName());
            }
            return null;
        }

    }

    /**
     * Delete process.
     *
     * @param id the id
     * @param reason the reason
     */
    public void deleteProcess(String id, String reason) {
        logger.debug("Deleting a BPMN process instance by id: [{}]", id);
        try {
            processEngine.getRuntimeService()
                         .deleteProcessInstance(id, reason);
            logger.info("Done deleting a BPMN process instance by id: [{}]", id);
        } catch (Exception e) {
            logger.error("Failed to delete process with id [{}], reason [{}]", id, reason, e);
        }
    }

    /**
     * Gets the tasks.
     *
     * @return the tasks
     */
    public String getTasks() {
        List<TaskData> tasksData = new ArrayList<>();
        TaskService taskService = getProcessEngine().getTaskService();
        List<Task> tasks = taskService.createTaskQuery()
                                      .list();
        for (Task task : tasks) {
            TaskData taskData = new TaskData();
            BeanUtils.copyProperties(task, taskData);
            tasksData.add(taskData);
        }
        return GsonHelper.toJson(tasksData);
    }

    /**
     * Gets the task variable.
     *
     * @param taskId the task id
     * @param variableName the variable name
     * @return the task variables
     */
    public Object getTaskVariable(String taskId, String variableName) {
        TaskService taskService = getProcessEngine().getTaskService();
        return taskService.getVariable(taskId, variableName);
    }

    /**
     * Gets the task variables.
     *
     * @param taskId the task id
     * @return the task variables
     */
    public Map<String, Object> getTaskVariables(String taskId) {
        TaskService taskService = getProcessEngine().getTaskService();
        return taskService.getVariables(taskId);
    }

    /**
     * Sets task variable.
     *
     * @param taskId the task id
     * @param variableName the variable name
     * @param variable the variable
     */
    public void setTaskVariable(String taskId, String variableName, Object variable) {
        TaskService taskService = getProcessEngine().getTaskService();
        taskService.setVariable(taskId, variableName, variable);
    }


    /**
     * Sets the task variables.
     *
     * @param taskId the task id
     * @param variables the variables
     */
    public void setTaskVariables(String taskId, Map<String, Object> variables) {
        TaskService taskService = getProcessEngine().getTaskService();
        taskService.setVariables(taskId, variables);
    }

    /**
     * Complete task.
     *
     * @param taskId the task id
     * @param variables the variables
     */
    public void completeTask(String taskId, String variables) {
        TaskService taskService = getProcessEngine().getTaskService();
        @SuppressWarnings("unchecked")
        Map<String, Object> processVariables = GsonHelper.fromJson(variables, HashMap.class);
        taskService.complete(taskId, processVariables);
    }

    /**
     * Gets the variable.
     *
     * @param executionId the execution id
     * @param variableName the variable name
     * @return the variable
     */
    public Object getVariable(String executionId, String variableName) {
        RuntimeService runtimeService = getProcessEngine().getRuntimeService();
        return runtimeService.getVariable(executionId, variableName);
    }

    public Map<String, Object> getVariables(String executionId) {
        RuntimeService runtimeService = getProcessEngine().getRuntimeService();
        return runtimeService.getVariables(executionId);
    }

    /**
     * Sets the variable.
     *
     * @param executionId the execution id
     * @param variableName the variable name
     * @param value the value
     */
    public void setVariable(String executionId, String variableName, Object value) {
        RuntimeService runtimeService = getProcessEngine().getRuntimeService();
        runtimeService.setVariable(executionId, variableName, value);
    }

    /**
     * Removes the variable.
     *
     * @param executionId the execution id
     * @param variableName the variable name
     */
    public void removeVariable(String executionId, String variableName) {
        RuntimeService runtimeService = getProcessEngine().getRuntimeService();
        runtimeService.removeVariable(executionId, variableName);
    }

    public void cleanup() {
        logger.info("Cleaning [{}]...", this.getClass());
        processEngine = null;
    }
}
