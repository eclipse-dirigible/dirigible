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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.eclipse.dirigible.components.engine.ftp.domain.FtpUser;
import org.eclipse.dirigible.components.engine.ftp.repository.FtpUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * The Class FtpServerConfiguration.
 */
@Configuration
public class FtpServerConfiguration {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(FtpServerConfiguration.class);

    /** The Constant REPOSITORY_ROOT. */
    private static final String REPOSITORY_ROOT = "./target/dirigible/cms/dirigible/repository/root";

    /**
     * File system factory.
     *
     * @return the file system factory
     */
    @Bean
    FileSystemFactory fileSystemFactory() {
        NativeFileSystemFactory fileSystemFactory = new NativeFileSystemFactory();
        fileSystemFactory.setCreateHome(true);
        fileSystemFactory.setCaseInsensitive(false);
        return fileSystemFactory::createFileSystemView;
    }

    /**
     * Nio listener.
     *
     * @return the listener
     */
    @Bean
    Listener nioListener() {
        String portValue = org.eclipse.dirigible.commons.config.Configuration.get("DIRIGIBLE_FTP_PORT", "8022");
        int port = 8022;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException e) {
            logger.error("Wrong configuration for FTP port provided: " + portValue);
        }
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(port);
        return listenerFactory.createListener();
    }

    /**
     * Ftp server.
     *
     * @param ftpletMap the ftplet map
     * @param userManager the user manager
     * @param nioListener the nio listener
     * @param fileSystemFactory the file system factory
     * @return the ftp server
     */
    @Bean
    FtpServer ftpServer(Map<String, Ftplet> ftpletMap, UserManager userManager, Listener nioListener, FileSystemFactory fileSystemFactory) {
        FtpServerFactory ftpServerFactory = new FtpServerFactory();
        ftpServerFactory.setListeners(Collections.singletonMap("default", nioListener));
        ftpServerFactory.setFileSystem(fileSystemFactory);
        ftpServerFactory.setFtplets(ftpletMap);
        ftpServerFactory.setUserManager(userManager);
        ftpServerFactory.setConnectionConfig(new ConnectionConfigFactory().createConnectionConfig());
        try {
            String username = org.eclipse.dirigible.commons.config.Configuration.get("DIRIGIBLE_FTP_USERNAME", "YWRtaW4="); // admin
            String password = org.eclipse.dirigible.commons.config.Configuration.get("DIRIGIBLE_FTP_PASSWORD", "YWRtaW4="); // admin
            userManager.save(new FtpUser(new String(new Base64().decode(username.getBytes()), StandardCharsets.UTF_8).trim(),
                    new String(new Base64().decode(password.getBytes()), StandardCharsets.UTF_8).trim(), true, Collections.EMPTY_LIST, -1,
                    REPOSITORY_ROOT, true));
        } catch (FtpException e) {
            throw new RuntimeException(e);
        }
        return ftpServerFactory.createServer();
    }

    /**
     * Destroys ftp server.
     *
     * @param ftpServer the ftp server
     * @return the disposable bean
     */
    @Bean
    DisposableBean destroysFtpServer(FtpServer ftpServer) {
        return ftpServer::stop;
    }

    /**
     * Starts ftp server.
     *
     * @param ftpServer the ftp server
     * @return the initializing bean
     */
    @Bean
    InitializingBean startsFtpServer(FtpServer ftpServer) {
        return ftpServer::start;
    }

    /**
     * User manager.
     *
     * @param root the root
     * @param ftpUserRepository the ftp user repository
     * @return the user manager
     */
    @Bean
    UserManager userManager(@Value("${ftp.root:${user.home}/ftp/root}") File root, FtpUserRepository ftpUserRepository) {
        Assert.isTrue(root.exists() || root.mkdirs(), "the root directory must exist.");
        return new FtpUserManager(root, ftpUserRepository);
    }

}
