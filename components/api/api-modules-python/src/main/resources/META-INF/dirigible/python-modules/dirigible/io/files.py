# Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

import streams
import bytes
import java

class Files:
    FilesFacade = java.type("org.eclipse.dirigible.components.api.io.FilesFacade")

    @staticmethod
    def exists(path):
        return FilesFacade.exists(path)

    @staticmethod
    def isExecutable(path):
        return FilesFacade.isExecutable(path)

    @staticmethod
    def isReadable(path):
        return FilesFacade.isReadable(path)

    @staticmethod
    def isWritable(path):
        return FilesFacade.isWritable(path)

    @staticmethod
    def isHidden(path):
        return FilesFacade.isHidden(path)

    @staticmethod
    def isDirectory(path):
        return FilesFacade.isDirectory(path)

    @staticmethod
    def isFile(path):
        return FilesFacade.isFile(path)

    @staticmethod
    def isSameFile(path1, path2):
        return FilesFacade.isSameFile(path1, path2)

    @staticmethod
    def getCanonicalPath(path):
        return FilesFacade.getCanonicalPath(path)

    @staticmethod
    def getName(path):
        return FilesFacade.getName(path)

    @staticmethod
    def getParentPath(path):
        return FilesFacade.getParentPath(path)

    @staticmethod
    def readBytes(path):
        native = FilesFacade.readBytes(path)
        data = bytes.toPythonBytes(native)
        return data

    @staticmethod
    def readBytesNative(path):
        return FilesFacade.readBytes(path)

    @staticmethod
    def readText(path):
        return FilesFacade.readText(path)

    @staticmethod
    def writeBytes(path, data):
        native = bytes.toJavaBytes(data)
        FilesFacade.writeBytesNative(path, native)

    @staticmethod
    def writeBytesNative(path, data):
        FilesFacade.writeBytesNative(path, data)

    @staticmethod
    def writeText(path, text):
        FilesFacade.writeText(path, text)

    @staticmethod
    def getLastModified(path):
        return FilesFacade.getLastModified(path)

    @staticmethod
    def setLastModified(path, time):
        FilesFacade.setLastModified(path, time.getMilliseconds())

    @staticmethod
    def getOwner(path):
        return FilesFacade.getOwner(path)

    @staticmethod
    def setOwner(path, owner):
        FilesFacade.setOwner(path, owner)

    @staticmethod
    def getPermissions(path):
        return FilesFacade.getPermissions(path)

    @staticmethod
    def setPermissions(path, permissions):
        FilesFacade.setPermissions(path, permissions)

    @staticmethod
    def size(path):
        return FilesFacade.size(path)

    @staticmethod
    def createFile(path):
        FilesFacade.createFile(path)

    @staticmethod
    def createDirectory(path):
        FilesFacade.createDirectory(path)

    @staticmethod
    def copy(source, target):
        FilesFacade.copy(source, target)

    @staticmethod
    def move(source, target):
        FilesFacade.move(source, target)

    @staticmethod
    def deleteFile(path):
        FilesFacade.deleteFile(path)

    @staticmethod
    def deleteDirectory(path, forced):
        FilesFacade.deleteDirectory(path, forced)

    @staticmethod
    def createTempFile(prefix, suffix):
        return FilesFacade.createTempFile(prefix, suffix)

    @staticmethod
    def createTempDirectory(prefix):
        return FilesFacade.createTempDirectory(prefix)

    @staticmethod
    def createInputStream(path):
        native = FilesFacade.createInputStream(path)
        return streams.InputStream(native)

    @staticmethod
    def createOutputStream(path):
        native = FilesFacade.createOutputStream(path)
        return streams.OutputStream(native)

    @staticmethod
    def traverse(path):
        return FilesFacade.traverse(path)

    @staticmethod
    def list(path):
        return list(map(lambda e: e['path'], JSON.parse(FilesFacade.list(path))))

    @staticmethod
    def find(path, pattern):
        return JSON.parse(FilesFacade.find(path, pattern))
