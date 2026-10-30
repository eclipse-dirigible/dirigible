/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.script.ScriptEngineAvailability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * What a shell reads to bootstrap: the aggregated extensions (perspectives, views, shells, menus,
 * editors, themes, projections, locales), the current user's name, the application languages and
 * country, the reports the user may read, and the branding script, which also tells whether the
 * JavaScript engine is deployed - answered without running a script.
 *
 * <p>
 * Each is served at its canonical path under {@code /services/platform/} and at the script URL that
 * used to answer it ({@code /services/js/platform-core/extension-services/<name>.js},
 * {@code /services/js/platform-core/services/<name>.js},
 * {@code /services/js/platform-branding/branding.js}, and their {@code /public/js/} twins), so a
 * page generated before the scripts were ported keeps working unchanged. A literal mapping wins
 * over the JavaScript endpoint's pattern, and the scripts themselves are gone.
 *
 * <p>
 * Every answer is content, not an asset - publishing a module changes it, and the perspectives,
 * views, menus and projections are filtered by the user's roles - so none may be shared or served
 * from a cache without revalidating.
 */
@RestController
class ShellBootstrapEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellBootstrapEndpoint.class);

    private static final String CANONICAL = "/services/platform/";

    private static final String CANONICAL_EXTENSIONS = CANONICAL + "extensions/";

    private static final String SCRIPT_EXTENSIONS = "/services/js/platform-core/extension-services/";

    private static final String PUBLIC_SCRIPT_EXTENSIONS = "/public/js/platform-core/extension-services/";

    private static final String SCRIPT_SERVICES = "/services/js/platform-core/services/";

    private static final String PUBLIC_SCRIPT_SERVICES = "/public/js/platform-core/services/";

    private static final String CACHE_CONTROL = "private, no-cache";

    private static final String EXTENSION_POINTS = "extensionPoints";

    private static final String GUEST = "guest";

    private static final MediaType JAVASCRIPT = MediaType.parseMediaType("text/javascript;charset=UTF-8");

    private final PerspectivesAggregator perspectives;

    private final ViewsAggregator views;

    private final ShellsAggregator shells;

    private final MenusAggregator menus;

    private final EditorsAggregator editors;

    private final ThemesAggregator themes;

    private final ProjectionsAggregator projections;

    private final LocalesAggregator locales;

    private final ReportsAggregator reports;

    private final ScriptEngineAvailability scriptEngine;

    /**
     * Instantiates a new shell bootstrap endpoint.
     *
     * @param perspectives the perspectives aggregator
     * @param views the views aggregator
     * @param shells the shells aggregator
     * @param menus the menus aggregator
     * @param editors the editors aggregator
     * @param themes the themes aggregator
     * @param projections the projections aggregator
     * @param locales the locales aggregator
     * @param reports the reports aggregator
     * @param scriptEngine whether the JavaScript engine is deployed, stated by the branding script
     */
    ShellBootstrapEndpoint(PerspectivesAggregator perspectives, ViewsAggregator views, ShellsAggregator shells, MenusAggregator menus,
            EditorsAggregator editors, ThemesAggregator themes, ProjectionsAggregator projections, LocalesAggregator locales,
            ReportsAggregator reports, ScriptEngineAvailability scriptEngine) {
        this.perspectives = perspectives;
        this.views = views;
        this.shells = shells;
        this.menus = menus;
        this.editors = editors;
        this.themes = themes;
        this.projections = projections;
        this.locales = locales;
        this.reports = reports;
        this.scriptEngine = scriptEngine;
    }

    @GetMapping({CANONICAL_EXTENSIONS + "perspectives", SCRIPT_EXTENSIONS + "perspectives.js",
            PUBLIC_SCRIPT_EXTENSIONS + "perspectives.js"})
    ResponseEntity<Object> perspectives(HttpServletRequest request) {
        return json(perspectives.aggregate(values(request, EXTENSION_POINTS, PerspectivesAggregator.DEFAULT_EXTENSION_POINT))
                                .toJson());
    }

    @GetMapping({CANONICAL_EXTENSIONS + "views", SCRIPT_EXTENSIONS + "views.js", PUBLIC_SCRIPT_EXTENSIONS + "views.js"})
    ResponseEntity<Object> views(HttpServletRequest request) {
        return json(views.aggregate(values(request, EXTENSION_POINTS, ViewsAggregator.DEFAULT_EXTENSION_POINT)));
    }

    @GetMapping({CANONICAL_EXTENSIONS + "shells", SCRIPT_EXTENSIONS + "shells.js", PUBLIC_SCRIPT_EXTENSIONS + "shells.js"})
    ResponseEntity<Object> shells(HttpServletRequest request) {
        return json(shells.aggregate(values(request, EXTENSION_POINTS, ShellsAggregator.DEFAULT_EXTENSION_POINT)));
    }

    @GetMapping({CANONICAL_EXTENSIONS + "menus", SCRIPT_EXTENSIONS + "menus.js", PUBLIC_SCRIPT_EXTENSIONS + "menus.js"})
    ResponseEntity<Object> menus(HttpServletRequest request) {
        return json(menus.aggregate(values(request, EXTENSION_POINTS, MenusAggregator.DEFAULT_EXTENSION_POINT),
                values(request, "perspectiveExtPoints", PerspectivesAggregator.DEFAULT_EXTENSION_POINT),
                values(request, "viewExtPoints", ViewsAggregator.DEFAULT_EXTENSION_POINT),
                values(request, "shellExtPoints", ShellsAggregator.DEFAULT_EXTENSION_POINT)));
    }

    @GetMapping({CANONICAL_EXTENSIONS + "editors", SCRIPT_EXTENSIONS + "editors.js", PUBLIC_SCRIPT_EXTENSIONS + "editors.js"})
    ResponseEntity<Object> editors(HttpServletRequest request) {
        return json(editors.aggregate(values(request, EXTENSION_POINTS, EditorsAggregator.DEFAULT_EXTENSION_POINT)));
    }

    @GetMapping({CANONICAL_EXTENSIONS + "themes", SCRIPT_EXTENSIONS + "themes.js", PUBLIC_SCRIPT_EXTENSIONS + "themes.js"})
    ResponseEntity<Object> themes(HttpServletRequest request) {
        return json(themes.aggregate(values(request, EXTENSION_POINTS, ThemesAggregator.DEFAULT_EXTENSION_POINT)));
    }

    @GetMapping({CANONICAL_EXTENSIONS + "projections", SCRIPT_EXTENSIONS + "projections.js", PUBLIC_SCRIPT_EXTENSIONS + "projections.js"})
    ResponseEntity<Object> projections(HttpServletRequest request) {
        return json(Map.of("projections",
                projections.aggregate(values(request, EXTENSION_POINTS, ProjectionsAggregator.DEFAULT_EXTENSION_POINT))));
    }

    @GetMapping({CANONICAL_EXTENSIONS + "locales", SCRIPT_EXTENSIONS + "locales.js", PUBLIC_SCRIPT_EXTENSIONS + "locales.js"})
    ResponseEntity<Object> locales(HttpServletRequest request) {
        try {
            return json(locales.aggregate(values(request, EXTENSION_POINTS, LocalesAggregator.DEFAULT_EXTENSION_POINT),
                    values(request, "langs"), values(request, "namespaces")));
        } catch (RuntimeException e) {
            LOGGER.error("Error while loading locale modules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body("Error while loading locale modules: " + e.getMessage());
        }
    }

    /**
     * The standalone reports of every published application the caller may read, as one manifest - the
     * application shell's Reports section and dashboard read it instead of walking the registry and
     * fetching each {@code .report} (#8345).
     */
    @GetMapping(CANONICAL + "reports")
    ResponseEntity<Object> reports() {
        return json(reports.aggregate());
    }

    @GetMapping({CANONICAL + "user-name", SCRIPT_SERVICES + "user-name.js", PUBLIC_SCRIPT_SERVICES + "user-name.js"})
    ResponseEntity<String> userName(HttpServletRequest request) {
        // Followed by a line break, as the script printed it - an IDE client reads the body verbatim.
        return ResponseEntity.ok()
                             .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                             .contentType(MediaType.TEXT_PLAIN)
                             .body(userName(request.getRemoteUser()) + "\n");
    }

    /**
     * The user's name: the authenticated one, else the configured anonymous user, else a guest - what
     * the script API's {@code user.getName()} answered at the start of a request.
     */
    private static String userName(String remoteUser) {
        if (remoteUser != null) {
            return remoteUser;
        }
        if (Configuration.isAnonymousUserEnabled()) {
            String nameProperty = Configuration.get(DirigibleConfig.ANONYMOUS_USER_NAME_PROPERTY_NAME.getKey());
            String name = nameProperty == null ? null : Configuration.get(nameProperty);
            if (name != null) {
                return name;
            }
        }
        return GUEST;
    }

    /**
     * The platform's supported languages ({@code DIRIGIBLE_APPLICATION_LANGUAGES}, tenant-overridable),
     * the first the default. The Region and Language picker of every Harmonia shell offers exactly
     * these; modules only provide translations for them.
     */
    @GetMapping({CANONICAL + "languages", SCRIPT_SERVICES + "application-languages.js",
            PUBLIC_SCRIPT_SERVICES + "application-languages.js"})
    ResponseEntity<Object> languages() {
        List<String> languages = Arrays.stream(Configuration.get(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en")
                                                            .split(","))
                                       .map(String::trim)
                                       .filter(code -> !code.isEmpty())
                                       .toList();
        return json(languages.isEmpty() ? List.of("en") : languages);
    }

    /**
     * The tenant's country ({@code DIRIGIBLE_APPLICATION_COUNTRY}, an ISO 3166-1 alpha-2 code, blank
     * when none is declared). The shared i18n runtime resolves country-scoped field labels against it.
     */
    @GetMapping({CANONICAL + "country", SCRIPT_SERVICES + "application-country.js", PUBLIC_SCRIPT_SERVICES + "application-country.js"})
    ResponseEntity<Object> country() {
        String configured = Configuration.get(DirigibleConfig.APPLICATION_COUNTRY.getKey(), "");
        return json(Map.of("country", configured == null ? ""
                : configured.trim()
                            .toUpperCase(Locale.ROOT)));
    }

    @GetMapping({CANONICAL + "clear-cache", SCRIPT_SERVICES + "clear-cache.js", PUBLIC_SCRIPT_SERVICES + "clear-cache.js"})
    ResponseEntity<Void> clearCache() {
        return ResponseEntity.ok()
                             .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                             .header("Clear-Site-Data", "\"cache\"")
                             .build();
    }

    @GetMapping({CANONICAL + "branding.js", "/services/js/platform-branding/branding.js", "/public/js/platform-branding/branding.js"})
    ResponseEntity<String> branding() {
        return ResponseEntity.ok()
                             .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                             .contentType(JAVASCRIPT)
                             .body(BrandingScript.render(scriptEngine.isAvailable()) + "\n");
    }

    private static ResponseEntity<Object> json(Object body) {
        return ResponseEntity.ok()
                             .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(body);
    }

    private static List<String> values(HttpServletRequest request, String parameter, String defaultValue) {
        List<String> values = values(request, parameter);
        return values == null ? List.of(defaultValue) : values;
    }

    /**
     * Reads a parameter's values as the script API's {@code request.getParameterValues} did: every
     * occurrence, never split on commas, {@code null} when the parameter is absent.
     */
    private static List<String> values(HttpServletRequest request, String parameter) {
        String[] values = request.getParameterValues(parameter);
        return values == null ? null : List.of(values);
    }
}
