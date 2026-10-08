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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The grouped, described view of the catalogued configuration keys: which group each key belongs
 * to, its default, its effective value and where that value comes from.
 * <p>
 * Every value a descriptor carries goes through {@link SensitiveConfigs#mask(String, String)}, so a
 * secret never leaves the server in clear. The effective value follows the instance-level
 * precedence of {@link Configuration#get(String)} - runtime, environment, deployment, module - and
 * falls back to the {@link DirigibleConfig} default. The per-request tenant overrides are not part
 * of it: they are described per tenant, next to the platform value they override.
 * <p>
 * {@code type}, {@code restartRequired} and {@code deprecatedBy} stay {@code null} until the keys
 * carry explicit metadata (#7759).
 */
public final class ConfigDescriptors {

    /** The value comes from the runtime (set through the API or the configurations endpoint). */
    public static final String SOURCE_RUNTIME = "runtime";

    /** The value comes from an environment variable or a system property. */
    public static final String SOURCE_ENVIRONMENT = "environment";

    /** The value comes from the deployment's properties files. */
    public static final String SOURCE_DEPLOYMENT = "deployment";

    /** The value comes from a module's properties file. */
    public static final String SOURCE_MODULE = "module";

    /** No source sets the key; the value is the key's default. */
    public static final String SOURCE_DEFAULT = "default";

    /** No source sets the key and it has no default. */
    public static final String SOURCE_UNSET = "unset";

    /**
     * One group with its key counts.
     *
     * @param id the group id
     * @param section the section id
     * @param label the default label
     * @param order the display position
     * @param total the number of keys in the group
     * @param set the number of keys any source sets
     */
    public record GroupSummary(String id, String section, String label, int order, int total, int set) {
    }

    /**
     * One described configuration key.
     *
     * @param key the configuration key
     * @param subgroup the subgroup within the group, or {@code null}
     * @param type the value type, {@code null} until declared (#7759)
     * @param defaultValue the default, masked when sensitive
     * @param value the effective value, masked when sensitive
     * @param source which source the effective value comes from, one of the {@code SOURCE_*} constants
     * @param values the value of each source (runtime, environment, deployment, module), each masked
     *        when sensitive
     * @param sensitive whether the value is a secret
     * @param restartRequired whether a change needs a restart, {@code null} until declared (#7759)
     * @param tenantOverridable whether a tenant may override the key
     * @param deprecatedBy the key that replaces this one, {@code null} until declared (#7759)
     */
    public record Entry(String key, String subgroup, String type, String defaultValue, String value, String source,
            Map<String, String> values, boolean sensitive, Boolean restartRequired, boolean tenantOverridable, String deprecatedBy) {
    }

    /**
     * One group with its described keys.
     *
     * @param group the group id
     * @param section the section id
     * @param label the default label
     * @param entries the described keys, ordered by key
     */
    public record Group(String group, String section, String label, List<Entry> entries) {
    }

    private static final Map<String, String> DEFAULTS = Arrays.stream(DirigibleConfig.values())
                                                              .filter(config -> config.getDefaultValue() != null)
                                                              .collect(Collectors.toMap(DirigibleConfig::getKey,
                                                                      DirigibleConfig::getDefaultValue, (a, b) -> a));

    private static volatile Predicate<String> tenantOverridable = key -> false;

    private ConfigDescriptors() {}

    /**
     * Registers the policy deciding which keys a tenant may override. The tenant configuration
     * component registers it on startup; until keys carry a {@code tenantOverridable} flag (#7759),
     * that component is the only one that knows.
     *
     * @param policy the policy, {@code null} resets it to "none"
     */
    public static void setTenantOverridablePolicy(Predicate<String> policy) {
        tenantOverridable = policy == null ? key -> false : policy;
    }

    /**
     * Parses group ids, ignoring blanks and unknown ids.
     *
     * @param ids the group ids, may be {@code null}
     * @return the groups, empty when none were given or none is known
     */
    public static Set<ConfigGroup> parseGroups(Collection<String> ids) {
        Set<ConfigGroup> groups = EnumSet.noneOf(ConfigGroup.class);
        if (ids != null) {
            for (String id : ids) {
                if (id != null) {
                    for (String part : id.split(",")) {
                        ConfigGroup.fromId(part.trim())
                                   .ifPresent(groups::add);
                    }
                }
            }
        }
        return groups;
    }

    /**
     * Whether a key belongs to one of the groups.
     *
     * @param key the configuration key
     * @param groups the groups; {@code null} or empty means every group
     * @return true when the key is in one of them
     */
    public static boolean inGroups(String key, Collection<ConfigGroup> groups) {
        return groups == null || groups.isEmpty() || groups.contains(ConfigGroups.resolve(key));
    }

    /**
     * Every group, in display order, with the number of catalogued keys and of keys any source sets.
     * Groups without catalogued keys are left out.
     *
     * @return the groups
     */
    public static List<GroupSummary> groups() {
        Map<ConfigGroup, int[]> counts = new LinkedHashMap<>();
        for (String key : Configuration.getConfigurationParameters()) {
            int[] count = counts.computeIfAbsent(ConfigGroups.resolve(key), group -> new int[2]);
            count[0]++;
            if (isSet(key)) {
                count[1]++;
            }
        }
        List<GroupSummary> result = new ArrayList<>();
        for (ConfigGroup group : ConfigGroup.values()) {
            int[] count = counts.get(group);
            if (count != null) {
                result.add(new GroupSummary(group.getId(), group.getSection(), group.getLabel(), group.getOrder(), count[0], count[1]));
            }
        }
        return result;
    }

    /**
     * The described keys, grouped.
     *
     * @param groups the groups to include; {@code null} or empty means every group
     * @param query a case-insensitive text the key or its displayed value must contain; {@code null} or
     *        blank matches every key
     * @param onlySet whether to keep only the keys some source sets
     * @return the groups that have matching keys, in display order
     */
    public static List<Group> describe(Collection<ConfigGroup> groups, String query, boolean onlySet) {
        String needle = query == null || query.isBlank() ? null
                : query.trim()
                       .toLowerCase(Locale.ROOT);
        Map<ConfigGroup, List<Entry>> byGroup = new LinkedHashMap<>();
        Arrays.stream(Configuration.getConfigurationParameters())
              .sorted(Comparator.naturalOrder())
              .filter(key -> inGroups(key, groups))
              .map(ConfigDescriptors::describe)
              .filter(entry -> !onlySet || isSet(entry))
              .filter(entry -> needle == null || matches(entry, needle))
              .forEach(entry -> byGroup.computeIfAbsent(ConfigGroups.resolve(entry.key()), group -> new ArrayList<>())
                                       .add(entry));
        List<Group> result = new ArrayList<>();
        for (ConfigGroup group : ConfigGroup.values()) {
            List<Entry> entries = byGroup.get(group);
            if (entries != null) {
                result.add(new Group(group.getId(), group.getSection(), group.getLabel(), entries));
            }
        }
        return result;
    }

    /**
     * Describes one configuration key.
     *
     * @param key the configuration key
     * @return the descriptor
     */
    public static Entry describe(String key) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(SOURCE_RUNTIME, Configuration.getRuntimeVariables()
                                                .get(key));
        values.put(SOURCE_ENVIRONMENT, Configuration.getEnvironmentVariables()
                                                    .get(key));
        values.put(SOURCE_DEPLOYMENT, Configuration.getDeploymentVariables()
                                                   .get(key));
        values.put(SOURCE_MODULE, Configuration.getModuleVariables()
                                               .get(key));
        String source = SOURCE_UNSET;
        String value = null;
        for (Map.Entry<String, String> candidate : values.entrySet()) {
            if (candidate.getValue() != null) {
                source = candidate.getKey();
                value = candidate.getValue();
                break;
            }
        }
        String defaultValue = DEFAULTS.get(key);
        if (value == null && defaultValue != null) {
            source = SOURCE_DEFAULT;
            value = defaultValue;
        }
        values.replaceAll((name, raw) -> SensitiveConfigs.mask(key, raw));
        return new Entry(key, ConfigGroups.subgroup(key), null, SensitiveConfigs.mask(key, defaultValue), SensitiveConfigs.mask(key, value),
                source, values, SensitiveConfigs.isSensitive(key), null, tenantOverridable.test(key), null);
    }

    /**
     * The value the instance itself gives a key, without any tenant override: the effective value of
     * {@link #describe(String)}, masked when sensitive.
     *
     * @param key the configuration key
     * @return the platform value, or {@code null} when nothing sets it and it has no default
     */
    public static String platformValue(String key) {
        return describe(key).value();
    }

    /**
     * The default of a key, as declared in {@link DirigibleConfig}.
     *
     * @param key the configuration key
     * @return the default, or {@code null} when none is declared
     */
    public static String defaultValue(String key) {
        return DEFAULTS.get(key);
    }

    private static boolean isSet(String key) {
        return Configuration.getRuntimeVariables()
                            .get(key) != null
                || Configuration.getEnvironmentVariables()
                                .get(key) != null
                || Configuration.getDeploymentVariables()
                                .get(key) != null
                || Configuration.getModuleVariables()
                                .get(key) != null;
    }

    private static boolean isSet(Entry entry) {
        return !SOURCE_DEFAULT.equals(entry.source()) && !SOURCE_UNSET.equals(entry.source());
    }

    private static boolean matches(Entry entry, String needle) {
        return entry.key()
                    .toLowerCase(Locale.ROOT)
                    .contains(needle)
                || (entry.value() != null && entry.value()
                                                  .toLowerCase(Locale.ROOT)
                                                  .contains(needle));
    }

}
