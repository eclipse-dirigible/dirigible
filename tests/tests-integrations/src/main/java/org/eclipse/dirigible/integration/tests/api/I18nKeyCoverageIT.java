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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Every UI label key the generated pages and the shells ask for exists in every language the
 * platform ships (dirigible #7610).
 *
 * <p>
 * A missing key fails nowhere: {@code T(key, fallback)} renders the English fallback, so a page in
 * Bulgarian quietly shows "your step", "Save anyway" or "is waiting for you" in English and every
 * build stays green. Three keys were asked for by a template and existed in no catalog at all, and
 * a whole dialog asked for un-namespaced keys no catalog could ever hold. So this reads every
 * literal key out of the generated-application templates and the shells and requires it in:
 * <ul>
 * <li>the template catalog ({@code translations.json.template}) the generator mints into each
 * module, for a generated page's own key;</li>
 * <li>the platform's copy of that catalog's chrome sections, in every shipped language
 * ({@code application-core/i18n/<locale>/generated.json}) - the translation a module's own catalog
 * falls back to;</li>
 * <li>the shell catalog in every shipped language, for an {@code application-core:} key.</li>
 * </ul>
 * The language files must also agree with each other key for key and placeholder for placeholder.
 * Then the shipped {@code i18n.js} is evaluated over the shipped Bulgarian catalogs to prove the
 * fallback is actually taken.
 */
class I18nKeyCoverageIT {

    private static final String DIRIGIBLE = "META-INF/dirigible/";

    private static final String CORE_I18N = DIRIGIBLE + "application-core/i18n/";

    private static final String TEMPLATE_CATALOG = DIRIGIBLE + "template-application-ui-harmonia-java/ui/translations.json.template";

    /** The template catalog's sections every module carries unchanged - the platform ships these. */
    private static final Set<String> CHROME_SECTIONS = Set.of("aria", "state", "messages", "defaults");

    /** Where the keys are asked for: the generated-application templates and the shells. */
    private static final List<String> SOURCES = List.of("classpath*:" + DIRIGIBLE + "template-application-ui-harmonia-java/**/*.template",
            "classpath*:" + DIRIGIBLE + "template-application-ui-harmonia-java/**/*.js",
            "classpath*:" + DIRIGIBLE + "application-core/**/*.js", "classpath*:" + DIRIGIBLE + "application-core/**/*.html",
            "classpath*:" + DIRIGIBLE + "application/**/*.js", "classpath*:" + DIRIGIBLE + "application/**/*.html",
            "classpath*:" + DIRIGIBLE + "admin/**/*.js", "classpath*:" + DIRIGIBLE + "admin/**/*.html",
            "classpath*:" + DIRIGIBLE + "partner/**/*.js", "classpath*:" + DIRIGIBLE + "partner/**/*.html",
            "classpath*:" + DIRIGIBLE + "personal/**/*.js", "classpath*:" + DIRIGIBLE + "personal/**/*.html");

    /**
     * A generated page's own key: {@code T('$projectName:${tprefix}.<path>', ...)} with a literal path.
     */
    private static final Pattern GENERATED_KEY = Pattern.compile("\\bT\\(\\s*'\\$projectName:\\$\\{tprefix\\}\\.([A-Za-z0-9_.]+)'");

    /** A shell key, wherever it is written: {@code 'application-core:<path>'}. */
    private static final Pattern CORE_KEY = Pattern.compile("['\"]application-core:([A-Za-z0-9_.]+)['\"]");

    /**
     * The shell stores' helpers ({@code $store.tenant.t('shell...')}, the warnings dialog's
     * {@code t('shell...')}) prefix {@code application-core:} themselves.
     */
    private static final Pattern SHELL_HELPER_KEY = Pattern.compile("(?<![\\w$])t\\(\\s*'(shell\\.[A-Za-z0-9_.]+)'");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Fewer languages than this means the pattern stopped matching, not that a language was dropped.
     */
    private static final int MIN_LANGUAGES = 2;

    @Test
    void everyGeneratedPageKeyIsInTheTemplateCatalog() {
        Map<String, String> catalog = flatten(json(TEMPLATE_CATALOG));
        Map<String, Set<String>> used = usedKeys(GENERATED_KEY);
        assertTrue(used.size() > 50, "only " + used.size() + " generated-page keys found - a source pattern stopped matching");
        List<String> missing = new ArrayList<>();
        used.forEach((key, files) -> {
            if (!catalog.containsKey(key)) {
                missing.add(key + "  (used in " + files + ")");
            }
        });
        assertNoneMissing("keys a generated page asks for that " + TEMPLATE_CATALOG + " does not mint", missing);
    }

    @Test
    void theTemplateChromeShipsInEveryLanguage() {
        Map<String, String> chrome = new TreeMap<>();
        flatten(json(TEMPLATE_CATALOG)).forEach((key, value) -> {
            if (key.indexOf('.') > 0 && CHROME_SECTIONS.contains(key.substring(0, key.indexOf('.')))) {
                chrome.put("generated." + key, value);
            }
        });
        List<String> problems = new ArrayList<>();
        for (String locale : languages()) {
            Map<String, String> catalog = coreCatalog(locale);
            for (Map.Entry<String, String> entry : chrome.entrySet()) {
                String translated = catalog.get(entry.getKey());
                if (translated == null || translated.isBlank()) {
                    problems.add(locale + ": " + entry.getKey() + " missing");
                } else if (!placeholders(translated).equals(placeholders(entry.getValue()))) {
                    problems.add(locale + ": " + entry.getKey() + " placeholders " + placeholders(translated) + " != "
                            + placeholders(entry.getValue()));
                }
            }
            for (String key : catalog.keySet()) {
                if (key.startsWith("generated.") && !chrome.containsKey(key)) {
                    problems.add(locale + ": " + key + " is not in the template catalog any more");
                }
            }
        }
        assertNoneMissing("generated.json out of step with the template catalog", problems);
    }

    @Test
    void everyShellKeyShipsInEveryLanguage() {
        Map<String, Set<String>> used = usedKeys(CORE_KEY);
        usedKeys(SHELL_HELPER_KEY).forEach((key, files) -> used.computeIfAbsent(key, k -> new TreeSet<>())
                                                               .addAll(files));
        // A key built at run time ('application-core:' + key, '...error.' + code) is not a key.
        used.keySet()
            .removeIf(key -> key.isEmpty() || key.endsWith("."));
        assertTrue(used.size() > 150, "only " + used.size() + " shell keys found - a source pattern stopped matching");
        List<String> missing = new ArrayList<>();
        for (String locale : languages()) {
            Map<String, String> catalog = coreCatalog(locale);
            used.forEach((key, files) -> {
                if (!catalog.containsKey(key)) {
                    missing.add(locale + ": application-core:" + key + "  (used in " + files + ")");
                }
            });
        }
        assertNoneMissing("shell keys missing from a shipped language", missing);
    }

    @Test
    void theShippedLanguagesAgreeKeyForKey() {
        List<String> locales = languages();
        Map<String, String> reference = coreCatalog(locales.get(0));
        List<String> problems = new ArrayList<>();
        for (String locale : locales.subList(1, locales.size())) {
            Map<String, String> catalog = coreCatalog(locale);
            for (Map.Entry<String, String> entry : reference.entrySet()) {
                String other = catalog.get(entry.getKey());
                if (other == null) {
                    problems.add(locale + ": " + entry.getKey() + " missing (present in " + locales.get(0) + ")");
                } else if (!placeholders(other).equals(placeholders(entry.getValue()))) {
                    problems.add(locale + ": " + entry.getKey() + " placeholders " + placeholders(other) + " != " + locales.get(0) + " "
                            + placeholders(entry.getValue()));
                }
            }
            for (String key : catalog.keySet()) {
                if (!reference.containsKey(key)) {
                    problems.add(locale + ": " + key + " is in no " + locales.get(0) + " catalog");
                }
            }
        }
        assertNoneMissing("application-core catalogs disagree across languages", problems);
    }

    /**
     * The fallback is real: with the shipped Bulgarian catalogs loaded, a generated page's chrome key
     * its module never translated renders in Bulgarian, the module's own entry still wins, a model
     * label never borrows from the platform, and the warnings dialog's keys resolve.
     */
    @Test
    void aGeneratedPageInBulgarianFallsBackToThePlatformChrome() {
        ObjectNode moduleCatalog = JSON.createObjectNode();
        moduleCatalog.putObject("orders-model")
                     .putObject("messages")
                     .put("saved", "Записано от модула");
        try (Context context = loadI18n("bg", moduleCatalog)) {
            assertEquals("вашата стъпка", t(context, "orders:orders-model.messages.workflowStep", "your step"));
            assertEquals("очаква вашето действие", t(context, "orders:orders-model.messages.workflowWaiting", "is waiting for you"));
            assertEquals("Запази", t(context, "orders:orders-model.defaults.save", "Save"));
            assertEquals("Записано от модула", t(context, "orders:orders-model.messages.saved", "Saved"),
                    "the module's own translation must win over the platform's");
            assertEquals("Customer", t(context, "orders:orders-model.t.Customer", "Customer"),
                    "a model label has no platform translation to fall back to");
            assertEquals("Моля, потвърдете", t(context, "application-core:shell.warnings.title", "Please confirm"));
            assertEquals("Запази въпреки това", t(context, "application-core:shell.warnings.confirm", "Save anyway"));
        }
        try (Context context = loadI18n("en", moduleCatalog)) {
            assertEquals("your step", t(context, "orders:orders-model.messages.workflowStep", "your step"),
                    "the default language renders the baked English");
        }
    }

    private static String t(Context context, String key, String fallback) {
        context.getBindings("js")
               .putMember("__key", key);
        context.getBindings("js")
               .putMember("__fallback", fallback);
        return context.eval("js", "T(__key, __fallback)")
                      .asString();
    }

    /**
     * Evaluate the shipped {@code i18n.js} over stubs of what it touches - the {@code alpine:init}
     * listener, an Alpine store registry, localStorage holding the saved language, the locales service
     * answering with the SHIPPED application-core catalogs plus one module's, and an i18next that
     * resolves {@code ns:path} keys in its active language.
     */
    private static Context loadI18n(String savedLanguage, ObjectNode moduleCatalog) {
        ObjectNode translations = JSON.createObjectNode();
        for (String locale : languages()) {
            ObjectNode namespaces = translations.putObject(locale);
            ObjectNode core = namespaces.putObject("application-core");
            for (Resource resource : resources("classpath*:" + CORE_I18N + locale + "/*.json")) {
                core.setAll((ObjectNode) json(resource));
            }
            namespaces.set("orders", moduleCatalog);
        }
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // See HarmoniaDateFormatIT: the interpreter-only notice corrupts the forked JVM's channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.getBindings("js")
               .putMember("__translations", translations.toString());
        context.eval("js", """
                var window = this;
                var __listeners = [];
                var document = {
                  addEventListener: function (type, listener) { if (type === 'alpine:init') __listeners.push(listener); },
                  createElement: function () { return {}; },
                  head: { appendChild: function (script) { Promise.resolve().then(function () { script.onload(); }); } }
                };
                var __stored = { 'codbex.harmonia.language': '%s' };
                var localStorage = { getItem: function (key) { return __stored[key] || null; } };
                var __stores = {};
                var Alpine = {
                  store: function (name, value) {
                    if (value === undefined) return __stores[name];
                    __stores[name] = value;
                  }
                };
                var App = { config: { projectName: 'orders' } };
                var fetch = function (url) {
                  return Promise.resolve({ ok: true, json: function () {
                    return Promise.resolve({ locales: [{ id: 'en-US' }, { id: 'bg-BG' }], translations: JSON.parse(__translations) });
                  } });
                };
                var i18next = {
                  init: function (options) { this.resources = options.resources; this.language = options.lng; return Promise.resolve(); },
                  lookup: function (key) {
                    var colon = key.indexOf(':');
                    var node = (this.resources[this.language] || {})[key.substring(0, colon)];
                    var path = key.substring(colon + 1).split('.');
                    for (var i = 0; i < path.length; i++) {
                      if (node === null || typeof node !== 'object') return undefined;
                      node = node[path[i]];
                    }
                    return typeof node === 'string' ? node : undefined;
                  },
                  exists: function (key) { return this.lookup(key) !== undefined; },
                  t: function (key) { return this.lookup(key); }
                };
                """.formatted(savedLanguage));
        context.eval("js", read(DIRIGIBLE + "application-core/shell/js/services/i18n.js"));
        context.eval("js", "__listeners.forEach(function (listener) { listener(); });");
        return context;
    }

    /** The languages application-core ships a shell catalog for, the default one first. */
    private static List<String> languages() {
        List<String> locales = new ArrayList<>();
        for (Resource resource : resources("classpath*:" + CORE_I18N + "*/shell.json")) {
            String path = url(resource);
            String parent = path.substring(0, path.lastIndexOf('/'));
            locales.add(parent.substring(parent.lastIndexOf('/') + 1));
        }
        locales.sort((a, b) -> "en-US".equals(a) ? -1 : "en-US".equals(b) ? 1 : a.compareTo(b));
        assertTrue(locales.size() >= MIN_LANGUAGES && "en-US".equals(locales.get(0)), "application-core languages found: " + locales);
        return locales;
    }

    /** The application-core namespace of one language: every catalog file of it, merged. */
    private static Map<String, String> coreCatalog(String locale) {
        Map<String, String> catalog = new TreeMap<>();
        List<Resource> files = resources("classpath*:" + CORE_I18N + locale + "/*.json");
        for (Resource resource : files) {
            catalog.putAll(flatten(json(resource)));
        }
        assertTrue(catalog.keySet()
                          .stream()
                          .anyMatch(key -> key.startsWith("generated.")),
                locale + " ships no generated.json - the generated pages' chrome has no translation to fall back to");
        return catalog;
    }

    /** Every literal key the pattern finds in the sources, with the files asking for it. */
    private static Map<String, Set<String>> usedKeys(Pattern pattern) {
        Map<String, Set<String>> used = new TreeMap<>();
        for (String location : SOURCES) {
            for (Resource resource : resources(location)) {
                String path = url(resource);
                if (path.contains("/i18n/")) {
                    continue;
                }
                Matcher matcher = pattern.matcher(read(resource));
                while (matcher.find()) {
                    used.computeIfAbsent(matcher.group(1), key -> new TreeSet<>())
                        .add(path.substring(path.indexOf(DIRIGIBLE) + DIRIGIBLE.length()));
                }
            }
        }
        return used;
    }

    private static Set<String> placeholders(String text) {
        Set<String> names = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static Map<String, String> flatten(JsonNode node) {
        Map<String, String> flat = new TreeMap<>();
        flatten(node, "", flat);
        return flat;
    }

    private static void flatten(JsonNode node, String prefix, Map<String, String> flat) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue()
                     .isObject()) {
                flatten(field.getValue(), prefix + field.getKey() + ".", flat);
            } else if (field.getValue()
                            .isTextual()) {
                flat.put(prefix + field.getKey(), field.getValue()
                                                       .asText());
            }
        }
    }

    private static void assertNoneMissing(String what, List<String> problems) {
        if (!problems.isEmpty()) {
            fail(problems.size() + " " + what + ":\n  " + String.join("\n  ", problems));
        }
    }

    private static List<Resource> resources(String pattern) {
        try {
            return List.of(new PathMatchingResourcePatternResolver().getResources(pattern));
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot resolve " + pattern, ex);
        }
    }

    private static String url(Resource resource) {
        try {
            return resource.getURL()
                           .toString();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static JsonNode json(String resource) {
        try {
            return JSON.readTree(read(resource));
        } catch (IOException ex) {
            throw new UncheckedIOException("Not JSON: " + resource, ex);
        }
    }

    private static JsonNode json(Resource resource) {
        try {
            return JSON.readTree(read(resource));
        } catch (IOException ex) {
            throw new UncheckedIOException("Not JSON: " + url(resource), ex);
        }
    }

    private static String read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + url(resource), ex);
        }
    }

    private static String read(String resource) {
        try (InputStream in = I18nKeyCoverageIT.class.getClassLoader()
                                                     .getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + resource, ex);
        }
    }
}
