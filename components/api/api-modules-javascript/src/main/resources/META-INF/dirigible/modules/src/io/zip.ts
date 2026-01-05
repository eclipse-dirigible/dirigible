/**
 * Provides a façade for handling ZIP archive operations, including
 * file compression, decompression, and stream-based entry processing.
 */
import { Bytes } from "@aerokit/sdk/io/bytes";
import { InputStream, OutputStream } from "@aerokit/sdk/io/streams";

const ZipFacade = Java.type("org.eclipse.dirigible.components.api.io.ZipFacade");

/**
 * The Zip class provides static utility methods for managing ZIP archives
 * at both file path level and stream level.
 */
export class Zip {

    /**
     * Zips the content of a source directory or file into a target ZIP file.
     *
     * @param sourcePath The file system path to the content to be compressed.
     * @param zipTargetPath The file system path where the resulting ZIP file should be saved.
     */
    public static zip(sourcePath: string, zipTargetPath: string): void {
        ZipFacade.exportZip(sourcePath, zipTargetPath);
    }

    /**
     * Unzips an existing ZIP file into a target directory.
     *
     * @param zipPath The file system path to the ZIP file to be extracted.
     * @param targetPath The file system path to the directory where content should be extracted.
     */
    public static unzip(zipPath: string, targetPath: string): void {
        ZipFacade.importZip(zipPath, targetPath);
    }

    /**
     * Creates a {@link ZipInputStream} that reads ZIP archive data from a provided
     * generic {@link InputStream}. This allows for reading ZIP entries without
     * writing the archive to disk first.
     *
     * @param inputStream The source stream containing the raw ZIP data.
     * @returns A new {@link ZipInputStream} instance.
     */
    public static createZipInputStream(inputStream: InputStream): ZipInputStream {
        const native = ZipFacade.createZipInputStream(inputStream.native);
        return new ZipInputStream(native);
    }

    /**
     * Creates a {@link ZipOutputStream} that writes compressed ZIP archive data
     * to a provided generic {@link OutputStream}. This allows for creating ZIP archives
     * in memory or streaming them directly.
     *
     * @param outputStream The destination stream where the raw ZIP data will be written.
     * @returns A new {@link ZipOutputStream} instance.
     */
    public static createZipOutputStream(outputStream: OutputStream): ZipOutputStream {
        const native = ZipFacade.createZipOutputStream(outputStream.native);
        return new ZipOutputStream(native);
    }
}

/**
 * Represents an input stream for reading data from a ZIP archive.
 * Data is accessed sequentially by iterating through {@link ZipEntry} objects.
 */
export class ZipInputStream {

    private readonly native: any;

    /**
     * @param native The underlying native ZipInputStream object.
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * Reads the next ZIP file entry and positions the stream at the beginning of the entry data.
     * Must be called before reading data for an entry.
     *
     * @returns The next {@link ZipEntry} object, or null if there are no more entries.
     */
    public getNextEntry(): ZipEntry {
        const native = this.native.getNextEntry();
        // If native is null (end of stream), the ZipEntry constructor will handle the null check or throw/return invalid.
        return new ZipEntry(native);
    }

    /**
     * Reads the data for the current entry and returns it as a JavaScript byte array.
     *
     * @returns A JavaScript array (`number[]`) of the byte values for the current entry.
     */
    public read(): any[] {
        const native = ZipFacade.readNative(this.native);
        return Bytes.toJavaScriptBytes(native);
    }

    /**
     * Reads the data for the current entry and returns the native Java byte array.
     *
     * @returns The native Java byte array object.
     */
    public readNative(): any[] {
        return ZipFacade.readNative(this.native);
    }

    /**
     * Reads the data for the current entry and converts it to a string
     * using the platform's default character encoding.
     *
     * @returns The content of the current entry as a string.
     */
    public readText(): string {
        return ZipFacade.readText(this.native);
    }

    /**
     * Closes the underlying native ZipInputStream.
     */
    public close(): void {
        this.native.close();
    }

}

/**
 * Represents an output stream for writing data to a ZIP archive.
 * Entries must be explicitly created and closed.
 */
export class ZipOutputStream {

    private readonly native: any;

    /**
     * @param native The underlying native ZipOutputStream object.
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * Creates a new {@link ZipEntry} with the given name, and begins writing the
     * entry's header to the archive stream. All subsequent write operations
     * will apply to this entry until {@link closeEntry} is called.
     *
     * @param name The file or directory name to use inside the ZIP archive.
     * @returns The newly created {@link ZipEntry} object.
     */
    public createZipEntry(name: string): ZipEntry {
        const nativeNext = ZipFacade.createZipEntry(name);
        const zipEntry = new ZipEntry(nativeNext);
        this.native.putNextEntry(nativeNext);
        return zipEntry;
    }

    /**
     * Writes the data from a JavaScript byte array to the current active entry in the stream.
     *
     * @param data The JavaScript array (`number[]`) of byte values to write.
     */
    public write(data: any[]): void {
        const native = Bytes.toJavaBytes(data);
        ZipFacade.writeNative(this.native, native);
    }

    /**
     * Writes the data from a native Java byte array to the current active entry in the stream.
     *
     * @param data The native Java byte array object to write.
     */
    public writeNative(data: any[]): void {
        ZipFacade.writeNative(this.native, data);
    }

    /**
     * Converts the string to bytes and writes it to the current active entry in the stream.
     *
     * @param text The string content to write.
     */
    public writeText(text: string): void {
        ZipFacade.writeText(this.native, text);
    }

    /**
     * Closes the current active ZIP entry and positions the stream for the next entry.
     */
    public closeEntry(): void {
        this.native.closeEntry();
    }

    /**
     * Finalizes the writing of the ZIP file, flushes the stream, and closes the native object.
     * This must be called after all entries have been written.
     */
    public close(): void {
        this.native.finish();
        this.native.flush();
        this.native.close();
    }
}

/**
 * Represents an entry (file or directory) within a ZIP archive.
 * It holds metadata about the archived item.
 */
export class ZipEntry {

    private readonly native: any;

    /**
     * @param native The underlying native ZipEntry object.
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * Gets the name of the entry (path relative to the ZIP root).
     * @returns The name of the entry.
     */
    public getName(): string {
        return this.native.getName();
    }

    /**
     * Gets the uncompressed size of the entry data.
     * @returns The size in bytes.
     */
    public getSize(): number {
        return this.native.getSize();
    }

    /**
     * Gets the compressed size of the entry data.
     * @returns The compressed size in bytes.
     */
    public getCompressedSize(): number {
        return this.native.getCompressedSize();
    }

    /**
     * Gets the modification time of the entry.
     * @returns The time as a numerical timestamp.
     */
    public getTime(): number {
        return this.native.getTime();
    }

    /**
     * Gets the CRC-32 checksum of the uncompressed entry data.
     * @returns The CRC value.
     */
    public getCrc(): number {
        return this.native.getCrc();
    }

    /**
     * Gets the optional comment for the entry.
     * @returns The comment string.
     */
    public getComment(): string {
        return this.native.getComment();
    }

    /**
     * Checks if the entry represents a directory.
     * @returns True if it is a directory, false otherwise.
     */
    public isDirectory(): boolean {
        return this.native.isDirectory();
    }

    /**
     * Checks if the underlying native ZipEntry object is defined and non-null.
     * @returns True if the entry is valid, false otherwise.
     */
    public isValid(): boolean {
        return this.native !== undefined && this.native !== null;
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Zip;
}
