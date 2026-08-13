/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.template;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.ide.template.service.model.GeneratedFile;
import org.eclipse.dirigible.components.ide.template.service.model.ModelGenerationService;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the model-generation pipeline over every template it serves.
 *
 * <p>
 * This started as a parity harness: it generated each fixture through the JavaScript pipeline and
 * through the Java one and compared them byte for byte, which is what let the Java port replace the
 * JavaScript path. The JavaScript path is gone, and with it the oracle - so what remains is the net
 * that oracle left behind: every template still renders, renders the same thing twice, and renders
 * something for each of the shapes that used to be compared (the entity layers, the process glue, a
 * form, a report, a mapping, the additional-page views, and a model another project contributes
 * fields to).
 *
 * <p>
 * Rendering twice is not a formality. The rendered graph is handed to template engines that have
 * been caught writing into it (the Mustache engine adds an indexed twin of every collection), and
 * the descriptor recording what a model was generated with is re-read on the next regeneration - so
 * anything a render leaves behind in the parameters accumulates in a committed file. The second
 * render, and the descriptor assertion below, are what would catch that coming back.
 */
class ModelGenerationIT extends IntegrationTest {

    /** The template rendering the database schema. */
    private static final String TEMPLATE_SCHEMA = "template-application-schema/template/template.js";

    /** The template rendering the Java data-access layer. */
    private static final String TEMPLATE_DAO = "template-application-dao-java/template/template.js";

    /** The template rendering the Java REST layer. */
    private static final String TEMPLATE_REST = "template-application-rest-java/template/template.js";

    /** The template rendering the full application stack. */
    private static final String TEMPLATE_APPLICATION = "template-application-ui-harmonia-java/template/template.js";

    /** The template rendering the process glue. */
    private static final String TEMPLATE_GLUE = "template-application-events-java/template/template.js";

    /** The template rendering a form. */
    private static final String TEMPLATE_FORM = "template-form-builder-harmonia/template/template.js";

    /** The template rendering a standalone report. */
    private static final String TEMPLATE_REPORT = "template-application-ui-harmonia-java/template/template-report-file.js";

    /** The template rendering a mapping into a client-Java mapper. */
    private static final String TEMPLATE_MAPPING = "template-mapping-java/template/template.js";

    /** The project every case generates in. */
    private static final String PROJECT = "generation";

    /** The extension of the descriptor recording what a model was generated with. */
    private static final String DESCRIPTOR_EXTENSION = ".gen";

    /** The sibling project contributing an entity extension to the model under generation. */
    private static final String CONTRIBUTOR = "contributor";

    /** The column the contributed extension adds, which the merged output has to carry. */
    private static final String CONTRIBUTED_COLUMN = "BOOK_ISBN";

    /**
     * A key an engine appended to the parameter graph rather than one a generator put there: the
     * Mustache engine names its indexed collection twins by suffixing an underscore.
     */
    private static final Pattern ENGINE_ADDED_KEY = Pattern.compile("\"[A-Za-z0-9]+_\"\\s*:");

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** The pipeline under test. */
    @Autowired
    private ModelGenerationService modelGenerationService;

    /**
     * One fixture generated with one template.
     *
     * @param fixture the model file name
     * @param templateId the template's module path
     * @param withContributor whether a sibling project contributing an entity extension is seeded
     *        alongside, which makes generation fold its fields into this model
     */
    private record Case(String fixture, String templateId, boolean withContributor) {
    }

    /** Every fixture paired with each template that generates from it. */
    private static final List<Case> CASES = List.of(//
            new Case("sales-order.model", TEMPLATE_SCHEMA, false), //
            new Case("sales-order.model", TEMPLATE_DAO, false), //
            new Case("sales-order.model", TEMPLATE_REST, false), //
            new Case("sales-order.model", TEMPLATE_APPLICATION, false), //
            new Case("simple.model", TEMPLATE_SCHEMA, false), //
            new Case("simple.model", TEMPLATE_APPLICATION, false), //
            // The additional-page views (#6547): a calendar entity and a slot-picker entity, each
            // keeping its own MANAGE layout - the partitions no other fixture exercises, because none
            // of them declares a view.
            new Case("views.model", TEMPLATE_APPLICATION, false), //
            new Case("orders.glue", TEMPLATE_GLUE, false), //
            new Case("leave-request.form", TEMPLATE_FORM, false), //
            new Case("revenue.report", TEMPLATE_REPORT, false), //
            // The mapping compiler: the one template whose preparation derives Java expressions rather
            // than marshalling a model.
            new Case("prices.mapping", TEMPLATE_MAPPING, false), //
            // The same model again, this time with another project contributing fields to its Book
            // entity - one new field to merge, and a primary key, a colliding name and an audit column
            // to skip.
            new Case("extended.model", TEMPLATE_SCHEMA, true), //
            new Case("extended.model", TEMPLATE_APPLICATION, true));

    /**
     * Runs every case in one go, reporting all problems rather than stopping at the first.
     *
     * <p>
     * One test method on purpose: the base class discards the application context after each method, so
     * a method per case would boot the whole application once per case. Each message names the fixture
     * and the template, which is what a diagnosis needs.
     *
     * @throws IOException when a fixture or a template is missing, or rendering fails
     */
    @Test
    void everyTemplateRendersItsModel() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Case testCase : CASES) {
            problems.addAll(check(testCase));
        }
        assertTrue(problems.isEmpty(),
                () -> "The generation pipeline has " + problems.size() + " problem(s):\n\n" + String.join("\n\n", problems));
    }

    /**
     * Renders one fixture with one template, twice, and collects what is wrong with the result.
     *
     * @param testCase the case to run
     * @return the problems found, empty when the case is sound
     * @throws IOException when a fixture or a template is missing, or rendering fails
     */
    private List<String> check(Case testCase) throws IOException {
        String fixture = testCase.fixture();
        String templateId = testCase.templateId();
        String workspace = workspaceName(fixture, templateId);
        seed(workspace, fixture, readFixture(fixture));
        if (testCase.withContributor()) {
            seed(workspace, CONTRIBUTOR, CONTRIBUTOR + ".model", readFixture(CONTRIBUTOR + ".model"));
        }

        String label = fixture + " with " + templateId;
        List<String> problems = new ArrayList<>();
        Map<String, String> rendered = render(workspace, fixture, templateId);
        if (rendered.isEmpty()) {
            problems.add("[" + label + "] rendered nothing");
            return problems;
        }

        for (Map.Entry<String, String> file : rendered.entrySet()) {
            if (file.getValue() == null || file.getValue()
                                               .isBlank()) {
                problems.add("[" + label + "] rendered an empty file: " + file.getKey());
            }
            if (file.getKey()
                    .contains("{{")) {
                problems.add("[" + label + "] a rendered path kept its placeholder: " + file.getKey());
            }
        }
        String descriptorPath = baseName(fixture) + DESCRIPTOR_EXTENSION;
        String descriptor = rendered.get(descriptorPath);
        if (descriptor == null) {
            problems.add("[" + label + "] rendered no " + descriptorPath + " descriptor, which regeneration reads to decide what changed");
        } else if (ENGINE_ADDED_KEY.matcher(descriptor)
                                   .find()) {
            problems.add("[" + label + "] the descriptor carries a key an engine added to the parameter graph, which regeneration would"
                    + " accumulate:\n" + descriptor);
        }

        // Same input, same output: the pipeline may not carry state from one render into the next.
        Map<String, String> again = render(workspace, fixture, templateId);
        TreeSet<String> firstPaths = new TreeSet<>(rendered.keySet());
        TreeSet<String> secondPaths = new TreeSet<>(again.keySet());
        if (!firstPaths.equals(secondPaths)) {
            problems.add("[" + label + "] rendering twice produced different files: " + firstPaths + " then " + secondPaths);
        } else {
            for (Map.Entry<String, String> file : rendered.entrySet()) {
                if (!file.getValue()
                         .equals(again.get(file.getKey()))) {
                    problems.add("[" + label + "] rendering twice produced different content for " + file.getKey() + "\n--- first ---\n"
                            + file.getValue() + "\n--- second ---\n" + again.get(file.getKey()));
                }
            }
        }

        // Agreeing on nothing is not coverage: assert the contributed column actually reached the
        // output, or a pipeline that dropped the extension entirely would still look sound.
        if (testCase.withContributor() && rendered.values()
                                                  .stream()
                                                  .noneMatch(content -> content.contains(CONTRIBUTED_COLUMN))) {
            problems.add("[" + label + "] the contributed extension field " + CONTRIBUTED_COLUMN + " is missing from the output, so the"
                    + " cross-project entity extension was not merged at all");
        }
        return problems;
    }

    /**
     * Renders one case, without writing anything into the project.
     *
     * @param workspace the workspace name
     * @param fixture the model file name
     * @param templateId the template's module path
     * @return the rendered files, keyed by project-relative path
     * @throws IOException when a fixture or a template is missing, or rendering fails
     */
    private Map<String, String> render(String workspace, String fixture, String templateId) throws IOException {
        Map<String, String> files = new TreeMap<>();
        for (GeneratedFile file : modelGenerationService.render(workspace, PROJECT, fixture, templateId, new LinkedHashMap<>())) {
            files.put(file.path(), file.content());
        }
        return files;
    }

    /**
     * Seeds a model file into the case's own project.
     *
     * @param workspace the workspace name
     * @param fixture the model file name
     * @param content the model content
     */
    private void seed(String workspace, String fixture, String content) {
        seed(workspace, PROJECT, fixture, content);
    }

    /**
     * Seeds a model file into a project of the case's workspace, creating both as needed.
     *
     * @param workspace the workspace name
     * @param project the project name
     * @param fixture the model file name
     * @param content the model content
     */
    private void seed(String workspace, String project, String fixture, String content) {
        Workspace workspaceObject = workspaceService.existsWorkspace(workspace) ? workspaceService.getWorkspace(workspace)
                : workspaceService.createWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        if (projectObject == null || !projectObject.exists()) {
            projectObject = workspaceObject.createProject(project);
        }
        projectObject.createFile(fixture, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reads a fixture from the test resources.
     *
     * @param fixture the fixture file name
     * @return the fixture content
     * @throws IOException when the fixture is missing
     */
    private String readFixture(String fixture) throws IOException {
        try (InputStream in = ModelGenerationIT.class.getResourceAsStream("/ModelGenerationIT/" + fixture)) {
            if (in == null) {
                throw new IOException("Missing fixture [" + fixture + "]");
            }
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    /**
     * Builds a workspace name unique to one case, so the cases stay isolated from each other.
     *
     * @param fixture the fixture file name
     * @param templateId the template's module path
     * @return the workspace name
     */
    private static String workspaceName(String fixture, String templateId) {
        String template = templateId.substring(templateId.lastIndexOf('/') + 1, templateId.lastIndexOf('.'));
        String module = templateId.substring(0, templateId.indexOf('/'));
        return ("generation-" + baseName(fixture) + "-" + module + "-" + template).toLowerCase()
                                                                                  .replaceAll("[^a-z0-9-]", "-");
    }

    /**
     * Reads a model file's base name, the way generation derives it.
     *
     * @param fixture the fixture file name
     * @return the base name
     */
    private static String baseName(String fixture) {
        int dot = fixture.indexOf('.');
        return dot > 0 ? fixture.substring(0, dot) : fixture;
    }

}
