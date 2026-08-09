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

import io.restassured.http.ContentType;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.ide.template.service.model.GeneratedFile;
import org.eclipse.dirigible.components.ide.template.service.model.ModelGenerationService;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts that the Java model-generation pipeline produces byte-identical output to the JavaScript
 * one it replaces.
 *
 * <p>
 * Each case seeds the same model file into two isolated workspaces, generates it with the
 * JavaScript endpoint in one and renders it with the Java pipeline against the other, then compares
 * every produced path and every byte of content. The {@code .gen} descriptor is compared along with
 * the rest, because its formatting is part of the contract - it is content-compared across
 * regenerations to decide whether a model changed.
 *
 * <p>
 * Two workspaces rather than two projects, for two reasons: generation folds in the entity
 * extensions that <em>sibling projects</em> contribute, so two copies of a fixture in one workspace
 * would each become the other's sibling; and the project name is baked into the generated output,
 * so the two copies have to carry the same name.
 *
 * <p>
 * This is the test that has to pass before any consumer is pointed at the Java pipeline. It needs
 * no browser.
 */
class GenerationParityIT extends IntegrationTest {

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

    /** The project both workspaces hold. Identical, because it is baked into the generated output. */
    private static final String PROJECT = "parity";

    /** The project descriptor, which several templates render by preserving the existing one. */
    private static final String PROJECT_DESCRIPTOR = "project.json";

    /** The sibling project contributing an entity extension to the model under generation. */
    private static final String CONTRIBUTOR = "contributor";

    /** The column the contributed extension adds, which the merged output has to carry. */
    private static final String CONTRIBUTED_COLUMN = "BOOK_ISBN";

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** The repository, for reading what the JavaScript pipeline wrote as another user. */
    @Autowired
    private IRepository repository;

    /** The Java pipeline under test. */
    @Autowired
    private ModelGenerationService modelGenerationService;

    /** The executor giving the JavaScript endpoint an authenticated request. */
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

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
            new Case("orders.glue", TEMPLATE_GLUE, false), //
            new Case("leave-request.form", TEMPLATE_FORM, false), //
            new Case("revenue.report", TEMPLATE_REPORT, false), //
            // The same model again, this time with another project contributing fields to its Book
            // entity - one new field to merge, and a primary key, a colliding name and an audit column
            // to skip.
            new Case("extended.model", TEMPLATE_SCHEMA, true), //
            new Case("extended.model", TEMPLATE_APPLICATION, true));

    /**
     * Runs every case in one go, reporting all differences rather than stopping at the first.
     *
     * <p>
     * One test method on purpose: the base class discards the application context after each method, so
     * a method per case would boot the whole application once per case. The failure message names the
     * fixture and the template, which is what a diagnosis needs.
     *
     * @throws IOException when a fixture or a template is missing, or rendering fails
     */
    @Test
    void everyTemplateMatchesTheJavaScriptPipeline() throws IOException {
        List<String> differences = new ArrayList<>();
        for (Case testCase : CASES) {
            differences.addAll(compare(testCase));
        }
        assertTrue(differences.isEmpty(), () -> "The Java pipeline diverged from the JavaScript one in " + differences.size()
                + " place(s):\n\n" + String.join("\n\n", differences));
    }

    /**
     * Generates one fixture with one template through both pipelines and collects the differences.
     *
     * @param testCase the case to run
     * @return the differences found, empty when the two pipelines agree
     * @throws IOException when a fixture or a template is missing, or rendering fails
     */
    private List<String> compare(Case testCase) throws IOException {
        String fixture = testCase.fixture();
        String templateId = testCase.templateId();
        String modelText = readFixture(fixture);
        // One workspace name per case, because the name is baked into the generated descriptor and so
        // has to match on both sides. The two copies do not collide: a workspace is scoped to the user
        // who owns it, and the two pipelines run as different users here - the JavaScript one over HTTP
        // as the test tenant, the Java one in-process on the test's own thread.
        String workspace = workspaceName(fixture, templateId);
        seedForJavaScript(workspace, fixture, modelText);
        // The JavaScript side is seeded first so the Java side can start from the very same project
        // descriptor. Several templates render project.json by returning the project's existing one
        // unchanged, so the two projects have to begin identical or that file can never match.
        String seededDescriptor = readProjectDescriptor(workspace);
        seedForJava(workspace, fixture, modelText, seededDescriptor);
        if (testCase.withContributor()) {
            seedContributor(workspace);
        }

        List<GeneratedFile> rendered = modelGenerationService.render(workspace, PROJECT, fixture, templateId, new LinkedHashMap<>());
        Map<String, String> actual = new TreeMap<>();
        for (GeneratedFile file : rendered) {
            actual.put(file.path(), file.content());
        }

        generateWithJavaScript(workspace, fixture, templateId);
        Map<String, String> expected = readGenerated(workspace, fixture);
        // A template that does not render the project descriptor leaves the seeded one sitting in the
        // project, where it is not generated output and does not belong in the comparison. A template
        // that does render it stays in and is compared - including the common case of a descriptor
        // whose rendering deliberately returns the project's existing content unchanged, which is why
        // "unchanged" alone cannot be the test.
        boolean renderedByJava = actual.containsKey(PROJECT_DESCRIPTOR);
        boolean unchangedSinceSeeding = seededDescriptor != null && seededDescriptor.equals(expected.get(PROJECT_DESCRIPTOR));
        if (!renderedByJava && unchangedSinceSeeding) {
            expected.remove(PROJECT_DESCRIPTOR);
        }

        String label = fixture + " with " + templateId;
        List<String> differences = new ArrayList<>();
        if (expected.isEmpty()) {
            differences.add("[" + label + "] the JavaScript pipeline generated nothing");
            return differences;
        }
        Set<String> onlyInJavaScript = new TreeSet<>(expected.keySet());
        onlyInJavaScript.removeAll(actual.keySet());
        if (!onlyInJavaScript.isEmpty()) {
            differences.add("[" + label + "] only the JavaScript pipeline generated: " + onlyInJavaScript);
        }
        Set<String> onlyInJava = new TreeSet<>(actual.keySet());
        onlyInJava.removeAll(expected.keySet());
        if (!onlyInJava.isEmpty()) {
            differences.add("[" + label + "] only the Java pipeline generated: " + onlyInJava);
        }
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String produced = actual.get(entry.getKey());
            if (produced != null && !entry.getValue()
                                          .equals(produced)) {
                differences.add("[" + label + "] content differs for " + entry.getKey() + "\n--- expected (JavaScript) ---\n"
                        + entry.getValue() + "\n--- actual (Java) ---\n" + produced);
            }
        }
        // Agreeing on nothing is not parity: if both pipelines dropped the contributed extension, every
        // file would still match. Assert the merged column actually reached the output.
        if (testCase.withContributor() && actual.values()
                                                .stream()
                                                .noneMatch(content -> content.contains(CONTRIBUTED_COLUMN))) {
            differences.add("[" + label + "] the contributed extension field " + CONTRIBUTED_COLUMN + " is missing from the output, so the"
                    + " cross-project entity extension was not merged at all");
        }
        return differences;
    }

    /**
     * Creates the workspace the Java pipeline reads, owned by whoever the test thread is.
     *
     * @param workspace the workspace name
     * @param fixture the model file name
     * @param modelText the model content
     * @param projectDescriptor the project.json to start from, so both sides begin identical
     */
    private void seedForJava(String workspace, String fixture, String modelText, String projectDescriptor) {
        Workspace workspaceObject = workspaceService.existsWorkspace(workspace) ? workspaceService.getWorkspace(workspace)
                : workspaceService.createWorkspace(workspace);
        Project project = workspaceObject.getProject(PROJECT);
        if (project == null || !project.exists()) {
            project = workspaceObject.createProject(PROJECT);
        }
        project.createFile(fixture, modelText.getBytes(StandardCharsets.UTF_8));
        if (projectDescriptor != null) {
            project.createFile(PROJECT_DESCRIPTOR, projectDescriptor.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Seeds the sibling project that contributes an entity extension to the model under generation,
     * into both users' copies of the workspace.
     *
     * <p>
     * Generation scans every sibling project of the model's own workspace for extension entities naming
     * this project, and folds their fields into the entities they reference. The contributor is
     * therefore only visible to a pipeline that reads the workspace it was seeded into, which is why it
     * has to be seeded twice, once per user.
     *
     * @param workspace the workspace name
     * @throws IOException when the contributor fixture is missing
     */
    private void seedContributor(String workspace) throws IOException {
        String contributorModel = readFixture(CONTRIBUTOR + ".model");
        restAssuredExecutor.execute(() -> {
            given().when()
                   .post("/services/ide/workspaces/{workspace}/{project}", workspace, CONTRIBUTOR)
                   .then()
                   .statusCode(anyOf(is(201), is(304)));
            given().contentType(ContentType.TEXT)
                   .body(contributorModel)
                   .when()
                   .post("/services/ide/workspaces/{workspace}/{project}/{path}", workspace, CONTRIBUTOR, CONTRIBUTOR + ".model")
                   .then()
                   .statusCode(anyOf(is(201), is(304)));
        });
        Workspace workspaceObject = workspaceService.getWorkspace(workspace);
        Project contributor = workspaceObject.getProject(CONTRIBUTOR);
        if (contributor == null || !contributor.exists()) {
            contributor = workspaceObject.createProject(CONTRIBUTOR);
        }
        contributor.createFile(CONTRIBUTOR + ".model", contributorModel.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reads the project descriptor the workspace endpoint wrote when it created the project.
     *
     * @param workspace the workspace name
     * @return the descriptor content, or null when there is none
     */
    private String readProjectDescriptor(String workspace) {
        IResource resource = repository.getResource(projectPath(workspace) + "/" + PROJECT_DESCRIPTOR);
        return resource.exists() ? new String(resource.getContent(), StandardCharsets.UTF_8) : null;
    }

    /**
     * Creates the workspace the JavaScript pipeline reads, owned by the authenticated test tenant.
     *
     * @param workspace the workspace name
     * @param fixture the model file name
     * @param modelText the model content
     */
    private void seedForJavaScript(String workspace, String fixture, String modelText) {
        restAssuredExecutor.execute(() -> {
            // Both creations are idempotent and answer 304 once the resource is there.
            given().when()
                   .post("/services/ide/workspaces/{workspace}", workspace)
                   .then()
                   .statusCode(anyOf(is(201), is(304)));
            given().when()
                   .post("/services/ide/workspaces/{workspace}/{project}", workspace, PROJECT)
                   .then()
                   .statusCode(anyOf(is(201), is(304)));
            given().contentType(ContentType.TEXT)
                   .body(modelText)
                   .when()
                   .post("/services/ide/workspaces/{workspace}/{project}/{path}", workspace, PROJECT, fixture)
                   .then()
                   .statusCode(anyOf(is(201), is(304)));
        });
    }

    /**
     * Runs the JavaScript generation endpoint.
     *
     * @param workspace the workspace name
     * @param fixture the model file name
     * @param templateId the template's module path
     */
    private void generateWithJavaScript(String workspace, String fixture, String templateId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("template", templateId);
        payload.put("parameters", new LinkedHashMap<>());
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(payload)
                                                 .queryParam("path", fixture)
                                                 .when()
                                                 .post("/services/js/service-generate/generate.mjs/model/{workspace}/{project}", workspace,
                                                         PROJECT)
                                                 .then()
                                                 .statusCode(201));
    }

    /**
     * Reads everything the JavaScript pipeline wrote into its project, keyed by project-relative path,
     * excluding the model file it was generated from.
     *
     * <p>
     * Read straight out of the repository rather than through the workspace service, because a
     * workspace path is resolved against the <em>calling</em> user and the files belong to the
     * authenticated test tenant rather than to the test thread.
     *
     * @param workspace the workspace name
     * @param fixture the model file name
     * @return the written files
     */
    private Map<String, String> readGenerated(String workspace, String fixture) {
        String projectPath = projectPath(workspace);
        Map<String, String> written = new TreeMap<>();
        collect(repository.getCollection(projectPath), projectPath, written);
        written.remove(fixture);
        return written;
    }

    /**
     * Builds the repository path of the project the JavaScript pipeline works in.
     *
     * @param workspace the workspace name
     * @return the repository path
     */
    private static String projectPath(String workspace) {
        return IRepositoryStructure.PATH_USERS + "/" + DirigibleTestTenant.createDefaultTenant()
                                                                          .getUsername()
                + "/" + workspace + "/" + PROJECT;
    }

    /**
     * Collects a collection's resources recursively.
     *
     * @param collection the collection
     * @param projectPath the project's own path, to make the collected paths relative
     * @param collected the files collected so far
     */
    private static void collect(ICollection collection, String projectPath, Map<String, String> collected) {
        if (!collection.exists()) {
            return;
        }
        for (IResource resource : collection.getResources()) {
            collected.put(resource.getPath()
                                  .substring(projectPath.length() + 1),
                    new String(resource.getContent(), StandardCharsets.UTF_8));
        }
        for (ICollection child : collection.getCollections()) {
            collect(child, projectPath, collected);
        }
    }

    /**
     * Reads a fixture from the test resources.
     *
     * @param fixture the fixture file name
     * @return the fixture content
     * @throws IOException when the fixture is missing
     */
    private String readFixture(String fixture) throws IOException {
        try (InputStream in = GenerationParityIT.class.getResourceAsStream("/GenerationParityIT/" + fixture)) {
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
     * @param pipeline the pipeline the workspace belongs to
     * @return the workspace name
     */
    private static String workspaceName(String fixture, String templateId) {
        String template = templateId.substring(templateId.lastIndexOf('/') + 1, templateId.lastIndexOf('.'));
        String module = templateId.substring(0, templateId.indexOf('/'));
        return ("parity-" + baseName(fixture) + "-" + module + "-" + template).toLowerCase()
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
