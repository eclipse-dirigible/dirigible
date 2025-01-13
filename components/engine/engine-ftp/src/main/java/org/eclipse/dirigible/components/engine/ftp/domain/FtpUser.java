/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.ftp.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;
import org.eclipse.dirigible.components.base.converters.ListOfStringsToCsvConverter;
import org.eclipse.dirigible.components.base.encryption.Encrypted;

import com.google.gson.annotations.Expose;

/**
 * The Class FtpUser.
 */
@Entity
@Table(name = "DIRIGIBLE_FTP_USERS")
public class FtpUser implements User {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FTPUSER_ID", nullable = false)
    private Long id;

    /** The username. */
    @Column(name = "FTPUSER_USERNAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String username;

    /** The password. */
    @Column(name = "FTPUSER_PASSWORD", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Encrypted
    private String password;

    /** The enabled. */
    @Column(name = "FTPUSER_ENABLED")
    @Expose
    private boolean enabled;

    /** The max idle time. */
    @Column(name = "FTPUSER_MAXIDLETIME")
    @Expose
    private int maxIdleTime;

    /** The home directory. */
    @Column(name = "FTPUSER_HOME_DIRECTORY", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Nullable
    @Expose
    private String homeDirectory;

    /** The authorities. */
    @Transient
    private transient List<Authority> authorities = new ArrayList<>();

    /** The enabled. */
    @Column(name = "FTPUSER_ADMIN")
    @Expose
    private boolean admin;

    /**
     * Instantiates a new ftp user.
     *
     * @param name the name
     * @param password the pw
     * @param enabled the enabled
     * @param auths the auths
     * @param maxIdleTime the max idle time
     * @param homeDirectory the home directory
     * @param admin the admin
     */
    public FtpUser(String name, String password, boolean enabled, List<? extends Authority> auths, int maxIdleTime, String homeDirectory,
            boolean admin) {
        this.username = name;
        this.maxIdleTime = maxIdleTime == -1 ? 60_000 : maxIdleTime;
        this.homeDirectory = homeDirectory;
        this.password = password;
        this.enabled = enabled;
        if (auths != null) {
            this.authorities.addAll(auths);
        }
        this.admin = admin;
    }

    /**
     * Instantiates a new ftp user.
     */
    public FtpUser() {}

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return this.username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * Gets the authorities.
     *
     * @return the authorities
     */
    @Override
    public List<? extends Authority> getAuthorities() {
        return this.authorities;
    }

    /**
     * Gets the authorities.
     *
     * @param aClass the a class
     * @return the authorities
     */
    @Override
    public List<? extends Authority> getAuthorities(Class<? extends Authority> aClass) {
        return this.authorities.stream()
                               .filter(a -> a.getClass()
                                             .isAssignableFrom(aClass))
                               .collect(Collectors.toList());
    }

    /**
     * Authorize.
     *
     * @param req the req
     * @return the authorization request
     */
    @Override
    public AuthorizationRequest authorize(AuthorizationRequest req) {
        return this.getAuthorities()
                   .stream()
                   .filter(a -> a.canAuthorize(req))
                   .map(a -> a.authorize(req))
                   .filter(Objects::nonNull)
                   .findFirst()
                   .orElse(null);
    }

    /**
     * Gets the max idle time.
     *
     * @return the max idle time
     */
    @Override
    public int getMaxIdleTime() {
        return this.maxIdleTime;
    }

    /**
     * Gets the enabled.
     *
     * @return the enabled
     */
    @Override
    public boolean getEnabled() {
        return this.enabled;
    }

    /**
     * Gets the home directory.
     *
     * @return the home directory
     */
    @Override
    public String getHomeDirectory() {
        return new File(this.homeDirectory).getAbsolutePath();
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
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the enabled.
     *
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets the max idle time.
     *
     * @param maxIdleTime the maxIdleTime to set
     */
    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    /**
     * Sets the authorities.
     *
     * @param authorities the authorities to set
     */
    public void setAuthorities(List<Authority> authorities) {
        this.authorities = authorities;
    }

    /**
     * Sets the home directory.
     *
     * @param homeDirectory the homeDirectory to set
     */
    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    /**
     * Checks if is admin.
     *
     * @return the admin
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * Sets the admin.
     *
     * @param admin the admin to set
     */
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

}
