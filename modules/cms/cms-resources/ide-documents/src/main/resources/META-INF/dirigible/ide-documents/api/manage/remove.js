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
var folderLib = require("ide-documents/api/lib/folder");
var requestHandler = require("ide-documents/api/lib/request-handler");

requestHandler.handleRequest({
	handlers : {
		DELETE: handleDelete
	},
});

function handleDelete(){
	var forceDelete = request.getParameter('force');
	var objects = getJsonRequestBody();
	for (var i in objects){
		var object = cmisObjectLib.getObject(objects[i]);
		var isFolder = object.getType().getId() === 'cmis:folder';
		if(isFolder && forceDelete === 'true'){
			folderLib.deleteTree(object);
		} else {
			cmisObjectLib.deleteObject(object);
		}
	}
	response.setStatus(response.NO_CONTENT);
}

function getJsonRequestBody(){
	var input = request.getText();
    var requestBody = JSON.parse(input);
    return requestBody;
}
