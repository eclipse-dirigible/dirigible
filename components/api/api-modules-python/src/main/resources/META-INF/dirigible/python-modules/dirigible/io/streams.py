# Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

import bytes
import java

StreamsFacade = java.type("org.eclipse.dirigible.components.api.io.StreamsFacade")

class InputStream:
    def __init__(self, native):
        self.native = native

    def read(self):
        return StreamsFacade.read(self.native)

    def readBytes(self):
        native = StreamsFacade.readBytes(self.native)
        return bytes.toPythonBytes(native)

    def readBytesNative(self):
        return StreamsFacade.readBytes(self.native)

    def readText(self):
        return StreamsFacade.readText(self.native)

    def close(self):
        StreamsFacade.close(self.native)

    def isValid(self):
        return self.native is not None

class OutputStream:
    def __init__(self, native):
        self.native = native

    def write(self, byte):
        StreamsFacade.write(self.native, byte)

    def writeBytes(self, data):
        native = bytes.toJavaBytes(data)
        StreamsFacade.writeBytes(self.native, native)

    def writeBytesNative(self, data):
        StreamsFacade.writeBytes(self.native, data)

    def writeText(self, text):
        StreamsFacade.writeText(self.native, text)

    def close(self):
        StreamsFacade.close(self.native)

    def getBytes(self):
        native = StreamsFacade.getBytes(self.native)
        data = bytes.toPythonBytes(native)
        return data

    def getBytesNative(self):
        native = StreamsFacade.getBytes(self.native)
        return native

    def getText(self):
        value = StreamsFacade.getText(self.native)
        return value

    def isValid(self):
        return self.native is not None

def copy(input, output):
    StreamsFacade.copy(input.native, output.native)

def copyLarge(input, output):
    StreamsFacade.copyLarge(input.native, output.native)

def getResourceAsByteArrayInputStream(path):
    native = StreamsFacade.getResourceAsByteArrayInputStream(path)
    return InputStream(native)

def createByteArrayInputStream(data):
    array = bytes.toJavaBytes(data)
    native = StreamsFacade.createByteArrayInputStream(array)
    return InputStream(native)

def createByteArrayOutputStream():
    native = StreamsFacade.createByteArrayOutputStream()
    return OutputStream(native)

def createInputStream(native):
    inputStream = InputStream(native)
    return inputStream

def createOutputStream(native):
    outputStream = OutputStream(native)
    return outputStream
