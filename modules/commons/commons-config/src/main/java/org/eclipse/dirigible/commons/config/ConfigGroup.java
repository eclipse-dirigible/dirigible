/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import java.util.Arrays;
import java.util.Optional;

/**
 * The groups the configuration keys are shown in: 18 groups under 5 sections, in display order.
 * {@link ConfigGroups#resolve(String)} places a key; a key no rule matches falls to {@link #OTHER}.
 * <p>
 * The label is the default (English) text; a UI translates it under
 * {@code configurations.groups.<id>}.
 */
public enum ConfigGroup {

    INSTANCE("platform", "instance", "Instance & Product"), //
    BRANDING("platform", "branding", "Branding & Theme"), //
    LOCALE("platform", "locale", "Region & Language"), //
    TENANCY("platform", "tenancy", "Multi-tenancy"), //
    AUTH("security", "auth", "Authentication & Identity"), //
    WEB("security", "web", "Web Security"), //
    DATABASE("data", "database", "Databases & Data Sources"), //
    REPOSITORY("data", "repository", "Repository & Registry"), //
    DOCUMENTS("data", "documents", "Documents, CMS & Print"), //
    SYNC("data", "sync", "Synchronization, Publishing & Readiness"), //
    BPM("processing", "bpm", "Processes"), //
    JOBS("processing", "jobs", "Jobs & Scheduler"), //
    MESSAGING("processing", "messaging", "Messaging & Events"), //
    MAIL("processing", "mail", "Mail"), //
    INTEGRATIONS("integration", "integrations", "Destinations & Connectivity"), //
    AI("integration", "ai", "AI Assistant"), //
    RUNTIMES("development", "runtimes", "Runtimes & Dependencies"), //
    DEVTOOLS("development", "devtools", "Developer Tools"), //
    OTHER("other", "other", "Other");

    private final String section;

    private final String id;

    private final String label;

    ConfigGroup(String section, String id, String label) {
        this.section = section;
        this.id = id;
        this.label = label;
    }

    /**
     * The section the group belongs to: platform, security, data, processing, integration, development
     * or other.
     *
     * @return the section id
     */
    public String getSection() {
        return section;
    }

    /**
     * The stable id used in URLs ({@code ?group=database}) and i18n keys.
     *
     * @return the group id
     */
    public String getId() {
        return id;
    }

    /**
     * The default (English) label.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * The display position, starting at 1.
     *
     * @return the order
     */
    public int getOrder() {
        return ordinal() + 1;
    }

    /**
     * Finds a group by its id, ignoring case.
     *
     * @param id the group id
     * @return the group, or empty when no group has that id
     */
    public static Optional<ConfigGroup> fromId(String id) {
        return Arrays.stream(values())
                     .filter(group -> group.id.equalsIgnoreCase(id))
                     .findFirst();
    }

}
