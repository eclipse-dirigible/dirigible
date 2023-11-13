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

FilesFacade = java.type("org.eclipse.dirigible.components.api.io.FilesFacade")

def exists(path):
    return FilesFacade.exists(path)

def isExecutable(path):
    return FilesFacade.isExecutable(path)

def isReadable(path):
    return FilesFacade.isReadable(path)

def isWritable(path):
    return FilesFacade.isWritable(path)

def isHidden(path):
    return FilesFacade.isHidden(path)

def isDirectory(path):
    return FilesFacade.isDirectory(path)

def isFile(path):
    return FilesFacade.isFile(path)

def isSameFile(path1, path2):
    return FilesFacade.isSameFile(path1, path2)

def getCanonicalPath(path):
    return FilesFacade.getCanonicalPath(path)

def getName(path):
    return FilesFacade.getName(path)

def getParentPath(path):
    return FilesFacade.getParentPath(path)

def readBytes(path):
    native = FilesFacade.readBytes(path)
    data = bytes.toPythonBytes(native)
    return data

def readBytesNative(path):
    return FilesFacade.readBytes(path)

def readText(path):
    return FilesFacade.readText(path)

def writeBytes(path, data):
    native = bytes.toJavaBytes(data)
    FilesFacade.writeBytesNative(path, native)

def writeBytesNative(path, data):
    FilesFacade.writeBytesNative(path, data)

def writeText(path, text):
    FilesFacade.writeText(path, text)

def getLastModified(path):
    return FilesFacade.getLastModified(path)

def setLastModified(path, time):
    FilesFacade.setLastModified(path, time.getMilliseconds())

def getOwner(path):
    return FilesFacade.getOwner(path)

def setOwner(path, owner):
    FilesFacade.setOwner(path, owner)

def getPermissions(path):
    return FilesFacade.getPermissions(path)

def setPermissions(path, permissions):
    FilesFacade.setPermissions(path, permissions)

def size(path):
    return FilesFacade.size(path)

def createFile(path):
    FilesFacade.createFile(path)

def createDirectory(path):
    FilesFacade.createDirectory(path)

def copy(source, target):
    FilesFacade.copy(source, target)

def move(source, target):
    FilesFacade.move(source, target)

def deleteFile(path):
    FilesFacade.deleteFile(path)

def deleteDirectory(path, forced):
    FilesFacade.deleteDirectory(path, forced)

def createTempFile(prefix, suffix):
    return FilesFacade.createTempFile(prefix, suffix)

def createTempDirectory(prefix):
    return FilesFacade.createTempDirectory(prefix)

def createInputStream(path):
    native = FilesFacade.createInputStream(path)
    return streams.InputStream(native)

def createOutputStream(path):
    native = FilesFacade.createOutputStream(path)
    return streams.OutputStream(native)

def traverse(path):
    return FilesFacade.traverse(path)

def list(path):
    return list(map(lambda e: e['path'], JSON.parse(FilesFacade.list(path)))

def find(path, pattern):
    return JSON.parse(FilesFacade.find(path, pattern))
