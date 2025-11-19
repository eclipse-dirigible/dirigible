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
import { streams } from "@aerokit/sdk/io";
import * as objectUtils from "./object";
const path = org.eclipse.dirigible.repository.api.RepositoryPath;

let cmisSession = cmis.getSession();

function DocumentSerializer(cmisDocument) {
	this.id = cmisDocument.getId();
	this.name = cmisDocument.getName();
}

export const uploadDocument = (folder, document) => {
	let fileName = getDocumentFileName(document);
	let mimetype = document.getContentType();
	let size = document.getSize();
	let inputStream = document.getInputStream();
	let newDocument = createDocument(folder, fileName, size, mimetype, inputStream);
	return new DocumentSerializer(newDocument);
};

export const uploadDocumentOverwrite = (folder, document) => {
	let timestamp = new Date().getTime();
	if (document.name === null || document.name === undefined) {
		document.name = document.getName();
	}
	let newName = document.name + "-" + timestamp;
	let oldName = document.name;

	document.name = newName;
	uploadDocument(folder, document);

	try {
		let docPath = path.normalizePath(folder.getPath(), oldName)
		let oldDoc = objectUtils.getObject(docPath);
		objectUtils.deleteObject(oldDoc);
	} catch (e) {
		//do nothing
	}
	let docPath = path.normalizePath(folder.getPath(), newName)
	let newDoc = objectUtils.getObject(docPath);
	objectUtils.renameObject(newDoc, oldName);
};

function getDocumentFileName(document) {
	//check for .name first as it's passed from uploadDocumentOverwrite
	return document.name ?? document.getName();
}

function createDocument(folder, fileName, size, mimetype, inputStream) {
	let contentStream = cmisSession.getObjectFactory().createContentStream(fileName, size, mimetype, inputStream);
	let properties = {};
	properties[cmis.OBJECT_TYPE_ID] = cmis.OBJECT_TYPE_DOCUMENT;
	properties[cmis.NAME] = fileName;
	let newDocument = folder.createDocument(properties, contentStream, cmis.VERSIONING_STATE_MAJOR);
	return newDocument;
}

export const getDocumentStream = (document) => {
	let contentStream = document.getContentStream();
	return contentStream;
};

export const getDocNameAndStream = (document) => {
	let stream = getDocumentStream(document);
	let name = document.getName();
	return [name, stream];
};

export const getDocument = (path) => {
	return objectUtils.getObject(path);
}

export const existDocument = (path) => {
	return objectUtils.existObject(path);
}

export const createFromBytes = (folder, fileName, bytes) => {
	let inputStream = streams.createByteArrayInputStream(bytes);
	let mimeType = "application/octet-stream";

	createDocument(folder, fileName, bytes.length, mimeType, inputStream);
};
