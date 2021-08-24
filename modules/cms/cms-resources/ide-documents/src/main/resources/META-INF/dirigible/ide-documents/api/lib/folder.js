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
var cmis = require('cms/v4/cmis');
var cmisObjectLib = require("ide-documents/api/lib/object");

var cmisSession = cmis.getSession();

function ChildSerializer(cmisObject){
	this.name = cmisObject.getName();
	this.type = cmisObject.getType().getId();
	this.id = cmisObject.getId();
}

function FolderSerializer(cmisFolder){
	
	this.name = cmisFolder.getName();
	this.id = cmisFolder.getId();
	this.path = cmisFolder.getPath();
	this.parentId = null;
	this.children = [];

	if (!cmisFolder.isRootFolder())	{
		var parent = cmisFolder.getFolderParent();
		if (parent !== null){
			this.parentId = parent.getId();
		}
	}

	var children = cmisFolder.getChildren();
	for (var i in children){
		var child = new ChildSerializer(children[i]);
		this.children.push(child);
	}
}

exports.readFolder = function(folder){
	return new FolderSerializer(folder);
};

exports.createFolder = function(parentFolder, name){
	var properties = {};
	properties[cmis.OBJECT_TYPE_ID] = cmis.OBJECT_TYPE_FOLDER;
	properties[cmis.NAME] = name;
	var newFolder = parentFolder.createFolder(properties);
	
	return new FolderSerializer(newFolder);
};

exports.getFolderOrRoot = function(folderPath){
	if (folderPath === null) {
		var rootFolder = cmisSession.getRootFolder();
		return rootFolder;
	}
	var folder = null;
	try {
		folder = exports.getFolder(folderPath);
	} catch(e){
		folder = cmisSession.getRootFolder();
	}
	return folder;
};

exports.getFolder = function(path){
	return cmisObjectLib.getObject(path);
};

exports.deleteTree = function(folder){
	folder.deleteTree();
};
