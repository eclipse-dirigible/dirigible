# Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

import bytes
import java


ZipFacade = java.type("org.eclipse.dirigible.components.api.io.ZipFacade")

def zip(sourcePath, zipTargetPath):
    ZipFacade.exportZip(sourcePath, zipTargetPath)

def unzip(zipPath, targetPath):
    ZipFacade.importZip(zipPath, targetPath)

def createZipInputStream(inputStream):
    native = ZipFacade.createZipInputStream(inputStream.native)
    return ZipInputStream(native)

class ZipInputStream:
    @staticmethod
    def __init__(self, native):
        self.native = native

    @staticmethod
    def getNextEntry(self):
        native = self.native.getNextEntry()
        zipEntry = ZipEntry(native)
        return zipEntry

    @staticmethod
    def read(self):
        native = ZipFacade.readNative(self.native)
        return bytes.toPythonBytes(native)

    @staticmethod
    def readNative(self):
        return ZipFacade.readNative(self.native)

    @staticmethod
    def readText(self):
        return ZipFacade.readText(self.native)

    @staticmethod
    def close(self):
        self.native.close()

def createZipOutputStream(outputStream):
    native = ZipFacade.createZipOutputStream(outputStream.native)
    return ZipOutputStream(native)

class ZipOutputStream:
    @staticmethod
    def __init__(self, native):
        self.native = native

    @staticmethod
    def createZipEntry(self, name):
        nativeNext = ZipFacade.createZipEntry(name)
        zipEntry = ZipEntry(nativeNext)
        self.native.putNextEntry(nativeNext)
        return zipEntry

    @staticmethod
    def write(self, data):
        native = bytes.toJavaBytes(data)
        ZipFacade.writeNative(self.native, native)

    @staticmethod
    def writeNative(self, data):
        ZipFacade.writeNative(self.native, data)

    @staticmethod
    def writeText(self, text):
        ZipFacade.writeText(self.native, text)

    @staticmethod
    def closeEntry(self):
        self.native.closeEntry()

    @staticmethod
    def close(self):
        self.native.finish()
        self.native.flush()
        self.native.close()

class ZipEntry:
    @staticmethod
    def __init__(self, native):
        self.native = native

    @staticmethod
    def getName(self):
        return self.native.getName()

    @staticmethod
    def getSize(self):
        return self.native.getSize()

    @staticmethod
    def getCompressedSize(self):
        return self.native.getCompressedSize()

    @staticmethod
    def getTime(self):
        return self.native.getTime()

    @staticmethod
    def getCrc(self):
        return self.native.getCrc()

    @staticmethod
    def getComment(self):
        return self.native.getComment()

    @staticmethod
    def isDirectory(self):
        return self.native.isDirectory()

    @staticmethod
    def isValid(self):
        return self.native is not None
