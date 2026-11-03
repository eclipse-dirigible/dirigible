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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.ActionIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.ExportIntent;
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
 * silently discarded - unless an {@code actions:} entry binds it with {@code can:}, which makes it
 * the gate of that action's button (issue #8090).</li>
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
     * @param extraRead the deployment's roles every read gate admits besides its own ({@code .settings}
     *        {@code access.extraRoles} + {@code access.extraRolesReadOnly})
     * @param extraWrite the deployment's roles every write gate admits besides its own
     *        ({@code .settings} {@code access.extraRoles})
     * @param exclusive the resources an {@code exclusive: true} permission binds: their gates are the
     *        module's roles only, the deployment's extras never appended (issue #8112)
     */
    public record Gates(Map<String, Set<String>> read, Map<String, Set<String>> write, List<String> issues, List<String> advisories,
            List<String> extraRead, List<String> extraWrite, Set<String> exclusive) {

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

        /**
         * A read gate as the generated surfaces check it: the given comma-separated roles followed by the
         * deployment's extra read roles - none for an {@link #exclusive} resource. The gate's own roles
         * stay FIRST - the template layer declares a gate's first role as its default role, and the extras
         * are declared in {@code <intent>.roles}.
         *
         * @param resource the resource whose gate this is (the one its authored roles are bound to)
         * @param gate the gate's own comma-separated roles (authored or convention)
         * @return the gate with the extras appended, without repeats
         */
        public String appendRead(String resource, String gate) {
            return String.join(",", appendRead(resource, List.of(gate.split(","))));
        }

        /**
         * A write gate as the generated surfaces check it - see {@link #appendRead(String, String)}.
         *
         * @param resource the resource whose gate this is (the one its authored roles are bound to)
         * @param gate the gate's own comma-separated roles (authored or convention)
         * @return the gate with the extras appended, without repeats
         */
        public String appendWrite(String resource, String gate) {
            return String.join(",", append(List.of(gate.split(",")), exclusive.contains(resource) ? List.of() : extraWrite));
        }

        /**
         * The given read roles followed by the deployment's extra read roles - none for an
         * {@link #exclusive} resource.
         *
         * @param resource the resource whose gate this is (the one its authored roles are bound to)
         * @param roles the gate's own roles
         * @return the roles with the extras appended, without repeats
         */
        public List<String> appendRead(String resource, Collection<String> roles) {
            return append(roles, exclusive.contains(resource) ? List.of() : extraRead);
        }

        /** Every extra role the deployment appends to some gate - what {@code <intent>.roles} declares. */
        public Set<String> extraRoles() {
            Set<String> roles = new LinkedHashSet<>(extraWrite);
            roles.addAll(extraRead);
            return roles;
        }

        /**
         * The write gate of an entity as every generated surface checks it: the roles a {@code can:} token
         * authors for the entity (or, for a composition child without its own, for its composition root),
         * else the convention role {@code <project>.<root>.<Entity>FullAccess} - and the deployment's extra
         * write roles after either, unless that grant is {@code exclusive}. The entity's REST controller
         * and anything else that writes its rows on a user's behalf (a file import's upload) must agree on
         * it.
         *
         * @param projectName the project the convention role is prefixed with
         * @param rolePerspective the entity's composition root (the entity itself when it is one)
         * @param entity the entity's declared name
         * @return the comma-separated write roles
         */
        public String writeGate(String projectName, String rolePerspective, String entity) {
            String gateName = covers(entity) ? entity : rolePerspective;
            String authored = covers(gateName) ? writeRoles(gateName) : null;
            return appendWrite(gateName, authored != null ? authored : projectName + "." + rolePerspective + "." + entity + "FullAccess");
        }

        /**
         * The read gate of an entity as the EDM emits it ({@code roleRead}): the roles a {@code can:} token
         * grants read on the entity (or, for a composition child without its own, on its composition root),
         * else the convention role {@code <project>.<root>.<Entity>ReadOnly} - and the deployment's extra
         * read roles after either, unless that grant is {@code exclusive}. The generated controller admits
         * the write gate on a read as well.
         *
         * @param projectName the project the convention role is prefixed with
         * @param rolePerspective the entity's composition root (the entity itself when it is one)
         * @param entity the entity's declared name
         * @return the comma-separated read roles
         */
        public String readGate(String projectName, String rolePerspective, String entity) {
            String gateName = covers(entity) ? entity : rolePerspective;
            String authored = covers(gateName) ? readRoles(gateName) : null;
            return appendRead(gateName, authored != null ? authored : projectName + "." + rolePerspective + "." + entity + "ReadOnly");
        }

        private static List<String> append(Collection<String> roles, List<String> extras) {
            Set<String> gate = new LinkedHashSet<>(roles);
            gate.addAll(extras);
            return List.copyOf(gate);
        }

        private static String join(Set<String> roles) {
            return roles == null || roles.isEmpty() ? null : String.join(",", roles);
        }
    }

    /**
     * Resolve the intent's {@code permissions} block into the gates the generators bind, with no
     * deployment extras.
     *
     * @param model the parsed intent
     * @return the gates; empty maps when the intent declares no {@code can:} tokens
     */
    public static Gates gates(IntentModel model) {
        return gates(model, null);
    }

    /**
     * Resolve the intent's {@code permissions} block into the gates the generators bind, together with
     * the roles the project's {@code .settings} append to every gate ({@code access.extraRoles} /
     * {@code access.extraRolesReadOnly}).
     *
     * @param model the parsed intent
     * @param settings the project's settings, or {@code null} for none
     * @return the gates; empty maps when the intent declares no {@code can:} tokens
     */
    public static Gates gates(IntentModel model, IntentSettings settings) {
        Map<String, Set<String>> read = new LinkedHashMap<>();
        Map<String, Set<String>> write = new LinkedHashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> advisories = new ArrayList<>();
        Set<String> gateBearing = gateBearingResources(model);
        Set<String> declared = declaredResources(model);
        Set<String> actionBound = actionBoundTokens(model);
        Set<String> exclusive = new LinkedHashSet<>();
        for (PermissionIntent permission : model.getPermissions()) {
            String role = permission.getRole();
            if (role == null || role.isBlank()) {
                continue;
            }
            for (String token : permission.getCan()) {
                String bound = bind(role.trim(), token, gateBearing, declared, actionBound, read, write, issues, advisories);
                if (bound != null && permission.isExclusive()) {
                    exclusive.add(bound);
                }
            }
        }
        List<String> extraWrite = settings == null ? List.of() : settings.extraRoles();
        List<String> extraRead = settings == null ? List.of() : Gates.append(extraWrite, settings.extraRolesReadOnly());
        return new Gates(Collections.unmodifiableMap(read), Collections.unmodifiableMap(write), Collections.unmodifiableList(issues),
                Collections.unmodifiableList(advisories), extraRead, extraWrite, Collections.unmodifiableSet(exclusive));
    }

    /** Binds one token into the gates; returns the resource it bound, or null when it bound none. */
    private static String bind(String role, String token, Set<String> gateBearing, Set<String> declared, Set<String> actionBound,
            Map<String, Set<String>> read, Map<String, Set<String>> write, List<String> issues, List<String> advisories) {
        if (token == null || token.isBlank()) {
            return null;
        }
        int colon = token.indexOf(':');
        if (colon < 0) {
            // The parser refuses a malformed token, so generation only ever sees `Resource:action`.
            return null;
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
            return null;
        }
        boolean readable = READ_ACTIONS.contains(action) || ALL_ACTIONS.contains(action);
        boolean writable = WRITE_ACTIONS.contains(action) || ALL_ACTIONS.contains(action);
        if (!readable && !writable) {
            if (actionBound.contains(token.trim())) {
                // The gate of a call action's button (issue #8090): enforced where the button is offered.
                return null;
            }
            advisories.add("permission [" + role + "] can [" + token + "] names action [" + action
                    + "], which maps to no generated gate (the generated surfaces gate read and write) - enforce it in a process guard"
                    + " or a hand-written custom/*.access");
            return null;
        }
        if (!gateBearing.contains(resource)) {
            advisories.add("permission [" + role + "] can [" + token + "] names [" + resource
                    + "], which publishes no generated read/write gate - only entities, reports and exports do");
            return null;
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
        return resource;
    }

    /**
     * The roles whose {@code permissions[].can} lists the token - who is offered the button of an
     * action bound to it with {@code can:} (issue #8090).
     *
     * @param model the parsed intent
     * @param token the {@code Resource:action} token
     * @return the roles, in declaration order; empty when no role lists the token
     */
    public static List<String> rolesListing(IntentModel model, String token) {
        Set<String> roles = new LinkedHashSet<>();
        for (PermissionIntent permission : model.getPermissions()) {
            String role = permission.getRole();
            if (role == null || role.isBlank()) {
                continue;
            }
            for (String listed : permission.getCan()) {
                if (listed != null && listed.trim()
                                            .equals(token.trim())) {
                    roles.add(role.trim());
                }
            }
        }
        return List.copyOf(roles);
    }

    /** The tokens the {@code actions:} entries bind with {@code can:}. */
    private static Set<String> actionBoundTokens(IntentModel model) {
        Set<String> tokens = new LinkedHashSet<>();
        for (ActionIntent action : model.getActions()) {
            if (action.getCan() != null && !action.getCan()
                                                  .isBlank()) {
                tokens.add(action.getCan()
                                 .trim());
            }
        }
        return tokens;
    }

    /**
     * The resources whose generated surfaces carry a read/write gate: entities, reports and exports.
     */
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
        // An export's download is a generated read surface of its own (#7855): `<Export>:read` is who
        // may run it and take the file.
        for (ExportIntent export : model.getExports()) {
            if (export.getName() != null) {
                resources.add(export.getName());
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
