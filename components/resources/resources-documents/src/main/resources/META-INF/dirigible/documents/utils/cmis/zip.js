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
import { zip } from "@aerokit/sdk/io";
import * as folderUtils from "./folder";
import * as documentUtils from "./document";
import * as objectUtils from "./object";

const SEPARATOR = '/';

export const unpackZip = (folderPath, zip) => {
	let inputStream = zip.getInputStream();
	createEntries(inputStream, folderPath);

	return folderUtils.readFolder(folderUtils.getFolder(folderPath));
};

function createEntries(inputStream, rootPath) {
	let zipInputStream = zip.createZipInputStream(inputStream);
	try {
		let zipEntry = zipInputStream.getNextEntry();
		while (zipEntry.isValid()) {
			console.log(zipEntry.getName());
			if (zipEntry.isDirectory()) {
				createFolder(rootPath, zipEntry, zipInputStream);
			} else {
				createFile(rootPath, zipEntry, zipInputStream);
			}
			zipEntry = zipInputStream.getNextEntry();
		}
	} finally {
		zipInputStream.close();
	}
}

function createFolder(rootPath, zipEntry, zipInputStream) {
	let pathAndName = getFullPathAndName(rootPath, zipEntry.getName());
	let path = pathAndName[0];
	let name = pathAndName[1];
	console.log('Creating folder with path:' + path + ' and name: ' + name + ' ...');
	let parent = folderUtils.getFolder(path);
	if (parent === null) {
		createParentFolder(path);
	}
	parent = folderUtils.getFolder(path);
	folderUtils.createFolder(parent, name);
}

function createParentFolder(path) {
	let upperPath = path.substring(0, path.lastIndexOf(SEPARATOR));
	let upper = folderUtils.getFolder(upperPath);
	let upperName = path.substring(path.lastIndexOf(SEPARATOR) + 1);

	console.warn('Creating the parent folder first with path: ' + upperPath + ' and name: ' + upperName + ' ...');
	try {
		let parentFolder = folderUtils.createFolder(upper, upperName);
		if (parentFolder === null) {
			createParentFolder(upperPath);
			folderUtils.createFolder(upper, upperName);
		}
	} catch (e) {
		console.error(e.message);
		createParentFolder(upperPath);
		folderUtils.createFolder(upper, upperName);
	}

}

function createFile(rootPath, zipEntry, zipInputStream) {
	let pathAndName = getFullPathAndName(rootPath, zipEntry.getName());
	let path = pathAndName[0];
	let name = pathAndName[1];
	console.log('Creating file with root: ' + rootPath + ' path: ' + path + ' and name: ' + name + ' ...');

	let parent = null;
	try {
		parent = folderUtils.getFolder(path);
		if (parent === null) {
			createParentFolder(path);
		}
	} catch (e) {
		console.error(e.message);
		createParentFolder(path);
	}
	parent = folderUtils.getFolder(path);

	let bytes = zipInputStream.read();
	documentUtils.createFromBytes(parent, name, bytes);
}

function getFullPathAndName(rootPath, fileFullName) {
	if (fileFullName.endsWith(SEPARATOR)) {
		fileFullName = fileFullName.substring(0, fileFullName.length - 1);
	}
	let splittedFullName = fileFullName.split(SEPARATOR);
	let innerPath = SEPARATOR + splittedFullName.slice(0, -1).join(SEPARATOR);
	let fullPath = rootPath + innerPath;
	let name = splittedFullName[splittedFullName.length - 1];
	return [fullPath, name];
}

export const makeZip = (folderPath, outputStream) => {
	let folder = folderUtils.getFolder(folderPath);
	console.info("Creating zip for folder: " + folderPath);

	let zipOutputStream = zip.createZipOutputStream(outputStream);
	try {
		traverseFolder(folder, folder.getName(), zipOutputStream);
	} finally {
		zipOutputStream.close();
	}
};

function traverseFolder(folder, path, zipOutputStream) {
	if (path === 'root') {
		path = '';
	}
	folder.getChildren().forEach(function (child) {
		console.info("Folder: " + folder.getName() + " Path: " + path + " Child: " + child.getName());
		let entryPath = path.lenght === 0 ? '' : path + SEPARATOR;
		entryPath += child.getName();
		let childObject = objectUtils.getById(child.getId());
		let zipEntry;
		if (isFolder(child)) {
			zipEntry = zipOutputStream.createZipEntry(entryPath + SEPARATOR);
			console.log("Zip directory entry: " + zipEntry.getName());
			traverseFolder(childObject, path + SEPARATOR + child.getName(), zipOutputStream);
		} else {
			zipOutputStream.createZipEntry(entryPath);
			let fileStream = childObject.getContentStream().getStream();
			zipOutputStream.write(fileStream.readBytes());
		}
	});
}

function isFolder(cmisObject) {
	return cmisObject.getType().getId() === "cmis:folder";
}
