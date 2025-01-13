/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.io;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Component;

/**
 * The Class FTPFacade.
 */
@Component
public class FTPFacade {

    /**
     * Connect.
     *
     * @param host the host
     * @param port the port
     * @param username the username
     * @param password the password
     * @return the FTP client
     * @throws SocketException the socket exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static FTPClient connect(String host, int port, String username, String password) throws SocketException, IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(host, port);
        if (username != null && password != null) {
            ftpClient.login(username, password);
        }
        return ftpClient;
    }

    /**
     * Disconnect.
     *
     * @param ftpClient the ftp client
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void disconnect(FTPClient ftpClient) throws IOException {
        if (ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }
}
