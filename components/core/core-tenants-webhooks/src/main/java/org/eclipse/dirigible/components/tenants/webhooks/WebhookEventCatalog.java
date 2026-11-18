/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The event types a webhook subscription may name: the ones the published applications declare, so
 * an internal channel (a {@code -rekeyed} notice, a phase, a step event) never becomes a public
 * contract by accident.
 *
 * <p>
 * An application declares them per entity in its intent ({@code webhooks: [created, updated,
 * deleted, transitioned]}); the Java DAO template renders each such entity's declaration into
 * {@code gen/<module>/data/<perspective>/<Entity>.webhooks.json}, which this catalog reads from the
 * registry. The scan is cached for {@link #TTL}: a publish shows within that time.
 *
 * <p>
 * A descriptor:
 *
 * <pre>
 * {"entity": "Leave", "idProperty": "Id", "uuidProperty": "Uuid", "statusProperty": "Status",
 *  "events": [{"action": "created", "type": "hr-Leave-Leave"}, {"action": "transitioned", "type": "hr-Leave-Leave-transitioned"}]}
 * </pre>
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class WebhookEventCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookEventCatalog.class);

    /** The suffix of a descriptor's name. */
    static final String DESCRIPTOR_SUFFIX = ".webhooks.json";

    /** The folder of a project the descriptors are generated under. */
    private static final String GEN_FOLDER = "gen";

    /** How long a scan is reused. */
    static final Duration TTL = Duration.ofSeconds(30);

    /** How deep below a project's {@code gen} folder a descriptor may sit. */
    private static final int MAX_DEPTH = 6;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final IRepository repository;

    /** The latest scan; replaced as a whole. */
    private volatile Snapshot snapshot = new Snapshot(Map.of(), Instant.EPOCH);

    /**
     * Instantiates the catalog.
     *
     * @param repository the repository the registry lives in
     */
    WebhookEventCatalog(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Every declared event type.
     *
     * @return the event types, sorted by type
     */
    Collection<WebhookEventType> types() {
        return current().types()
                        .values();
    }

    /**
     * One declared event type.
     *
     * @param type the type, i.e. the topic the entity event is published on
     * @return the type, empty when no published application declares it
     */
    Optional<WebhookEventType> find(String type) {
        return Optional.ofNullable(current().types()
                                            .get(type));
    }

    private Snapshot current() {
        Snapshot cached = snapshot;
        Instant now = Instant.now();
        if (now.isBefore(cached.readAt()
                               .plus(TTL))) {
            return cached;
        }
        Snapshot fresh = new Snapshot(scan(), now);
        snapshot = fresh;
        return fresh;
    }

    private Map<String, WebhookEventType> scan() {
        Map<String, WebhookEventType> types = new TreeMap<>();
        ICollection registry = repository.getCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        if (!registry.exists()) {
            return types;
        }
        for (ICollection project : registry.getCollections()) {
            ICollection gen = project.getCollection(GEN_FOLDER);
            if (gen.exists()) {
                collect(project.getName(), gen, 0, types);
            }
        }
        return types;
    }

    private void collect(String project, ICollection folder, int depth, Map<String, WebhookEventType> types) {
        for (IResource resource : folder.getResources()) {
            if (resource.getName()
                        .endsWith(DESCRIPTOR_SUFFIX)) {
                read(project, resource, types);
            }
        }
        if (depth < MAX_DEPTH) {
            for (ICollection child : folder.getCollections()) {
                collect(project, child, depth + 1, types);
            }
        }
    }

    private static void read(String project, IResource resource, Map<String, WebhookEventType> types) {
        JsonNode descriptor;
        try {
            descriptor = JSON.readTree(new String(resource.getContent(), StandardCharsets.UTF_8));
        } catch (JacksonException ex) {
            LOGGER.warn("Ignoring the webhook descriptor [{}]: it is not valid JSON", resource.getPath(), ex);
            return;
        }
        String entity = text(descriptor, "entity");
        String idProperty = text(descriptor, "idProperty");
        if (entity == null || idProperty == null) {
            LOGGER.warn("Ignoring the webhook descriptor [{}]: it names no entity or no idProperty", resource.getPath());
            return;
        }
        for (JsonNode event : descriptor.path("events")) {
            String type = text(event, "type");
            String action = text(event, "action");
            if (type == null || action == null) {
                continue;
            }
            types.put(type, new WebhookEventType(type, action, project, entity, idProperty, text(descriptor, "uuidProperty"),
                    text(descriptor, "statusProperty")));
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asString()
                                                       .isBlank() ? null : value.asString();
    }

    /**
     * One scan.
     *
     * @param types the declared types, by type
     * @param readAt when it was read
     */
    private record Snapshot(Map<String, WebhookEventType> types, Instant readAt) {
    }
}
