/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Permission grant. {@link #role} is the role name (e.g. {@code Sales}, {@code Administrator});
 * {@link #can} is a list of {@code Resource:action} tokens (e.g. {@code Customer:read},
 * {@code Order:write}, {@code OrderApproval:start}). Generates {@code .roles} + {@code .access}
 * artefacts and {@code @Roles} annotations on generated controllers.
 */
public class PermissionIntent {

    private String role;
    private List<String> can = new ArrayList<>();
    private String description;

    /**
     * Whether the resources this entry's tokens bind are gated by the roles the module names ONLY: the
     * deployment's {@code .settings} {@code access.extraRoles} / {@code extraRolesReadOnly} are not
     * appended to any of their gates (issue #8112).
     */
    private Boolean exclusive;

    /**
     * Whether this entry ADDS its role to the gates of the resources its tokens bind instead of
     * replacing their convention roles: the resource keeps
     * {@code <project>.<perspective>.<Entity>ReadOnly} / {@code FullAccess} declared and admitted, and
     * the role joins them (issue #8852). An entry is {@code additive} or {@code exclusive}, never both.
     */
    private Boolean additive;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getCan() {
        return can;
    }

    public void setCan(List<String> can) {
        this.can = can == null ? new ArrayList<>() : can;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isExclusive() {
        return Boolean.TRUE.equals(exclusive);
    }

    public void setExclusive(Boolean exclusive) {
        this.exclusive = exclusive;
    }

    public boolean isAdditive() {
        return Boolean.TRUE.equals(additive);
    }

    public void setAdditive(Boolean additive) {
        this.additive = additive;
    }
}
