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

/**
 * The metadata of a {@link DirigibleConfig} entry beyond its key and default value, written as a
 * chain next to the entry: {@code meta().secret()}, {@code meta().tenant(3)},
 * {@code meta().aliasOf("DIRIGIBLE_...")}. An entry without one gets the defaults below.
 */
public final class ConfigMeta {

    /** The group, or null when the key prefix decides. */
    private String group;

    /** The subgroup within the group, or null. */
    private String subgroup;

    /** The explicit type, or null when it is inferred. */
    private ConfigType type;

    /** Whether the value must never be shown in clear. */
    private boolean sensitive;

    /** The position in the tenant settings, or -1 when a tenant may not override the key. */
    private int tenantOrder = -1;

    /** Whether a change applies only after a restart. */
    private boolean restartRequired = true;

    /** Whether the key is deprecated. */
    private boolean deprecated;

    /** The key that replaces this one, or null. */
    private String deprecatedBy;

    /** Whether this key is a misspelling of {@link #deprecatedBy}, resolved in its place. */
    private boolean alias;

    private ConfigMeta() {}

    /**
     * A new metadata chain with the defaults: no explicit group or type, not sensitive, not
     * tenant-overridable, restart required, not deprecated.
     *
     * @return the metadata
     */
    public static ConfigMeta meta() {
        return new ConfigMeta();
    }

    /**
     * Sets the group explicitly. It wins over the group the key prefix would resolve to.
     *
     * @param group the group id
     * @return this
     */
    public ConfigMeta group(String group) {
        this.group = group;
        return this;
    }

    /**
     * Sets the subgroup.
     *
     * @param subgroup the subgroup id
     * @return this
     */
    public ConfigMeta subgroup(String subgroup) {
        this.subgroup = subgroup;
        return this;
    }

    /**
     * Sets the type explicitly.
     *
     * @param type the type
     * @return this
     */
    public ConfigMeta type(ConfigType type) {
        this.type = type;
        return this;
    }

    /**
     * Marks the value as a secret: type {@link ConfigType#SECRET}, sensitive.
     *
     * @return this
     */
    public ConfigMeta secret() {
        this.type = ConfigType.SECRET;
        this.sensitive = true;
        return this;
    }

    /**
     * Lets a tenant override the key through its tenant configuration. The value is read per call, so
     * no restart is required.
     *
     * @param order the position in the tenant settings
     * @return this
     */
    public ConfigMeta tenant(int order) {
        this.tenantOrder = order;
        this.restartRequired = false;
        return this;
    }

    /**
     * Marks the key as deprecated, with no replacement.
     *
     * @return this
     */
    public ConfigMeta deprecated() {
        this.deprecated = true;
        return this;
    }

    /**
     * Marks the key as deprecated in favour of another one. The value is not carried over: the
     * replacement may take a value of another shape.
     *
     * @param replacement the key that replaces this one
     * @return this
     */
    public ConfigMeta deprecatedBy(String replacement) {
        this.deprecated = true;
        this.deprecatedBy = replacement;
        return this;
    }

    /**
     * Marks the key as a misspelling of another one: deprecated, and a value set under either spelling
     * is read under both.
     *
     * @param canonical the correctly spelled key
     * @return this
     */
    public ConfigMeta aliasOf(String canonical) {
        deprecatedBy(canonical);
        this.alias = true;
        return this;
    }

    String getGroup() {
        return group;
    }

    String getSubgroup() {
        return subgroup;
    }

    ConfigType getType() {
        return type;
    }

    boolean isSensitive() {
        return sensitive;
    }

    int getTenantOrder() {
        return tenantOrder;
    }

    boolean isRestartRequired() {
        return restartRequired;
    }

    boolean isDeprecated() {
        return deprecated;
    }

    String getDeprecatedBy() {
        return deprecatedBy;
    }

    boolean isAlias() {
        return alias;
    }
}
