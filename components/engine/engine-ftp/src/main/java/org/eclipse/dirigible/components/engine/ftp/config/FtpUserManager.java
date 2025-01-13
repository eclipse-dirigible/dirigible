/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.ftp.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.eclipse.dirigible.components.engine.ftp.domain.FtpUser;
import org.eclipse.dirigible.components.engine.ftp.repository.FtpUserRepository;
import org.springframework.util.Assert;

/**
 * The Class FtpUserManager.
 */
public class FtpUserManager implements UserManager {

    /** The root. */
    private File root;

    /** The ftp user repository. */
    private FtpUserRepository ftpUserRepository;

    /** The admin authorities. */
    public static final List<Authority> adminAuthorities = List.of(new WritePermission());

    /** The anonymous authorities. */
    public static final List<Authority> anonymousAuthorities =
            List.of(new ConcurrentLoginPermission(20, 20), new TransferRatePermission(4800, 4800));

    /**
     * Instantiates a new ftp user manager.
     *
     * @param root the root
     * @param ftpUserRepository the ftp user repository
     */
    public FtpUserManager(File root, FtpUserRepository ftpUserRepository) {
        this.root = root;
        this.ftpUserRepository = ftpUserRepository;
    }

    /**
     * Gets the user by name.
     *
     * @param name the name
     * @return the user by name
     */
    @Override
    public User getUserByName(String name) {
        List<FtpUser> users = this.ftpUserRepository.findAllByUsername(name);
        Assert.isTrue(users.size() > 0, "There must be a user with name: " + name);
        FtpUser user = users.get(0);
        List<Authority> authorities = new ArrayList<>(FtpUserManager.anonymousAuthorities);
        if (user.isAdmin()) {
            authorities.addAll(FtpUserManager.adminAuthorities);
        }
        user.setAuthorities(authorities);
        return user;
    }

    /**
     * Gets the all user names.
     *
     * @return the all user names
     */
    @Override
    public String[] getAllUserNames() {
        List<String> userNames = ftpUserRepository.findAll()
                                                  .stream()
                                                  .map(FtpUser::getUsername)
                                                  .collect(Collectors.toList());
        return userNames.toArray(new String[0]);
    }

    /**
     * Delete.
     *
     * @param name the name
     */
    @Override
    public void delete(String name) {
        this.ftpUserRepository.deleteByUsername(name);
    }

    /**
     * Save.
     *
     * @param user the user
     * @throws FtpException the ftp exception
     */
    @Override
    public void save(User user) throws FtpException {
        File home = new File(new File(root, user.getName()), "home");
        Assert.isTrue(home.exists() || home.mkdirs(), "The home directory " + home.getAbsolutePath() + " must exist");
        FtpUser ftpuser = new FtpUser(user.getName(), user.getPassword(), user.getEnabled(), user.getAuthorities(), user.getMaxIdleTime(),
                user.getHomeDirectory(), user.getAuthorities()
                                             .equals(adminAuthorities));
        this.ftpUserRepository.save(ftpuser);
    }

    /**
     * Does exist.
     *
     * @param username the username
     * @return true, if successful
     * @throws FtpException the ftp exception
     */
    @Override
    public boolean doesExist(String username) throws FtpException {
        return this.getUserByName(username) != null;
    }

    /**
     * Authenticate.
     *
     * @param authentication the authentication
     * @return the user
     * @throws AuthenticationFailedException the authentication failed exception
     */
    @Override
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        Assert.isTrue(authentication instanceof UsernamePasswordAuthentication,
                "The given authentication must support username and password authentication");
        UsernamePasswordAuthentication upw = (UsernamePasswordAuthentication) authentication;
        String user = upw.getUsername();
        return Optional.ofNullable(this.getUserByName(user))
                       .filter(u -> {
                           String incomingPw = u.getPassword();
                           return encode(incomingPw).equalsIgnoreCase(u.getPassword());
                       })
                       .orElseThrow(() -> new AuthenticationFailedException("Authentication has failed! Try your username and password."));
    }

    /**
     * TODO do something more responsible than this!.
     *
     * @param password the pw
     * @return the string
     */
    private String encode(String password) {
        return password;
    }

    /**
     * Gets the admin name.
     *
     * @return the admin name
     */
    @Override
    public String getAdminName() {
        return "admin";
    }

    /**
     * Checks if is admin.
     *
     * @param s the s
     * @return true, if is admin
     */
    @Override
    public boolean isAdmin(String s) {
        return getAdminName().equalsIgnoreCase(s);
    }

}
