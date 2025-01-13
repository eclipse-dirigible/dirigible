/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.encryption;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * https://github.com/galovics/hibernate-encryption-listener
 */
@Component
public class Decrypter {

    /**
     * Decrypt.
     *
     * @param value the value
     * @return the string
     */
    public String decrypt(String value) {
        return new String(Base64.getDecoder()
                                .decode(value.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
    }
}
