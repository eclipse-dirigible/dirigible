# Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

from java.lang import String, Byte
import java.lang.reflect.Array as JArray

import java

class Bytes:
    BytesFacade = java.type('org.eclipse.dirigible.components.api.io')
    @staticmethod
    def toJavaBytes(bytes):
        internalBytes = JArray.newInstance(Byte.TYPE, len(bytes))
        for i in range(len(bytes)):
            internalBytes[i] = bytes[i]
        return internalBytes

    @staticmethod
    def toJavaScriptBytes(internalBytes):
        bytes = []
        for i in range(internalBytes.length):
            bytes.append(internalBytes[i])
        return bytes

    @staticmethod
    def textToByteArray(text):
        javaString = String(text)
        native = BytesFacade.textToByteArray(text)
        return toJavaScriptBytes(native)

    @staticmethod
    def byteArrayToText(data):
        native = toJavaBytes(data)
        return "".join(map(chr, toJavaScriptBytes(native)))

    @staticmethod
    def intToByteArray(value, byteOrder):
        return BytesFacade.intToByteArray(value, byteOrder)

    @staticmethod
    def byteArrayToInt(data, byteOrder):
        return BytesFacade.byteArrayToInt(data, byteOrder)
