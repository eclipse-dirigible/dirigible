# Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

from org.eclipse.dirigible.commons.config import Configuration

def get(key, defaultValue=None):
    if defaultValue is not None:
        return Configuration.get(key, defaultValue)
    return Configuration.get(key)

def set(key, value):
    Configuration.set(key, value)

def remove(key):
    Configuration.remove(key)

def getKeys():
    keys = []
    keysAsArray = Configuration.getKeys()
    for i in range(len(keysAsArray)):
        keys.append(keysAsArray[i])
    return keys

def load(path):
    Configuration.load(path)

def update():
    Configuration.update()

def getOS():
    return Configuration.getOS()

def isOSWindows():
    return Configuration.isOSWindows()

def isOSMac():
    return Configuration.isOSMac()

def isOSUNIX():
    return Configuration.isOSUNIX()

def isOSSolaris():
    return Configuration.isOSSolaris()
