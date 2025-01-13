/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.domain;

import org.eclipse.dirigible.components.security.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The Class UserRoleAssignment.
 */
@Entity
@Table(name = "DIRIGIBLE_USER_ROLE_ASSIGNMENTS", uniqueConstraints = {@UniqueConstraint(columnNames = {"USER_ID", "ROLE_ID"})})
public class UserRoleAssignment {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    /** The user. */
    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    /** The role. */
    @ManyToOne
    @JoinColumn(name = "ROLE_ID")
    private Role role;

    /**
     * Instantiates a new user role assignment.
     */
    public UserRoleAssignment() {}

    /**
     * Instantiates a new user role assignment.
     *
     * @param id the id
     * @param user the user
     * @param role the role
     */
    public UserRoleAssignment(Long id, User user, Role role) {
        this.id = id;
        this.user = user;
        this.role = role;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the user.
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user.
     *
     * @param user the new user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the role.
     *
     * @return the role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Sets the role.
     *
     * @param role the new role
     */
    public void setRole(Role role) {
        this.role = role;
    }

}
