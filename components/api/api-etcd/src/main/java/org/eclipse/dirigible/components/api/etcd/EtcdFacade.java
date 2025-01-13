/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.etcd;

import org.eclipse.dirigible.commons.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.google.common.base.Charsets;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;

/**
 * The Class EtcdFacade.
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
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(EtcdFacade.class);

    /**
     * Gets the etcd client.
     *
     * @return the etcd client object
     */
    public static KV getClient() {

        String clientEndpoint = Configuration.get(DIRIGIBLE_ETCD_CLIENT_ENDPOINT, CLIENT_ENDPOINT);

        Client client = Client.builder()
                              .endpoints(clientEndpoint)
                              .build();

        return client.getKVClient();
    }

    /**
     * Converts a string to etcd byte sequence.
     *
     * @param str the string to be converted
     * @return the ByteSequence of the string
     */
    public static ByteSequence stringToByteSequence(String str) {
        return ByteSequence.from(str, Charsets.UTF_8);
    }

    /**
     * Converts a byte array to etcd byte sequence.
     *
     * @param arr the byte array to be converted
     * @return the ByteSequence of the byte array
     */
    public static ByteSequence byteArrayToByteSequence(byte[] arr) {
        return ByteSequence.from(arr);
    }

    /**
     * Converts an etcd byte sequence to string.
     *
     * @param value the byte sequence to be converted
     * @return the string of the byte sequence
     */
    public static String byteSequenceToString(ByteSequence value) {
        return value.toString(Charsets.UTF_8);
    }

}
