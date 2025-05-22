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

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.BpmProvider;
import org.eclipse.dirigible.components.engine.bpm.flowable.diagram.DirigibleProcessDiagramGenerator;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskData;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.PrincipalType;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.job.api.Job;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfoQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * The Class BpmProviderFlowable. NOTE! - all methods in the class should be tenant aware
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
    private final TenantContext tenantContext;

    public BpmProviderFlowable(DataSource datasource, IRepository repository, PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext, TenantContext tenantContext) {
        this.datasource = datasource;
        this.repository = repository;
        this.transactionManager = transactionManager;
        this.applicationContext = applicationContext;
        this.tenantContext = tenantContext;
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
                                          .tenantId(getTenantId())
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
    synchronized ProcessEngine getProcessEngine() {
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

    private String getTenantId() {
        Tenant currentTenant = tenantContext.getCurrentTenant();
        logger.debug("Current tenant is [{}]", currentTenant);
        return currentTenant.getId();
    }

    /**
     * Undeploy process.
     *
     * @param deploymentId the deployment id
     */
    public void undeployProcess(String deploymentId) {
        validateDeployment(deploymentId);

        RepositoryService repositoryService = getProcessEngine().getRepositoryService();
        repositoryService.deleteDeployment(deploymentId, true);
    }

    private void validateDeployment(String deploymentId) {
        RepositoryService repositoryService = getProcessEngine().getRepositoryService();

        Deployment deployment = repositoryService.createDeploymentQuery()
                                                 .deploymentId(deploymentId)
                                                 .deploymentTenantId(getTenantId())
                                                 .singleResult();

        if (deployment == null) {
            throw new IllegalArgumentException("Deployment with id [" + deploymentId + "] not found or does not belong to current tenant.");
        }

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
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId(key, variables, getTenantId());
            logger.info("Started process instance with id [{}], key [{}]", processInstance.getId(), key);
            return processInstance.getId();
        } catch (Exception e) {
            logger.error("Failed to start process with key [{}]", key, e);
            return null;
        }

    }

    /**
     * Delete process.
     *
     * @param processInstanceId the processInstanceId
     * @param reason the reason
     */
    public void deleteProcess(String processInstanceId, String reason) {
        validateProcessInstanceId(processInstanceId);

        logger.debug("Deleting a BPMN process instance by processInstanceId: [{}]", processInstanceId);
        try {
            getProcessEngine().getRuntimeService()
                              .deleteProcessInstance(processInstanceId, reason);
            logger.info("Done deleting a BPMN process instance by processInstanceId: [{}]", processInstanceId);
        } catch (Exception e) {
            logger.error("Failed to delete process with processInstanceId [{}], reason [{}]", processInstanceId, reason, e);
        }
    }

    private void validateProcessInstanceId(String processInstanceId) {
        RuntimeService runtimeService = getProcessEngine().getRuntimeService();

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                                                        .processInstanceId(processInstanceId)
                                                        .processInstanceTenantId(getTenantId())
                                                        .singleResult();

        if (processInstance == null) {
            throw new IllegalArgumentException(
                    "Process instance with id [" + processInstanceId + "] not found or does not belong to current tenant.");
        }

    }

    /**
     * Gets the tasks.
     *
     * @return the tasks
     */
    public String getTasks() {
        List<TaskData> tasksData = new ArrayList<>();
        TaskService taskService = getTaskService();
        List<Task> tasks = taskService.createTaskQuery()
                                      .taskTenantId(getTenantId())
                                      .list();
        for (Task task : tasks) {
            TaskData taskData = new TaskData();
            BeanUtils.copyProperties(task, taskData);
            tasksData.add(taskData);
        }
        return GsonHelper.toJson(tasksData);
    }

    private TaskService getTaskService() {
        return getProcessEngine().getTaskService();
    }

    /**
     * Gets the task variable.
     *
     * @param taskId the task id
     * @param variableName the variable name
     * @return the task variables
     */
    public Object getTaskVariable(String taskId, String variableName) {
        validateTask(taskId);

        TaskService taskService = getTaskService();
        return taskService.getVariable(taskId, variableName);
    }

    private void validateTask(String taskId) {
        TaskService taskService = getTaskService();
        Task task = taskService.createTaskQuery()
                               .taskId(taskId)
                               .taskTenantId(getTenantId())
                               .singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task with id [" + taskId + "] not found or does not belong to current tenant");
        }
    }

    /**
     * Gets the task variables.
     *
     * @param taskId the task id
     * @return the task variables
     */
    public Map<String, Object> getTaskVariables(String taskId) {
        validateTask(taskId);

        TaskService taskService = getTaskService();
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
        validateTask(taskId);

        TaskService taskService = getTaskService();
        taskService.setVariable(taskId, variableName, variable);
    }

    /**
     * Sets the task variables.
     *
     * @param taskId the task id
     * @param variables the variables
     */
    public void setTaskVariables(String taskId, Map<String, Object> variables) {
        validateTask(taskId);

        TaskService taskService = getTaskService();
        taskService.setVariables(taskId, variables);
    }

    /**
     * Complete task.
     *
     * @param taskId the task id
     * @param variables the variables
     */
    public void completeTask(String taskId, String variables) {
        Map<String, Object> processVariables = GsonHelper.fromJson(variables, HashMap.class);
        completeTask(taskId, processVariables);
    }

    /**
     * Gets the variable.
     *
     * @param executionId the execution id
     * @param variableName the variable name
     * @return the variable
     */
    public Object getVariable(String executionId, String variableName) {
        validateExecutionId(executionId);

        RuntimeService runtimeService = getProcessEngine().getRuntimeService();
        return runtimeService.getVariable(executionId, variableName);
    }

    private void validateExecutionId(String executionId) {
        RuntimeService runtimeService = getProcessEngine().getRuntimeService();

        Execution execution = runtimeService.createExecutionQuery()
                                            .executionId(executionId)
                                            .executionTenantId(getTenantId())
                                            .singleResult();

        if (execution == null) {
            throw new IllegalArgumentException("Execution with id [" + executionId + "] not found or does not belong to current tenant.");
        }
    }

    public Map<String, Object> getVariables(String executionId) {
        validateExecutionId(executionId);

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
        validateExecutionId(executionId);

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
        validateExecutionId(executionId);

        RuntimeService runtimeService = getProcessEngine().getRuntimeService();
        runtimeService.removeVariable(executionId, variableName);
    }

    public void cleanup() {
        logger.info("Cleaning [{}]...", this.getClass());
        processEngine = null;
    }

    public List<IdentityLink> getTaskIdentityLinks(String taskId) {
        validateTask(taskId);

        TaskService taskService = getTaskService();
        return taskService.getIdentityLinksForTask(taskId);
    }

    public void claimTask(String taskId, String userId) {
        validateTask(taskId);

        TaskService taskService = getTaskService();
        taskService.claim(taskId, userId);
    }

    public void unclaimTask(String taskId) {
        validateTask(taskId);

        TaskService taskService = getTaskService();
        taskService.unclaim(taskId);
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        validateTask(taskId);

        TaskService taskService = getTaskService();
        taskService.complete(taskId, variables);
    }

    public ProcessInstance getProcessInstance(String processInstanceId) {
        return getProcessEngine().getRuntimeService()
                                 .createProcessInstanceQuery()
                                 .processInstanceId(processInstanceId)
                                 .processInstanceTenantId(getTenantId())
                                 .singleResult();
    }

    public List<ProcessDefinition> getProcessDefinitions(Optional<String> key) {
        ProcessEngine processEngine = getProcessEngine();

        ProcessDefinitionQuery processDefinitionsQuery = processEngine.getRepositoryService()
                                                                      .createProcessDefinitionQuery();
        processDefinitionsQuery.processDefinitionTenantId(getTenantId());
        if (key.isPresent() && !key.get()
                                   .isEmpty()) {
            processDefinitionsQuery.processDefinitionKey(key.get());
        }
        processDefinitionsQuery.processDefinitionTenantId(getTenantId());

        return processDefinitionsQuery.list();
    }

    public ProcessDefinition getProcessDefinitionByKey(String processDefinitionKey) {
        return getProcessEngine().getRepositoryService()
                                 .createProcessDefinitionQuery()
                                 .processDefinitionTenantId(getTenantId())
                                 .processDefinitionKey(processDefinitionKey)
                                 .singleResult();

    }

    public ProcessDefinition getProcessDefinitionById(String processDefinitionId) {
        return getProcessEngine().getRepositoryService()
                                 .createProcessDefinitionQuery()
                                 .processDefinitionTenantId(getTenantId())
                                 .processDefinitionId(processDefinitionId)
                                 .singleResult();
    }

    public List<HistoricVariableInstance> getProcessHistoricInstanceVariables(String processInstanceId) {
        validateProcessInstanceId(processInstanceId);

        return getProcessEngine().getHistoryService()
                                 .createHistoricVariableInstanceQuery()
                                 .processInstanceId(processInstanceId)
                                 .list();
    }

    public List<VariableInstance> getProcessInstanceVariables(String processInstanceId) {
        validateProcessInstanceId(processInstanceId);

        return getProcessEngine().getRuntimeService()
                                 .createVariableInstanceQuery()
                                 .processInstanceId(processInstanceId)
                                 .list();
    }

    public List<Job> getDeadLetterJobs(String processInstanceId) {
        return getProcessEngine().getManagementService()
                                 .createDeadLetterJobQuery()
                                 .processInstanceId(processInstanceId)
                                 .jobTenantId(getTenantId())
                                 .list();
    }

    public Optional<byte[]> getProcessDefinitionImage(String processDefinitionKey) throws IOException {
        RepositoryService repositoryService = getProcessEngine().getRepositoryService();

        // TODO: do we need to add tenant id here?
        ProcessDefinition process = repositoryService.createProcessDefinitionQuery()
                                                     .processDefinitionKey(processDefinitionKey)
                                                     .latestVersion()
                                                     .singleResult();

        if (process == null) {
            return Optional.empty();
        }
        String deploymentId = process.getDeploymentId();
        String diagramResourceName = process.getDiagramResourceName();

        return Optional.of(repositoryService.getResourceAsStream(deploymentId, diagramResourceName)
                                            .readAllBytes());
    }

    public Optional<byte[]> getProcessInstanceImage(String processInstanceId) {
        ProcessEngine processEngine = getProcessEngine();

        RepositoryService repositoryService = processEngine.getRepositoryService();

        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        ProcessInstance processInstance = getProcessInstance(processInstanceId);

        if (processInstance == null) {
            return Optional.empty();
        }
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

        if (processDefinition != null && processDefinition.hasGraphicalNotation()) {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            InputStream resource = diagramGenerator.generateDiagram(bpmnModel, "png",
                    runtimeService.getActiveActivityIds(processInstance.getId()), Collections.emptyList(),
                    processEngineConfiguration.getActivityFontName(), processEngineConfiguration.getLabelFontName(),
                    processEngineConfiguration.getAnnotationFontName(), processEngineConfiguration.getClassLoader(), 1.0,
                    processEngineConfiguration.isDrawSequenceFlowNameWithNoLabelDI());

            try {
                byte[] byteArray = IOUtils.toByteArray(resource);
                return Optional.of(byteArray);
            } catch (Exception e) {
                throw new IllegalArgumentException("Error exporting diagram", e);
            }

        } else {
            throw new IllegalArgumentException("Process instance with id '" + processInstanceId + "' has no graphical notation defined.");
        }
    }

    public void retryDeadLetterJob(String jobId, int retries) {
        getProcessEngine().getManagementService()
                          .moveDeadLetterJobToExecutableJob(jobId, retries);
    }

    public void addProcessInstanceVariable(String processInstanceId, String key, String value) {
        getProcessEngine().getRuntimeService()
                          .setVariable(processInstanceId, key, value);
    }

    public List<ProcessInstance> getProcessInstances(Optional<String> key, Optional<String> businessKey) {
        ProcessEngine processEngine = getProcessEngine();
        ProcessInstanceQuery processInstanceQuery = processEngine.getRuntimeService()
                                                                 .createProcessInstanceQuery();

        if (key.isPresent() && !key.get()
                                   .isEmpty()) {
            processInstanceQuery.processDefinitionKey(key.get());
        }

        if (businessKey.isPresent() && !businessKey.get()
                                                   .isEmpty()) {
            processInstanceQuery.processInstanceBusinessKeyLike("%" + businessKey.get() + "%");
        }
        processInstanceQuery.processInstanceTenantId(getTenantId());

        return processInstanceQuery.list();
    }

    public List<HistoricProcessInstance> getCompletedProcessInstances(Optional<String> definitionKey, Optional<String> businessKey) {
        HistoricProcessInstanceQuery historicProcessInstanceQuery = getProcessEngine().getHistoryService()
                                                                                      .createHistoricProcessInstanceQuery();

        if (definitionKey.isPresent() && !definitionKey.get()
                                                       .isEmpty()) {
            historicProcessInstanceQuery.processDefinitionKey(definitionKey.get());
        }

        if (businessKey.isPresent() && !businessKey.get()
                                                   .isEmpty()) {
            historicProcessInstanceQuery.processInstanceBusinessKeyLike("%" + businessKey.get() + "%");
        }
        historicProcessInstanceQuery.processInstanceTenantId(getTenantId());

        return historicProcessInstanceQuery.finished()
                                           .list();
    }

    public Deployment deployProcess(String deploymentKey, String resourceName, byte[] content) {
        return getProcessEngine().getRepositoryService()
                                 .createDeployment()
                                 .key(deploymentKey)
                                 .tenantId(getTenantId())
                                 .addBytes(resourceName, content)
                                 .deploy();
    }

    public ProcessDefinition getProcessDefinitionByDeploymentId(String deploymentId) {
        return getProcessEngine().getRepositoryService()
                                 .createProcessDefinitionQuery()
                                 .deploymentId(deploymentId)
                                 .processDefinitionTenantId(getTenantId())
                                 .singleResult();
    }

    public List<Deployment> getDeploymentsByKey(String deploymentKey) {
        return getProcessEngine().getRepositoryService()
                                 .createDeploymentQuery()
                                 .deploymentKey(deploymentKey)
                                 .deploymentTenantId(getTenantId())
                                 .list();
    }

    public void deleteDeployment(String deploymentId) {
        validateDeployment(deploymentId);

        getProcessEngine().getRepositoryService()
                          .deleteDeployment(deploymentId, true);
    }

    /**
     * Find tasks by process instance id.
     *
     * @param processInstanceId the process instance id
     * @param type the type
     * @return the list
     */
    public List<Task> findTasks(String processInstanceId, PrincipalType type) {
        if (UserFacade.getUserRoles()
                      .isEmpty()) {
            return new ArrayList<>();
        }
        TaskInfoQuery<TaskQuery, Task> taskQuery = prepareQuery(type);
        taskQuery.processInstanceId(processInstanceId);
        return taskQuery.list();
    }

    private TaskInfoQuery<TaskQuery, Task> prepareQuery(PrincipalType type) {
        TaskQuery taskQuery = getTaskService().createTaskQuery()
                                              .taskTenantId(getTenantId());
        if (PrincipalType.CANDIDATE_GROUPS.equals(type)) {
            return taskQuery.taskCandidateGroupIn(UserFacade.getUserRoles());
        } else if (PrincipalType.ASSIGNEE.equals(type)) {
            return taskQuery.taskAssignee(UserFacade.getName());
        } else {
            throw new IllegalArgumentException("Unrecognised principal type: " + type);
        }
    }

    public List<Task> findTasks(PrincipalType type) {
        if (UserFacade.getUserRoles()
                      .isEmpty()) {
            return new ArrayList<Task>();
        }
        TaskInfoQuery<TaskQuery, Task> taskQuery = prepareQuery(type);
        return taskQuery.list();
    }

    public long processDefinitionsCount() {
        return getProcessEngine().getRepositoryService()
                                 .createProcessDefinitionQuery()
                                 .count();
    }

    public long getProcessInstancesCount() {
        return getProcessEngine().getRuntimeService()
                                 .createProcessInstanceQuery()
                                 .count();
    }

    public long getFinishedHistoricProcessInstancesCount() {
        return getProcessEngine().getHistoryService()
                                 .createHistoricProcessInstanceQuery()
                                 .finished()
                                 .count();
    }

    public long getTasksCount() {
        return getProcessEngine().getTaskService()
                                 .createTaskQuery()
                                 .count();
    }

    public long getTotalCompletedTasksCount() {
        return getProcessEngine().getHistoryService()
                                 .createHistoricTaskInstanceQuery()
                                 .finished()
                                 .count();
    }

    public long getCompletedTasksForToday() {
        return getProcessEngine().getHistoryService()
                                 .createHistoricTaskInstanceQuery()
                                 .finished()
                                 .taskCompletedAfter(new Date(System.currentTimeMillis() - secondsForDays(1)))
                                 .count();
    }

    private long secondsForDays(int days) {
        int hour = 60 * 60 * 1000;
        int day = 24 * hour;
        return (long) days * day;
    }

    public long getCompletedActivities() {
        return getProcessEngine().getHistoryService()
                                 .createHistoricActivityInstanceQuery()
                                 .finished()
                                 .count();
    }
}
