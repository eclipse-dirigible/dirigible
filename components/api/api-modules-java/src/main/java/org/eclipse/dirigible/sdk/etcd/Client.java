/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import org.eclipse.dirigible.components.api.etcd.EtcdFacade;

/**
 * Hands back a connected jetcd {@link KV} client against the platform's etcd configuration. Use for
 * distributed configuration storage, leader election, and lock primitives — the full jetcd async
 * surface is available on the returned client.
 * <p>
 * etcd keys and values are byte sequences; the {@code toBytes} / {@code toString} helpers here
 * convert between Java {@link String} / {@code byte[]} and jetcd {@link ByteSequence} so callers
 * don't have to import jetcd's encoding helpers directly.
 * <p>
 * The jetcd client library is an add-on that the default bundle does not ship (#7783): add
 * {@code org.eclipse.dirigible:dirigible-components-api-etcd-client} ({@code <type>pom</type>}) to
 * the application. Without it every method here throws
 * {@link org.eclipse.dirigible.components.api.etcd.EtcdNotAvailableException}.
 */
public final class Client {

    private Client() {}

    public static KV getClient() {
        return (KV) EtcdFacade.getClient();
    }

    public static ByteSequence toBytes(String value) {
        return (ByteSequence) EtcdFacade.stringToByteSequence(value);
    }

    public static ByteSequence toBytes(byte[] value) {
        return (ByteSequence) EtcdFacade.byteArrayToByteSequence(value);
    }

    public static String toString(ByteSequence value) {
        return EtcdFacade.byteSequenceToString(value);
    }
}
