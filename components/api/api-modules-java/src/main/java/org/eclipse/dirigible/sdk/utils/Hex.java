/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.utils;

import org.apache.commons.codec.DecoderException;
import org.eclipse.dirigible.components.api.utils.HexFacade;

/**
 * Hex (base-16) encoding helpers. Lower-case alphabet, no separators — useful for rendering
 * digests, fingerprints, and binary identifiers as printable strings.
 * <p>
 * {@link #decode(String)} throws {@link DecoderException} for invalid input (odd length, non-hex
 * character); reach for it when input is user-supplied and you want to surface a client-friendly
 * error rather than crash on a corrupt byte.
 */
public final class Hex {

    private Hex() {}

    public static String encode(String input) {
        return HexFacade.encode(input);
    }

    public static String encode(byte[] input) {
        return HexFacade.encode(input);
    }

    public static byte[] decode(String input) throws DecoderException {
        return HexFacade.decode(input);
    }

    public static byte[] encodeNative(byte[] input) {
        return HexFacade.encodeNative(input);
    }

    public static byte[] decodeNative(byte[] input) throws DecoderException {
        return HexFacade.decodeNative(input);
    }
}
