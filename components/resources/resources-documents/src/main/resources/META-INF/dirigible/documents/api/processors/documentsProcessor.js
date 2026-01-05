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
import * as objectUtils from "../../utils/cmis/object";
import * as folderUtils from "../../utils/cmis/folder";
import * as documentUtils from "../../utils/cmis/document";
import * as contentTypeHandler from "../../utils/content-type-handler";
import { registry } from "@aerokit/sdk/platform";
import { formatPath } from "../../utils/string";
import { user } from "@aerokit/sdk/security";

export const get = (path) => {
	let document = documentUtils.getDocument(path);
	let nameAndStream = documentUtils.getDocNameAndStream(document);
	let contentStream = nameAndStream[1];
	let contentType = contentStream.getMimeType();

	let result = {
		name: nameAndStream[0],
		content: nameAndStream[1],
		contentType: contentTypeHandler.getContentTypeBeforeDownload(nameAndStream[0], contentType)
	};
	return result;
};

export const list = (path) => {
	let folder = folderUtils.getFolderOrRoot(path);
	let result = folderUtils.readFolder(folder);
	filterByAccessDefinitions(result);
	return result;
};

export const create = (path, documents, overwrite) => {
	let result = [];
	for (let i = 0; i < documents.size(); i++) {
		let folder = folderUtils.getFolder(path);
		if (overwrite) {
			result.push(documentUtils.uploadDocumentOverwrite(folder, documents.get(i)));
		} else {
			result.push(documentUtils.uploadDocument(folder, documents.get(i)));
		}
	}
	return result;
};

export const createFolder = (path, name) => {
	let folder = folderUtils.getFolderOrRoot(path);
	let result = folderUtils.createFolder(folder, name);
	return result;
};

export const rename = (path, name) => {
	let object = objectUtils.getObject(path);
	objectUtils.renameObject(object, name);
};

export const remove = (objects, forceDelete) => {
	for (let i in objects) {
		let object = objectUtils.getObject(objects[i]);
		let isFolder = object.getType().getId() === 'cmis:folder';
		if (isFolder && forceDelete) {
			folderUtils.deleteTree(object);
		} else {
			objectUtils.deleteObject(object);
		}
	}
};

function filterByAccessDefinitions(folder) {
	let accessDefinitions = JSON.parse(registry.getText("documents/security/roles.access"));
	folder.children = folder.children.filter(e => {
		let path = formatPath(folder.path + "/" + e.name);
		if (path.startsWith("/__internal")) {
			return false;
		}
		return hasAccessPermissions(accessDefinitions.constraints, path);
	});
}

function hasAccessPermissions(constraints, path) {
	for (let i = 0; i < constraints.length; i++) {
		let method = constraints[i].method;
		let constraintPath = constraints[i].path;
		constraintPath = formatPath(constraintPath);
		if (constraintPath.length === 0 || (path.length >= constraintPath.length && constraintPath.startsWith(path))) {
			if (method !== null && method !== undefined && (method.toUpperCase() === "READ" || method === "*")) {
				let roles = constraints[i].roles;
				if (roles && roles.length) {
					for (let j = 0; j < roles.length; j++) {
						if (!user.isInRole(roles[j])) {
							return false;
						}
					}
				}

			}
		}
	}
	return true;
}
