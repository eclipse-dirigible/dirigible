/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { cmis } from "@aerokit/sdk/cms";
import { user } from "@aerokit/sdk/security";
import * as objectUtils from "./object";

let cmisSession = cmis.getSession();

function ChildSerializer(cmisObject) {
	this.name = cmisObject.getName();
	this.type = cmisObject.getType().getId();
	this.id = cmisObject.getId();
	this.path = cmisObject.getPath();

	let readAccessDefinitions = getReadAccessDefinitions(this.path);
	let writeAccessDefinitions = getWriteAccessDefinitions(this.path);

	let readOnly = false;
	let readable = true;
	let pathReadAccessDefinitionFound = false;
	let pathWriteAccessDefinitionFound = false;
	if (readAccessDefinitions.length > 0) {
		for (let i = 0; i < readAccessDefinitions.length; i++) {
			if (readAccessDefinitions[i].path === this.path) {
				readable = hasAccess([readAccessDefinitions[i]]);
				readOnly = true;
				pathReadAccessDefinitionFound = true;
				break;
			}
		}
	}
	if (writeAccessDefinitions.length > 0) {
		for (let i = 0; i < writeAccessDefinitions.length; i++) {
			if (writeAccessDefinitions[i].path === this.path) {
				readOnly = !hasAccess([writeAccessDefinitions[i]]);
				pathWriteAccessDefinitionFound = true;
				break;
			}
		}
	}

	if (!pathReadAccessDefinitionFound && !pathWriteAccessDefinitionFound) {
		let readOnlyAccessDefinitions = readAccessDefinitions.filter(e => e.method === cmis.METHOD_READ);
		let writeOnlyAccessDefinitions = writeAccessDefinitions.filter(e => e.method === cmis.METHOD_WRITE);
		if (readOnlyAccessDefinitions.length > 0) {
			readable = hasAccess(readOnlyAccessDefinitions);
		}
		if (writeOnlyAccessDefinitions.length > 0) {
			readOnly = !hasAccess(writeOnlyAccessDefinitions);
		}
	}

	this.readOnly = readOnly;
	this.readable = readable;
}

function FolderSerializer(cmisFolder) {

	this.name = cmisFolder.getName();
	this.id = cmisFolder.getId();
	this.path = cmisFolder.getPath();
	this.parentId = null;
	this.children = [];


	if (!cmisFolder.isRootFolder()) {
		let parent = cmisFolder.getFolderParent();
		if (parent !== null) {
			this.parentId = parent.getId();
		}
	}

	let children = cmisFolder.getChildren();
	for (let i in children) {
		let child = new ChildSerializer(children[i]);
		this.children.push(child);
	}
	this.children = this.children.sort((x, y) => x.path > y.path ? 1 : -1);
}

export const readFolder = (folder) => {
	return new FolderSerializer(folder);
};

export const createFolder = (parentFolder, name) => {
	let properties = {};
	properties[cmis.OBJECT_TYPE_ID] = cmis.OBJECT_TYPE_FOLDER;
	properties[cmis.NAME] = name;
	let newFolder = parentFolder.createFolder(properties);

	return new FolderSerializer(newFolder);
};

export const getFolderOrRoot = (folderPath) => {
	if (folderPath === null) {
		return cmisSession.getRootFolder();
	}
	try {
		return getFolder(folderPath);
	} catch (e) {
		console.error("Failed to get folder: " + e);
		return cmisSession.getRootFolder();
	}
};

export const getFolder = (path) => {
	return objectUtils.getObject(path);
};

export const existFolder = (path) => {
	return objectUtils.existObject(path);
};

export const deleteTree = (folder) => {
	folder.deleteTree();
};

function getReadAccessDefinitions(path) {
	return cmis.getAccessDefinitions(path, cmis.METHOD_READ);
}

function getWriteAccessDefinitions(path) {
	return cmis.getAccessDefinitions(path, cmis.METHOD_WRITE);
}

function hasAccess(accessDefinitions) {
	for (let i = 0; i < accessDefinitions.length; i++) {
		if (!user.isInRole(accessDefinitions[i].role)) {
			return false;
		}
	}
	return true;
}
