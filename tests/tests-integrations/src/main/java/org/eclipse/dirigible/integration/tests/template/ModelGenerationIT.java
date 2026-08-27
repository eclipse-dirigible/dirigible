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
import java.util.regex.Matcher;
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

    /**
     * A Velocity reference that survived rendering. {@code ${JavaTask}} is excluded: it is a literal
     * Flowable delegate expression that generated code carries on purpose.
     */
    private static final Pattern UNRESOLVED_REFERENCE = Pattern.compile("\\$\\{(?!JavaTask\\b)[A-Za-z]\\w*\\}?");

    /**
     * An indented {@code package} or {@code import}, which Java never has. It is what template
     * whitespace looks like once it reaches the output: Velocity gobbles the indentation of a line
     * holding only a directive but not of a line holding only a {@code ##} comment, so an indented
     * comment inside a loop emits its leading spaces once per iteration and they flush in front of the
     * next literal the template renders - the first import (#6754).
     */
    private static final Pattern INDENTED_TOP_LEVEL_DECLARATION = Pattern.compile("^([ \\t]+)(package|import)[ \\t]", Pattern.MULTILINE);

    /**
     * Two or more consecutive blank lines, which Java never has. It is what a collapsed
     * {@code #if}/{@code #end} block leaves behind: Velocity gobbles the newline of a line holding only
     * a directive but keeps the literal blank lines around it, so a block that emits nothing when its
     * condition is false yet is surrounded by blank-line separators leaves both separators back to back
     * (#6823).
     */
    private static final Pattern CONSECUTIVE_BLANK_LINES = Pattern.compile("\\n[ \\t]*\\n[ \\t]*\\n");

    /**
     * A member access whose member is missing - {@code parent. == null}, {@code target. = row.X} or
     * {@code derived.put("X", parent.)} - which is what an emitted-code helper produces when the field
     * name it interpolates is empty. Deliberately same-line only: generated code breaks a chained call
     * after the dot, so a dot at end of line is normal and its member is on the next one.
     *
     * <p>
     * Matched against {@link #code(String)} rather than the raw file: outside code the same three
     * characters are ordinary prose. A parenthetical remark that ends in a full stop - "(... a flow
     * that never runs.)" - reads as {@code s.)}, and a template comment worded that way turned master
     * red for two days (#6862) while the generated code it described was perfectly sound.
     */
    private static final Pattern DANGLING_MEMBER_ACCESS = Pattern.compile("\\w\\.[ \\t]*[=;),!]");

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
        List<String> problems = new ArrayList<>(danglingDetectorProblems());
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
            problems.addAll(unresolvedReferences(label, file.getKey(), file.getValue()));
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

    /**
     * Collects what would not compile in a rendered {@code .java} file.
     *
     * <p>
     * Generated Java is the one output whose validity can be judged without running it, and it is the
     * output where a rendering gap is most expensive: the client-Java runtime compiles the whole
     * registry in ONE javac task, so a single broken generated file unregisters every controller in the
     * application - the symptom is a 404 on an endpoint that has nothing to do with the defect. Both
     * checks below are here because both defects reached master:
     * <ul>
     * <li>a surviving {@code ${reference}} - Velocity renders an undefined reference verbatim, so a key
     * the binder forgot to put in the context (it was {@code fromGenFolder}) becomes a syntax error
     * rather than a missing value;</li>
     * <li>a dangling {@code .} - what an emitted-code helper produces when a field name it interpolates
     * is the empty string, which is what the intent glue writes for an unused optional field;</li>
     * <li>an indented {@code package} or {@code import} - template whitespace that leaked into the
     * output. It still compiles, but {@code gen/} is committed so that template changes are diffable,
     * and a leak puts a run of spaces into every generated file of every module.</li>
     * <li>two consecutive blank lines - what a collapsed {@code #if}/{@code #end} block whose
     * surrounding separators both survive leaves behind. Harmless to javac, but it modifies every
     * generated file that omits the block, obscuring real changes in review (#6823).</li>
     * </ul>
     *
     * @param label the case label
     * @param path the rendered file path
     * @param content the rendered content
     * @return the problems found, empty when the file is sound
     */
    private static List<String> unresolvedReferences(String label, String path, String content) {
        if (!path.endsWith(".java") || content == null) {
            return List.of();
        }
        List<String> problems = new ArrayList<>();
        Matcher reference = UNRESOLVED_REFERENCE.matcher(content);
        while (reference.find()) {
            problems.add("[" + label + "] " + path + " kept an unresolved template reference: " + reference.group());
        }
        Matcher dangling = DANGLING_MEMBER_ACCESS.matcher(code(content));
        while (dangling.find()) {
            problems.add("[" + label + "] " + path + " emitted a member access with no member, so an interpolated field name was"
                    + " empty: " + dangling.group()
                                           .trim());
        }
        Matcher indented = INDENTED_TOP_LEVEL_DECLARATION.matcher(content);
        while (indented.find()) {
            problems.add("[" + label + "] " + path + " preceded its " + indented.group(2) + " with " + indented.group(1)
                                                                                                               .length()
                    + " characters of whitespace, so a template leaked its indentation into the output");
        }
        Matcher blankLines = CONSECUTIVE_BLANK_LINES.matcher(content);
        while (blankLines.find()) {
            problems.add("[" + label + "] " + path + " has two consecutive blank lines, so a collapsed template block left both"
                    + " of its surrounding separators in the output");
        }
        return problems;
    }

    /**
     * What {@link #DANGLING_MEMBER_ACCESS} must and must not see, pinned on literal samples.
     *
     * <p>
     * The detector reports a defect in someone else's template, so a false positive costs whoever wrote
     * that template a red master and a hunt through generated code that turns out to be correct - which
     * is exactly what a comment reading "(... a flow that never runs.)" did (#6862). Checked here
     * rather than in a test of its own because the base class discards the application context per
     * method, and these two samples do not need an application at all.
     *
     * @return the problems found, empty when the detector draws the line where it should
     */
    private static List<String> danglingDetectorProblems() {
        List<String> problems = new ArrayList<>();
        for (String prose : List.of("        // duplicate start rather than a flow that never runs.)",
                "        LOG.warn(\"the record's own flow ended.\");", "        /* a remark that ends in a full stop. */")) {
            if (DANGLING_MEMBER_ACCESS.matcher(code(prose))
                                      .find()) {
                problems.add("[detector] a comment or a string literal was read as a dangling member access: " + prose.trim());
            }
        }
        for (String broken : List.of("        if (entity. == null) {", "        target. = row.X;",
                "        derived.put(\"X\", parent.);")) {
            if (!DANGLING_MEMBER_ACCESS.matcher(code(broken))
                                       .find()) {
                problems.add("[detector] an empty interpolated field name went unnoticed: " + broken.trim());
            }
        }
        return problems;
    }

    /**
     * The file with its comments and its string/character literals blanked out, so a pattern that
     * describes CODE is not matched against prose.
     *
     * <p>
     * Blanked rather than removed: every remaining character keeps its offset and every line keeps its
     * length, so what a match reports still lines up with the file the reader opens.
     *
     * @param content the rendered file
     * @return the same text with everything that is not code replaced by spaces
     */
    private static String code(String content) {
        char[] out = content.toCharArray();
        int index = 0;
        while (index < out.length) {
            char current = content.charAt(index);
            char next = index + 1 < out.length ? content.charAt(index + 1) : 0;
            if (current == '/' && next == '/') {
                while (index < out.length && content.charAt(index) != '\n') {
                    out[index++] = ' ';
                }
            } else if (current == '/' && next == '*') {
                out[index++] = ' ';
                out[index++] = ' ';
                while (index < out.length
                        && !(content.charAt(index) == '*' && index + 1 < out.length && content.charAt(index + 1) == '/')) {
                    blank(out, content, index++);
                }
                // An unterminated block comment runs to the end of the file; there is no closer to skip.
                if (index + 1 < out.length) {
                    out[index++] = ' ';
                    out[index++] = ' ';
                }
            } else if (current == '"' || current == '\'') {
                // A quote inside a comment never reaches here - the comment consumed it - so a string
                // opened here is a real literal, and an escaped quote inside it does not close it.
                out[index++] = ' ';
                while (index < out.length && content.charAt(index) != current) {
                    boolean escape = content.charAt(index) == '\\' && index + 1 < out.length;
                    blank(out, content, index++);
                    if (escape) {
                        blank(out, content, index++);
                    }
                }
                if (index < out.length) {
                    out[index++] = ' ';
                }
            } else {
                index++;
            }
        }
        return new String(out);
    }

    /** Blanks one character, keeping newlines so line numbers and line lengths survive. */
    private static void blank(char[] out, String content, int index) {
        if (content.charAt(index) != '\n') {
            out[index] = ' ';
        }
    }

}
