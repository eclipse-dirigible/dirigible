/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.domain.UserRoleAssignment;
import org.eclipse.dirigible.components.tenants.exceptions.TenantNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * The Class UserService.
 */
@Service
public class UserService {

    /** The tenant service. */
    private final TenantService tenantService;

    /** The role service. */
    private final RoleService roleService;

    /** The user repository. */
    private final UserRepository userRepository;

    /** The password encoder. */
    private final BCryptPasswordEncoder passwordEncoder;

    /** The assignment repository. */
    private final UserRoleAssignmentRepository assignmentRepository;

    /**
     * Instantiates a new user service.
     *
     * @param tenantService the tenant service
     * @param roleService the role service
     * @param userRepository the user repository
     * @param passwordEncoder the password encoder
     * @param assignmentRepository the assignment repository
     */
    public UserService(TenantService tenantService, RoleService roleService, UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder, UserRoleAssignmentRepository assignmentRepository) {
        this.tenantService = tenantService;
        this.roleService = roleService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    public final List<User> getAll() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> {
            List<UserRoleAssignment> assignments = assignmentRepository.findByUser(user);
            List<Role> roles = new ArrayList<Role>();
            assignments.forEach(assignment -> roles.add(roleService.findById(assignment.getRole()
                                                                                       .getId())));
            user.setRoles(roles.toArray(new Role[] {}));
        });
        return users;
    }

    /**
     * Creates the new user.
     *
     * @param username the username
     * @param password the password
     * @param tenantId the tenant id
     * @return the user
     */
    public User createNewUser(String username, String password, String tenantId) {
        Tenant tenant = tenantService.findById(tenantId)
                                     .orElseThrow(() -> new TenantNotFoundException("Tenant " + tenantId + " not found."));
        User user = new User(tenant, username, passwordEncoder.encode(password));
        user.updateKey();
        return userRepository.save(user);
    }

    /**
     * Update user.
     *
     * @param id the id
     * @param username the username
     * @param password the password
     * @param tenantId the tenant id
     * @return the user
     */
    public User updateUser(String id, String username, String password, String tenantId) {
        Tenant tenant = tenantService.findById(tenantId)
                                     .orElseThrow(() -> new TenantNotFoundException("Tenant " + tenantId + " not found."));
        User user = userRepository.findById(id)
                                  .orElseThrow(() -> new TenantNotFoundException("User " + id + " not found."));
        user.setName(username);
        user.setUsername(username);
        user.setPassword(password);
        user.setTenant(tenant);
        user.updateKey();
        return userRepository.save(user);
    }

    /**
     * Delete user.
     *
     * @param id the id
     */
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                                  .orElseThrow(() -> new TenantNotFoundException("User " + id + " not found."));
        assignmentRepository.findByUser(user)
                            .forEach(a -> assignmentRepository.delete(a));
        userRepository.delete(user);
    }

    /**
     * Find user by username and tenant id.
     *
     * @param username the username
     * @param tenantId the tenant id
     * @return the optional
     */
    public Optional<User> findUserByUsernameAndTenantId(String username, String tenantId) {
        return userRepository.findUserByUsernameAndTenantId(username, tenantId);
    }

    /**
     * Find users by tenant id.
     *
     * @param tenantId the tenant id
     * @return the list
     */
    public List<User> findUsersByTenantId(String tenantId) {
        return userRepository.findUsersByTenantId(tenantId);
    }

    /**
     * Find by id.
     *
     * @param id the id
     * @return the optional
     */
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Gets the user role names.
     *
     * @param user the user
     * @return the user role names
     */
    public Set<String> getUserRoleNames(User user) {
        return getUserRoles(user).stream()
                                 .map(Role::getName)
                                 .collect(Collectors.toSet());
    }

    /**
     * Gets the user roles.
     *
     * @param user the user
     * @return the user roles
     */
    private Set<Role> getUserRoles(User user) {
        return assignmentRepository.findByUser(user)
                                   .stream()
                                   .map(a -> a.getRole())
                                   .collect(Collectors.toSet());
    }

    /**
     * Assign user roles.
     *
     * @param user the user
     * @param roles the roles
     */
    public void assignUserRoles(User user, Role... roles) {
        for (Role role : roles) {
            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setUser(user);
            assignment.setRole(role);
            if (!assignmentRepository.findByUserAndRole(user, role)
                                     .isPresent()) {
                assignmentRepository.save(assignment);
            }
        }
    }

    /**
     * Assign user role by id.
     *
     * @param user the user
     * @param roleIds the role ids
     */
    public void assignUserRolesByIds(User user, Long[] roleIds) {
        for (Long roleId : roleIds) {
            Role role = roleService.findById(roleId);
            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setUser(user);
            assignment.setRole(role);
            if (!assignmentRepository.findByUserAndRole(user, role)
                                     .isPresent()) {
                assignmentRepository.save(assignment);
            }
        }
    }
}
