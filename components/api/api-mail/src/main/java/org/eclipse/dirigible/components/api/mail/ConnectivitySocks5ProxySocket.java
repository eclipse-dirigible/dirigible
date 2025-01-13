/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 * The Class ConnectivitySocks5ProxySocket.
 */
public class ConnectivitySocks5ProxySocket extends Socket {

    /** The Constant SOCKS5_VERSION. */
    private static final byte SOCKS5_VERSION = 0x05;

    /** The Constant SOCKS5_PASSWORD_AUTHENTICATION_METHOD. */
    private static final byte SOCKS5_PASSWORD_AUTHENTICATION_METHOD = (byte) 0x02;

    /** The Constant SOCKS5_PASSWORD_AUTHENTICATION_METHOD_VERSION. */
    private static final byte SOCKS5_PASSWORD_AUTHENTICATION_METHOD_VERSION = 0x01;

    /** The Constant SOCKS5_COMMAND_CONNECT_BYTE. */
    private static final byte SOCKS5_COMMAND_CONNECT_BYTE = 0x01;

    /** The Constant SOCKS5_COMMAND_REQUEST_RESERVED_BYTE. */
    private static final byte SOCKS5_COMMAND_REQUEST_RESERVED_BYTE = 0x00;

    /** The Constant SOCKS5_COMMAND_ADDRESS_TYPE_IPv4_BYTE. */
    private static final byte SOCKS5_COMMAND_ADDRESS_TYPE_IPv4_BYTE = 0x01;

    /** The Constant SOCKS5_COMMAND_ADDRESS_TYPE_DOMAIN_BYTE. */
    private static final byte SOCKS5_COMMAND_ADDRESS_TYPE_DOMAIN_BYTE = 0x03;

    /** The Constant SOCKS5_AUTHENTICATION_METHODS_COUNT. */
    private static final byte SOCKS5_AUTHENTICATION_METHODS_COUNT = 0x01;

    /** The Constant SOCKS5_PASSWORD_AUTHENTICATION_METHOD_UNSIGNED_VALUE. */
    private static final int SOCKS5_PASSWORD_AUTHENTICATION_METHOD_UNSIGNED_VALUE = 0x02 & 0xFF;

    /** The Constant SOCKS5_AUTHENTICATION_SUCCESS_BYTE. */
    private static final byte SOCKS5_AUTHENTICATION_SUCCESS_BYTE = 0x00;

    /** The proxy address. */
    private final InetSocketAddress proxyAddress;

    /** The username. */
    private final String username;

    /** The password. */
    private final String password;

    /**
     * Instantiates a new connectivity socks 5 proxy socket.
     *
     * @param host the host
     * @param port the port
     * @param username the username
     * @param password the password
     */
    public ConnectivitySocks5ProxySocket(String host, String port, String username, String password) {
        this.proxyAddress = new InetSocketAddress(host, Integer.parseInt(port));
        this.username = username;
        this.password = password;
    }

    /**
     * Connect.
     *
     * @param endpoint the endpoint
     * @param timeout the timeout
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        super.connect(this.proxyAddress, timeout);

        OutputStream outputStream = getOutputStream();

        executeSOCKS5InitialRequest(outputStream);

        executeSOCKS5AuthenticationRequest(outputStream);

        executeSOCKS5ConnectRequest(outputStream, (InetSocketAddress) endpoint);
    }

    /**
     * Connect.
     *
     * @param endpoint the endpoint
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        this.connect(endpoint, 0);
    }

    /**
     * Execute SOCKS 5 initial request.
     *
     * @param outputStream the output stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void executeSOCKS5InitialRequest(OutputStream outputStream) throws IOException {
        byte[] initialRequest = createInitialSOCKS5Request();
        outputStream.write(initialRequest);

        assertServerInitialResponse();
    }

    /**
     * Creates the initial SOCKS 5 request.
     *
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private byte[] createInitialSOCKS5Request() throws IOException {
        try (ByteArrayOutputStream byteArraysStream = new ByteArrayOutputStream()) {
            byteArraysStream.write(SOCKS5_VERSION);
            byteArraysStream.write(SOCKS5_AUTHENTICATION_METHODS_COUNT);
            byteArraysStream.write(SOCKS5_PASSWORD_AUTHENTICATION_METHOD);
            return byteArraysStream.toByteArray();
        }
    }

    /**
     * Assert server initial response.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void assertServerInitialResponse() throws IOException {
        try (InputStream inputStream = getInputStream()) {
            int versionByte = inputStream.read();
            if (SOCKS5_VERSION != versionByte) {
                throw new SocketException("Unsupported SOCKS version - expected " + SOCKS5_VERSION + ", but received " + versionByte);
            }

            int authenticationMethodValue = inputStream.read();
            if (SOCKS5_PASSWORD_AUTHENTICATION_METHOD_UNSIGNED_VALUE != authenticationMethodValue) {
                throw new SocketException("Unsupported authentication method value - expected "
                        + SOCKS5_PASSWORD_AUTHENTICATION_METHOD_UNSIGNED_VALUE + ", but received " + authenticationMethodValue);
            }
        }
    }

    /**
     * Execute SOCKS 5 authentication request.
     *
     * @param outputStream the output stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void executeSOCKS5AuthenticationRequest(OutputStream outputStream) throws IOException {
        byte[] authenticationRequest = createPasswordAuthenticationRequest();
        outputStream.write(authenticationRequest);

        assertAuthenticationResponse();
    }

    /**
     * Creates the password authentication request.
     *
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private byte[] createPasswordAuthenticationRequest() throws IOException {
        try (ByteArrayOutputStream byteArraysStream = new ByteArrayOutputStream()) {
            byteArraysStream.write(SOCKS5_PASSWORD_AUTHENTICATION_METHOD_VERSION);
            byteArraysStream.write(ByteBuffer.allocate(1)
                                             .put((byte) this.username.getBytes().length)
                                             .array());
            byteArraysStream.write(this.username.getBytes());
            byteArraysStream.write(ByteBuffer.allocate(1)
                                             .put((byte) this.password.getBytes().length)
                                             .array());
            byteArraysStream.write(this.password.getBytes());
            return byteArraysStream.toByteArray();
        }
    }

    /**
     * Assert authentication response.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void assertAuthenticationResponse() throws IOException {
        try (InputStream inputStream = getInputStream()) {
            int authenticationMethodVersion = inputStream.read();
            if (SOCKS5_PASSWORD_AUTHENTICATION_METHOD_VERSION != authenticationMethodVersion) {
                throw new SocketException("Unsupported authentication method version - expected "
                        + SOCKS5_PASSWORD_AUTHENTICATION_METHOD_VERSION + ", but received " + authenticationMethodVersion);
            }

            int authenticationStatus = inputStream.read();
            if (SOCKS5_AUTHENTICATION_SUCCESS_BYTE != authenticationStatus) {
                throw new SocketException("Authentication failed!");
            }
        }
    }

    /**
     * Execute SOCKS 5 connect request.
     *
     * @param outputStream the output stream
     * @param endpoint the endpoint
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void executeSOCKS5ConnectRequest(OutputStream outputStream, InetSocketAddress endpoint) throws IOException {
        byte[] commandRequest = createConnectCommandRequest(endpoint);
        outputStream.write(commandRequest);

        assertConnectCommandResponse();
    }

    /**
     * Creates the connect command request.
     *
     * @param endpoint the endpoint
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private byte[] createConnectCommandRequest(InetSocketAddress endpoint) throws IOException {
        String host = endpoint.getHostName();
        int port = endpoint.getPort();
        try (ByteArrayOutputStream byteArraysStream = new ByteArrayOutputStream()) {
            byteArraysStream.write(SOCKS5_VERSION);
            byteArraysStream.write(SOCKS5_COMMAND_CONNECT_BYTE);
            byteArraysStream.write(SOCKS5_COMMAND_REQUEST_RESERVED_BYTE);
            byte[] hostToIPv4 = parseHostToIPv4(host);
            if (hostToIPv4 != null) {
                byteArraysStream.write(SOCKS5_COMMAND_ADDRESS_TYPE_IPv4_BYTE);
                byteArraysStream.write(hostToIPv4);
            } else {
                byteArraysStream.write(SOCKS5_COMMAND_ADDRESS_TYPE_DOMAIN_BYTE);
                byteArraysStream.write(ByteBuffer.allocate(1)
                                                 .put((byte) host.getBytes().length)
                                                 .array());
                byteArraysStream.write(host.getBytes());
            }
            byteArraysStream.write(ByteBuffer.allocate(2)
                                             .putShort((short) port)
                                             .array());
            return byteArraysStream.toByteArray();
        }
    }

    /**
     * Assert connect command response.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void assertConnectCommandResponse() throws IOException {
        try (InputStream inputStream = getInputStream()) {
            int versionByte = inputStream.read();
            if (SOCKS5_VERSION != versionByte) {
                throw new SocketException("Unsupported SOCKS version - expected " + SOCKS5_VERSION + ", but received " + versionByte);
            }

            int connectStatusByte = inputStream.read();
            assertConnectStatus(connectStatusByte);

            readRemainingCommandResponseBytes(inputStream);
        }
    }

    /**
     * Assert connect status.
     *
     * @param commandConnectStatus the command connect status
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void assertConnectStatus(int commandConnectStatus) throws IOException {
        if (commandConnectStatus == 0) {
            return;
        }

        String commandConnectStatusTranslation;
        switch (commandConnectStatus) {
            case 1:
                commandConnectStatusTranslation = "FAILURE";
                break;
            case 2:
                commandConnectStatusTranslation = "FORBIDDEN";
                break;
            case 3:
                commandConnectStatusTranslation = "NETWORK_UNREACHABLE";
                break;
            case 4:
                commandConnectStatusTranslation = "HOST_UNREACHABLE";
                break;
            case 5:
                commandConnectStatusTranslation = "CONNECTION_REFUSED";
                break;
            case 6:
                commandConnectStatusTranslation = "TTL_EXPIRED";
                break;
            case 7:
                commandConnectStatusTranslation = "COMMAND_UNSUPPORTED";
                break;
            case 8:
                commandConnectStatusTranslation = "ADDRESS_UNSUPPORTED";
                break;
            default:
                commandConnectStatusTranslation = "UNKNOWN";
                break;
        }
        throw new SocketException("SOCKS5 command failed with status: " + commandConnectStatusTranslation);
    }

    /**
     * Parses the host to I pv 4.
     *
     * @param hostName the host name
     * @return the byte[]
     */
    private byte[] parseHostToIPv4(String hostName) {
        byte[] parsedHostName = null;
        String[] virtualHostOctets = hostName.split("\\.", -1);
        int octetsCount = virtualHostOctets.length;
        if (octetsCount == 4) {
            try {
                byte[] ipOctets = new byte[octetsCount];
                for (int i = 0; i < octetsCount; i++) {
                    int currentOctet = Integer.parseInt(virtualHostOctets[i]);
                    if ((currentOctet < 0) || (currentOctet > 255)) {
                        throw new IllegalArgumentException("Provided octet %s is not in the range of [0-255]: " + currentOctet);
                    }
                    ipOctets[i] = (byte) currentOctet;
                }
                parsedHostName = ipOctets;
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        return parsedHostName;
    }

    /**
     * Read remaining command response bytes.
     *
     * @param inputStream the input stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void readRemainingCommandResponseBytes(InputStream inputStream) throws IOException {
        inputStream.read(); // skipping over SOCKS5 reserved byte
        int addressTypeByte = inputStream.read();
        if (SOCKS5_COMMAND_ADDRESS_TYPE_IPv4_BYTE == addressTypeByte) {
            for (int i = 0; i < 6; i++) {
                inputStream.read();
            }
        } else if (SOCKS5_COMMAND_ADDRESS_TYPE_DOMAIN_BYTE == addressTypeByte) {
            int domainNameLength = inputStream.read();
            int portBytes = 2;
            inputStream.read(new byte[domainNameLength + portBytes], 0, domainNameLength + portBytes);
        }
    }
}
