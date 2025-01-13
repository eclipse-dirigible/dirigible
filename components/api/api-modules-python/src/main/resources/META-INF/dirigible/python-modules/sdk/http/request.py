# Copyright (c) 2024 Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2024 Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

import json
import java

class Request:
    HttpRequestFacade = java.type('org.eclipse.dirigible.components.api.http.HttpRequestFacade')
    textData = None

    @staticmethod
    def isValid():
        return Request.HttpRequestFacade.isValid()

    @staticmethod
    def getMethod():
        return Request.HttpRequestFacade.getMethod()

    @staticmethod
    def getRemoteUser():
        return Request.HttpRequestFacade.getRemoteUser()

    @staticmethod
    def getPathInfo():
        return Request.HttpRequestFacade.getPathInfo()

    @staticmethod
    def getPathTranslated():
        return Request.HttpRequestFacade.getPathTranslated()

    @staticmethod
    def getHeader(name):
        return Request.HttpRequestFacade.getHeader(name)

    @staticmethod
    def isUserInRole(role):
        return Request.HttpRequestFacade.isUserInRole(role)

    @staticmethod
    def getAttribute(name):
        return Request.HttpRequestFacade.getAttribute(name)

    @staticmethod
    def getAuthType():
        return Request.HttpRequestFacade.getAuthType()

    @staticmethod
    def getCookies():
        cookiesJson = Request.HttpRequestFacade.getCookies()
        return json.loads(cookiesJson)

    @staticmethod
    def getAttributeNames():
        attrNamesJson = Request.HttpRequestFacade.getAttributeNames()
        return json.loads(attrNamesJson)

    @staticmethod
    def getCharacterEncoding():
        return Request.HttpRequestFacade.getCharacterEncoding()

    @staticmethod
    def getContentLength():
        return Request.HttpRequestFacade.getContentLength()

    @staticmethod
    def getHeaders(name):
        headersJson = Request.HttpRequestFacade.getHeaders(name)
        return json.loads(headersJson)

    @staticmethod
    def getContentType():
        return Request.HttpRequestFacade.getContentType()

    @staticmethod
    def getBytes():
        bytesJson = Request.HttpRequestFacade.getBytes()
        return json.loads(bytesJson)

    @staticmethod
    def getText():
        if Request.textData is None:
            Request.textData = Request.HttpRequestFacade.getText()
        return Request.textData

    @staticmethod
    def getJSON():
        try:
            text = Request.getText()
            return json.loads(text)
        except Exception as e:
            return None

    @staticmethod
    def getParameter(name):
        return Request.HttpRequestFacade.getParameter(name)

    @staticmethod
    def getParameters():
        paramsJson = Request.HttpRequestFacade.getParameters()
        return json.loads(paramsJson)

    @staticmethod
    def getResourcePath():
        return Request.HttpRequestFacade.getResourcePath()

    @staticmethod
    def getHeaderNames():
        headerNamesJson = Request.HttpRequestFacade.getHeaderNames()
        return json.loads(headerNamesJson)

    @staticmethod
    def getParameterNames():
        paramNamesJson = Request.HttpRequestFacade.getParameterNames()
        return json.loads(paramNamesJson)

    @staticmethod
    def getParameterValues(name):
        paramValuesJson = Request.HttpRequestFacade.getParameterValues(name)
        return json.loads(paramValuesJson)

    @staticmethod
    def getProtocol():
        return Request.HttpRequestFacade.getProtocol()

    @staticmethod
    def getScheme():
        return Request.HttpRequestFacade.getScheme()

    @staticmethod
    def getContextPath():
        return Request.HttpRequestFacade.getContextPath()

    @staticmethod
    def getServerName():
        return Request.HttpRequestFacade.getServerName()

    @staticmethod
    def getServerPort():
        return Request.HttpRequestFacade.getServerPort()

    @staticmethod
    def getQueryString():
        return Request.HttpRequestFacade.getQueryString()

    @staticmethod
    def getQueryParametersMap():
        queryString = Request.getQueryString()
        if not queryString:
            return {}

        queryString = decodeURI(queryString)
        queryStringSegments = queryString.split('&')

        queryMap = {}
        for seg in queryStringSegments:
            seg = seg.replace('amp;', '')
            kv = seg.split('=')
            key = kv[0].strip()
            value = kv[1] if len(kv) > 1 else True
            value = value.strip()
            if key in queryMap:
                if not isinstance(queryMap[key], list):
                    queryMap[key] = [queryMap[key]]
                queryMap[key].append(value)
            else:
                queryMap[key] = value

        return queryMap

    @staticmethod
    def getRemoteAddress():
        return Request.HttpRequestFacade.getRemoteAddress()

    @staticmethod
    def getRemoteHost():
        return Request.HttpRequestFacade.getRemoteHost()

    @staticmethod
    def setAttribute(name, value):
        Request.HttpRequestFacade.setAttribute(name, value)

    @staticmethod
    def removeAttribute(name):
        Request.HttpRequestFacade.removeAttribute(name)

    @staticmethod
    def getLocale():
        localeJson = Request.HttpRequestFacade.getLocale()
        return json.loads(localeJson)

    @staticmethod
    def getRequestURI():
        return Request.HttpRequestFacade.getRequestURI()

    @staticmethod
    def isSecure():
        return Request.HttpRequestFacade.isSecure()

    @staticmethod
    def getRequestURL():
        return Request.HttpRequestFacade.getRequestURL()

    @staticmethod
    def getServicePath():
        return Request.HttpRequestFacade.getServicePath()

    @staticmethod
    def getRemotePort():
        return Request.HttpRequestFacade.getRemotePort()

    @staticmethod
    def getLocalName():
        return Request.HttpRequestFacade.getLocalName()

    @staticmethod
    def getLocalAddress():
        return Request.HttpRequestFacade.getLocalAddress()

    @staticmethod
    def getLocalPort():
        return Request.HttpRequestFacade.getLocalPort()

    @staticmethod
    def getInputStream():
        raise Exception("getInputStream not implemented yet.")
        # TODO: implement "dirigible.streams" module
        # return streams.createInputStream(Request.HttpRequestFacade.getInputStream())
