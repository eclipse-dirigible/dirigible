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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.api.helpers.NamingHelper;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The standalone reports of every published application the caller may read, in the shape of a
 * generated application's report manifest ({@code App.config.reports}): the application shell lists
 * them in its Reports section and on its dashboard from this one answer (#8345).
 *
 * <p>
 * A report is a generated page {@code <project>/gen/<folder>/reports/<Name>/index.html}; its
 * display metadata and its read gate come from the {@code <project>/<Name>.report} beside it. A
 * report whose {@code security.roleRead} names roles the caller holds none of is left out - its
 * generated controller would refuse that caller anyway.
 */
@Component
class ReportsAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportsAggregator.class);

    private static final String GEN = "gen";

    private static final String REPORTS = "reports";

    private static final String PAGE = "index.html";

    private static final String REPORT_EXTENSION = ".report";

    /** The {@code .report} keys the manifest carries as they are, when present. */
    private static final List<String> DISPLAY_KEYS = List.of("label", "description", "dashboard", "chart", "tId", "descriptionTId");

    private static final JsonMapper JSON = JsonMapper.builder()
                                                     .build();

    private static final TypeReference<Map<String, Object>> OBJECT = new TypeReference<>() {};

    private final IRepository repository;

    /**
     * Instantiates a new reports aggregator.
     *
     * @param repository the repository
     */
    ReportsAggregator(IRepository repository) {
        this.repository = repository;
    }

    /**
     * The readable reports of every published project.
     *
     * @return one manifest entry per report
     */
    List<Map<String, Object>> aggregate() {
        List<Map<String, Object>> reports = new ArrayList<>();
        ICollection registry = repository.getCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        if (!registry.exists()) {
            return reports;
        }
        for (ICollection project : registry.getCollections()) {
            ICollection gen = project.getCollection(GEN);
            if (!gen.exists()) {
                continue;
            }
            for (ICollection folder : gen.getCollections()) {
                ICollection pages = folder.getCollection(REPORTS);
                if (!pages.exists()) {
                    continue;
                }
                for (ICollection page : pages.getCollections()) {
                    if (page.getResource(PAGE)
                            .exists()) {
                        Map<String, Object> definition = definition(project, page.getName());
                        if (readable(definition)) {
                            reports.add(entry(project.getName(), folder.getName(), page.getName(), definition));
                        }
                    }
                }
            }
        }
        return reports;
    }

    /**
     * The manifest entry of one report page. The URLs follow the generated layout: the page under its
     * gen folder, the controller under the Java package that folder's name sanitizes to.
     */
    private static Map<String, Object> entry(String project, String folder, String name, Map<String, Object> definition) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("project", project);
        for (String key : DISPLAY_KEYS) {
            Contributions.putIfPresent(entry, key, definition.get(key));
        }
        // Only a KPI tile reads the columns (a list widget shows the report's selected ones).
        if (definition.get("widget") != null) {
            entry.put("widget", definition.get("widget"));
            entry.put("columns", definition.get("columns") instanceof List<?> columns ? columns : List.of());
        }
        entry.put("url", "/services/web/" + project + "/" + GEN + "/" + folder + "/" + REPORTS + "/" + name + "/" + PAGE);
        entry.put("apiBase", "/services/java/" + project + "/" + GEN + "/" + NamingHelper.sanitizeJavaIdentifier(folder) + "/api/" + REPORTS
                + "/" + name + "Controller");
        return entry;
    }

    /** Whether the caller passes the report's read gate; a report without one is readable. */
    private static boolean readable(Map<String, Object> definition) {
        if (!(definition.get("security") instanceof Map<?, ?> security)) {
            return true;
        }
        return !(security.get("roleRead") instanceof String roles) || CallerRoles.holdsAnyOf(roles);
    }

    /**
     * The report's {@code .report} document, empty when it is missing or unreadable - the page is then
     * listed with the defaults its name gives it.
     */
    private static Map<String, Object> definition(ICollection project, String name) {
        IResource resource = project.getResource(name + REPORT_EXTENSION);
        if (!resource.exists()) {
            return Map.of();
        }
        try {
            return JSON.readValue(resource.getContent(), OBJECT);
        } catch (JacksonException e) {
            LOGGER.warn("Report [{}] is not a JSON object; listed without its metadata.", resource.getPath(), e);
            return Map.of();
        }
    }
}
