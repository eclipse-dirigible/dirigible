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

import java.nio.charset.StandardCharsets;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;

/**
 * The only class of this module that links against jetcd. {@link EtcdFacade} reaches it after
 * checking that the client library is present, so a bundle without the add-on never loads it.
 */
final class JetcdClient {

    private JetcdClient() {}

    static Object getClient(String endpoint) {
        return Client.builder()
                     .endpoints(endpoint)
                     .build()
                     .getKVClient();
    }

    static Object stringToByteSequence(String str) {
        return ByteSequence.from(str, StandardCharsets.UTF_8);
    }

    static Object byteArrayToByteSequence(byte[] arr) {
        return ByteSequence.from(arr);
    }

    static String byteSequenceToString(Object value) {
        return ((ByteSequence) value).toString(StandardCharsets.UTF_8);
    }
}
