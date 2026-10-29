/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.deprecation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.ide.workspace.deprecation.ServerSideScripts.Kind;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * What is left to port of a project's server-side JavaScript and TypeScript, file by file (#8051).
 *
 * <p>
 * The deprecation entry counts a project's scripts; moving the project to Java needs the files
 * themselves, and what each one is made of. Every script the {@link ServerSideScriptDetector} rule
 * counts is listed with the modules it imports and the decorators it uses - the constructs that
 * decide its Java counterpart - and a script under {@code gen/} is marked as generated, with the
 * template its descriptor records: generated code is regenerated with the Java templates, never
 * ported by hand. Browser code that calls a script's URL is listed too, because a ported script
 * answers under another one.
 */
final class ServerSideScriptInventory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSideScriptInventory.class);

    private static final String GENERATION_FOLDER = "gen/";

    private static final String DESCRIPTOR_EXTENSION = ".gen";

    /** An import, a dynamic import or a require of a module, capturing the module's specifier. */
    private static final Pattern IMPORT = Pattern.compile("(?:\\bfrom|\\bimport|\\brequire)\\s*\\(?\\s*['\"]([^'\"\\s]+)['\"]");

    /** A decorator at the start of a line, capturing its name. */
    private static final Pattern DECORATOR = Pattern.compile("(?m)^\\s*@([A-Z]\\w*)");

    /** A URL the server-side scripts of a project answer under. */
    private static final Pattern SCRIPT_URL = Pattern.compile("/(?:services|public)/(?:ts|js)/[\\w$@./-]+");

    private static final String TYPESCRIPT_SERVICES = "/services/ts/";

    private static final String TYPESCRIPT_PUBLIC = "/public/ts/";

    private static final String JAVASCRIPT_SERVICES = "/services/js/";

    private static final String JAVASCRIPT_PUBLIC = "/public/js/";

    /** The URLs listed per browser file; beyond this the file is a generated page calling many. */
    private static final int MAX_URLS_PER_FILE = 20;

    private ServerSideScriptInventory() {}

    /**
     * Lists what is left to port of a project.
     *
     * @param name the project's name
     * @param project the project's folder in the registry
     * @return the inventory
     */
    static Inventory of(String name, ICollection project) {
        Map<String, String> templates = generationTemplates(project);
        String root = project.getPath() + "/";
        List<Script> scripts = new ArrayList<>();
        List<Reference> references = new ArrayList<>();
        ServerSideScriptDetector.walk(project, new ServerSideScriptDetector.Visitor() {

            @Override
            public void script(IResource resource, Kind kind) {
                String path = relative(resource, root);
                scripts.add(describe(resource, path, kind, templateOf(path, templates)));
            }

            @Override
            public void other(IResource resource) {
                if (!ServerSideScriptDetector.isBrowserCode(resource)) {
                    return;
                }
                List<String> urls = matches(SCRIPT_URL, ServerSideScriptDetector.content(resource), 0).stream()
                                                                                                      .filter(url -> callsAScript(url,
                                                                                                              name))
                                                                                                      .toList();
                if (!urls.isEmpty()) {
                    String path = relative(resource, root);
                    references.add(new Reference(path, path.startsWith(GENERATION_FOLDER),
                            urls.subList(0, Math.min(urls.size(), MAX_URLS_PER_FILE))));
                }
            }
        });
        return new Inventory(name, List.copyOf(scripts), List.copyOf(references));
    }

    /**
     * Tests whether a URL calls a project's server-side script: any TypeScript service, and a
     * JavaScript one of this project. A {@code /services/js/} URL into a platform module - the loader
     * every generated page includes, say - is answered by the platform itself, and is no business of
     * the migration.
     */
    private static boolean callsAScript(String url, String project) {
        return url.startsWith(TYPESCRIPT_SERVICES) || url.startsWith(TYPESCRIPT_PUBLIC)
                || url.startsWith(JAVASCRIPT_SERVICES + project + "/") || url.startsWith(JAVASCRIPT_PUBLIC + project + "/");
    }

    private static Script describe(IResource resource, String path, Kind kind, String template) {
        boolean generated = path.startsWith(GENERATION_FOLDER);
        return switch (kind) {
            case TYPESCRIPT_FILE, JAVASCRIPT_FILE -> {
                String content = ServerSideScriptDetector.content(resource);
                yield new Script(path, kind, generated, template, matches(IMPORT, content, 1), matches(DECORATOR, content, 1), null,
                        List.of());
            }
            case EXTENSION_MODULE -> {
                JsonObject extension = parseObject(resource);
                yield new Script(path, kind, generated, template, List.of(), List.of(), string(extension, "extensionPoint"),
                        listOf(string(extension, "module")));
            }
            case JOB, LISTENER, WEBSOCKET, PROCESS, ROUTE -> new Script(path, kind, generated, template, List.of(), List.of(), null,
                    ServerSideScriptDetector.scriptsNamedIn(resource));
        };
    }

    /**
     * The template each generation folder was generated with, from the descriptors at the project's
     * root: {@code genFolderName} to {@code templateId}.
     */
    private static Map<String, String> generationTemplates(ICollection project) {
        Map<String, String> templates = new HashMap<>();
        for (IResource resource : project.getResources()) {
            if (!resource.getName()
                         .endsWith(DESCRIPTOR_EXTENSION)) {
                continue;
            }
            JsonObject descriptor = parseObject(resource);
            String folder = string(descriptor, "genFolderName");
            String template = string(descriptor, "templateId");
            if (folder != null && template != null) {
                templates.putIfAbsent(folder, template);
            }
        }
        return templates;
    }

    private static String templateOf(String path, Map<String, String> templates) {
        if (!path.startsWith(GENERATION_FOLDER)) {
            return null;
        }
        String rest = path.substring(GENERATION_FOLDER.length());
        int slash = rest.indexOf('/');
        return slash > 0 ? templates.get(rest.substring(0, slash)) : null;
    }

    private static List<String> matches(Pattern pattern, String content, int group) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            found.add(matcher.group(group));
        }
        return List.copyOf(found);
    }

    private static JsonObject parseObject(IResource resource) {
        try {
            JsonElement parsed = JsonParser.parseString(ServerSideScriptDetector.content(resource));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (JsonParseException e) {
            LOGGER.debug("[{}] is not JSON, so it names nothing to list", LogSanitizer.sanitize(resource.getPath()), e);
            return new JsonObject();
        }
    }

    private static String string(JsonObject object, String property) {
        JsonElement value = object.get(property);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static List<String> listOf(String value) {
        return value == null ? List.of() : List.of(value);
    }

    private static String relative(IResource resource, String root) {
        String path = resource.getPath();
        return path.startsWith(root) ? path.substring(root.length()) : path;
    }

    /**
     * What is left to port of a project.
     *
     * @param project the project's name
     * @param scripts its server-side scripts
     * @param references the browser code calling a script's URL
     */
    record Inventory(String project, List<Script> scripts, List<Reference> references) {
    }

    /**
     * One server-side script.
     *
     * @param path the project-relative path
     * @param kind what kind of script it is
     * @param generated whether it lies under {@code gen/}, so is regenerated rather than ported
     * @param template the template its generation descriptor records, when it is generated and has one
     * @param imports the modules a script file imports, in order of appearance
     * @param decorators the decorators a script file uses, in order of appearance
     * @param extensionPoint the extension point an extension contributes to
     * @param runs the script an extension, job, listener, websocket, process or route runs
     */
    record Script(String path, Kind kind, boolean generated, String template, List<String> imports, List<String> decorators,
            String extensionPoint, List<String> runs) {
    }

    /**
     * Browser code calling the URL of a server-side script.
     *
     * @param path the project-relative path
     * @param generated whether it lies under {@code gen/}
     * @param urls the script URLs it calls
     */
    record Reference(String path, boolean generated, List<String> urls) {
    }
}
