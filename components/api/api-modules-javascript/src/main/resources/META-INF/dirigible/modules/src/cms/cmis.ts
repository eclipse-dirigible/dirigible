/**
 * API CMIS
 * * Note: This module is supported only with the Mozilla Rhino engine
 * * Provides static access to the CMIS (Content Management Interoperability Services) repository session and utility constants.
 */

import * as streams from "sdk/io/streams";
const CmisFacade = Java.type("org.eclipse.dirigible.components.api.cms.CmisFacade");
const Gson = Java.type("com.google.gson.Gson");
const HashMap = Java.type("java.util.HashMap");

export class Cmis {

	// CONSTANTS
	/**
	 * CMIS method constant for read operations.
	 */
	public static readonly METHOD_READ = "READ";
	/**
	 * CMIS method constant for write operations.
	 */
	public static readonly METHOD_WRITE = "WRITE";

	// ---- Base CMIS Properties ----
	/** CMIS property: Object name. */
	public static readonly NAME = "cmis:name";
	/** CMIS property: Unique object identifier. */
	public static readonly OBJECT_ID = "cmis:objectId";
	/** CMIS property: Object type identifier. */
	public static readonly OBJECT_TYPE_ID = "cmis:objectTypeId";
	/** CMIS property: Base object type identifier. */
	public static readonly BASE_TYPE_ID = "cmis:baseTypeId";
	/** CMIS property: User who created the object. */
	public static readonly CREATED_BY = "cmis:createdBy";
	/** CMIS property: Timestamp of object creation. */
	public static readonly CREATION_DATE = "cmis:creationDate";
	/** CMIS property: User who last modified the object. */
	public static readonly LAST_MODIFIED_BY = "cmis:lastModifiedBy";
	/** CMIS property: Timestamp of last modification. */
	public static readonly LAST_MODIFICATION_DATE = "cmis:lastModificationDate";
	/** CMIS property: Change token for object change tracking. */
	public static readonly CHANGE_TOKEN = "cmis:changeToken";

	// ---- Document CMIS Properties ----
	/** CMIS property: Indicates if the document is immutable. */
	public static readonly IS_IMMUTABLE = "cmis:isImmutable";
	/** CMIS property: Indicates if the document is the latest version in the version series. */
	public static readonly IS_LATEST_VERSION = "cmis:isLatestVersion";
	/** CMIS property: Indicates if the document is a major version. */
	public static readonly IS_MAJOR_VERSION = "cmis:isMajorVersion";
	/** CMIS property: Indicates if the document is the latest major version. */
	public static readonly IS_LATEST_MAJOR_VERSION = "cmis:isLatestMajorVersion";
	/** CMIS property: Label of the document version. */
	public static readonly VERSION_LABEL = "cmis:versionLabel";
	/** CMIS property: ID of the version series. */
	public static readonly VERSION_SERIES_ID = "cmis:versionSeriesId";
	/** CMIS property: Indicates if the version series is checked out. */
	public static readonly IS_VERSION_SERIES_CHECKED_OUT = "cmis:isVersionSeriesCheckedOut";
	/** CMIS property: User who checked out the version series. */
	public static readonly VERSION_SERIES_CHECKED_OUT_BY = "cmis:versionSeriesCheckedOutBy";
	/** CMIS property: ID of the checked-out document object. */
	public static readonly VERSION_SERIES_CHECKED_OUT_ID = "cmis:versionSeriesCheckedOutId";
	/** CMIS property: Comment associated with the check-in operation. */
	public static readonly CHECKIN_COMMENT = "cmis:checkinComment";
	/** CMIS property: Length of the content stream in bytes. */
	public static readonly CONTENT_STREAM_LENGTH = "cmis:contentStreamLength";
	/** CMIS property: MIME type of the content stream. */
	public static readonly CONTENT_STREAM_MIME_TYPE = "cmis:contentStreamMimeType";
	/** CMIS property: Original file name of the content stream. */
	public static readonly CONTENT_STREAM_FILE_NAME = "cmis:contentStreamFileName";
	/** CMIS property: ID of the content stream. */
	public static readonly CONTENT_STREAM_ID = "cmis:contentStreamId";

	// ---- Folder CMIS Properties ----
	/** CMIS property: Object ID of the parent folder. */
	public static readonly PARENT_ID = "cmis:parentId";
	/** CMIS property: List of allowed object type IDs for children. */
	public static readonly ALLOWED_CHILD_OBJECT_TYPE_IDS = "cmis:allowedChildObjectTypeIds";
	/** CMIS property: Path of the folder in the repository. */
	public static readonly PATH = "cmis:path";

	// ---- Relationship CMIS Properties ----
	/** CMIS property: Object ID of the relationship source. */
	public static readonly SOURCE_ID = "cmis:sourceId";
	/** CMIS property: Object ID of the relationship target. */
	public static readonly TARGET_ID = "cmis:targetId";

	// ---- Policy CMIS Properties ----
	/** CMIS property: Text content of the policy. */
	public static readonly POLICY_TEXT = "cmis:policyText";

	// ---- Versioning States ----
	/** CMIS Versioning State: No versioning. */
	public static readonly VERSIONING_STATE_NONE = "none";
	/** CMIS Versioning State: Create a new major version. */
	public static readonly VERSIONING_STATE_MAJOR = "major";
	/** CMIS Versioning State: Create a new minor version. */
	public static readonly VERSIONING_STATE_MINOR = "minor";
	/** CMIS Versioning State: Document is checked out. */
	public static readonly VERSIONING_STATE_CHECKEDOUT = "checkedout";

	// ---- Object Types ----
	/** CMIS Object Type ID: Document. */
	public static readonly OBJECT_TYPE_DOCUMENT = "cmis:document";
	/** CMIS Object Type ID: Folder. */
	public static readonly OBJECT_TYPE_FOLDER = "cmis:folder";
	/** CMIS Object Type ID: Relationship. */
	public static readonly OBJECT_TYPE_RELATIONSHIP = "cmis:relationship";
	/** CMIS Object Type ID: Policy. */
	public static readonly OBJECT_TYPE_POLICY = "cmis:policy";
	/** CMIS Object Type ID: Item. */
	public static readonly OBJECT_TYPE_ITEM = "cmis:item";
	/** CMIS Object Type ID: Secondary. */
	public static readonly OBJECT_TYPE_SECONDARY = "cmis:secondary";

	/**
	 * Gets a new CMIS session instance to interact with the repository.
	 * @returns A new {@link Session} instance.
	 */
	public static getSession(): Session {
		const native = CmisFacade.getSession();
		return new Session(native);
	}

	/**
	 * Retrieves access control definitions for a specific path and method.
	 * @param path The path of the CMIS object.
	 * @param method The operation method (e.g., {@link Cmis.METHOD_READ}, {@link Cmis.METHOD_WRITE}).
	 * @returns A list of access definitions.
	 */
	public static getAccessDefinitions(path: string, method: string): AccessDefinition[] {
		const accessDefinitions = CmisFacade.getAccessDefinitions(path, method);
		return JSON.parse(new Gson().toJson(accessDefinitions));
	}
}

/**
 * Represents an access control entry (ACE) for a CMIS object, detailing
 * who has what permissions on a path.
 */
interface AccessDefinition {
	/** Gets the unique identifier for the access definition. */
	getId(): string;
	/** Gets the scope of the definition (e.g., 'user', 'group'). */
	getScope(): string;
	/** Gets the path of the CMIS object this definition applies to. */
	getPath(): string;
	/** Gets the method/operation this definition applies to (e.g., 'READ', 'WRITE'). */
	getMethod(): string;
	/** Gets the security role (e.g., 'Administrator', 'Viewer'). */
	getRole(): string;
}

/**
 * Session object
 * * Represents an active connection to a CMIS repository, used as the main entry point for CMIS operations.
 */
class Session {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets information about the repository this session is connected to.
	 * @returns Repository metadata.
	 */
	public getRepositoryInfo(): RepositoryInfo {
		const native = this.native.getRepositoryInfo();
		return new RepositoryInfo(native);
	}

	/**
	 * Gets the root folder of the repository.
	 * @returns The root {@link Folder} object.
	 */
	public getRootFolder(): Folder {
		const native = this.native.getRootFolder();
		return new Folder(native, null);
	}

	/**
	 * Gets the object factory for creating new CMIS objects like ContentStream.
	 * @returns An {@link ObjectFactory} instance.
	 */
	public getObjectFactory(): ObjectFactory {
		const native = this.native.getObjectFactory();
		return new ObjectFactory(native);
	}

	/**
	 * Retrieves a CMIS object (Document or Folder) by its unique ID.
	 * @param objectId The unique ID of the object.
	 * @returns A {@link Document} or {@link Folder} instance.
	 * @throws Error if the object type is unsupported.
	 */
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

	/**
	 * Retrieves a CMIS object (Document or Folder) by its path.
	 * @param path The path of the object in the repository (e.g., "/path/to/object").
	 * @returns A {@link Document} or {@link Folder} instance.
	 * @throws Error if read access is not allowed or the object type is unsupported.
	 */
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

	/**
	 * Creates a folder structure recursively based on a path. This is a convenience method that
	 * creates all non-existent parent folders.
	 * Example: `createFolder("/new/path/structure")`
	 * @param location The path of the folder to create.
	 * @returns The newly created (or existing) innermost {@link Folder} object.
	 */
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

	/**
	 * Creates a new document at the specified location. This method ensures the parent folder structure exists.
	 * @param location The path of the parent folder (e.g., "/path/to/folder").
	 * @param properties A map of CMIS properties for the new document (must include {@link Cmis.NAME}).
	 * @param contentStream The content stream containing the document's binary data.
	 * @param versioningState The versioning state (e.g., {@link Cmis.VERSIONING_STATE_MAJOR}).
	 * @returns The newly created {@link Document} object.
	 */
	public createDocument(location: string, properties: { [key: string]: any }, contentStream: ContentStream, versioningState: string): Document {
		const folder = this.createFolder(location);
		return folder.createDocument(properties, contentStream, versioningState);
	}

}

/**
 * RepositoryInfo object
 * * Provides basic information about the connected CMIS repository.
 */
class RepositoryInfo {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the unique identifier of the repository.
	 * @returns The repository ID.
	 */
	public getId(): string {
		return this.native.getId();
	}

	/**
	 * Gets the name of the repository.
	 * @returns The repository name.
	 */
	public getName(): string {
		return this.native.getName();
	}
}

/**
 * Folder object
 * * Represents a CMIS folder object, allowing operations like creating children, deleting, and renaming.
 */
export class Folder {
	private readonly native: any;
	private path: any;

	constructor(native: any, path: any) {
		this.native = native;
		this.path = path;
	}

	/**
	 * Gets the unique identifier of the folder.
	 * @returns The folder ID.
	 */
	public getId(): string {
		return this.native.getId();
	}

	/**
	 * Gets the name of the folder.
	 * @returns The folder name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Creates a new folder within this folder.
	 * @param properties A map of CMIS properties for the new folder (must include {@link Cmis.NAME}).
	 * @returns The newly created {@link Folder} object.
	 * @throws Error if write access is not allowed.
	 */
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

	/**
	 * Creates a new document within this folder.
	 * @param properties A map of CMIS properties for the new document (must include {@link Cmis.NAME}).
	 * @param contentStream The content stream containing the document's binary data.
	 * @param versioningState The versioning state (e.g., {@link Cmis.VERSIONING_STATE_MAJOR}).
	 * @returns The newly created {@link Document} object.
	 * @throws Error if write access is not allowed.
	 */
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

	/**
	 * Retrieves the children of this folder.
	 * @returns A list of generic {@link CmisObject} wrappers for the children.
	 * @throws Error if read access is not allowed.
	 */
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

	/**
	 * Gets the path of the folder.
	 * @returns The folder path.
	 */
	public getPath(): string {
		return this.path;
	}

	/**
	 * Checks if this folder is the root folder of the repository.
	 * @returns True if it is the root folder, false otherwise.
	 */
	public isRootFolder(): boolean {
		return this.native.isRootFolder();
	}

	/**
	 * Gets the parent folder of this folder.
	 * @returns The parent {@link Folder} object.
	 */
	public getFolderParent(): Folder {
		const native = this.native.getFolderParent();
		return new Folder(native, null);
	};

	/**
	 * Deletes this folder (must be empty to succeed).
	 * @throws Error if write access is not allowed.
	 */
	public delete(): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		this.native.delete();
	};

	/**
	 * Renames this folder.
	 * @param newName The new name for the folder.
	 * @throws Error if write access is not allowed.
	 */
	public rename(newName: string): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		this.native.rename(newName);
	}

	/**
	 * Deletes this folder and all its contents recursively.
	 * @throws Error if write access is not allowed.
	 */
	public deleteTree(): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		const unifiedObjectDelete = CmisFacade.getUnifiedObjectDelete();
		this.native.deleteTree(true, unifiedObjectDelete, true);
	}

	/**
	 * Gets the type definition of the folder.
	 * @returns The folder's {@link TypeDefinition}.
	 */
	public getType(): TypeDefinition {
		const native = this.native.getType();
		return new TypeDefinition(native);
	}
}

/**
 * CmisObject object
 * * A generic wrapper for CMIS objects, used primarily for children lists.
 */
class CmisObject {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the unique identifier of the object.
	 * @returns The object ID.
	 */
	public getId(): string {
		return this.native.getId();
	}

	/**
	 * Gets the name of the object.
	 * @returns The object name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Gets the path of the CMIS object. Handles differences in native CMIS implementations.
	 * @returns The object path.
	 * @throws Error if the path cannot be determined.
	 */
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

	/**
	 * Gets the type definition of the object.
	 * @returns The object's {@link TypeDefinition}.
	 */
	public getType(): TypeDefinition {
		const native = this.native.getType();
		return new TypeDefinition(native);
	}

	/**
	 * Deletes the CMIS object.
	 */
	public delete(): void {
		this.native.delete();
	}

	/**
	 * Renames the CMIS object.
	 * @param newName The new name for the object.
	 */
	public rename(newName: string): void {
		this.native.rename(newName);
	}

}

/**
 * ObjectFactory object
 * * Provides methods to create content streams.
 */
class ObjectFactory {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Creates a new content stream instance that can be used to create or update document content.
	 * @param filename The name of the file.
	 * @param length The size of the content stream in bytes.
	 * @param mimetype The MIME type of the content.
	 * @param inputStream The input stream containing the data.
	 * @returns A new {@link ContentStream} object.
	 */
	public createContentStream(filename: string, length: number, mimetype: string, inputStream: streams.InputStream): ContentStream {
		// @ts-ignore
		const native = this.native.createContentStream(filename, length, mimetype, inputStream.native);
		return new ContentStream(native);
	}
}

/**
 * ContentStream object
 * * Represents the binary content stream of a CMIS Document.
 */
class ContentStream {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the Java-backed input stream for reading the content.
	 * @returns An {@link streams.InputStream} wrapper.
	 */
	public getStream(): streams.InputStream {
		const native = this.native.getStream();
		return new streams.InputStream(native);
	}

	/**
	 * Gets the MIME type of the content stream.
	 * @returns The MIME type string.
	 */
	public getMimeType(): string {
		return this.native.getMimeType();
	}
}

/**
 * Document object
 * * Represents a CMIS document object, allowing operations like reading content, deleting, and renaming.
 */
export class Document {

	private readonly native: any;
	private path: string;

	constructor(native: any, path: string) {
		this.native = native;
		this.path = path;
	}

	/**
	 * Gets the unique identifier of the document.
	 * @returns The document ID.
	 */
	public getId(): string {
		return this.native.getId();
	}

	/**
	 * Gets the name of the document.
	 * @returns The document name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Gets the type definition of the document.
	 * @returns The document's {@link TypeDefinition}.
	 */
	public getType(): TypeDefinition {
		const native = this.native.getType();
		return new TypeDefinition(native);
	}

	/**
	 * Gets the path of the document.
	 * @returns The document path.
	 */
	public getPath(): string {
		return this.path;
	}

	/**
	 * Deletes this document.
	 * @throws Error if write access is not allowed.
	 */
	public delete(): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		return this.native.delete(true);
	}

	/**
	 * Gets the binary content stream of the document.
	 * @returns The {@link ContentStream} object, or `null` if the document has no content.
	 */
	public getContentStream(): ContentStream | null {
		const native = this.native.getContentStream();
		if (native !== null) {
			return new ContentStream(native);
		}
		return null;
	};

	/**
	 * Gets the size of the document's content stream in bytes.
	 * @returns The size in bytes.
	 */
	public getSize(): number {
		return this.native.getSize();
	}

	/**
	 * Renames this document.
	 * @param newName The new name for the document.
	 * @throws Error if write access is not allowed.
	 */
	public rename(newName: string): void {
		const allowed = CmisFacade.isAllowed(this.getPath(), Cmis.METHOD_WRITE);
		if (!allowed) {
			throw new Error("Write access not allowed on: " + this.getPath());
		}
		this.native.rename(newName);
	}
}

/**
 * Represents the definition of a CMIS object type (e.g., cmis:document, cmis:folder).
 */
class TypeDefinition {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the unique ID of the object type (e.g., 'cmis:document').
	 * @returns The type ID.
	 */
	public getId(): string {
		return this.native.getId();
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Cmis;
}