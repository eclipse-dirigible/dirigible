/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.project.ProjectAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * The Class WorkspacesCoreServiceTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class ActionsServiceTest {


    /** The actions service. */
    @Autowired
    private ActionsService actionsService;

    /** The workspaces core service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** Captures what the service logs while a test runs. */
    private ListAppender<ILoggingEvent> appender;

    /** The logger of the service under test. */
    private Logger serviceLogger;

    /** The project json content. */
    private static final String PROJECT_JSON_CONTENT = """
            {
            	  "guid": "TestProject1",
            	  "actions": [{
            		  "name": "MyAction",
            		  "commands": [
            			  {
            				"os": "unix",
            				"command": "echo test"
            			  },
            			  {
            			    "os": "windows",
            		  	    "command": "cmd /c echo test"
            			  }
            		  ],
            		  "publish": "true"
            	  }]
            }
            """;

    /** A descriptor without an actions section - what a project that declares none looks like. */
    private static final String PROJECT_JSON_WITHOUT_ACTIONS = """
            {
            	  "guid": "TestProject2"
            }
            """;

    /**
     * Attaches the log appender. Spring re-initializes logback while the application context starts, so
     * the appender is attached per test rather than in a field initializer.
     */
    @BeforeEach
    public void attachLogAppender() {
        appender = new ListAppender<>();
        appender.start();
        serviceLogger = (Logger) LoggerFactory.getLogger(ActionsService.class);
        serviceLogger.addAppender(appender);
    }

    /**
     * Detaches the log appender.
     */
    @AfterEach
    public void detachLogAppender() {
        serviceLogger.detachAppender(appender);
        appender.stop();
    }


    /**
     * Publish with action test.
     */
    @Test
    public void publishWithActionTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        assertNotNull(workspace1);
        assertNotNull(workspace1.getInternal());
        assertEquals("TestWorkspace1", workspace1.getName());
        assertEquals("/users/guest/TestWorkspace1", workspace1.getInternal()
                                                              .getPath());
        Project project1 = workspaceService.createProject("TestWorkspace1", "TestProject1");
        assertNotNull(project1);
        assertNotNull(project1.getInternal());
        assertEquals("TestProject1", project1.getName());
        assertEquals("/users/guest/TestWorkspace1/TestProject1", project1.getInternal()
                                                                         .getPath());
        File projectJson = workspaceService.createFile("TestWorkspace1", "TestProject1", "project.json", PROJECT_JSON_CONTENT.getBytes(),
                "application/json");
        assertNotNull(projectJson);
        assertNotNull(projectJson.getInternal());
        assertEquals("project.json", projectJson.getName());
        assertEquals("/users/guest/TestWorkspace1/TestProject1/project.json", projectJson.getInternal()
                                                                                         .getPath());
        int result = actionsService.executeAction("TestWorkspace1", "TestProject1", "MyAction");
        assertEquals(0, result);
        workspaceService.deleteWorkspace("TestWorkspace1");
    }


    /**
     * Publish without action test.
     */
    @Test
    public void publishWithoutActionTest() {
        Configuration.set("DIRIGIBLE_PROJECT_TYPESCRIPT", "false");
        try {
            Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
            assertNotNull(workspace1);
            assertNotNull(workspace1.getInternal());
            assertEquals("TestWorkspace1", workspace1.getName());
            assertEquals("/users/guest/TestWorkspace1", workspace1.getInternal()
                                                                  .getPath());
            Project project1 = workspaceService.createProject("TestWorkspace1", "TestProject1");
            assertNotNull(project1);
            assertNotNull(project1.getInternal());
            assertEquals("TestProject1", project1.getName());
            assertEquals("/users/guest/TestWorkspace1/TestProject1", project1.getInternal()
                                                                             .getPath());
            int result = actionsService.executeAction("TestWorkspace1", "TestProject1", "MyAction");
            assertEquals(-1, result);
            workspaceService.deleteWorkspace("TestWorkspace1");
        } finally {
            Configuration.set("DIRIGIBLE_PROJECT_TYPESCRIPT", "true");
        }
    }

    /**
     * A project descriptor without an actions section is the normal case - it yields no actions and
     * must not be reported as an error.
     */
    @Test
    public void projectWithoutActionsSectionIsNotAnError() {
        workspaceService.createWorkspace("TestWorkspace2");
        workspaceService.createProject("TestWorkspace2", "TestProject2");
        workspaceService.createFile("TestWorkspace2", "TestProject2", "project.json", PROJECT_JSON_WITHOUT_ACTIONS.getBytes(),
                "application/json");
        try {
            List<ProjectAction> actions = actionsService.listRegisteredActions("TestWorkspace2", "TestProject2");
            assertTrue(actions.isEmpty());

            int result = actionsService.executeAction("TestWorkspace2", "TestProject2", "MyAction");
            assertEquals(-1, result);

            List<String> errors = loggedErrors();
            assertTrue(errors.isEmpty(), "unexpected error logged: " + errors);
        } finally {
            workspaceService.deleteWorkspace("TestWorkspace2");
        }
    }

    /**
     * Logged errors.
     *
     * @return the messages the service under test logged at ERROR
     */
    private List<String> loggedErrors() {
        return appender.list.stream()
                            .filter(event -> event.getLevel() == Level.ERROR)
                            .map(ILoggingEvent::getFormattedMessage)
                            .toList();
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
