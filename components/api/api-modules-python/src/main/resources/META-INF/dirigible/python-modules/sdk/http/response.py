# Copyright (c) 2024 Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2024 Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

import json
import java

class Response:

    HttpResponseFacade = java.type('org.eclipse.dirigible.components.api.http.HttpResponseFacade')
    OutputStreamWriter = java.type('java.io.OutputStreamWriter')
    StandardCharsets = java.type('java.nio.charset.StandardCharsets')

    @staticmethod
    def isValid():
        return Response.HttpResponseFacade.isValid()

    @staticmethod
    def json(obj):
        Response.addHeader("Content-Type", "application/json")
        objJson = json.dumps(obj)
        print(objJson)

    @staticmethod
    def print(text):
        text = text and str(text) or ""
        out = Response.HttpResponseFacade.getOutputStream()
        writer = Response.OutputStreamWriter(out, Response.StandardCharsets.UTF_8)
        writer.write(text)
        writer.flush()

    @staticmethod
    def println(text):
        text = text and str(text) or ""
        out = Response.HttpResponseFacade.getOutputStream()
        writer = Response.OutputStreamWriter(out, Response.StandardCharsets.UTF_8)
        writer.write(text + "\n")
        writer.flush()

    @staticmethod
    def write(bytes):
        if not bytes:
            bytes = ""
        Response.HttpResponseFacade.write(bytes)

    @staticmethod
    def isCommitted():
        return Response.HttpResponseFacade.isCommitted()

    @staticmethod
    def setContentType(contentType):
        Response.HttpResponseFacade.setContentType(contentType)

    @staticmethod
    def flush():
        Response.HttpResponseFacade.flush()

    @staticmethod
    def close():
        Response.HttpResponseFacade.close()

    @staticmethod
    def addCookie(cookie):
        cookieJson = json.dumps(cookie)
        Response.HttpResponseFacade.addCookie(cookieJson)

    @staticmethod
    def containsHeader(name):
        return Response.HttpResponseFacade.containsHeader(name)

    @staticmethod
    def encodeURL(url):
        return Response.HttpResponseFacade.encodeURL(url)

    @staticmethod
    def getCharacterEncoding():
        return Response.HttpResponseFacade.getCharacterEncoding()

    @staticmethod
    def encodeRedirectURL(url):
        return Response.HttpResponseFacade.encodeRedirectURL(url)

    @staticmethod
    def getContentType():
        return Response.HttpResponseFacade.getContentType()

    @staticmethod
    def sendError(status, message=None):
        if message:
            Response.HttpResponseFacade.sendError(status, message)
        else:
            Response.HttpResponseFacade.sendError(status)

    @staticmethod
    def setCharacterEncoding(charset):
        Response.HttpResponseFacade.setCharacterEncoding(charset)

    @staticmethod
    def sendRedirect(location):
        Response.HttpResponseFacade.sendRedirect(location)

    @staticmethod
    def setContentLength(length):
        Response.HttpResponseFacade.setContentLength(length)

    @staticmethod
    def setHeader(name, value):
        Response.HttpResponseFacade.setHeader(name, value)

    @staticmethod
    def addHeader(name, value):
        Response.HttpResponseFacade.addHeader(name, value)

    @staticmethod
    def setStatus(status):
        Response.HttpResponseFacade.setStatus(status)

    @staticmethod
    def reset():
        Response.HttpResponseFacade.reset()

    @staticmethod
    def getHeader(name):
        return Response.HttpResponseFacade.getHeader(name)

    @staticmethod
    def setLocale(language, country=None, variant=None):
        return Response.HttpResponseFacade.setLocale(language, country, variant)

    @staticmethod
    def getHeaders(name):
        headersJson = Response.HttpResponseFacade.getHeaders(name)
        return json.loads(headersJson)

    @staticmethod
    def getHeaderNames():
        headerNamesJson = Response.HttpResponseFacade.getHeaderNames()
        return json.loads(headerNamesJson)

    @staticmethod
    def getLocale():
        return Response.HttpResponseFacade.getLocale()

    @staticmethod
    def getOutputStream():
        raise Exception("getOutputStream not implemented yet.")
        # TODO: implement "dirigible.streams" module
        # native = Response.HttpResponseFacade.getOutputStream()
        # return io.OutputStream(native)

    @staticmethod
    def getReason(code):
        if not isinstance(code, int) or code < 100 or code > 505:
            raise ValueError("Invalid HTTP code. Valid HTTP codes are integers in the range [100-505].")
        return HttpCodesReasons[str(code)]

    ACCEPTED = 202
    BAD_GATEWAY = 502
    BAD_REQUEST = 400
    CONFLICT = 409
    CONTINUE = 100
    CREATED = 201
    EXPECTATION_FAILED = 417
    FORBIDDEN = 403
    FOUND = 302
    GATEWAY_TIMEOUT = 504
    GONE = 410
    HTTP_VERSION_NOT_SUPPORTED = 505
    INTERNAL_SERVER_ERROR = 500
    LENGTH_REQUIRED = 411
    METHOD_NOT_ALLOWED = 405
    MOVED_PERMANENTLY = 301
    MOVED_TEMPORARILY = 302
    MULTIPLE_CHOICES = 300
    NO_CONTENT = 204
    NON_AUTHORITATIVE_INFORMATION = 203
    NOT_ACCEPTABLE = 406
    NOT_FOUND = 404
    NOT_IMPLEMENTED = 501
    NOT_MODIFIED = 304
    OK = 200
    PARTIAL_CONTENT = 206
    PAYMENT_REQUIRED = 402
    PRECONDITION_FAILED = 412
    PROXY_AUTHENTICATION_REQUIRED = 407
    REQUEST_ENTITY_TOO_LARGE = 413
    REQUEST_TIMEOUT = 408
    REQUEST_URI_TOO_LONG = 414
    REQUESTED_RANGE_NOT_SATISFIABLE = 416
    RESET_CONTENT = 205
    SEE_OTHER = 303
    SERVICE_UNAVAILABLE = 503
    SWITCHING_PROTOCOLS = 101
    TEMPORARY_REDIRECT = 307
    UNAUTHORIZED = 401
    UNSUPPORTED_MEDIA_TYPE = 415
    USE_PROXY = 305

    HttpCodesReasons = {
        "100": "Continue",
        "101": "Switching Protocols",
        "200": "OK",
        "201": "Created",
        "202": "Accepted",
        "203": "Non-Authoritative Information",
        "204": "No Content",
        "205": "Reset Content",
        "206": "Partial Content",
        "300": "Multiple Choices",
        "301": "Moved Permanently",
        "302": "Found",
        "303": "See Other",
        "304": "Not Modified",
        "305": "Use Proxy",
        "307": "Temporary Redirect",
        "400": "Bad Request",
        "401": "Unauthorized",
        "402": "Payment Required",
        "403": "Forbidden",
        "404": "Not Found",
        "405": "Method Not Allowed",
        "406": "Not Acceptable",
        "407": "Proxy Authentication Required",
        "408": "Request Timeout",
        "409": "Conflict",
        "410": "Gone",
        "411": "Length Required",
        "412": "Precondition Failed",
        "413": "Payload Too Large",
        "414": "URI Too Large",
        "415": "Unsupported Media Type",
        "416": "Range Not Satisfiable",
        "417": "Expectation Failed",
        "426": "Upgrade Required",
        "500": "Internal Server Error",
        "501": "Not Implemented",
        "502": "Bad Gateway",
        "503": "Service Unavailable",
        "504": "Gateway Timeout",
        "505": "HTTP Version Not Supported",
    }

    @staticmethod
    def getReason(code):
        if not isinstance(code, int) or code < 100 or code > 505:
            raise ValueError("Invalid HTTP code. Valid HTTP codes are integers in the range [100-505].")
        return Response.HttpCodesReasons()[str(code)]
