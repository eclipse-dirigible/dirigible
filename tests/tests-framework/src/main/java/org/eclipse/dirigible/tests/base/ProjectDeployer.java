/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import com.google.gson.reflect.TypeToken;
import io.restassured.http.ContentType;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.eclipse.dirigible.tests.framework.util.SynchronizationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;

/**
 * Brings a fixture project into the running instance without opening a browser: the project is
 * copied into the default user's workspace, published through the very endpoints the IDE's Publish
 * and the entity modeler's Regenerate call, and the synchronizers are then run to completion.
 *
 * <p>
 * It exists for the tests whose verification is a REST call, a log assertion or a database query -
 * there the browser's only job is pressing Publish, and paying for Chrome and a login to press it
 * costs more than the test itself.
 */
@Lazy
@Component
public class ProjectDeployer {

    /** The workspace every fixture is deployed from - the one the IDE opens by default. */
    private static final String WORKSPACE = "workspace";

    /** The extension of the descriptor a model file is generated with. */
    private static final String DESCRIPTOR_EXTENSION = ".gen";

    /**
     * The descriptor keys the entity modeler sends as request fields of their own rather than as
     * template parameters. Its Regenerate destructures exactly these out of the loaded descriptor
     * before posting the rest as the parameters, so a deployment that wants the same output has to
     * split them the same way.
     */
    private static final Set<String> DESCRIPTOR_REQUEST_FIELDS =
            Set.of("models", "perspectives", "templateId", "filePath", "workspaceName", "projectName");

    /** The type the generation descriptor is read as. */
    private static final Type DESCRIPTOR_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDeployer.class);

    private final ProjectUtil projectUtil;
    private final RestAssuredExecutor restAssuredExecutor;
    private final IRepository repository;
    private final SynchronizationProcessor synchronizationProcessor;

    ProjectDeployer(ProjectUtil projectUtil, RestAssuredExecutor restAssuredExecutor, IRepository repository,
            SynchronizationProcessor synchronizationProcessor) {
        this.projectUtil = projectUtil;
        this.restAssuredExecutor = restAssuredExecutor;
        this.repository = repository;
        this.synchronizationProcessor = synchronizationProcessor;
    }

    /**
     * Copies the fixture project into the default user's workspace, publishes it and runs the
     * synchronizers, so that on return the project's artefacts are live.
     *
     * @param projectResourcesFolder the test resources folder holding the project
     */
    public void deploy(String projectResourcesFolder) {
        String project = ProjectUtil.extractFolderName(projectResourcesFolder);

        projectUtil.copyResourceProjectToDefaultUserWorkspace(projectResourcesFolder);
        publish(project);
        synchronize();
    }

    /**
     * Same as {@link #deploy(String)}, with the project's model file generated first - the headless
     * counterpart of opening the model in the entity modeler and pressing Regenerate. Generation
     * publishes the project itself, exactly as it does for the modeler.
     *
     * @param projectResourcesFolder the test resources folder holding the project
     * @param modelFileName the project-relative path of the model file to generate from
     */
    public void deployGeneratedFromModel(String projectResourcesFolder, String modelFileName) {
        String project = ProjectUtil.extractFolderName(projectResourcesFolder);

        projectUtil.copyResourceProjectToDefaultUserWorkspace(projectResourcesFolder);
        generateFromModel(project, modelFileName);
        synchronize();
    }

    private void publish(String project) {
        LOGGER.info("Publishing project [{}] of workspace [{}]", project, WORKSPACE);

        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                 .then()
                                                 .statusCode(200));
    }

    private void generateFromModel(String project, String modelFileName) {
        Map<String, Object> descriptor = readDescriptor(project, modelFileName);

        Object templateId = descriptor.get("templateId");
        if (templateId == null) {
            throw new IllegalStateException("Model [" + modelFileName + "] of project [" + project + "] has no template in its "
                    + DESCRIPTOR_EXTENSION + " descriptor. The modeler would ask which template to use, which a test cannot answer.");
        }

        Map<String, Object> parameters = new LinkedHashMap<>(descriptor);
        DESCRIPTOR_REQUEST_FIELDS.forEach(parameters::remove);

        LOGGER.info("Generating [{}] of project [{}] with template [{}]", modelFileName, project, templateId);

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("template", templateId, "parameters", parameters))
                                                 .when()
                                                 .post("/services/ide/generate/model/" + WORKSPACE + "/" + project + "?path="
                                                         + modelFileName)
                                                 .then()
                                                 .statusCode(201));
    }

    private Map<String, Object> readDescriptor(String project, String modelFileName) {
        int dot = modelFileName.lastIndexOf('.');
        String descriptorPath = (dot < 0 ? modelFileName : modelFileName.substring(0, dot)) + DESCRIPTOR_EXTENSION;

        String user = DirigibleTestTenant.createDefaultTenant()
                                         .getUsername();
        String location = IRepositoryStructure.PATH_USERS + "/" + user + "/" + WORKSPACE + "/" + project + "/" + descriptorPath;

        IResource resource = repository.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Missing generation descriptor [" + location + "]");
        }
        return JsonHelper.fromJson(new String(resource.getContent(), StandardCharsets.UTF_8), DESCRIPTOR_TYPE);
    }

    /**
     * Runs a full synchronization pass and waits for the follow-up the registry watcher schedules for
     * the freshly published files, so the caller can assert on live artefacts rather than on a race.
     */
    private void synchronize() {
        synchronizationProcessor.forceProcessSynchronizers();
        SynchronizationUtil.waitForSynchronizationExecution();
    }
}
