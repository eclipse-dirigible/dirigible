/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.etcd;

import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.stereotype.Component;

/**
 * The Class EtcdFacade.
 * <p>
 * The etcd client library (jetcd) is an add-on that the default bundle does not ship (#7783). The
 * signatures here therefore carry no jetcd type - Spring and the JavaScript host introspect them
 * whether or not the library is present - and every call throws {@link EtcdNotAvailableException}
 * when it is absent. The values are jetcd's {@code KV} and {@code ByteSequence} when it is present.
 */
@Component
public class EtcdFacade {

    /**
     * The Constant DIRIGIBLE_ETCD_CLIENT_ENDPOINT.
     */
    private static final String DIRIGIBLE_ETCD_CLIENT_ENDPOINT = "DIRIGIBLE_ETCD_CLIENT_ENDPOINT";

    /**
     * The Constant CLIENT_ENDPOINT.
     */
    private static final String CLIENT_ENDPOINT = "http://localhost:2379";

    /**
     * The class whose presence means the client library is bundled.
     */
    static final String CLIENT_LIBRARY_CLASS = "io.etcd.jetcd.Client";

    /**
     * Gets the etcd client.
     *
     * @return the etcd client object, a jetcd {@code KV}
     * @throws EtcdNotAvailableException when the etcd client library is not bundled
     */
    public static Object getClient() {
        requireClientLibrary();
        String clientEndpoint = Configuration.get(DIRIGIBLE_ETCD_CLIENT_ENDPOINT, CLIENT_ENDPOINT);
        return JetcdClient.getClient(clientEndpoint);
    }

    /**
     * Converts a string to etcd byte sequence.
     *
     * @param str the string to be converted
     * @return the jetcd {@code ByteSequence} of the string
     * @throws EtcdNotAvailableException when the etcd client library is not bundled
     */
    public static Object stringToByteSequence(String str) {
        requireClientLibrary();
        return JetcdClient.stringToByteSequence(str);
    }

    /**
     * Converts a byte array to etcd byte sequence.
     *
     * @param arr the byte array to be converted
     * @return the jetcd {@code ByteSequence} of the byte array
     * @throws EtcdNotAvailableException when the etcd client library is not bundled
     */
    public static Object byteArrayToByteSequence(byte[] arr) {
        requireClientLibrary();
        return JetcdClient.byteArrayToByteSequence(arr);
    }

    /**
     * Converts an etcd byte sequence to string.
     *
     * @param value the jetcd {@code ByteSequence} to be converted
     * @return the string of the byte sequence
     * @throws EtcdNotAvailableException when the etcd client library is not bundled
     */
    public static String byteSequenceToString(Object value) {
        requireClientLibrary();
        return JetcdClient.byteSequenceToString(value);
    }

    /**
     * Checks that the etcd client library is on the classpath.
     *
     * @throws EtcdNotAvailableException when it is not
     */
    private static void requireClientLibrary() {
        try {
            Class.forName(CLIENT_LIBRARY_CLASS, false, EtcdFacade.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new EtcdNotAvailableException(e);
        }
    }

}
