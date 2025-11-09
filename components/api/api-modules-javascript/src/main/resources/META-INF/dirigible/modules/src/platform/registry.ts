/**
 * Provides a set of utilities and data structures for interacting with the platform's
 * Registry/Repository, which manages files and directories (Artefacts and Collections).
 */
import { Bytes } from "sdk/io/bytes";
import { Repository } from "sdk/platform/repository";
const RegistryFacade = Java.type("org.eclipse.dirigible.components.api.platform.RegistryFacade");

/**
 * @class Registry
 * @description Static utility class providing high-level access to the RegistryFacade for
 * retrieving content and metadata by path.
 */
export class Registry {

	/**
	 * Retrieves the content of a registry resource at the given path, converting it into a
	 * JavaScript-friendly byte array format.
	 *
	 * @param {string} path The absolute path to the resource (e.g., "/registry/public/myFile.txt").
	 * @returns {any[]} The resource content as a JavaScript byte array.
	 */
	public static getContent(path: string): any[] {
		return Bytes.toJavaScriptBytes(RegistryFacade.getContent(path));
	}

	/**
	 * Retrieves the content of a registry resource at the given path in its native Java byte array format.
	 *
	 * @param {string} path The absolute path to the resource.
	 * @returns {any[]} The resource content as a native Java byte array.
	 */
	public static getContentNative(path: string): any[] {
		return RegistryFacade.getContent(path);
	}

	/**
	 * Retrieves the content of a registry resource at the given path as a string.
	 *
	 * @param {string} path The absolute path to the resource.
	 * @returns {string} The resource content as plain text.
	 */
	public static getText(path: string): string {
		return RegistryFacade.getText(path);
	}

	/**
	 * Searches the registry starting from a given path for resources matching a glob pattern.
	 *
	 * @param {string} path The starting path for the search.
	 * @param {string} pattern The glob pattern to match resource names against (e.g., "*.js").
	 * @returns {string[]} An array of registry paths (strings) that match the search criteria.
	 */
	public static find(path: string, pattern: string): string[] {
		return JSON.parse(RegistryFacade.find(path, pattern));
	}

	/**
	 * Gets the root directory object for the public registry space.
	 *
	 * @returns {Directory} A Directory instance representing the root public collection.
	 */
	public static getRoot(): Directory {
		return new Directory(Repository.getCollection("/registry/public"));
	}
}

/**
 * @class Artefact
 * @description Represents a file or resource (non-collection) within the Registry.
 */
export class Artefact {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance representing the repository resource.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the artefact (file name).
	 * @returns {string} The name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Gets the full registry path of the artefact.
	 * @returns {string} The registry path.
	 */
	public getPath(): string {
		return RegistryFacade.toRegistryPath(this.native.getPath());
	}

	/**
	 * Gets the parent directory of this artefact.
	 * @returns {Directory} The parent Directory instance.
	 */
	public getParent(): Directory {
		const collectionInstance = this.native.getParent();
		return new Directory(collectionInstance);
	}

	/**
	 * Gets detailed metadata about the artefact.
	 * @returns {ArtefactInformation} The metadata object.
	 */
	public getInformation(): ArtefactInformation {
		const informationInstance = this.native.getInformation();
		return new ArtefactInformation(informationInstance);
	}

	/**
	 * Checks if the artefact currently exists in the registry.
	 * @returns {boolean} True if the artefact exists, false otherwise.
	 */
	public exists(): boolean {
		return this.native.exists();
	}

	/**
	 * Checks if the artefact (file) is empty (has zero size).
	 * @returns {boolean} True if the content is empty, false otherwise.
	 */
	public isEmpty(): boolean {
		return this.native.isEmpty();
	}

	/**
	 * Gets the content of the artefact as a text string.
	 * @returns {string} The text content.
	 */
	public getText(): string {
		return Bytes.byteArrayToText(this.getContent());
	}

	/**
	 * Gets the content of the artefact as a JavaScript-friendly byte array.
	 * @returns {any[]} The content bytes.
	 */
	public getContent(): any[] {
		const nativeContent = this.native.getContent();
		return Bytes.toJavaScriptBytes(nativeContent);
	}

	/**
	 * Gets the content of the artefact as its native Java byte array representation.
	 * @returns {any[]} The content bytes.
	 */
	public getContentNative(): any[] {
		return this.native.getContent();
	}

	/**
	 * Checks if the artefact content is determined to be binary.
	 * @returns {boolean} True if binary, false if text.
	 */
	public isBinary(): boolean {
		return this.native.isBinary();
	}

	/**
	 * Gets the content type (MIME type) of the artefact.
	 * @returns {string} The content type string.
	 */
	public getContentType(): string {
		return this.native.getContentType();
	}
}

/**
 * @class Directory
 * @description Represents a collection or folder within the Registry.
 */
export class Directory {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance representing the repository collection.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the directory (folder name).
	 * @returns {string} The name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Gets the full registry path of the directory.
	 * @returns {string} The registry path.
	 */
	public getPath(): string {
		return RegistryFacade.toRegistryPath(this.native.getPath());
	}

	/**
	 * Gets the parent directory.
	 * @returns {Directory} The parent Directory instance.
	 */
	public getParent(): Directory {
		const collectionInstance = this.native.getParent();
		return new Directory(collectionInstance);
	}

	/**
	 * Gets detailed metadata about the directory.
	 * @returns {ArtefactInformation} The metadata object.
	 */
	public getInformation(): ArtefactInformation {
		const informationInstance = this.native.getInformation();
		return new ArtefactInformation(informationInstance);
	}

	/**
	 * Checks if the directory currently exists in the registry.
	 * @returns {boolean} True if the directory exists, false otherwise.
	 */
	public exists(): boolean {
		return this.native.exists();
	}

	/**
	 * Checks if the directory is empty (contains no files or sub-directories).
	 * @returns {boolean} True if empty, false otherwise.
	 */
	public isEmpty(): boolean {
		return this.native.isEmpty();
	}

	/**
	 * Gets the names of all sub-directories within this directory.
	 * @returns {string[]} An array of sub-directory names.
	 */
	public getDirectoriesNames(): string[] {
		return this.native.getCollectionsNames();
	}

	/**
	 * Gets a specific sub-directory by name.
	 * @param {string} name The name of the sub-directory.
	 * @returns {Directory} The child Directory instance.
	 */
	public getDirectory(name: string): Directory {
		const collectionInstance = this.native.getCollection(RegistryFacade.toResourcePath(name));
		return new Directory(collectionInstance);
	}

	/**
	 * Gets the names of all files (artefacts) within this directory.
	 * @returns {string[]} An array of artefact names.
	 */
	public getArtefactsNames(): string[] {
		return this.native.getResourcesNames();
	}

	/**
	 * Gets a specific file (artefact) by name.
	 * @param {string} name The name of the artefact.
	 * @returns {Artefact} The child Artefact instance.
	 */
	public getArtefact(name: string): Artefact {
		const resourceInstance = this.native.getResource(RegistryFacade.toResourcePath(name));
		return new Artefact(resourceInstance);
	}

}

/**
 * @class ArtefactInformation
 * @description Represents detailed metadata (creation date, size, permissions, etc.) for a
 * Directory or Artefact.
 */
export class ArtefactInformation {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance holding the artefact information.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the resource.
	 * @returns {string} The name.
	 */
	public getName(): string {
		return this.native.getName();
	}

	/**
	 * Gets the full registry path of the resource.
	 * @returns {string} The registry path.
	 */
	public getPath(): string {
		return RegistryFacade.toRegistryPath(this.native.getPath());
	}

	/**
	 * Gets the access permissions for the resource (typically an integer bitmask).
	 * @returns {number} The permissions value.
	 */
	public getPermissions(): number {
		return this.native.getPermissions();
	}

	/**
	 * Gets the size of the resource content in bytes.
	 * @returns {number} The size in bytes.
	 */
	public getSize(): number {
		return this.native.getSize();
	}

	/**
	 * Gets the user who created the resource.
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
	 * Gets the user who last modified the resource.
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
	module.exports = Registry;
}