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
/**
 * API CMIS
 * 
 * Note: This module is supported only with the Mozilla Rhino engine
 */

import * as streams from "sdk/io/streams";
const CmisFacade = Java.type("org.eclipse.dirigible.components.api.cms.CmisFacade");
const Gson = Java.type("com.google.gson.Gson");
const HashMap = Java.type("java.util.HashMap");

export class Cmis {

	// CONSTANTS
	public static readonly METHOD_READ = "READ";
	public static readonly METHOD_WRITE = "WRITE";

	// ---- Base ----
	public static readonly NAME = "cmis:name";
	public static readonly OBJECT_ID = "cmis:objectId";
	public static readonly OBJECT_TYPE_ID = "cmis:objectTypeId";
	public static readonly BASE_TYPE_ID = "cmis:baseTypeId";
	public static readonly CREATED_BY = "cmis:createdBy";
	public static readonly CREATION_DATE = "cmis:creationDate";
	public static readonly LAST_MODIFIED_BY = "cmis:lastModifiedBy";
	public static readonly LAST_MODIFICATION_DATE = "cmis:lastModificationDate";
	public static readonly CHANGE_TOKEN = "cmis:changeToken";

	// ---- Document ----
	public static readonly IS_IMMUTABLE = "cmis:isImmutable";
	public static readonly IS_LATEST_VERSION = "cmis:isLatestVersion";
	public static readonly IS_MAJOR_VERSION = "cmis:isMajorVersion";
	public static readonly IS_LATEST_MAJOR_VERSION = "cmis:isLatestMajorVersion";
	public static readonly VERSION_LABEL = "cmis:versionLabel";
	public static readonly VERSION_SERIES_ID = "cmis:versionSeriesId";
	public static readonly IS_VERSION_SERIES_CHECKED_OUT = "cmis:isVersionSeriesCheckedOut";
	public static readonly VERSION_SERIES_CHECKED_OUT_BY = "cmis:versionSeriesCheckedOutBy";
	public static readonly VERSION_SERIES_CHECKED_OUT_ID = "cmis:versionSeriesCheckedOutId";
	public static readonly CHECKIN_COMMENT = "cmis:checkinComment";
	public static readonly CONTENT_STREAM_LENGTH = "cmis:contentStreamLength";
	public static readonly CONTENT_STREAM_MIME_TYPE = "cmis:contentStreamMimeType";
	public static readonly CONTENT_STREAM_FILE_NAME = "cmis:contentStreamFileName";
	public static readonly CONTENT_STREAM_ID = "cmis:contentStreamId";

	// ---- Folder ----
	public static readonly PARENT_ID = "cmis:parentId";
	public static readonly ALLOWED_CHILD_OBJECT_TYPE_IDS = "cmis:allowedChildObjectTypeIds";
	public static readonly PATH = "cmis:path";

	// ---- Relationship ----
	public static readonly SOURCE_ID = "cmis:sourceId";
	public static readonly TARGET_ID = "cmis:targetId";

	// ---- Policy ----
	public static readonly POLICY_TEXT = "cmis:policyText";

	// ---- Versioning States ----
	public static readonly VERSIONING_STATE_NONE = "none";
	public static readonly VERSIONING_STATE_MAJOR = "major";
	public static readonly VERSIONING_STATE_MINOR = "minor";
	public static readonly VERSIONING_STATE_CHECKEDOUT = "checkedout";

	// ---- Object Types ----
	public static readonly OBJECT_TYPE_DOCUMENT = "cmis:document";
	public static readonly OBJECT_TYPE_FOLDER = "cmis:folder";
	public static readonly OBJECT_TYPE_RELATIONSHIP = "cmis:relationship";
	public static readonly OBJECT_TYPE_POLICY = "cmis:policy";
	public static readonly OBJECT_TYPE_ITEM = "cmis:item";
	public static readonly OBJECT_TYPE_SECONDARY = "cmis:secondary";

	public static getSession(): Session {
		const native = CmisFacade.getSession();
		return new Session(native);
	}

	public static getAccessDefinitions(path: string, method: string): AccessDefinition[] {
		const accessDefinitions = CmisFacade.getAccessDefinitions(path, method);
		return JSON.parse(new Gson().toJson(accessDefinitions));
	}
}

interface AccessDefinition {
	getId(): string;
	getScope(): string;
	getPath(): string;
	getMethod(): string;
	getRole(): string;
}

/**
 * Session object
 */
class Session {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getRepositoryInfo(): RepositoryInfo {
		const native = this.native.getRepositoryInfo();
		return new RepositoryInfo(native);
	}

	public getRootFolder(): Folder {
		const native = this.native.getRootFolder();
		return new Folder(native, null);
	}

	public getObjectFactory(): ObjectFactory {
		const native = this.native.getObjectFactory();
		return new ObjectFactory(native);
	}

	public getObject(objectId: string): Folder | Document {
		const objectInstance = this.native.getObject(objectId);
		const objectInstanceType = objectInstance.getType();
		const objectInstanceTypeId = objectInstanceType.getId();
		if (objectInstanceTypeId === Cmis.OBJECT_TYPE_DOCUMENT) {
			return new Document(objectInstance, null);
		} else if (objectInstanceTypeId === Cmis.OBJECT_TYPE_FOLDER) {
			return new Folder(objectInstance, null);
		}
		throw new Error("Unsupported CMIS object type: " + objectInstanceTypeId);
	}

	public getObjectByPath(path: string): Folder | Document {
		const allowed = CmisFacade.isAllowed(path, Cmis.METHOD_READ);
		if (!allowed) {
			throw new Error("Read access not allowed on: " + path);
		}
		const objectInstance = this.native.getObjectByPath(path);
		const objectInstanceType = objectInstance.getType();
		const objectInstanceTypeId = objectInstanceType.getId();
		if (objectInstanceTypeId === Cmis.OBJECT_TYPE_DOCUMENT) {
			return new Document(objectInstance, path);
		} else if (objectInstanceTypeId === Cmis.OBJECT_TYPE_FOLDER) {
			return new Folder(objectInstance, path);
		}
		throw new Error("Unsupported CMIS object type: " + objectInstanceTypeId);
	}

	public createFolder(location: string): Folder {
		if (location.startsWith("/")) {
			location = location.substring(1, location.length);
		}
		if (location.endsWith("/")) {
			location = location.substring(0, location.length - 1);
		}
		const segments = location.split("/");
		let folder = this.getRootFolder();
		for (const next of segments) {
			const properties = {
				[Cmis.OBJECT_TYPE_ID]: Cmis.OBJECT_TYPE_FOLDER,
				[Cmis.NAME]: next
			};
			folder = folder.createFolder(properties);
		}
		return folder;
	}

	public createDocument(location: string, properties: { [key: string]: any }, contentStream: ContentStream, versioningState: string): Document {
		const folder = this.createFolder(location);
		return folder.createDocument(properties, contentStream, versioningState);
	}

}

/**
 * RepositoryInfo object
 */
class RepositoryInfo {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getId(): string {
		return this.native.getId();
	}

	public getName(): string {
		return this.native.getName();
	}
}

/**
 * Folder object
 */
export class Folder {
	private readonly native: any;
	private path: any;

	constructor(native: any, path: any) {
		this.native = native;
		this.path = path;
	}

	public getId(): string {
		return this.native.getId();
	}

	public getName(): string {
		return this.native.getName();
	}

	public createFolder(properties: { [key: string]: any }): Folder {
		var allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		var mapInstance = new HashMap();
		for (var property in properties) {
			if (properties.hasOwnProperty(property)) {
				mapInstance.put(property, properties[property]);
			}
		}
		var native = this.native.createFolder(mapInstance);
		var folder = new Folder(native, null);
		return folder;
	}

	public createDocument(properties: { [key: string]: any }, contentStream: ContentStream, versioningState: string): Document {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		const mapInstance = new HashMap();
		for (const property in properties) {
			if (properties.hasOwnProperty(property)) {
				mapInstance.put(property, properties[property]);
			}
		}
		const state = CmisFacade.getVersioningState(versioningState);

		// @ts-ignore
		const native = this.native.createDocument(mapInstance, contentStream.native, state);
		return new Document(native, null);
	};

	public getChildren(): CmisObject[] {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_READ);
		if (!allowed) {
			throw new Error("Read access not allowed on: " + this.getPath());
		}
		const children = [];
		const childrenInstance = this.native.getChildren();
		const childrenInstanceIterator = childrenInstance.iterator();
		while (childrenInstanceIterator.hasNext()) {
			const cmisObjectInstance = childrenInstanceIterator.next();
			const cmisObject = new CmisObject(cmisObjectInstance);
			children.push(cmisObject);
		}
		return children;
	}

	public getPath(): string {
		return this.path;
	}

	public isRootFolder(): boolean {
		return this.native.isRootFolder();
	}

	public getFolderParent(): Folder {
		const native = this.native.getFolderParent();
		return new Folder(native, null);
	};

	public delete(): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		this.native.delete();
	};

	public rename(newName: string): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		this.native.rename(newName);
	}

	public deleteTree(): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		const unifiedObjectDelete = CmisFacade.getUnifiedObjectDelete();
		this.native.deleteTree(true, unifiedObjectDelete, true);
	}

	public getType(): TypeDefinition {
		const native = this.native.getType();
		return new TypeDefinition(native);
	}
}

/**
 * CmisObject object
 */
class CmisObject {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getId(): string {
		return this.native.getId();
	}

	public getName(): string {
		return this.native.getName();
	}

	public getPath(): string {
		//this is caused by having different underlying native objects in different environments.
		if (this.native.getPath) {
			return this.native.getPath();
		}

		//Apache Chemistry CmisObject has no getPath() but getPaths() - https://chemistry.apache.org/docs/cmis-samples/samples/retrieve-objects/index.html
		if (this.native.getPaths) {
			return this.native.getPaths()[0];
		}

		throw new Error(`Path not found for CmisObject with id ${this.getId()}`);
	}

	public getType(): TypeDefinition {
		const native = this.native.getType();
		return new TypeDefinition(native);
	}

	public delete(): void {
		this.native.delete();
	}

	public rename(newName: string): void {
		this.native.rename(newName);
	}

}

/**
 * ObjectFactory object
 */
class ObjectFactory {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public createContentStream(filename: string, length: number, mimetype: string, inputStream: streams.InputStream): ContentStream {
		// @ts-ignore
		const native = this.native.createContentStream(filename, length, mimetype, inputStream.native);
		return new ContentStream(native);
	}
}

/**s
 * ContentStream object
 */
class ContentStream {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getStream(): streams.InputStream {
		const native = this.native.getStream();
		return new streams.InputStream(native);
	}

	public getMimeType(): string {
		return this.native.getMimeType();
	}
}

/**
 * Document object
 */
export class Document {

	private readonly native: any;
	private path: string;

	constructor(native: any, path: string) {
		this.native = native;
		this.path = path;
	}

	public getId(): string {
		return this.native.getId();
	}

	public getName(): string {
		return this.native.getName();
	}

	public getType(): TypeDefinition {
		const native = this.native.getType();
		return new TypeDefinition(native);
	}

	public getPath(): string {
		return this.path;
	}

	public delete(): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		return this.native.delete(true);
	}

	public getContentStream(): ContentStream | null {
		const native = this.native.getContentStream();
		if (native !== null) {
			return new ContentStream(native);
		}
		return null;
	};

	public getSize(): number {
		return this.native.getSize();
	}

	public rename(newName: string): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		this.native.rename(newName);
	}
}

class TypeDefinition {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getId(): string {
		return this.native.getId();
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Cmis;
}
