/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.PermissionIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;

/**
 * Turns the intent's {@code permissions[].can: [Resource:action, ...]} tokens into the read / write
 * role sets the generated application actually enforces.
 *
 * <p>
 * Until this existed the two halves lived in disjoint namespaces: {@code permissions[].role} became
 * {@code <intent>.roles} while the generated controller's gate was the convention-derived
 * {@code <project>.<perspective>.<Entity>FullAccess} - a name no intent construct mentions - so
 * granting an authored role granted nothing at all, silently. The {@code can:} tokens are what says
 * WHICH role gates WHICH resource, so they are what the gates are bound from.
 *
 * <p>
 * The generated gate is binary (read / write) while the token's action half is free-form, so the
 * vocabulary is closed here and every token outside it is REPORTED rather than dropped:
 * <ul>
 * <li>{@code read} / {@code view} / {@code list} grant the read gate.</li>
 * <li>{@code write} / {@code create} / {@code update} / {@code edit} / {@code delete} /
 * {@code manage} grant the write gate - and the read gate with it, because a caller who may change
 * a record must be able to load it first.</li>
 * <li>{@code *} and {@code all} grant both.</li>
 * <li>Anything else ({@code approve}, {@code start}, ...) is a business action with no generated
 * URL behind it. It is legitimate to author - the process guard or a hand-written
 * {@code custom/*.access} enforces it - so it is kept as an advisory naming the token, never
 * silently discarded.</li>
 * </ul>
 * A token whose resource half names nothing the intent declares is an ISSUE (a typo that would gate
 * nothing), and a malformed token is refused by the parser before generation runs.
 */
public final class PermissionSupport {

    /** Actions that grant the read gate. */
    private static final Set<String> READ_ACTIONS = Set.of("read", "view", "list");

    /** Actions that grant the write gate (and, with it, the read gate). */
    private static final Set<String> WRITE_ACTIONS = Set.of("write", "create", "update", "edit", "delete", "manage");

    /** Actions that grant both gates. */
    private static final Set<String> ALL_ACTIONS = Set.of("*", "all");

    private PermissionSupport() {}

    /**
     * The authored gates of one intent: per resource name, the roles that may read it and the roles
     * that may change it, plus what could not be bound.
     *
     * @param read resource name to the roles granted its read gate (insertion-ordered)
     * @param write resource name to the roles granted its write gate (insertion-ordered)
     * @param issues actionable problems - a token naming an undeclared resource
     * @param advisories observations - a token whose action maps to no generated gate
     */
    public record Gates(Map<String, Set<String>> read, Map<String, Set<String>> write, List<String> issues, List<String> advisories) {

        /** Whether any {@code can:} token named this resource, i.e. whether it has authored gates. */
        public boolean covers(String resource) {
            return read.containsKey(resource) || write.containsKey(resource);
        }

        /** The comma-separated read roles of a resource, or {@code null} when none are authored. */
        public String readRoles(String resource) {
            return join(read.get(resource));
        }

        /** The comma-separated write roles of a resource, or {@code null} when none are authored. */
        public String writeRoles(String resource) {
            return join(write.get(resource));
        }

        private static String join(Set<String> roles) {
            return roles == null || roles.isEmpty() ? null : String.join(",", roles);
        }
    }

    /**
     * Resolve the intent's {@code permissions} block into the gates the generators bind.
     *
     * @param model the parsed intent
     * @return the gates; empty maps when the intent declares no {@code can:} tokens
     */
    public static Gates gates(IntentModel model) {
        Map<String, Set<String>> read = new LinkedHashMap<>();
        Map<String, Set<String>> write = new LinkedHashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> advisories = new ArrayList<>();
        Set<String> gateBearing = gateBearingResources(model);
        Set<String> declared = declaredResources(model);
        for (PermissionIntent permission : model.getPermissions()) {
            String role = permission.getRole();
            if (role == null || role.isBlank()) {
                continue;
            }
            for (String token : permission.getCan()) {
                bind(role.trim(), token, gateBearing, declared, read, write, issues, advisories);
            }
        }
        return new Gates(Collections.unmodifiableMap(read), Collections.unmodifiableMap(write), Collections.unmodifiableList(issues),
                Collections.unmodifiableList(advisories));
    }

    private static void bind(String role, String token, Set<String> gateBearing, Set<String> declared, Map<String, Set<String>> read,
            Map<String, Set<String>> write, List<String> issues, List<String> advisories) {
        if (token == null || token.isBlank()) {
            return;
        }
        int colon = token.indexOf(':');
        if (colon < 0) {
            // The parser refuses a malformed token, so generation only ever sees `Resource:action`.
            return;
        }
        String resource = token.substring(0, colon)
                               .trim();
        String action = token.substring(colon + 1)
                             .trim()
                             .toLowerCase(Locale.ROOT);
        if (!declared.contains(resource)) {
            issues.add("permission [" + role + "] can [" + token + "] names resource [" + resource
                    + "], which the intent does not declare - it gates nothing (a resource owned by another model is"
                    + " gated where it is declared)");
            return;
        }
        boolean readable = READ_ACTIONS.contains(action) || ALL_ACTIONS.contains(action);
        boolean writable = WRITE_ACTIONS.contains(action) || ALL_ACTIONS.contains(action);
        if (!readable && !writable) {
            advisories.add("permission [" + role + "] can [" + token + "] names action [" + action
                    + "], which maps to no generated gate (the generated surfaces gate read and write) - enforce it in a process guard"
                    + " or a hand-written custom/*.access");
            return;
        }
        if (!gateBearing.contains(resource)) {
            advisories.add("permission [" + role + "] can [" + token + "] names [" + resource
                    + "], which publishes no generated read/write gate - only entities and reports do");
            return;
        }
        if (writable) {
            write.computeIfAbsent(resource, key -> new LinkedHashSet<>())
                 .add(role);
        }
        // A write grant carries the read gate: a caller who may change a record must be able to load
        // it, and the generated read check is not satisfied by holding the write role alone once the
        // read gate names authored roles.
        read.computeIfAbsent(resource, key -> new LinkedHashSet<>())
            .add(role);
    }

    /** The resources whose generated surfaces carry a read/write gate: entities and reports. */
    private static Set<String> gateBearingResources(IntentModel model) {
        Set<String> resources = new LinkedHashSet<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                resources.add(entity.getName());
            }
        }
        for (ReportIntent report : model.getReports()) {
            if (report.getName() != null) {
                resources.add(report.getName());
            }
        }
        return resources;
    }

    /**
     * Every name a {@code can:} token may legitimately reference - the gate-bearing resources plus the
     * processes and forms a non-CRUD action names ({@code OrderApproval:start}).
     */
    public static Set<String> declaredResources(IntentModel model) {
        Set<String> resources = gateBearingResources(model);
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() != null) {
                resources.add(process.getName());
            }
        }
        for (FormIntent form : model.getForms()) {
            if (form.getName() != null) {
                resources.add(form.getName());
            }
        }
        return resources;
    }
}
