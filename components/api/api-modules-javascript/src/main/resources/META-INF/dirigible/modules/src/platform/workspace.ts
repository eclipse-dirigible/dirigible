/**
 * Provides a wrapper for the platform's WorkspaceFacade to manage Workspaces,
 * Projects, Folders, and Files.
 */
import { Bytes } from "@aerokit/sdk/io/bytes";

const WorkspaceFacade = Java.type("org.eclipse.dirigible.components.api.platform.WorkspaceFacade");

/**
 * @class Workspace
 * @description Represents a logical container for projects, providing static methods for
 * high-level workspace management and instance methods for project management within the workspace.
 */
export class Workspace {

	/**
	 * Creates a new workspace with the given name.
	 *
	 * @param {string} name The name of the workspace to create.
	 * @returns {Workspace} The newly created Workspace instance.
	 */
	public static createWorkspace(name: string): Workspace {
		const native = WorkspaceFacade.createWorkspace(name);
		return new Workspace(native);
	}

	/**
	 * Retrieves an existing workspace by name.
	 *
	 * @param {string} name The name of the workspace to retrieve.
	 * @returns {Workspace} The Workspace instance.
	 */
	public static getWorkspace(name: string): Workspace {
		const native = WorkspaceFacade.getWorkspace(name);
		return new Workspace(native);
	}

	/**
	 * Retrieves the names of all existing workspaces.
	 *
	 * @returns {string[]} An array of workspace names.
	 */
	public static getWorkspacesNames(): string[] {
		const workspacesNames = WorkspaceFacade.getWorkspacesNames();
		if (workspacesNames) {
			// The native method returns a JSON string array that needs to be parsed.
			return JSON.parse(workspacesNames);
		}
		// If null or undefined is returned, return the raw value (which will be null/undefined).
		return workspacesNames;
	}

	/**
	 * Deletes the workspace with the specified name.
	 *
	 * @param {string} name The name of the workspace to delete.
	 */
	public static deleteWorkspace(name: string): void {
		WorkspaceFacade.deleteWorkspace(name);
	}

	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance representing the workspace.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets a collection of all projects within this workspace.
	 *
	 * @returns {Projects} A Projects collection instance.
	 */
	public getProjects(): Projects {
		const native = this.native.getProjects();
		return new Projects(native);
	}

	/**
	 * Creates a new project within this workspace.
	 *
	 * @param {string} name The name of the project to create.
	 * @returns {Project} The newly created Project instance.
	 */
	public createProject(name: string): Project {
		const native = this.native.createProject(name);
		return new Project(native);
	}

	/**
	 * Retrieves an existing project by name from this workspace.
	 *
	 * @param {string} name The name of the project to retrieve.
	 * @returns {Project} The Project instance.
	 */
	public getProject(name: string): Project {
		const native = this.native.getProject(name);
		return new Project(native);
	}

	/**
	 * Deletes a project from this workspace by name.
	 *
	 * @param {string} name The name of the project to delete.
	 */
	public deleteProject(name: string): void {
		this.native.deleteProject(name);
	}

	/**
	 * Checks if the workspace currently exists.
	 *
	 * @returns {boolean} True if the workspace exists, false otherwise.
	 */
	public exists(): boolean {
		return this.native.exists();
	}

	/**
	 * Checks if a specific folder path exists within the workspace's filesystem structure.
	 *
	 * @param {string} path The relative path to the folder.
	 * @returns {boolean} True if the folder exists.
	 */
	public existsFolder(path: string): boolean {
		return this.native.existsFolder(path);
	}

	/**
	 * Checks if a specific file path exists within the workspace's filesystem structure.
	 *
	 * @param {string} path The relative path to the file.
	 * @returns {boolean} True if the file exists.
	 */
	public existsFile(path: string): boolean {
		return this.native.existsFile(path);
	}

	/**
	 * Copies a project from a source name to a target name within the workspace.
	 *
	 * @param {string} source The name of the project to copy.
	 * @param {string} target The name of the new project copy.
	 */
	public copyProject(source: string, target: string): void {
		this.native.copyProject(source, target);
	}

	/**
	 * Moves a project from a source name to a target name (renaming it).
	 *
	 * @param {string} source The current name of the project.
	 * @param {string} target The new name/path of the project.
	 */
	public moveProject(source: string, target: string): void {
		this.native.moveProject(source, target);
	}

}

/**
 * @class Projects
 * @description A collection/list of projects within a workspace.
 */
export class Projects {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object representing the list of projects.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the number of projects in the collection.
	 * @returns {number} The size of the collection.
	 */
	public size(): number {
		return this.native.size();
	}

	/**
	 * Gets a Project instance at the specified index.
	 * @param {number} index The index of the project.
	 * @returns {Project} The Project instance.
	 */
	public get(index: number): Project {
		const native = this.native.get(index);
		return new Project(native);
	}

}

/**
 * @class Project
 * @description Represents a Project within a workspace. It provides methods for managing
 * folders and files within the project.
 */
export class Project {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance representing the project.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the project.
	 * @returns {string} The project name.
	 */
	public getName(): string {
		const collection = this.native.getInternal();
		return collection.getName();
	}

	/**
	 * Gets the path of the project.
	 * @returns {string} The project path (relative to the repository/workspace root).
	 */
	public getPath(): string {
		const collection = this.native.getInternal();
		return collection.getPath();
	}

	/**
	 * Creates a new folder within the project.
	 *
	 * @param {string} path The path of the folder to create (relative to the project root).
	 * @returns {Folder} The newly created Folder instance.
	 */
	public createFolder(path: string): Folder {
		const native = this.native.createFolder(path);
		return new Folder(native);
	}

	/**
	 * Checks if the project itself exists.
	 * @returns {boolean} True if the project exists.
	 */
	public exists(): boolean {
		return this.native.exists();
	}

	/**
	 * Checks if a specific folder path exists within the project.
	 *
	 * @param {string} path The relative path to the folder.
	 * @returns {boolean} True if the folder exists.
	 */
	public existsFolder(path: string): boolean {
		return this.native.existsFolder(path);
	}

	/**
	 * Retrieves a folder by its path relative to the project root.
	 *
	 * @param {string} path The relative path to the folder.
	 * @returns {Folder} The Folder instance.
	 */
	public getFolder(path: string): Folder {
		const native = this.native.getFolder(path);
		return new Folder(native);
	}

	/**
	 * Retrieves a collection of folders at a specific path.
	 *
	 * @param {string} path The path containing the folders to retrieve.
	 * @returns {Folders} The Folders collection instance.
	 */
	public getFolders(path: string): Folders {
		const native = this.native.getFolders(path);
		return new Folders(native);
	}

	/**
	 * Deletes a folder from the project.
	 *
	 * @param {string} path The path of the folder to delete (relative to the project root).
	 */
	public deleteFolder(path: string): void {
		this.native.deleteFolder(path);
	}

	/**
	 * Creates a new file within the project.
	 *
	 * @param {string} path The path of the file to create (relative to the project root).
	 * @param {any[]} [input=[]] Optional initial content as a byte array.
	 * @returns {File} The newly created File instance.
	 */
	public createFile(path: string, input: any[] = []): File {
		const native = this.native.createFile(path, input);
		return new File(native);
	}

	/**
	 * Checks if a specific file path exists within the project.
	 *
	 * @param {string} path The relative path to the file.
	 * @returns {boolean} True if the file exists.
	 */
	public existsFile(path: string): boolean {
		return this.native.existsFile(path);
	}

	/**
	 * Retrieves a file by its path relative to the project root.
	 *
	 * @param {string} path The relative path to the file.
	 * @returns {File} The File instance.
	 */
	public getFile(path: string): File {
		const native = this.native.getFile(path);
		return new File(native);
	}

	/**
	 * Retrieves a collection of files at a specific path.
	 *
	 * @param {string} path The path containing the files to retrieve.
	 * @returns {Files} The Files collection instance.
	 */
	public getFiles(path: string): Files {
		const native = this.native.getFiles(path);
		return new Files(native);
	}

	/**
	 * Deletes a file from the project.
	 *
	 * @param {string} path The path of the file to delete (relative to the project root).
	 */
	public deleteFile(path: string): void {
		this.native.deleteFile(path);
	}

}

/**
 * @class Folders
 * @description A collection/list of folders.
 */
export class Folders {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object representing the list of folders.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the number of folders in the collection.
	 * @returns {number} The size of the collection.
	 */
	public size(): number {
		return this.native.size();
	}

	/**
	 * Gets a Folder instance at the specified index.
	 * @param {number} index The index of the folder.
	 * @returns {Folder} The Folder instance.
	 */
	public get(index: number): Folder {
		const native = this.native.get(index);
		return new Folder(native);
	}

}

/**
 * @class Files
 * @description A collection/list of files.
 */
export class Files {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object representing the list of files.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the number of files in the collection.
	 * @returns {number} The size of the collection.
	 */
	public size(): number {
		return this.native.size();
	}

	/**
	 * Gets a File instance at the specified index.
	 * @param {number} index The index of the file.
	 * @returns {File} The File instance.
	 */
	public get(index: number): File {
		const native = this.native.get(index);
		return new File(native);
	}

}

/**
 * @class Folder
 * @description Represents a directory or folder within a project, providing methods for
 * managing sub-folders and files.
 */
export class Folder {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance representing the folder.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the folder.
	 * @returns {string} The folder name.
	 */
	public getName(): string {
		const collection = this.native.getInternal();
		return collection.getName();
	};

	/**
	 * Gets the full path of the folder.
	 * @returns {string} The folder path.
	 */
	public getPath(): string {
		const collection = this.native.getInternal();
		return collection.getPath();
	}

	/**
	 * Creates a new sub-folder within this folder.
	 *
	 * @param {string} path The path of the sub-folder to create (relative to this folder).
	 * @returns {Folder} The newly created Folder instance.
	 */
	public createFolder(path: string): Folder {
		const native = this.native.createFolder(path);
		return new Folder(native);
	}

	/**
	 * Checks if the folder itself exists.
	 * @returns {boolean} True if the folder exists.
	 */
	public exists(): boolean {
		return this.native.exists();
	}

	/**
	 * Checks if a specific sub-folder path exists within this folder.
	 *
	 * @param {string} path The relative path to the sub-folder.
	 * @returns {boolean} True if the sub-folder exists.
	 */
	public existsFolder(path: string): boolean {
		return this.native.existsFolder(path);
	}

	/**
	 * Retrieves a sub-folder by its path relative to this folder.
	 *
	 * @param {string} path The relative path to the sub-folder.
	 * @returns {Folder} The Folder instance.
	 */
	public getFolder(path: string): Folder {
		const native = this.native.getFolder(path);
		return new Folder(native);
	}

	/**
	 * Retrieves a collection of folders at a specific path relative to this folder.
	 *
	 * @param {string} path The path containing the folders to retrieve.
	 * @returns {Folders} The Folders collection instance.
	 */
	public getFolders(path: string): Folders {
		const native = this.native.getFolders(path);
		return new Folders(native);
	}

	/**
	 * Deletes a sub-folder from this folder.
	 *
	 * @param {string} path The path of the sub-folder to delete (relative to this folder).
	 */
	public deleteFolder(path: string): void {
		this.native.deleteFolder(path);
	}

	/**
	 * Creates a new file within this folder.
	 *
	 * @param {string} path The path of the file to create (relative to this folder).
	 * @param {any[]} [input=[]] Optional initial content as a byte array.
	 * @returns {File} The newly created File instance.
	 */
	public createFile(path: string, input: any[] = []): File {
		const native = this.native.createFile(path, input);
		return new File(native);
	}

	/**
	 * Checks if a specific file path exists within this folder.
	 *
	 * @param {string} path The relative path to the file.
	 * @returns {boolean} True if the file exists.
	 */
	public existsFile(path: string): boolean {
		return this.native.existsFile(path);
	}

	/**
	 * Retrieves a file by its path relative to this folder.
	 *
	 * @param {string} path The relative path to the file.
	 * @returns {File} The File instance.
	 */
	public getFile(path: string): File {
		const native = this.native.getFile(path);
		return new File(native);
	}

	/**
	 * Retrieves a collection of files at a specific path relative to this folder.
	 *
	 * @param {string} path The path containing the files to retrieve.
	 * @returns {Files} The Files collection instance.
	 */
	public getFiles(path: string): Files {
		const native = this.native.getFiles(path);
		return new Files(native);
	}

	/**
	 * Deletes a file from this folder.
	 *
	 * @param {string} path The path of the file to delete (relative to this folder).
	 */
	public deleteFile(path: string): void {
		this.native.deleteFile(path);
	}

}

/**
 * @class File
 * @description Represents a file (resource) within the workspace, providing methods for
 * content access and manipulation.
 */
export class File {
	private readonly native: any;

	/**
	 * @constructor
	 * @param {any} native The native Java object instance representing the file.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Gets the name of the file.
	 * @returns {string} The file name.
	 */
	public getName(): string {
		const collection = this.native.getInternal();
		return collection.getName();
	}

	/**
	 * Gets the full path of the file.
	 * @returns {string} The file path.
	 */
	public getPath(): string {
		const collection = this.native.getInternal();
		return collection.getPath();
	}

	/**
	 * Gets the content type (MIME type) of the file.
	 * @returns {string} The content type string.
	 */
	public getContentType(): string {
		return this.native.getContentType();
	}

	/**
	 * Checks if the file content is determined to be binary.
	 * @returns {boolean} True if binary, false if text.
	 */
	public isBinary(): boolean {
		return this.native.isBinary();
	}

	/**
	 * Gets the content of the file as a JavaScript-friendly byte array.
	 * @returns {any[]} The content bytes.
	 */
	public getContent(): any[] {
		const output = WorkspaceFacade.getContent(this.native);
		if (output) {
			// Ensure it's returned if it exists
			return output;
		}
		return output;
	}

	/**
	 * Gets the content of the file as a text string.
	 * @returns {string} The text content.
	 */
	public getText(): string {
		const bytesOutput = this.getContent();
		return Bytes.byteArrayToText(bytesOutput);
	}

	/**
	 * Sets the content of the file using a byte array.
	 * @param {any[]} input The new content bytes.
	 */
	public setContent(input: any[]): void {
		WorkspaceFacade.setContent(this.native, input);
	}

	/**
	 * Sets the content of the file using a text string.
	 * The string is converted to a byte array before saving.
	 * @param {string} input The new text content.
	 */
	public setText(input: string): void {
		const bytesInput = Bytes.textToByteArray(input);
		this.setContent(bytesInput);
	}

	/**
	 * Checks if the file exists.
	 * @returns {boolean} True if the file exists.
	 */
	public exists(): boolean {
		return this.native.exists();
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Workspace;
}
