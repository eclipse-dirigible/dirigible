/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class TableRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Import(TestConfig.class)
@Transactional
public class TaskStateServiceTest {

    /** The task state service. */
    @Autowired
    private TaskStateService taskStateService;

    /** The task state repository. */
    @Autowired
    private TaskStateRepository taskStateRepository;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {
        Configuration.set(TaskStateService.DIRIGIBLE_TRACING_TASK_ENABLED, "true");

        cleanup();
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    public void cleanup() throws Exception {
        // delete test task states
        taskStateService.deleteAll();
    }

    /**
     * Resets the global tracing flag so the mutated runtime configuration does not leak into other test
     * classes.
     */
    @AfterEach
    public void resetTracingFlag() {
        Configuration.remove(TaskStateService.DIRIGIBLE_TRACING_TASK_ENABLED);
    }

    /**
     * Verifies the enable then disable round-trip on the service (regression for #6608).
     */
    @Test
    public void enableThenDisableTracing() {
        taskStateService.enableTracing();
        assertTrue(taskStateService.isTracingEnabled());

        taskStateService.disableTracing();
        assertFalse(taskStateService.isTracingEnabled());
    }

    /**
     * Verifies the enable then disable round-trip on the static facade the engines consult (regression
     * for #6608).
     */
    @Test
    public void enableThenDisableTracingViaFacade() {
        TracingFacade.enableTracing();
        assertTrue(TracingFacade.isTracingEnabled());

        TracingFacade.disableTracing();
        assertFalse(TracingFacade.isTracingEnabled());
    }

    /**
     * Start finish.
     *
     * @throws Exception the exception
     */
    @Test
    public void startFinish() throws Exception {

        cleanup();

        Map<String, String> input = new TreeMap<String, String>();
        input.put("var1", "val1");
        input.put("var2", "val2");

        Map<String, String> output = new TreeMap<String, String>();
        output.put("var1", "val1_");
        output.put("var2", "val2_");

        TaskState taskState = taskStateService.taskStarted(TaskType.BPM, "exec1", "step1", input);

        Long id = taskStateService.getAll()
                                  .get(0)
                                  .getId();
        TaskState result = taskStateService.findById(id);
        assertNotNull(result);
        assertNotNull(result.getInput());
        assertNotNull(result.getOutput());
        assertEquals("val1", result.getInput()
                                   .get("var1"));

        taskStateService.taskSuccessful(taskState, output);

        assertEquals("val1_", result.getOutput()
                                    .get("var1"));
        assertNull(result.getError());
    }

    /**
     * Start fail.
     *
     * @throws Exception the exception
     */
    @Test
    public void startFail() throws Exception {

        cleanup();

        Map<String, String> input = new TreeMap<String, String>();
        input.put("var1", "val1");
        input.put("var2", "val2");

        Map<String, String> output = new TreeMap<String, String>();
        output.put("var1", "val1_");
        output.put("var2", "val2_");

        TaskState taskState = taskStateService.taskStarted(TaskType.BPM, "exec1", "step1", input);

        Long id = taskStateService.getAll()
                                  .get(0)
                                  .getId();
        TaskState result = taskStateService.findById(id);
        assertNotNull(result);
        assertEquals(TaskType.BPM, result.getType());
        assertEquals("exec1", result.getExecution());
        assertEquals("step1", result.getStep());
        assertNotNull(result.getInput());
        assertNotNull(result.getOutput());
        assertEquals("val1", result.getInput()
                                   .get("var1"));

        taskStateService.taskFailed(taskState, output, "error1");

        assertEquals("val1_", result.getOutput()
                                    .get("var1"));
        assertEquals("error1", result.getError());


    }

    /**
     * Verifies that reads and deletes are scoped to the current tenant (regression for #6607). Records
     * created under one tenant must never be visible to, or deletable by, another tenant.
     */
    @Test
    public void readsAndDeletesAreTenantScoped() {
        Tenant defaultTenant = mock(Tenant.class);
        when(defaultTenant.getId()).thenReturn("default-tenant");
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(false);
        Tenant tenantA = mock(Tenant.class);
        when(tenantA.getId()).thenReturn("tenant-a");
        Tenant tenantB = mock(Tenant.class);
        when(tenantB.getId()).thenReturn("tenant-b");
        TaskStateService service = new TaskStateService(taskStateRepository, tenantContext, defaultTenant);

        // two traces under tenant A
        when(tenantContext.getCurrentTenant()).thenReturn(tenantA);
        service.taskStarted(TaskType.BPM, "execA1", "step", null);
        service.taskStarted(TaskType.BPM, "execA2", "step", null);
        Long tenantAId = service.getAll()
                                .get(0)
                                .getId();

        // one trace under tenant B
        when(tenantContext.getCurrentTenant()).thenReturn(tenantB);
        service.taskStarted(TaskType.BPM, "execB1", "step", null);

        // tenant B sees only its own trace and cannot read tenant A's by id
        assertEquals(1, service.getAll()
                               .size());
        assertEquals(1, service.getPages(PageRequest.of(0, 100))
                               .getTotalElements());
        assertEquals(0, service.findByExecution("execA1")
                               .size());
        assertThrows(IllegalArgumentException.class, () -> service.findById(tenantAId));

        // tenant A sees only its own traces and can read its own by id
        when(tenantContext.getCurrentTenant()).thenReturn(tenantA);
        assertEquals(2, service.getAll()
                               .size());
        assertEquals(1, service.findByExecution("execA1")
                               .size());
        TaskState tenantAState = service.findById(tenantAId);
        assertNotNull(tenantAState);

        // tenant B cannot delete tenant A's trace
        when(tenantContext.getCurrentTenant()).thenReturn(tenantB);
        assertThrows(IllegalArgumentException.class, () -> service.delete(tenantAState));
        when(tenantContext.getCurrentTenant()).thenReturn(tenantA);
        assertEquals(2, service.getAll()
                               .size());

        // deleting under tenant A leaves tenant B's trace intact
        service.deleteAll();
        assertEquals(0, service.getAll()
                               .size());
        when(tenantContext.getCurrentTenant()).thenReturn(tenantB);
        assertEquals(1, service.getAll()
                               .size());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
