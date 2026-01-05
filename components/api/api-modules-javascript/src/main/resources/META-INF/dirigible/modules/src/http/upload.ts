/**
 * Provides a static façade (`Upload` class) for checking and parsing
 * multipart/form-data HTTP requests, typically used for file uploads.
 */
import { InputStream } from "@aerokit/sdk/io/streams"
import { Bytes } from "@aerokit/sdk/io/bytes"

const HttpUploadFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpUploadFacade");

/**
 * The static Upload class provides methods to determine if a request contains
 * multipart content and to parse that content into file items.
 */
export class Upload {

    /**
     * Checks if the current incoming HTTP request contains multipart content
     * (e.g., from an HTML form with `enctype="multipart/form-data"`).
     * @returns True if the request is multipart, false otherwise.
     */
    public static isMultipartContent(): boolean {
        return HttpUploadFacade.isMultipartContent();
    }

    /**
     * Parses the incoming multipart request content into a collection of file items.
     * This operation typically consumes the request body.
     * @returns A FileItems object representing all parts (files and form fields) of the request.
     */
    public static parseRequest(): FileItems {
        // The underlying native execution context (__context) is assumed to contain
        // the parsed file items under the "files" key after the native facade processing.
        // @ts-ignore: __context is assumed to be globally available in the runtime environment
        return new FileItems(__context.get("files"));
    }
}

/**
 * Represents a collection of uploaded file and form field items parsed from a multipart request.
 */
export class FileItems {

    private readonly native: any;

    /**
     * @param native The native Java collection object holding the file items.
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * Retrieves a specific item (file or form field) by its index in the collection.
     * @param index The zero-based index of the item.
     * @returns A FileItem object representing the item at the specified index.
     */
    public get(index: number): FileItem {
        const native = this.native.get(index);
        return new FileItem(native);
    }

    /**
     * Returns the total number of items (files and form fields) in the collection.
     * @returns The size of the collection.
     */
    public size(): number {
        return this.native.size();
    }
}

/**
 * Represents a single item (either an uploaded file or a regular form field)
 * within a multipart request.
 */
export class FileItem {

    private readonly native: any

    /**
     * @param native The native Java object representing the file item.
     */
    constructor(native: any) {
        this.native = native;
    }

    /**
     * For a file upload, returns the original filename as reported by the client.
     * For a regular form field, this is typically null or undefined.
     * @returns The original filename string.
     */
    public getName(): string {
        return this.native.getOriginalFilename();
    }

    /**
     * Returns the MIME type of the uploaded file or content part.
     * @returns The content type string (e.g., 'image/png', 'text/plain').
     */
    public getContentType(): string {
        return this.native.getContentType();
    }

    /**
     * Checks if the uploaded item is empty (e.g., a file upload with zero bytes).
     * @returns True if the item is empty, false otherwise.
     */
    public isEmpty(): boolean {
        return this.native.isEmpty();
    }

    /**
     * Returns the size of the uploaded item in bytes.
     * @returns The size as a number.
     */
    public getSize(): number {
        return this.native.getSize();
    }

    /**
     * Retrieves the content of the file item as a JavaScript array of bytes.
     * This uses a utility (`Bytes.toJavaScriptBytes`) to convert the native Java byte array.
     * @returns An array of bytes (`any[]`).
     */
    public getBytes(): any[] {
        const data = this.getBytesNative();
        return Bytes.toJavaScriptBytes(data);
    }

    /**
     * Retrieves the content of the file item as the native Java byte array.
     * @returns The native byte array (`any[]`).
     */
    public getBytesNative(): any[] {
        return this.native.getBytes();
    }

    /**
     * Retrieves the content of the file item as a string.
     * Note: This assumes the content is text and may not handle all encodings correctly.
     * It relies on JavaScript's `String.fromCharCode.apply` for conversion.
     * @returns The content as a string.
     */
    public getText(): string {
        return String.fromCharCode.apply(null, this.getBytesNative());
    }

    /**
     * Gets an input stream for reading the content of the file item.
     * This is useful for handling large files without loading the entire content into memory.
     * @returns An InputStream object wrapping the native input stream.
     */
    public getInputStream(): InputStream {
        const native = this.native.getInputStream();
        return new InputStream(native);
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Upload;
}
