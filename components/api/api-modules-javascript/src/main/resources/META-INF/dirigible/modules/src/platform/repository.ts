/**
 * Provides a wrapper for the platform's RepositoryFacade to manage files (Resources)
 * and folders (Collections), including CRUD operations, movement, and content handling.
 */
import { Bytes } from "../io/bytes";

const RepositoryFacade = Java.type("org.eclipse.dirigible.components.api.platform.RepositoryFacade");

/**
 * @class Repository
 * @description Static utility class providing high-level methods for interacting with the
 * repository facade to manage resources and collections by path.
 */
export class Repository {

	/**
	 * Retrieves a resource (file) object from the repository by its path.
	 *
	 * @param {string} path The absolute path to the resource.
	 * @returns {Resource} A Resource instance wrapping the native repository object.
	 */
	public static getResource(path: string): Resource {
		const resourceInstance = RepositoryFacade.getResource(path);
		return new Resource(resourceInstance);
	}

	/**
	 * Creates a new resource (file) with content provided as a string.
	 *
	 * @param {string} path The absolute path where the resource should be created.
	 * @param {string} content The string content for the resource.
	 * @param {string} contentType The MIME type of the content (e.g., "text/plain").
	 * @returns {Resource} The newly created Resource instance.
	 */
	public static createResource(path: string, content: string, contentType: string): Resource {
		const resourceInstance = RepositoryFacade.createResource(path, content, contentType);
		return new Resource(resourceInstance);
	}

	/**
	 * Creates a new resource (file) with content provided as a native byte array.
	 *
	 * @param {string} path The absolute path where the resource should be created.
	 * @param {any[]} content The native byte array content.
	 * @param {string} contentType The MIME type of the content.
	 * @returns {Resource} The newly created Resource instance.
	 */
	public static createResourceNative(path: string, content: any[], contentType: string): Resource {
		const resourceInstance = RepositoryFacade.createResourceNative(path, content, contentType);
		return new Resource(resourceInstance);
	}

	/**
	 * Updates the content of an existing resource using a string.
	 *
	 * @param {string} path The absolute path to the resource to update.
	 * @param {string} content The new string content.
	 * @returns {Resource} The updated Resource instance.
	 */
	public static updateResource(path: string, content: string): Resource {
		const resourceInstance = RepositoryFacade.updateResource(path, content);
		return new Resource(resourceInstance);
	}

	/**
	 * Updates the content of an existing resource using a native byte array.
	 *
	 * @param {string} path The absolute path to the resource to update.
	 * @param {any[]} content The new native byte array content.
	 * @returns {Resource} The updated Resource instance.
	 */
	public static updateResourceNative(path: string, content: any[]): Resource {
		const resourceInstance = RepositoryFacade.updateResourceNative(path, content);
		return new Resource(resourceInstance);
	}

	/**
	 * Deletes the resource (file) at the specified path.
	 *
	 * @param {string} path The absolute path of the resource to delete.
	 */
	public static deleteResource(path: string): void {
		RepositoryFacade.deleteResource(path);
	}

	/**
	 * Retrieves a collection (folder) object from the repository by its path.
	 *
	 * @param {string} path The absolute path to the collection.
	 * @returns {Collection} A Collection instance wrapping the native repository object.
	 */
	public static getCollection(path: string): Collection {
		const collectionInstance = RepositoryFacade.getCollection(path);
		return new Collection(collectionInstance);
	}

	/**
	 * Creates a new collection (folder) at the specified path.
	 *
	 * @param {string} path The absolute path where the collection should be created.
	 * @returns {Collection} The newly created Collection instance.
	 */
	public static createCollection(path: string): Collection {
		const collectionInstance = RepositoryFacade.createCollection(path);
		return new Collection(collectionInstance);
	}

	/**
	 * Deletes the collection (folder) at the specified path.
	 *
	 * @param {string} path The absolute path of the collection to delete.
	 */
	public static deleteCollection(path: string): void {
		RepositoryFacade.deleteCollection(path);
	}

	/**
	 * Searches the repository starting from a given path for resources matching a glob pattern.
	 *
	 * @param {string} path The starting path for the search.
	 * @param {string} pattern The glob pattern to match resource names against (e.g., "*.js").
	 * @returns {string[]} An array of repository paths (strings) that match the search criteria.
	 */
	public static find(path: string, pattern: string): string[] {
		return JSON.parse(RepositoryFacade.find(path, pattern));
	}
}

/**
 * @class Resource
 * @description Represents a file or resource (non-collection) within the Repository,
 * providing instance methods for file operations.
 */
export class Resource {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance representing the repository resource.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the resource (file name).
	 * @returns {string} The name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Gets the full repository path of the resource.
	 * @returns {string} The repository path.
	 */
	public getPath(): string {
		return this.native.getPath();
	}

	/**
	 * Gets the parent collection (folder) of this resource.
	 * @returns {Collection} The parent Collection instance.
	 */
	public getParent(): Collection {
		const collectionInstance = this.native.getParent();
		return new Collection(collectionInstance);
	}

	/**
	 * Gets detailed metadata about the resource.
	 * @returns {EntityInformation} The metadata object.
	 */
	public getInformation(): EntityInformation {
		const informationInstance = this.native.getInformation();
		return new EntityInformation(informationInstance);
	}

	/**
	 * Creates the resource if it does not already exist.
	 */
	public create(): void {
		this.native.create();
	}

	/**
	 * Deletes the resource from the repository.
	 */
	public delete(): void {
		this.native.delete();
	}

	/**
	 * Renames the resource within its current collection.
	 * @param {string} name The new name for the resource.
	 */
	public renameTo(name: string): void {
		this.native.renameTo(name);
	}

	/**
	 * Moves the resource to a new path.
	 * @param {string} path The new absolute path for the resource.
	 */
	public moveTo(path: string): void {
		this.native.moveTo(path);
	}

	/**
	 * Copies the resource to a new path.
	 * @param {string} path The new absolute path for the copied resource.
	 */
	public copyTo(path: string): void {
		this.native.copyTo(path);
	}

	/**
	 * Checks if the resource currently exists in the repository.
	 * @returns {boolean} True if the resource exists, false otherwise.
	 */
	public exists(): boolean {
		return this.native.exists();
	}

	/**
	 * Checks if the resource (file) is empty (has zero size).
	 * @returns {boolean} True if the content is empty, false otherwise.
	 */
	public isEmpty(): boolean {
		return this.native.isEmpty();
	}

	/**
	 * Gets the content of the resource as a text string.
	 * @returns {string} The text content.
	 */
	public getText(): string {
		return Bytes.byteArrayToText(this.getContent());
	}

	/**
	 * Gets the content of the resource as a JavaScript-friendly byte array.
	 * @returns {any[]} The content bytes.
	 */
	public getContent(): any[] {
		const nativeContent = this.native.getContent();
		return Bytes.toJavaScriptBytes(nativeContent);
	}

	/**
	 * Gets the content of the resource as its native Java byte array representation.
	 * @returns {any[]} The content bytes.
	 */
	public getContentNative(): any[] {
		return this.native.getContent();
	}

	/**
	 * Sets the content of the resource using a text string.
	 * The string is converted to a byte array before saving.
	 * @param {string} text The new text content.
	 */
	public setText(text: string): void {
		const content = Bytes.textToByteArray(text);
		this.setContent(content);
	}

	/**
	 * Sets the content of the resource using a JavaScript byte array.
	 * The array is converted to a native byte array before saving.
	 * @param {any[]} content The new content bytes.
	 */
	public setContent(content: any[]): void {
		const nativeContent = Bytes.toJavaBytes(content);
		this.native.setContent(nativeContent);
	}

	/**
	 * Sets the content of the resource using a native Java byte array.
	 * @param {any[]} content The new native content bytes.
	 */
	public setContentNative(content: any[]): void {
		this.native.setContent(content);
	}

	/**
	 * Checks if the resource content is determined to be binary.
	 * @returns {boolean} True if binary, false if text.
	 */
	public isBinary(): boolean {
		return this.native.isBinary();
	}

	/**
	 * Gets the content type (MIME type) of the resource.
	 * @returns {string} The content type string.
	 */
	public getContentType(): string {
		return this.native.getContentType();
	}
}

/**
 * @class Collection
 * @description Represents a directory or folder within the Repository, providing
 * instance methods for collection and resource management.
 */
export class Collection {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance representing the repository collection.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the collection (folder name).
	 * @returns {string} The name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Gets the full repository path of the collection.
	 * @returns {string} The repository path.
	 */
	public getPath(): string {
		return this.native.getPath();
	}

	/**
	 * Gets the parent collection (folder) of this collection.
	 * @returns {Collection} The parent Collection instance.
	 */
	public getParent(): Collection {
		const collectionInstance = this.native.getParent();
		return new Collection(collectionInstance);
	}

	/**
	 * Gets detailed metadata about the collection.
	 * @returns {EntityInformation} The metadata object.
	 */
	public getInformation(): EntityInformation {
		const informationInstance = this.native.getInformation();
		return new EntityInformation(informationInstance);
	}

	/**
	 * Creates the collection if it does not already exist.
	 */
	public create(): void {
		this.native.create();
	}

	/**
	 * Deletes the collection from the repository.
	 */
	public delete(): void {
		this.native.delete();
	}

	/**
	 * Renames the collection within its current parent.
	 * @param {string} name The new name for the collection.
	 */
	public renameTo(name: string): void {
		this.native.renameTo(name);
	}

	/**
	 * Moves the collection to a new path.
	 * @param {string} path The new absolute path for the collection.
	 */
	public moveTo(path: string): void {
		this.native.moveTo(path);
	}

	/**
	 * Copies the collection to a new path.
	 * @param {string} path The new absolute path for the copied collection.
	 */
	public copyTo(path: string): void {
		this.native.copyTo(path);
	}

	/**
	 * Checks if the collection currently exists in the repository.
	 * @returns {boolean} True if the collection exists, false otherwise.
	 */
	public exists(): boolean {
		return this.native.exists();
	}

	/**
	 * Checks if the collection is empty (contains no files or sub-directories).
	 * @returns {boolean} True if empty, false otherwise.
	 */
	public isEmpty(): boolean {
		return this.native.isEmpty();
	}

	/**
	 * Gets the names of all sub-collections (folders) within this collection.
	 * @returns {string[]} An array of sub-collection names.
	 */
	public getCollectionsNames(): string[] {
		return this.native.getCollectionsNames();
	}

	/**
	 * Creates a new sub-collection (folder) within this collection.
	 * @param {string} name The name of the new sub-collection.
	 * @returns {Collection} The newly created Collection instance.
	 */
	public createCollection(name: string): Collection {
		const collectionInstance = this.native.createCollection(name);
		return new Collection(collectionInstance);
	}

	/**
	 * Gets a specific sub-collection by name.
	 * @param {string} name The name of the sub-collection.
	 * @returns {Collection} The child Collection instance.
	 */
	public getCollection(name: string): Collection {
		const collectionInstance = this.native.getCollection(name);
		return new Collection(collectionInstance);
	}

	/**
	 * Removes a sub-collection by name.
	 * @param {string} name The name of the sub-collection to remove.
	 */
	public removeCollection(name: string): void {
		this.native.removeCollection(name);
	}

	/**
	 * Gets the names of all resources (files) within this collection.
	 * @returns {string[]} An array of resource names.
	 */
	public getResourcesNames(): string[] {
		return this.native.getResourcesNames();
	}

	/**
	 * Gets a specific resource (file) by name.
	 * @param {string} name The name of the resource.
	 * @returns {Resource} The child Resource instance.
	 */
	public getResource(name: string): Resource {
		const resourceInstance = this.native.getResource(name);
		return new Resource(resourceInstance);
	}

	/**
	 * Removes a resource (file) by name.
	 * @param {string} name The name of the resource to remove.
	 */
	public removeResource(name: string): void {
		this.native.removeResource(name);
	}

	/**
	 * Creates a new resource (file) within this collection.
	 * @param {string} name The name of the new resource.
	 * @param {string} content The string content for the resource.
	 * @returns {Resource} The newly created Resource instance.
	 */
	public createResource(name: string, content: string): Resource {
		const resourceInstance = this.native.createResource(name, content);
		return new Resource(resourceInstance);
	}
}

/**
 * @class EntityInformation
 * @description Represents detailed metadata (creation date, size, permissions, etc.) for a
 * Resource or Collection.
 */
export class EntityInformation {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance holding the entity information.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the entity (resource or collection).
	 * @returns {string} The name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Gets the full repository path of the entity.
	 * @returns {string} The repository path.
	 */
	public getPath(): string {
		return this.native.getPath();
	}

	/**
	 * Gets the access permissions for the entity (typically an integer bitmask).
	 * @returns {number} The permissions value.
	 */
	public getPermissions(): number {
		return this.native.getPermissions();
	}

	/**
	 * Gets the size of the resource content in bytes (0 for a collection).
	 * @returns {number} The size in bytes.
	 */
	public getSize(): number {
		return this.native.getSize();
	}

	/**
	 * Gets the user who created the entity.
	 * @returns {string} The creator's name.
	 */
	public getCreatedBy(): string {
		return this.native.getCreatedBy();
	}

	/**
	 * Gets the creation timestamp.
	 * @returns {Date} The creation date and time.
	 */
	public getCreatedAt(): Date {
		return this.native.getCreatedAt();
	}

	/**
	 * Gets the user who last modified the entity.
	 * @returns {string} The modifier's name.
	 */
	public getModifiedBy(): string {
		return this.native.getModifiedBy();
	}

	/**
	 * Gets the last modification timestamp.
	 * @returns {Date} The modification date and time.
	 */
	public getModifiedAt(): Date {
		return this.native.getModifiedAt();
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Repository;
}
