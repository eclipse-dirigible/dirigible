/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.ide.template.service.model.GeneratedFile;
import org.eclipse.dirigible.components.ide.template.service.model.ModelGenerationService;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Every Harmonia surface conforms to the pinned Harmonia release, as the release itself describes
 * it.
 *
 * <p>
 * Harmonia fails in one way nothing else catches: a directive it does not register, a utility class
 * it does not ship, a button variant it does not know or an icon placeholder it replaces (so the
 * Alpine bindings on it throw) all render SOMETHING - the element stays unstyled, the class does
 * nothing, the variant falls back to the default look - and no console, no server log and no DOM
 * assertion says so. An audit found dozens of such tokens across the shells and the generated
 * application templates, all of them green through every build.
 *
 * <p>
 * So the contract is read out of the pinned webjar rather than restated here: the directive
 * registry out of {@code dist/harmonia.min.js} (plus the Lucide plugin), the utility allowlist out
 * of the skill's {@code references/utility-classes.md}, both shipped inside the jar the platform
 * serves. A Harmonia bump that removes a directive or a class fails this test naming the page and
 * the token, instead of quietly degrading a page somewhere in the fleet.
 *
 * <p>
 * Two kinds of input: every Harmonia page on the classpath (the platform shells, the shared shell
 * views, the tenant picker) and the RENDERED output of the generated-application templates over the
 * {@code ModelGenerationIT} fixtures - rendered, so Velocity noise never reaches the scan and what
 * is checked is what a generated application really serves.
 */
class HarmoniaContractIT extends IntegrationTest {

    /** Classpath patterns of the Harmonia pages; a page is one that carries at least one directive. */
    private static final List<String> PAGE_PATTERNS =
            List.of("classpath*:META-INF/dirigible/application/**/*.html", "classpath*:META-INF/dirigible/admin/**/*.html",
                    "classpath*:META-INF/dirigible/personal/**/*.html", "classpath*:META-INF/dirigible/partner/**/*.html",
                    "classpath*:META-INF/dirigible/monitoring/**/*.html", "classpath*:META-INF/dirigible/database/**/*.html",
                    "classpath*:META-INF/dirigible/builder/**/*.html", "classpath*:META-INF/dirigible/home/**/*.html",
                    "classpath*:META-INF/dirigible/application-core/shell/views/*.html", "classpath*:static/tenant-selection.html");

    /** The shared shell runtime's stylesheet, loaded by every shell and every generated application. */
    private static final String CORE_CSS = "META-INF/dirigible/application-core/shell/css/app.css";

    /** Fewer pages than this means a pattern stopped matching, not that the fleet shrank. */
    private static final int MINIMUM_PAGES = 20;

    /** The template rendering the full application stack. */
    private static final String TEMPLATE_APPLICATION = "template-application-ui-harmonia-java/template/template.js";

    /** The template rendering a form. */
    private static final String TEMPLATE_FORM = "template-form-builder-harmonia/template/template.js";

    /** The template rendering a standalone report. */
    private static final String TEMPLATE_REPORT = "template-application-ui-harmonia-java/template/template-report-file.js";

    /** The project every fixture is rendered in. */
    private static final String PROJECT = "harmonia-contract";

    /** One fixture rendered with one template. */
    private record Case(String fixture, String templateId) {
    }

    /** The fixtures whose rendered pages are scanned: every view family the templates emit. */
    private static final List<Case> CASES =
            List.of(new Case("sales-order.model", TEMPLATE_APPLICATION), new Case("views.model", TEMPLATE_APPLICATION),
                    new Case("leave-request.form", TEMPLATE_FORM), new Case("revenue.report", TEMPLATE_REPORT));

    /** A directive name as the bundle registers it, {@code "h-button"}. */
    private static final Pattern REGISTERED_DIRECTIVE = Pattern.compile("\"(h-[a-z][a-z0-9-]*)\"");

    /**
     * A directive as a page writes it, modifier and value excluded: {@code x-h-text.muted} is read as
     * {@code h-text}.
     */
    private static final Pattern USED_DIRECTIVE = Pattern.compile("\\bx-(h-[a-z][a-z0-9-]*)");

    /** A fenced code block of the utility reference. */
    private static final Pattern FENCED_BLOCK = Pattern.compile("```\\n(.*?)\\n```", Pattern.DOTALL);

    /** A class selector in a stylesheet, {@code .name}. */
    private static final Pattern CSS_CLASS = Pattern.compile("\\.(-?[_a-zA-Z][\\w-]*)");

    private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private static final Pattern STYLE_BLOCK = Pattern.compile("<style[^>]*>(.*?)</style>", Pattern.DOTALL);

    private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * A static class attribute - not {@code :class}, not {@code x-bind:class}, not {@code data-class}.
     */
    private static final Pattern STATIC_CLASS = Pattern.compile("(?<![:\\w-])class=\"([^\"]*)\"");

    /** A bound class attribute, whose expression holds the classes as quoted strings. */
    private static final Pattern BOUND_CLASS = Pattern.compile("(?::|x-bind:)class=\"([^\"]*)\"");

    private static final Pattern QUOTED = Pattern.compile("'([^']*)'");

    /**
     * A comparison operator right before or right after a quoted string: the string is a value, not a
     * class.
     */
    private static final Pattern COMPARISON_BEFORE = Pattern.compile("[=!]==?\\s*$");

    private static final Pattern COMPARISON_AFTER = Pattern.compile("^\\s*[=!]==?");

    /** A Lucide placeholder on an element the plugin replaces - every Alpine binding on it throws. */
    private static final Pattern REPLACED_PLACEHOLDER = Pattern.compile("<i\\b[^>]*\\bx-h-lucide\\b");

    private static final Pattern TAG = Pattern.compile("<(\\w+)\\b([^>]*)>");

    /** A static variant attribute inside a tag's attribute list. */
    private static final Pattern STATIC_VARIANT = Pattern.compile("(?<![:\\w-])data-variant=\"([^\"]*)\"");

    private static final Set<String> BUTTON_VARIANTS =
            Set.of("default", "primary", "positive", "negative", "warning", "information", "outline", "transparent", "link");

    private static final Set<String> BADGE_VARIANTS = Set.of("primary", "positive", "negative", "warning", "information", "outline");

    /** The breakpoint prefixes the utility reference declares for its responsive sections. */
    private static final List<String> BREAKPOINTS = List.of("sm", "md", "lg", "xl");

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ModelGenerationService modelGenerationService;

    /** What the pinned release ships: the directive names and the utility classes. */
    private record Contract(Set<String> directives, Set<String> classes) {
    }

    /**
     * One test method on purpose: the base class discards the application context after each method,
     * and every finding names its page and its token, which is what a diagnosis needs.
     */
    @Test
    void everyHarmoniaSurfaceUsesOnlyWhatThePinnedReleaseShips() throws IOException {
        Contract contract = readContract();
        Set<String> coreClasses = cssClasses(readClasspath(CORE_CSS));

        List<String> problems = new ArrayList<>();
        int pages = 0;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String pattern : PAGE_PATTERNS) {
            for (Resource resource : resolver.getResources(pattern)) {
                String content = read(resource);
                if (!content.contains("x-h-")) {
                    continue; // not a Harmonia page (an AngularJS perspective sharing the folder name)
                }
                pages++;
                Set<String> defined = new HashSet<>(coreClasses);
                defined.addAll(cssClasses(readClasspath(moduleStylesheet(resource))));
                problems.addAll(check(resource.getURL()
                                              .getPath(),
                        content, contract, defined));
            }
        }
        assertTrue(pages >= MINIMUM_PAGES, "Only " + pages + " Harmonia pages were found on the classpath - has a shell moved?");

        for (Case testCase : CASES) {
            problems.addAll(checkRendered(testCase, contract, coreClasses));
        }

        assertTrue(problems.isEmpty(), () -> "Harmonia " + problems.size() + " contract violation(s):\n" + String.join("\n", problems));
    }

    /**
     * Renders one fixture and scans every page it produced, with the stylesheets it produced as the
     * page-defined classes.
     */
    private List<String> checkRendered(Case testCase, Contract contract, Set<String> coreClasses) throws IOException {
        String workspace = ("harmonia-contract-" + testCase.fixture()).replaceAll("[^a-z0-9-]", "-");
        String fixture = readClasspath("ModelGenerationIT/" + testCase.fixture());
        assertTrue(fixture != null, "Missing fixture ModelGenerationIT/" + testCase.fixture());
        seed(workspace, testCase.fixture(), fixture);
        List<GeneratedFile> rendered =
                modelGenerationService.render(workspace, PROJECT, testCase.fixture(), testCase.templateId(), new LinkedHashMap<>());

        Set<String> defined = new HashSet<>(coreClasses);
        for (GeneratedFile file : rendered) {
            if (file.path()
                    .endsWith(".css")) {
                defined.addAll(cssClasses(file.content()));
            }
        }
        List<String> problems = new ArrayList<>();
        int pages = 0;
        for (GeneratedFile file : rendered) {
            if (file.path()
                    .endsWith(".html")) {
                pages++;
                problems.addAll(check(testCase.fixture() + " -> " + file.path(), file.content(), contract, defined));
            }
        }
        if (pages == 0) {
            problems.add("[" + testCase.fixture() + " with " + testCase.templateId() + "] rendered no page at all");
        }
        return problems;
    }

    /** Every violation on one page, each naming the page and the token. */
    private static List<String> check(String page, String rawContent, Contract contract, Set<String> definedClasses) {
        String content = HTML_COMMENT.matcher(rawContent)
                                     .replaceAll("");
        Set<String> defined = new HashSet<>(definedClasses);
        Matcher style = STYLE_BLOCK.matcher(content);
        while (style.find()) {
            defined.addAll(cssClasses(style.group(1)));
        }
        List<String> problems = new ArrayList<>();

        Matcher directive = USED_DIRECTIVE.matcher(content);
        Set<String> reported = new HashSet<>();
        while (directive.find()) {
            String name = directive.group(1);
            if (!contract.directives()
                         .contains(name)
                    && reported.add(name)) {
                problems.add(page + ": x-" + name + " is not a directive the pinned Harmonia registers");
            }
        }

        if (REPLACED_PLACEHOLDER.matcher(content)
                                .find()) {
            problems.add(page + ": an <i x-h-lucide> placeholder - the Lucide plugin replaces the element, so every Alpine binding on it"
                    + " throws and aborts the walk; use <svg x-h-lucide>");
        }

        Matcher staticClass = STATIC_CLASS.matcher(content);
        while (staticClass.find()) {
            for (String token : staticClass.group(1)
                                           .trim()
                                           .split("\\s+")) {
                checkClass(page, token, contract, defined, problems);
            }
        }
        Matcher boundClass = BOUND_CLASS.matcher(content);
        while (boundClass.find()) {
            String expression = boundClass.group(1);
            Matcher quoted = QUOTED.matcher(expression);
            while (quoted.find()) {
                if (COMPARISON_BEFORE.matcher(expression.substring(0, quoted.start()))
                                     .find()
                        || COMPARISON_AFTER.matcher(expression.substring(quoted.end()))
                                           .find()) {
                    continue; // a compared value, not a class
                }
                for (String token : quoted.group(1)
                                          .trim()
                                          .split("\\s+")) {
                    checkClass(page, token, contract, defined, problems);
                }
            }
        }

        Matcher tag = TAG.matcher(content);
        while (tag.find()) {
            String attributes = tag.group(2);
            Matcher variant = STATIC_VARIANT.matcher(attributes);
            if (!variant.find()) {
                continue;
            }
            if (attributes.matches("(?s).*\\bx-h-button\\b.*") && !BUTTON_VARIANTS.contains(variant.group(1))) {
                problems.add(page + ": x-h-button data-variant=\"" + variant.group(1) + "\" is not a Harmonia button variant");
            }
            if (attributes.matches("(?s).*\\bx-h-badge\\b.*") && !BADGE_VARIANTS.contains(variant.group(1))) {
                problems.add(page + ": x-h-badge data-variant=\"" + variant.group(1) + "\" is not a Harmonia badge variant");
            }
        }
        return problems;
    }

    /**
     * A class is fine when the release ships it or the page / its module / the shared runtime defines
     * it.
     */
    private static void checkClass(String page, String token, Contract contract, Set<String> defined, List<String> problems) {
        if (token.isEmpty() || contract.classes()
                                       .contains(token)
                || defined.contains(token)) {
            return;
        }
        problems.add(page + ": class [" + token + "] is neither a utility the pinned Harmonia ships nor a class this page defines");
    }

    /**
     * Reads the contract out of the {@code codbex__harmonia} webjar on the test classpath: the
     * directive registry from the two bundles and the utility allowlist from the skill's reference.
     */
    private static Contract readContract() throws IOException {
        Map<String, String> entries = readWebjar(
                List.of("/dist/harmonia.min.js", "/dist/harmonia-lucide.min.js", "/skills/harmonia/references/utility-classes.md"));
        Set<String> directives = new HashSet<>();
        for (String bundle : List.of("/dist/harmonia.min.js", "/dist/harmonia-lucide.min.js")) {
            Matcher registered = REGISTERED_DIRECTIVE.matcher(entries.get(bundle));
            while (registered.find()) {
                directives.add(registered.group(1));
            }
        }
        assertTrue(directives.size() > 200,
                "Only " + directives.size() + " directives read out of the Harmonia bundle - has its shape changed?");
        return new Contract(directives, utilityClasses(entries.get("/skills/harmonia/references/utility-classes.md")));
    }

    /**
     * The utility reference lists five fenced blocks: the classes taking the {@code !} suffix, the
     * classes taking a min-width breakpoint prefix, the classes taking a max-width one, Harmonia's own
     * utilities and the Tailwind subset. Each block is recognised by a token it is known to hold, so a
     * reordered document fails here rather than silently accepting the wrong classes.
     */
    private static Set<String> utilityClasses(String reference) {
        List<List<String>> blocks = new ArrayList<>();
        Matcher block = FENCED_BLOCK.matcher(reference);
        while (block.find()) {
            blocks.add(List.of(block.group(1)
                                    .trim()
                                    .split("\\s+")));
        }
        assertTrue(blocks.size() == 5, "utility-classes.md holds " + blocks.size() + " code blocks, not the five the parser knows");
        List<String> important = blocks.get(0);
        List<String> responsive = blocks.get(1);
        List<String> maxResponsive = blocks.get(2);
        List<String> harmoniaSpecific = blocks.get(3);
        List<String> tailwind = blocks.get(4);
        assertTrue(
                important.contains("w-full") && responsive.contains("grid-cols-1") && maxResponsive.contains("rounded-none")
                        && harmoniaSpecific.contains("hbox") && tailwind.contains("wrap-anywhere"),
                "utility-classes.md changed shape - its code blocks are not in the order the parser reads them");

        Set<String> classes = new HashSet<>(harmoniaSpecific);
        classes.addAll(tailwind);
        for (String name : important) {
            classes.add(name + "!");
        }
        for (String breakpoint : BREAKPOINTS) {
            for (String name : responsive) {
                classes.add(breakpoint + ":" + name);
            }
            for (String name : maxResponsive) {
                classes.add("max-" + breakpoint + ":" + name);
            }
        }
        return classes;
    }

    /**
     * Reads entries out of the {@code codbex__harmonia} webjar found on {@code java.class.path}. Its
     * resource paths carry the version, so an entry is located by its tail rather than named outright.
     */
    private static Map<String, String> readWebjar(List<String> tails) throws IOException {
        for (String entry : System.getProperty("java.class.path")
                                  .split(File.pathSeparator)) {
            if (!new File(entry).getName()
                                .startsWith("codbex__harmonia")) {
                continue;
            }
            Map<String, String> found = new HashMap<>();
            try (JarFile jar = new JarFile(entry)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry candidate = entries.nextElement();
                    for (String tail : tails) {
                        if (candidate.getName()
                                     .endsWith(tail)) {
                            try (InputStream content = jar.getInputStream(candidate)) {
                                found.put(tail, new String(content.readAllBytes(), StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            }
            for (String tail : tails) {
                assertTrue(found.containsKey(tail), "The codbex__harmonia webjar [" + entry + "] carries no " + tail);
            }
            return found;
        }
        throw new IllegalStateException("The codbex__harmonia webjar is not on the test classpath");
    }

    /**
     * The stylesheet of the module a page belongs to: {@code META-INF/dirigible/<module>/css/app.css},
     * the shared runtime's own living one level deeper. A page outside a module (the tenant picker) has
     * none.
     */
    private static String moduleStylesheet(Resource resource) throws IOException {
        String path = resource.getURL()
                              .getPath();
        int start = path.indexOf("META-INF/dirigible/");
        if (start < 0) {
            return null;
        }
        String module = path.substring(start + "META-INF/dirigible/".length())
                            .split("/")[0];
        return "META-INF/dirigible/" + module + ("application-core".equals(module) ? "/shell" : "") + "/css/app.css";
    }

    private static Set<String> cssClasses(String stylesheet) {
        Set<String> classes = new HashSet<>();
        if (stylesheet == null) {
            return classes;
        }
        Matcher selector = CSS_CLASS.matcher(CSS_COMMENT.matcher(stylesheet)
                                                        .replaceAll(""));
        while (selector.find()) {
            classes.add(selector.group(1));
        }
        return classes;
    }

    /**
     * Seeds a fixture into the case's own project, creating the workspace and the project as needed.
     */
    private void seed(String workspace, String fixture, String content) {
        Workspace workspaceObject = workspaceService.existsWorkspace(workspace) ? workspaceService.getWorkspace(workspace)
                : workspaceService.createWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(PROJECT);
        if (projectObject == null || !projectObject.exists()) {
            projectObject = workspaceObject.createProject(PROJECT);
        }
        projectObject.createFile(fixture, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Resource resource) throws IOException {
        try (InputStream content = resource.getInputStream()) {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** A classpath resource's content, or {@code null} when there is no such resource. */
    private static String readClasspath(String path) throws IOException {
        if (path == null) {
            return null;
        }
        try (InputStream content = HarmoniaContractIT.class.getClassLoader()
                                                           .getResourceAsStream(path)) {
            return content == null ? null : new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
