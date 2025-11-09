/**
 * Provides a comprehensive static façade for file and directory operations,
 * abstracting the underlying Java file system implementation.
 */
import { InputStream, OutputStream } from "sdk/io/streams";
import { Bytes } from "sdk/io/bytes";

const FilesFacade = Java.type("org.eclipse.dirigible.components.api.io.FilesFacade");
const File = Java.type("java.io.File")

/**
 * Represents a generic file system object (file or directory).
 */
export interface FileObject {
    /** The simple name of the file or folder. */
    name: string;
    /** The absolute path of the file or folder. */
    path: string;
}

/**
 * Represents a folder object, extending the basic file object with lists of its contents.
 */
export interface FolderObject extends FileObject {
    /** A list of file objects contained within this folder. */
    files: FileObject[];
    /** A list of sub-folder objects contained within this folder. */
    folders: FolderObject[];
}

/**
 * The Files class provides static methods for high-level file system manipulation,
 * including checking properties, reading/writing content, and navigating the structure.
 */
export class Files {

	/**
	 * The file system-dependent name separator character (e.g., "/" on Unix, "\" on Windows).
	 */
	public static readonly separator: string = File.separator;

    /**
	 * Checks if a file or directory exists at the given path.
	 * @param path The path to check.
	 * @returns True if the path exists, false otherwise.
	 */
	public static exists(path: string): boolean {
		return FilesFacade.exists(path);
	}

	/**
	 * Checks if the file or directory at the given path is executable.
	 * @param path The path to check.
	 * @returns True if executable, false otherwise.
	 */
	public static isExecutable(path: string): boolean {
		return FilesFacade.isExecutable(path);
	}

	/**
	 * Checks if the file or directory at the given path is readable.
	 * @param path The path to check.
	 * @returns True if readable, false otherwise.
	 */
	public static isReadable(path: string): boolean {
		return FilesFacade.isReadable(path);
	}

	/**
	 * Checks if the file or directory at the given path is writable.
	 * @param path The path to check.
	 * @returns True if writable, false otherwise.
	 */
	public static isWritable(path: string): boolean {
		return FilesFacade.isWritable(path);
	}

	/**
	 * Checks if the file or directory at the given path is hidden.
	 * @param path The path to check.
	 * @returns True if hidden, false otherwise.
	 */
	public static isHidden(path: string): boolean {
		return FilesFacade.isHidden(path);
	}

	/**
	 * Checks if the path refers to a directory.
	 * @param path The path to check.
	 * @returns True if it's a directory, false otherwise.
	 */
	public static isDirectory(path: string): boolean {
		return FilesFacade.isDirectory(path);
	}

	/**
	 * Checks if the path refers to a regular file.
	 * @param path The path to check.
	 * @returns True if it's a file, false otherwise.
	 */
	public static isFile(path: string): boolean {
		return FilesFacade.isFile(path);
	}

	/**
	 * Checks if two paths refer to the same underlying file system object.
	 * @param path1 The first path.
	 * @param path2 The second path.
	 * @returns True if they reference the same file/directory, false otherwise.
	 */
	public static isSameFile(path1: string, path2: string): boolean {
		return FilesFacade.isSameFile(path1, path2);
	}

	/**
	 * Returns the canonical (absolute and normalized) path for the given path.
	 * @param path The path to normalize.
	 * @returns The canonical path string.
	 */
	public static getCanonicalPath(path: string): string {
		return FilesFacade.getCanonicalPath(path);
	}

	/**
	 * Gets the simple name of the file or directory at the given path (the last element).
	 * @param path The path.
	 * @returns The name.
	 */
	public static getName(path: string): string {
		return FilesFacade.getName(path);
	}

	/**
	 * Gets the path of the parent directory.
	 * @param path The path.
	 * @returns The parent path string, or null/empty if none exists.
	 */
	public static getParentPath(path: string): string {
		return FilesFacade.getParentPath(path);
	}

	/**
	 * Reads all bytes from a file into a JavaScript byte array (an array of numbers).
	 *
	 * Note: This method automatically converts the native Java byte array to a
	 * JavaScript array using `Bytes.toJavaScriptBytes()`.
	 * @param path The path to the file.
	 * @returns A JavaScript array of byte values.
	 */
	public static readBytes(path: string): any[] {
		const native = FilesFacade.readBytes(path);
		return Bytes.toJavaScriptBytes(native);
	}

	/**
	 * Reads all bytes from a file and returns the native Java byte array object.
	 * @param path The path to the file.
	 * @returns The native Java byte array.
	 */
	public static readBytesNative(path: string): any[] {
		return FilesFacade.readBytes(path);
	}

	/**
	 * Reads all text content from a file using the platform's default character encoding.
	 * @param path The path to the file.
	 * @returns The content of the file as a string.
	 */
	public static readText(path: string): string {
		return FilesFacade.readText(path);
	}

	/**
	 * Writes the content of a JavaScript byte array to a file. Overwrites existing content.
	 *
	 * Note: This method automatically converts the JavaScript array to a native
	 * Java byte array using `Bytes.toJavaBytes()` before writing.
	 * @param path The path to the file.
	 * @param data The JavaScript array of byte values to write.
	 */
	public static writeBytes(path: string, data: any[]): void {
		const native = Bytes.toJavaBytes(data);
		FilesFacade.writeBytesNative(path, native);
	}

	/**
	 * Writes the content of a native Java byte array to a file. Overwrites existing content.
	 * @param path The path to the file.
	 * @param data The native Java byte array to write.
	 */
	public static writeBytesNative(path: string, data: any[]): void {
		FilesFacade.writeBytesNative(path, data);
	}

	/**
	 * Writes a string of text to a file using the platform's default character encoding. Overwrites existing content.
	 * @param path The path to the file.
	 * @param text The string content to write.
	 */
	public static writeText(path: string, text: string): void {
		FilesFacade.writeText(path, text);
	}

	/**
	 * Gets the last modified time of the file or directory.
	 * @param path The path to the file or directory.
	 * @returns A JavaScript Date object representing the last modified time.
	 */
	public static getLastModified(path: string): Date {
		return new Date(FilesFacade.getLastModified(path));
	}

	/**
	 * Sets the last modified time of the file or directory.
	 * @param path The path to the file or directory.
	 * @param time The new Date object to set as the last modified time.
	 */
	public static setLastModified(path: string, time: Date): void {
		FilesFacade.setLastModified(path, time.getTime());
	}

	/**
	 * Gets the owner of the file or directory.
	 * @param path The path to the file or directory.
	 * @returns The owner name as a string.
	 */
	public static getOwner(path: string): string {
		return FilesFacade.getOwner(path);
	}

	/**
	 * Sets the owner of the file or directory.
	 * @param path The path to the file or directory.
	 * @param owner The new owner name.
	 */
	public static setOwner(path: string, owner: string): void {
		FilesFacade.setOwner(path, owner);
	}

	/**
	 * Gets the permissions string for the file or directory (implementation dependent).
	 * @param path The path to the file or directory.
	 * @returns The permissions string.
	 */
	public static getPermissions(path: string): string {
		return FilesFacade.getPermissions(path);
	}

	/**
	 * Sets the permissions for the file or directory (implementation dependent).
	 * @param path The path to the file or directory.
	 * @param permissions The permissions string.
	 */
	public static setPermissions(path: string, permissions: string): void {
		FilesFacade.setPermissions(path, permissions);
	}

	/**
	 * Gets the size of the file in bytes.
	 * @param path The path to the file.
	 * @returns The size in bytes.
	 */
	public static size(path: string): number {
		return FilesFacade.size(path);
	}

	/**
	 * Creates a new, empty file at the specified path.
	 * @param path The path where the file should be created.
	 */
	public static createFile(path: string): void {
		FilesFacade.createFile(path);
	}

	/**
	 * Creates a new directory at the specified path.
	 * @param path The path where the directory should be created.
	 */
	public static createDirectory(path: string): void {
		FilesFacade.createDirectory(path);
	}

	/**
	 * Copies a file or directory from a source path to a target path.
	 * @param source The source path.
	 * @param target The target path.
	 */
	public static copy(source: string, target: string): void {
		FilesFacade.copy(source, target);
	}

	/**
	 * Moves or renames a file or directory.
	 * @param source The source path.
	 * @param target The target path.
	 */
	public static move(source: string, target: string): void {
		FilesFacade.move(source, target);
	}

	/**
	 * Deletes the file at the specified path.
	 * @param path The path to the file to delete.
	 */
	public static deleteFile(path: string): void {
		FilesFacade.deleteFile(path);
	}

	/**
	 * Deletes the directory at the specified path.
	 * @param path The path to the directory to delete.
	 * @param forced If true, recursively deletes the directory and its contents.
	 */
	public static deleteDirectory(path: string, forced?: boolean): void {
		FilesFacade.deleteDirectory(path, forced);
	}

	/**
	 * Creates a new temporary file with the given prefix and suffix.
	 * @param prefix The prefix string to be used in generating the file's name.
	 * @param suffix The suffix string to be used in generating the file's name.
	 * @returns The path of the created temporary file.
	 */
	public static createTempFile(prefix: string, suffix: string): string {
		return FilesFacade.createTempFile(prefix, suffix);
	}

	/**
	 * Creates a new temporary directory with the given prefix.
	 * @param prefix The prefix string to be used in generating the directory's name.
	 * @returns The path of the created temporary directory.
	 */
	public static createTempDirectory(prefix: string): string {
		return FilesFacade.createTempDirectory(prefix);
	}

	/**
	 * Creates and returns a new {@link InputStream} for reading data from the file.
	 * @param path The path to the file.
	 * @returns A new InputStream instance.
	 */
	public static createInputStream(path: string): InputStream {
		const native = FilesFacade.createInputStream(path);
		return new InputStream(native);
	}

	/**
	 * Creates and returns a new {@link OutputStream} for writing data to the file.
	 * @param path The path to the file.
	 * @returns A new OutputStream instance.
	 */
	public static createOutputStream(path: string): OutputStream {
		const native = FilesFacade.createOutputStream(path);
		return new OutputStream(native);
	}

	/**
	 * Traverses a directory and returns a structured {@link FolderObject} hierarchy.
	 * @param path The path to the folder to traverse.
	 * @returns The root FolderObject containing the file system tree structure.
	 */
	public static traverse(path: string): FolderObject[] {
		return JSON.parse(FilesFacade.traverse(path));
	}

	/**
	 * Lists the direct children (files and folders) of a directory, returning only their paths.
	 * @param path The path to the directory.
	 * @returns An array of string paths for the contents of the directory.
	 */
	public static list(path: string): string[] {
		// The native facade returns a JSON array of objects, which is then mapped to an array of paths.
		return JSON.parse(FilesFacade.list(path)).map(e => e.path);
	}

	/**
	 * Finds files and directories matching a specified glob pattern within a directory tree.
	 * @param path The starting path for the search.
	 * @param pattern The glob pattern to match (e.g., "*.js", "**.txt").
	 * @returns An array of string paths that match the pattern.
	 */
	public static find(path: string, pattern: string): string[] {
		return JSON.parse(FilesFacade.find(path, pattern));
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Files;
}