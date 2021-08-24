/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
var request = require("http/v4/request");
var response = require("http/v4/response");
var cmisObjectLib = require("ide-documents/api/lib/object");
var requestHandler = require("ide-documents/api/lib/request-handler");

requestHandler.handleRequest({
	handlers : {
		PUT: handlePut
	},
});

function handlePut(){
	var body = getJsonRequestBody();
	if (!(body.path && body.name)){
		printError(response.BAD_REQUEST, 4, "Request body must contain 'path' and 'name'");
		return;
	}
	var object = cmisObjectLib.getObject(body.path);
	var result = cmisObjectLib.renameObject(object, body.name);
	response.setStatus(response.OK);
	response.print(JSON.stringify(body.name));
}

function getJsonRequestBody(){
	var input = request.getText();
    var requestBody = JSON.parse(input);
    return requestBody;
}

function printError(httpCode, errCode, errMessage, errContext) {
    var body = {'err': {'code': errCode, 'message': errMessage}};
    response.setStatus(httpCode);
    response.print(JSON.stringify(body));
    console.error(JSON.stringify(body));
    if (errContext !== null) {
    	console.error(JSON.stringify(errContext));
    }
}
