# Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

import java

Configuration = java.type('org.eclipse.dirigible.commons.config')

class Configurations:

    @staticmethod
    def get(key, defaultValue=None):
        if defaultValue is not None:
            return Configuration.get(key, defaultValue)
        return Configuration.get(key)

    @staticmethod
    def set(key, value):
        Configuration.set(key, value)

    @staticmethod
    def remove(key):
        Configuration.remove(key)

    @staticmethod
    def getKeys():
        keys = []
        keysAsArray = Configuration.getKeys()
        for i in range(len(keysAsArray)):
            keys.append(keysAsArray[i])
        return keys

    @staticmethod
    def load(path):
        Configuration.load(path)

    @staticmethod
    def update():
        Configuration.update()

    @staticmethod
    def getOS():
        return Configuration.getOS()

    @staticmethod
    def isOSWindows():
        return Configuration.isOSWindows()

    @staticmethod
    def isOSMac():
        return Configuration.isOSMac()

    @staticmethod
    def isOSUNIX():
        return Configuration.isOSUNIX()

    @staticmethod
    def isOSSolaris():
        return Configuration.isOSSolaris()
